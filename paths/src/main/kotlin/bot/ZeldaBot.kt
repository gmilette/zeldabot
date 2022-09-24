package bot

import bot.plan.NavUtil
import bot.state.*
import bot.plan.ActionPlan
import bot.plan.PlanRunner
import nintaco.api.API
import nintaco.api.ApiSource
import nintaco.api.Colors
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

    private val hyrule: Hyrule = Hyrule()

    var frameStateUpdater: FrameStateUpdater = FrameStateUpdater(api, hyrule)

    val plan = PlanRunner(PlanBuilder.makeMasterPlan(hyrule.mapCellsObject))

    private fun getAction(currentFrame: Int, currentGamePad: GamePad): GamePad {
        frameStateUpdater.updateFrame(currentFrame, currentGamePad)

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

        // always do
        val nextGamePad = if (frameStateUpdater.state.previousMove.skipped) {
            // this actually helped
            d { " !!skipped keep going "}
            NavUtil.randomDir()
        } else {
            plan.next(frameStateUpdater.state)
        }

        frameStateUpdater.state.previousGamePad = nextGamePad

        d { "current --> ${frameStateUpdater.state.frameState.mapLoc}" +
                " target ${plan.target()} " + "link " +
                "${frameStateUpdater.state.frameState.link.point} +" +
                " prev ${frameStateUpdater.state.previousMove}"}
        drawIt(plan.target(), frameStateUpdater.state.frameState
            .mapLoc.toString() + " ${frameStateUpdater.state.frameState.link
            .point}")
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
//        if (spriteX + SPRITE_SIZE == 255) {
//            spriteVx = -1
//        } else if (spriteX == 0) {
//            spriteVx = 1
//        }
//        if (spriteY + SPRITE_SIZE == 231) {
//            spriteVy = -1
//        } else if (spriteY == 8) {
//            spriteVy = 1
//        }
//        spriteX += spriteVx
//        spriteY += spriteVy
        api.color = Colors.DARK_BLUE
        api.fillRect(strX - 1, strY - 1, strWidth + 2, 9)
//        api.setColor(Colors.BLUE)
//        api.drawRect(strX - 2, strY - 2, strWidth + 3, 10)
        api.color = Colors.WHITE
        api.drawString("$text", strX, strY, false)
    }

    private fun renderFinished() {
        val currentFrame = api.frameCount
        d { "execute ### $currentFrame"}
        // just make link invincible 2 hearts
        api.writeCPU(0x066F, 21)

        val act = true
        if (act) {
            //  how many times does it press the key? //maybe it should
            //  release after press once?
            d { "press $currentGamePad" }
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
                else -> {}
            }
        }

        if (untilFrame <= currentFrame) {
            currentGamePad = getAction(currentFrame, currentGamePad)
            // crap sometimes link moves 2
//            currentGamePad = GamePad.MoveUp
//            d { "execute $currentGamePad" }
            untilFrame = currentFrame + pressTime
        }
    }

    companion object {
        private const val STRING = "####"
        private const val SPRITE_ID = 123
        private const val SPRITE_SIZE = 2

        @JvmStatic
        fun main(args: Array<String>) {
            ApiSource.initRemoteAPI("localhost", 9999)
            ZeldaBot().launch()
        }
    }
}