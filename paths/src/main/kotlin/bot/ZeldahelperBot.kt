package bot

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

class ZeldahelperBot(private val monitor: ZeldaMonitor) {
    private val api: API = ApiSource.getAPI()

    fun launch() {
        d { " master plan ${plan.masterPlan.toStringAll()}"}
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
    private val collectSkip = false

    private val hyrule: Hyrule = Hyrule()

    var frameStateUpdater: FrameStateUpdater = FrameStateUpdater(api, hyrule)

    val plan = PlanRunner(PlanBuilder.makeMasterPlan(hyrule.mapCellsObject, hyrule.levelMap))

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

    private fun refillBombsIfOut() {
        if (frameStateUpdater.state.frameState.inventory.numBombs == 0) {
            api.writeCPU(Addresses.numBombs, 8)
        }
        if (frameStateUpdater.state.frameState.inventory.numKeys == 0) {
            api.writeCPU(Addresses.numKeys, 8)
        }

    }

    private val skipDb = SkipPathDb()

    private val start = System.currentTimeMillis()

    private fun renderFinished() {
        val currentFrame = api.frameCount
        d { "execute ### $currentFrame"}
        val minutes = (System.currentTimeMillis() - start) / 1000 / 60
        val numInventoryItems = frameStateUpdater.state.frameState.inventory.items.size
        frameStateUpdater.updateFrame(currentFrame, currentGamePad)
        drawIt(plan.target(),
                "${frameStateUpdater.state.frameState.tenth}" +
                "$minutes : $numInventoryItems")
    }

    companion object {
        private const val STRING = "####"
        private const val SPRITE_ID = 123
        private const val SPRITE_SIZE = 2
        var doAct = true
        @JvmStatic
        fun startIt(monitor: ZeldaMonitor) {
            ApiSource.initRemoteAPI("localhost", 9999)
            ZeldahelperBot(monitor).launch()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            ApiSource.initRemoteAPI("localhost", 9999)
            ZeldahelperBot(NoOp()).launch()
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

