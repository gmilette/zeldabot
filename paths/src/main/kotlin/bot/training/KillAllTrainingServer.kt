package bot.training

import bot.DirectoryConstants
import bot.plan.action.cleared
import bot.state.EnemyState
import bot.state.FrameStateUpdater
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.Hyrule
import nintaco.api.ApiSource
import nintaco.api.GamepadButtons
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Standalone training server for RL KillAll policy.
 *
 * All api.readCPU() calls happen inside renderFinished() callback.
 * The socket handler thread only queues actions and waits for responses.
 *
 * Protocol (TCP port 5050):
 *   RESET: Python sends "RESET\n" (6 bytes) → server responds with 452 bytes (113 floats, big-endian)
 *   STEP:  Python sends 4-byte int (big-endian action) → server responds with 457 bytes
 *          (452 obs + 4 reward float + 1 done byte)
 */
object KillAllTrainingServer {
    private const val PORT = 5050
    private const val OBS_FLOATS = 113
    private const val OBS_BYTES = OBS_FLOATS * 4   // 452
    private const val STEP_RESP_BYTES = OBS_BYTES + 4 + 1 + 1  // 458: obs + reward + done + needsDelay
    private const val SAVE_FILE_KEY = "lev1_bat.save"

    private val actionQueue = LinkedBlockingQueue<Int>(1)
    private val responseQueue = LinkedBlockingQueue<StepResponse>(1)
    private val resetResponseQueue = LinkedBlockingQueue<FloatArray>(1)

    @Volatile private var resetPending = false
    @Volatile private var resetSawTransition = false  // true once game leaves normal state after loadState
    @Volatile private var expectedMapLoc = 114
    @Volatile private var episodeFrameCount = 0
    @Volatile private var frameCount = 0
    @Volatile private var totalSteps = 0
    @Volatile private var episodeCount = 0

    @Volatile private var prevState: MapLocationState? = null

    private lateinit var hyrule: Hyrule
    private lateinit var frameStateUpdater: FrameStateUpdater
    private lateinit var api: nintaco.api.API

    data class StepResponse(
        val obs: FloatArray,
        val reward: Float,
        val done: Boolean,
        val needsDelay: Boolean = false
    )

    @JvmStatic
    fun main(args: Array<String>) {
        println("KillAllTrainingServer: constructing Hyrule map data...")
        hyrule = Hyrule()

        println("KillAllTrainingServer: connecting to Nintaco on port 9999...")
        ApiSource.initRemoteAPI("localhost", 9999)
        api = ApiSource.getAPI()

        frameStateUpdater = FrameStateUpdater(api, hyrule)

        api.addFrameListener { renderFinished() }
        api.addActivateListener {
            println("KillAllTrainingServer: Nintaco API activated — setting speed to 400%")
            api.setSpeed(400)
        }
        api.addDeactivateListener { println("KillAllTrainingServer: Nintaco API deactivated") }

        Thread({ runSocketServer() }, "training-socket-server").apply {
            isDaemon = true
            start()
        }

        println("KillAllTrainingServer: calling api.run() — Nintaco event loop starting")
        api.run()
    }

    // ─── Nintaco callback (all game state reads happen here) ─────────────────

    private fun renderFinished() {
        frameCount++

        if (resetPending) {
            frameStateUpdater.updateFrame(frameCount, GamePad.None, null)
            val state = frameStateUpdater.state
            val fs = state.frameState
            val inNormalPlay = fs.gameMode == 5 && fs.mapLoc == expectedMapLoc
            // First wait for game to leave normal state (confirms load actually fired)
            if (!resetSawTransition && !inNormalPlay) {
                resetSawTransition = true
                println("  [reset] frame=$frameCount saw transition gameMode=${fs.gameMode} mapLoc=${fs.mapLoc}")
            }
            // Log every 60 frames so we can see progress without flooding
            if (frameCount % 60 == 0) {
                println("  [reset] frame=$frameCount sawTransition=$resetSawTransition gameMode=${fs.gameMode} mapLoc=${fs.mapLoc} (want gameMode=5 mapLoc=$expectedMapLoc)")
            }
            // gameMode=8 is the death screen; gameMode=0 is the title/startup screen.
            // Both require Start to be toggled (not held) to dismiss — alternate press/release every 4 frames.
            // Also retry loadState every 60 frames to re-anchor the state.
            if (fs.gameMode == 8 || fs.gameMode == 0) {
                val label = if (fs.gameMode == 8) "death" else "title"
                val pressing = frameCount % 4 < 2
                if (pressing) {
                    println("  [reset/$label] frame=$frameCount gameMode=${fs.gameMode} — pressing Start")
                    api.writeGamepad(0, GamepadButtons.Start, true)
                } else {
                    println("  [reset/$label] frame=$frameCount gameMode=${fs.gameMode} — releasing Start")
                    api.writeGamepad(0, GamepadButtons.Start, false)
                }
                if (frameCount % 60 == 0) {
                    val saveFile = "${DirectoryConstants.states}mapstate/$SAVE_FILE_KEY"
                    println("  [reset/$label] frame=$frameCount retrying loadState($saveFile)")
                    api.loadState(saveFile)
                }
                return
            }
            // Retry loadState every 60 frames if stuck in any other non-normal state
            if (!inNormalPlay && frameCount % 60 == 0) {
                val saveFile = "${DirectoryConstants.states}mapstate/$SAVE_FILE_KEY"
                println("  [reset/retry] frame=$frameCount gameMode=${fs.gameMode} mapLoc=${fs.mapLoc} — retrying loadState")
                api.loadState(saveFile)
            }

            if (resetSawTransition && inNormalPlay) {
                resetPending = false
                resetSawTransition = false
                prevState = state
                episodeFrameCount = 0
                episodeCount++
                println("[RESET done] episode=$episodeCount totalSteps=$totalSteps gameMode=${fs.gameMode} mapLoc=${fs.mapLoc} enemies=${fs.enemies.size} link=${fs.link.point}")
                resetResponseQueue.offer(KillAllStateEncoder.encode(state, 0))
                println("[RESET done] obs placed in queue")
            }
            releaseAllButtons()
            return
        }

        // Skip scrolling frames — game ignores input during transitions
        if (frameStateUpdater.state.frameState.isScrolling) {
            releaseAllButtons()
            return
        }

        val action = actionQueue.poll()
        if (action == null) {
            releaseAllButtons()
            return
        }

        releaseAllButtons()
        applyGamepad(action)

        val gamePad = ACTION_MAP.getOrElse(action) { GamePad.None }
        frameStateUpdater.updateFrame(frameCount, gamePad, null)
        val state = frameStateUpdater.state
        episodeFrameCount++

        val prev = prevState ?: state  // safe fallback on first step
        val fs = state.frameState
        val invalidState = fs.gameMode == -1 || fs.mapLoc == -1
        val leftRoom = !invalidState && fs.mapLoc != expectedMapLoc
        val reward = if (leftRoom || invalidState) -10.0f else computeReward(prev, state, episodeFrameCount)
        val isDead = fs.isDead
        val timeout = episodeFrameCount > 1500
        val screenCleared = state.cleared
        val done = isDead || timeout || screenCleared || leftRoom || invalidState
        totalSteps++

        if (leftRoom || invalidState) {
            val reason = if (invalidState) "invalidState(gameMode=${fs.gameMode} mapLoc=${fs.mapLoc})"
                         else "leftRoom(mapLoc=${fs.mapLoc} != $expectedMapLoc)"
            println("[$reason] — reloading now")
            triggerReload()
        }

        // Log every 50 steps, plus every done
        if (done || totalSteps % 50 == 0) {
            val reason = when {
                isDead -> "DEAD"
                screenCleared -> "CLEARED"
                timeout -> "TIMEOUT"
                leftRoom -> "LEFT_ROOM(${fs.mapLoc})"
                invalidState -> "INVALID(gameMode=${fs.gameMode} mapLoc=${fs.mapLoc})"
                else -> ""
            }
            println("[step] ep=$episodeCount epFrame=$episodeFrameCount total=$totalSteps " +
                "action=${ACTION_MAP.getOrElse(action){GamePad.None}} " +
                "reward=${"%+.2f".format(reward)} enemies=${fs.enemies.count { it.state == EnemyState.Alive }} " +
                "link=${fs.link.point} hearts=${"%.1f".format(fs.life)} done=$done $reason")
        }

        if (done) {
            frameStateUpdater.reset()
        }

        prevState = state
        val obs = KillAllStateEncoder.encode(state, episodeFrameCount)
        val needsDelay = isDead || leftRoom || invalidState  // timeout and cleared reset instantly
        responseQueue.offer(StepResponse(obs, reward, done, needsDelay))
    }

    // ─── Reward function ─────────────────────────────────────────────────────

    private fun computeReward(prev: MapLocationState, curr: MapLocationState, epFrameCount: Int): Float {
        var reward = 0.01f  // survival reward per frame

        val prevAlive = prev.frameState.enemies.count { it.state == EnemyState.Alive }
        val currAlive = curr.frameState.enemies.count { it.state == EnemyState.Alive }
        val killed = prevAlive - currAlive
        if (killed > 0) {
            reward += killed * 5.0f
        }

        if (curr.cleared) {
            reward += 20.0f
        }

        val prevHeart = prev.frameState.life
        val currHeart = curr.frameState.life
        if (prevHeart > 0 && currHeart < prevHeart) {
            val halfHeartsLost = ((prevHeart - currHeart) * 2).toInt().coerceAtLeast(1)
            reward -= halfHeartsLost * 3.0f
        }

        if (curr.frameState.isDead) {
            reward -= 15.0f
        } else if (epFrameCount > 1500) {
            reward -= 10.0f
        }

        return reward
    }

    // ─── Episode reset ───────────────────────────────────────────────────────

    /** Called from renderFinished() to eagerly reload without waiting for Python. */
    private fun triggerReload() {
        val saveFile = "${DirectoryConstants.states}mapstate/$SAVE_FILE_KEY"
        println("[triggerReload] frame=$frameCount clearing queues, loading $saveFile")
        frameStateUpdater.reset()
        actionQueue.clear()
        responseQueue.clear()
        resetResponseQueue.clear()
        expectedMapLoc = 114
        resetSawTransition = false
        resetPending = true
        api.loadState(saveFile)
        println("[triggerReload] loadState called, resetPending=true")
    }

    /** Called from socket thread when Python sends RESET.
     *  [out] is the client output stream — keepalive 0x01 bytes are sent every 2s while waiting. */
    private fun handleReset(out: DataOutputStream): FloatArray {
        println("[handleReset] called — resetPending=$resetPending actionQueueSize=${actionQueue.size} responseQueueSize=${responseQueue.size} resetResponseQueueSize=${resetResponseQueue.size}")
        actionQueue.clear()
        responseQueue.clear()
        if (!resetPending) {
            // Not already reloading (normal done reasons) — trigger it now
            resetResponseQueue.clear()
            resetSawTransition = false
            resetPending = true
            val saveFile = "${DirectoryConstants.states}mapstate/$SAVE_FILE_KEY"
            println("[handleReset] loading $saveFile")
            api.loadState(saveFile)
        } else {
            // Already reloading (leftRoom/invalidState triggered it eagerly).
            // Do NOT clear resetResponseQueue — the game may have already settled
            // and placed obs there. Clearing it would cause a deadlock.
            println("[handleReset] reload already in progress, resetResponseQueueSize=${resetResponseQueue.size}, waiting for settle...")
        }
        println("[handleReset] polling resetResponseQueue (keepalive every 2s)...")
        while (true) {
            val obs = resetResponseQueue.poll(2, TimeUnit.SECONDS)
            if (obs != null) {
                println("[handleReset] obs received (${obs.size} floats), returning to Python")
                return obs
            }
            println("[handleReset] still waiting for game to settle — sending keepalive ACK")
            out.write(byteArrayOf(0x01))
            out.flush()
        }
    }

    // ─── Button helpers ──────────────────────────────────────────────────────

    private fun releaseAllButtons() {
        listOf(
            GamepadButtons.A, GamepadButtons.B,
            GamepadButtons.Right, GamepadButtons.Left,
            GamepadButtons.Up, GamepadButtons.Down
        ).forEach { api.writeGamepad(0, it, false) }
    }

    private val ACTION_MAP = arrayOf(
        GamePad.None,       // 0 NOOP
        GamePad.MoveRight,  // 1
        GamePad.MoveLeft,   // 2
        GamePad.MoveDown,   // 3
        GamePad.MoveUp,     // 4
        GamePad.A,          // 5
        GamePad.B           // 6
    )

    private fun applyGamepad(actionIdx: Int) {
        when (ACTION_MAP.getOrElse(actionIdx) { GamePad.None }) {
            GamePad.MoveRight -> api.writeGamepad(0, GamepadButtons.Right, true)
            GamePad.MoveLeft  -> api.writeGamepad(0, GamepadButtons.Left, true)
            GamePad.MoveUp    -> api.writeGamepad(0, GamepadButtons.Up, true)
            GamePad.MoveDown  -> api.writeGamepad(0, GamepadButtons.Down, true)
            GamePad.A         -> api.writeGamepad(0, GamepadButtons.A, true)
            GamePad.B         -> api.writeGamepad(0, GamepadButtons.B, true)
            else -> {}
        }
    }

    // ─── TCP socket server ───────────────────────────────────────────────────

    private fun runSocketServer() {
        println("KillAllTrainingServer: listening on TCP port $PORT")
        ServerSocket(PORT).use { server ->
            while (true) {
                val client = server.accept()
                println("KillAllTrainingServer: client connected from ${client.inetAddress}")
                Thread({ handleClient(client) }, "training-client").apply {
                    isDaemon = true
                    start()
                }
            }
        }
    }

    private fun handleClient(client: java.net.Socket) {
        try {
            val inp = DataInputStream(client.getInputStream().buffered())
            val out = DataOutputStream(client.getOutputStream().buffered())

            while (true) {
                // Peek at first byte to determine message type
                val first = inp.readByte()

                if (first == 'R'.code.toByte()) {
                    // "RESET\n" — read remaining 5 bytes
                    val rest = ByteArray(5)
                    inp.readFully(rest)
                    println("[socket] << RESET received — sending ACK immediately")
                    out.write(byteArrayOf(0x01))  // phase-1 ACK: "got it, loading..."
                    out.flush()
                    println("[socket] >> ACK sent, now blocking on handleReset()...")
                    val obs = handleReset(out)
                    out.write(floatsToBytes(obs))
                    out.flush()
                    println("[socket] >> RESET obs sent (${OBS_BYTES} bytes)")
                } else {
                    // 4-byte big-endian action int, first byte already read
                    val b2 = inp.readByte()
                    val b3 = inp.readByte()
                    val b4 = inp.readByte()
                    val action = ((first.toInt() and 0xFF) shl 24) or
                                 ((b2.toInt() and 0xFF) shl 16) or
                                 ((b3.toInt() and 0xFF) shl 8) or
                                 (b4.toInt() and 0xFF)

                    actionQueue.put(action)
                    val resp = responseQueue.take()

                    val respBuf = ByteBuffer.allocate(STEP_RESP_BYTES).order(ByteOrder.BIG_ENDIAN)
                    for (f in resp.obs) respBuf.putFloat(f)
                    respBuf.putFloat(resp.reward)
                    respBuf.put(if (resp.done) 1.toByte() else 0.toByte())
                    respBuf.put(if (resp.needsDelay) 1.toByte() else 0.toByte())
                    out.write(respBuf.array())
                    out.flush()
//                    println("[socket] >> step response: reward=${"%.3f".format(resp.reward)} done=${resp.done} (${STEP_RESP_BYTES} bytes)")
                }
            }
        } catch (e: Exception) {
            println("KillAllTrainingServer: client disconnected: ${e.message}")
        } finally {
            client.close()
        }
    }

    private fun floatsToBytes(floats: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.BIG_ENDIAN)
        for (f in floats) buf.putFloat(f)
        return buf.array()
    }
}
