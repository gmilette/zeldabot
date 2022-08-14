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
        d { "Hello d" }
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
    var untilFrame = 0
    val pressTime = 1

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

    var releasePreviousAction: GamePad? = null

    private fun renderFinished() {
        val currentFrame = api.frameCount
//        d { "execute $currentFrame"}

//        if (releasePreviousAction != null) {
//            releasePreviousAction?.let {
//                d { "release $it" }
//                releaseButtons(it)
//                releasePreviousAction = null
//            }
//        } else {
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
//        }

        if (untilFrame <= currentFrame) {
//            currentGamePad = when (currentGamePad) {
//                GamePad.MoveRight -> GamePad.MoveUp
//                GamePad.MoveLeft -> GamePad.MoveDown
//                GamePad.MoveUp -> GamePad.MoveLeft
//                GamePad.MoveDown -> GamePad.MoveRight
//                else -> GamePad.None
//            }
            val previousAction = currentGamePad
            currentGamePad = getAction(currentFrame, currentGamePad)
            if (previousAction == currentGamePad) {
                releasePreviousAction = GamePad.Select
            }
            d { "execute $currentGamePad prev $previousAction" }
            untilFrame = currentFrame + pressTime
        }
    }

    fun releaseButtons(pad: GamePad) {
        when (pad) {
            GamePad.MoveRight -> api.writeGamepad(0, GamepadButtons.Right, true)
            GamePad.MoveLeft -> api.writeGamepad(0, GamepadButtons.Left, true)
            GamePad.MoveUp -> api.writeGamepad(0, GamepadButtons.Up, true)
            GamePad.MoveDown -> api.writeGamepad(0, GamepadButtons.Down, true)
            GamePad.A -> api.writeGamepad(0, GamepadButtons.A, true)
            GamePad.B -> api.writeGamepad(0, GamepadButtons.B, true)
            GamePad.Select -> api.writeGamepad(0, GamepadButtons.Select, true)
            GamePad.Start -> api.writeGamepad(0, GamepadButtons.Start, true)
            else -> {}
        }
//        when (pad) {
//            GamePad.MoveRight -> api.writeGamepad(
//                0,
//                GamepadButtons.Right,
//                false
//            )
//            GamePad.MoveLeft -> api.writeGamepad(0, GamepadButtons.Left, false)
//            GamePad.MoveUp -> api.writeGamepad(0, GamepadButtons.Up, false)
//            GamePad.MoveDown -> api.writeGamepad(0, GamepadButtons.Down, false)
//            GamePad.A -> api.writeGamepad(0, GamepadButtons.A, false)
//            GamePad.B -> api.writeGamepad(0, GamepadButtons.B, false)
//            GamePad.Select -> api.writeGamepad(0, GamepadButtons.Select, false)
//            GamePad.Start -> api.writeGamepad(0, GamepadButtons.Start, false)
//            else -> {}
//        }
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