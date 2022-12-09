package bot

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bot.state.*
import bot.plan.PlanRunner
import bot.plan.gastar.SkipPathDb
import bot.state.map.Hyrule
import bot.state.map.MapCell
import bot.state.map.MapConstants
import nintaco.api.API
import nintaco.api.ApiSource
import nintaco.api.Colors
import nintaco.api.GamepadButtons
import sequence.ZeldaItem
import util.d
import util.i

class ZeldaBot(private val monitor: ZeldaMonitor) {
    private val api: API = ApiSource.getAPI()

    fun launch() {
        d { " master plan ${plan.masterPlan.toStringAll()}"}
        api.addFrameListener { renderFinished() }
        api.addStatusListener { message: String -> statusChanged(message) }
        api.addActivateListener { apiEnabled() }
        api.addDeactivateListener { apiDisabled() }
        api.addStopListener { dispose() }
        api.run()

//        api.isPaused = true
//        api.stepToNextFrame()
    }

    private fun apiEnabled() {
        d { "Let's begin apiEnabled" }
        val sprite = IntArray(SPRITE_SIZE * SPRITE_SIZE)
        for (y in 0 until SPRITE_SIZE) {
            val Y = y - SPRITE_SIZE / 2 + 0.5
            for (x in 0 until SPRITE_SIZE) {
                val X = x - SPRITE_SIZE / 2 + 0.5
                sprite[SPRITE_SIZE * y + x] = if (X * X + Y * Y
                    <= SPRITE_SIZE * SPRITE_SIZE / 4
                ) Colors.ORANGE else -1
            }
        }
        api.createSprite(
            SPRITE_ID,
            SPRITE_SIZE,
            SPRITE_SIZE, sprite)
        strWidth = api.getStringWidth(STRING, false)
        strX = 0 //(256 - strWidth) / 2
        strY = (240 - 8) - 40  /// 2 // interesting that's size
    }

    private fun apiDisabled() {
        println("API disabled")
    }

    private fun dispose() {
        println("API stopped")
    }

    private fun statusChanged(message: String) {
        System.out.format("Status message: %s%n", message)
    }

    var currentGamePad = GamePad.MoveRight;
    private var untilFrame = 0
    private val pressTime = 1
    private val collectSkip = false

    private val hyrule: Hyrule = Hyrule()

    var frameStateUpdater: FrameStateUpdater = FrameStateUpdater(api, hyrule)

    val plan = PlanRunner(PlanBuilder.makeMasterPlan(hyrule.mapCellsObject, hyrule.levelMap))

    var previousLink: FramePoint = FramePoint()
    val collect = SkipLocationCollector()

    private fun getAction(currentFrame: Int, currentGamePad: GamePad): GamePad {
        frameStateUpdater.updateFrame(currentFrame, currentGamePad)
        if (collectSkip) {
            val link = frameStateUpdater.getLink()
            collect.collect(link, previousLink)
            previousLink = link
            d { collect.toString()}
            return GamePad.None
        }
        // filters..

        if (frameStateUpdater.state.frameState.isDoneScrolling) {
            d {" * clear history"}
            frameStateUpdater.state.clearHistory()
            // then skip action on this frame
            return GamePad.None
        }
        if (frameStateUpdater.state.frameState.isScrolling) {
            return GamePad.None
        }

        refillBombsIfOut()

        frameStateUpdater.setSword(ZeldaItem.WhiteSword)

        // always do
        val nextGamePad = if (false && frameStateUpdater.state.previousMove.skipped) { // && Random.nextBoolean()) {
            api.addStopListener {  }
            // this actually helped
            d { " !!skipped keep going "}
            // but do not exit
            val dirs = mutableListOf<GamePad>()
            val link = frameStateUpdater.state.frameState.link.point
            if (link.x > 50) {
                dirs.add(GamePad.MoveLeft)
            }
            if (link.x < MapConstants.MAX_X - 50) {
                dirs.add(GamePad.MoveRight)
            }
            if (link.y > 20) {
                dirs.add(GamePad.MoveUp)
            }
            if (link.y < MapConstants.MAX_Y - 50) {
                dirs.add(GamePad.MoveDown)
            }
            dirs.shuffle()
            dirs.firstOrNull() ?: GamePad.MoveRight
        } else {
            plan.next(frameStateUpdater.state)
        }

        frameStateUpdater.state.previousGamePad = nextGamePad

        d { plan.toString() }
        with (frameStateUpdater.state) {
            val currentCell = currentMapCell
            val locCoordinates = "${frameState.level}: ${frameState.mapLoc} : ${currentCell.mapData.name}"
            d { "current --> " +
                    "$locCoordinates " +
                    " target ${plan.target()} " + "link " +
                    "${frameState.link.point} +" +
                    " prev ${previousMove}"}

            val alive = this.frameState.enemiesClosestToLink(EnemyState.Alive).size
            val dead = this.frameState.enemiesClosestToLink(EnemyState.Dead).size
//            val unknown = this.frameState.enemiesClosestToLink(EnemyState.Unknown).size
            val killedCt = this.frameState.killedEnemyCount
            val saw = this.numEnemiesSeen
            val aliveT = "A: ${alive} d: $dead k ${killedCt} s $saw"
//            val killedT = "K: ${this.frameState.killedEnemyCount}"

            drawIt(plan.target(), "$locCoordinates $link $aliveT")
        }
//        drawMap(frameStateUpdater.state.mapCell)

        return nextGamePad
    }


    private var spriteX = 0
    private var spriteY = 8
    private var spriteVx = 1
    private var spriteVy = 1
    private var strWidth = 0
    private var strX = 0
    private var strY = 200

    private fun drawMap(cell: MapCell) {
        for (x in 0..255) {
            for (y in 0..167) {
//                val pt = point.toScreenY
                if (cell.passable.get(x, y)) {
                    api.drawSprite(SPRITE_ID, x, y + 61)
                }
            }
        }
    }

    private fun drawIt(point: FramePoint, text: String) {
        val pt = point.toScreenY
        api.drawSprite(SPRITE_ID, pt.x, pt.y)
        api.color = Colors.DARK_BLUE
        api.fillRect(strX - 1, strY - 1, strWidth + 2, 9)
//        api.setColor(Colors.BLUE)
//        api.drawRect(strX - 2, strY - 2, strWidth + 3, 10)
        api.color = Colors.WHITE
        api.drawString("$text", strX, strY, false)
    }

    private fun refillBombsIfOut() {
        if (frameStateUpdater.state.frameState.inventory.numBombs <= 2) {
            api.writeCPU(Addresses.numBombs, 8)
        }
        if (frameStateUpdater.state.frameState.inventory.numKeys == 0) {
            api.writeCPU(Addresses.numKeys, 8)
        }

    }

    private val skipDb = SkipPathDb()

    private fun renderFinished() {
        val currentFrame = api.frameCount
        if (collectSkip) {
            currentGamePad = getAction(currentFrame, currentGamePad)
        }

        d { "execute ### $currentFrame"}
        monitor.update(frameStateUpdater.state, plan)

        if (frameStateUpdater.state.currentMapCell.mapLoc != 55) {
            // just make link invincible 2 hearts
            api.writeCPU(Addresses.heartContainers, 51)
        }
//        return
        currentGamePad = getAction(currentFrame, currentGamePad)
        if (doAct) {
            skipDb.save(
                frameStateUpdater.state.frameState.level,
                frameStateUpdater.state.currentMapCell.mapLoc,
                frameStateUpdater.state.previousMove
            )
        }
        d { " action: ${frameStateUpdater.state.frameState.link.point} -> $currentGamePad"}

        val act = doAct && !collectSkip //currentFrame % 3 == 0
        if (act) {
            //  how many times does it press the key? //maybe it should
            //  release after press once?
//            d { "press $currentGamePad" }
            // add a or b?
            // this didn't help
            val toRelease = mutableSetOf<GamePad>(GamePad.MoveRight, GamePad.MoveUp, GamePad.MoveLeft, GamePad.MoveDown)
            toRelease.forEach { api.writeGamepad(0, it.toGamepadButton, false) }
            when (currentGamePad) {
                GamePad.MoveRight -> api.writeGamepad(
                    0,
                    GamepadButtons.Right,
                    true
                )
                GamePad.MoveLeft -> api.writeGamepad(
                    0,
                    GamepadButtons.Left,
                    true
                )
                GamePad.MoveUp -> api.writeGamepad(0, GamepadButtons.Up, true)
                GamePad.MoveDown -> api.writeGamepad(
                    0,
                    GamepadButtons.Down,
                    true
                )
                GamePad.A -> api.writeGamepad(0, GamepadButtons.A, true)
                GamePad.B -> api.writeGamepad(0, GamepadButtons.B, true)
                GamePad.Select -> api.writeGamepad(
                    0,
                    GamepadButtons.Select,
                    true
                )
                GamePad.Start -> api.writeGamepad(0, GamepadButtons.Start, true)
                GamePad.ReleaseA -> api.writeGamepad(0, GamepadButtons.A, false)
                GamePad.ReleaseB -> api.writeGamepad(0, GamepadButtons.B, false)
                else -> {}
            }
//            if (untilFrame <= currentFrame) {
//                currentGamePad = getAction(currentFrame, currentGamePad)
//                d { " action: ${frameStateUpdater.state.frameState.link.point} -> $currentGamePad"}
//                untilFrame = currentFrame + pressTime
//            }
        } else {
            currentGamePad = getAction(currentFrame, currentGamePad)
//            val toRelease = mutableSetOf<GamePad>(GamePad.MoveRight, GamePad.MoveUp, GamePad.MoveLeft, GamePad.MoveDown)
//            toRelease.forEach { api.writeGamepad(0, it.toGamepadButton, false) }
//            api.writeGamepad(0, GamePad.MoveRight.toGamepadButton, false)
//            api.writeGamepad(0, GamePad.MoveLeft.toGamepadButton, false)
//            api.writeGamepad(0, GamePad.MoveUp.toGamepadButton, false)
//            api.writeGamepad(0, GamePad.MoveDown.toGamepadButton, false)
        }
    }

    companion object {
        private const val STRING = "####"
        private const val SPRITE_ID = 123
        private const val SPRITE_SIZE = 2
        var doAct = true
        @JvmStatic
        fun startIt(monitor: ZeldaMonitor) {
            ApiSource.initRemoteAPI("localhost", 9999)
            ZeldaBot(monitor).launch()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            ApiSource.initRemoteAPI("localhost", 9999)
            ZeldaBot(NoOp()).launch()
        }
    }

    interface ZeldaMonitor {
        fun update(state: MapLocationState, plan: PlanRunner)
    }

    class NoOp: ZeldaMonitor {
        override fun update(state: MapLocationState, plan: PlanRunner) {
            // todo
        }
    }
}

