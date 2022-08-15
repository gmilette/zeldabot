package bot

import bot.state.FrameStateUpdater
import nintaco.api.API
import nintaco.api.ApiSource
import nintaco.api.GamepadButtons
import util.d

class ZeldaBot {
    private val api: API = ApiSource.getAPI()

    fun launch() {
        api.addFrameListener { renderFinished() }
        api.addStatusListener { message: String -> statusChanged(message) }
        api.addActivateListener { apiEnabled() }
        api.addDeactivateListener { apiDisabled() }
        api.addStopListener { dispose() }
        api.run()
    }

    private fun apiEnabled() {
        d { "Let's begin apiEnabled" }
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

    enum class GamePad {
        None, MoveRight, MoveLeft, MoveDown, MoveUp,
        A, B, Select, Start, ReleaseA
    }

    var frameStateUpdater: FrameStateUpdater = FrameStateUpdater(api)
    var plan: Plan = Plan()

    private fun getAction(currentFrame: Int, currentGamePad: GamePad): GamePad {
        frameStateUpdater.updateFrame(currentFrame, currentGamePad)
        return plan.current().nextStep(frameStateUpdater.state)
    }

    private fun renderFinished() {
        val currentFrame = api.frameCount
//        d { "execute $currentFrame"}
        // just make link invincible 2 hearts
        api.writeCPU(0x066F, 21)

        d { "press $currentGamePad" }
        when (currentGamePad) {
            GamePad.MoveRight -> api.writeGamepad(0, GamepadButtons.Right, true)
            GamePad.MoveLeft -> api.writeGamepad(0, GamepadButtons.Left, true)
            GamePad.MoveUp -> api.writeGamepad(0, GamepadButtons.Up, true)
            GamePad.MoveDown -> api.writeGamepad(0, GamepadButtons.Down, true)
            GamePad.A -> api.writeGamepad(0, GamepadButtons.A, true)
            GamePad.B -> api.writeGamepad(0, GamepadButtons.B, true)
            GamePad.Select -> api.writeGamepad(0, GamepadButtons.Select, true)
            GamePad.Start -> api.writeGamepad(0, GamepadButtons.Start, true)
            GamePad.ReleaseA -> api.writeGamepad(0, GamepadButtons.A, false)
            else -> {}
        }

        if (untilFrame <= currentFrame) {
            currentGamePad = getAction(currentFrame, currentGamePad)
            d { "execute $currentGamePad" }
            untilFrame = currentFrame + pressTime
        }
    }

    companion object {
        private const val STRING = "Hello, World! KOTLIN yo"
        private const val SPRITE_ID = 123
        private const val SPRITE_SIZE = 32

        @JvmStatic
        fun main(args: Array<String>) {
            ApiSource.initRemoteAPI("localhost", 9999)
            ZeldaBot().launch()
        }
    }
}