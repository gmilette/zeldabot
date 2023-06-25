package bot

import bot.plan.PlanBuilder
import bot.plan.ZeldaPlan
import bot.plan.action.KillRhino
import bot.plan.gastar.SkipLocationCollector
import bot.plan.runner.PlanRunner
import bot.plan.gastar.SkipPathDb
import bot.plan.runner.MasterPlan
import bot.state.*
import bot.state.map.Hyrule
import bot.state.map.MapCell
import bot.state.map.MapConstants
import nintaco.api.API
import nintaco.api.ApiSource
import nintaco.api.Colors
import nintaco.api.GamepadButtons
import sequence.ZeldaItem
import util.RunOnceLambda
import util.d

class ZeldaBot(private val monitor: ZeldaMonitor) {
    private val api: API = ApiSource.getAPI()

    fun launch() {
        // what is the relative dir?
        val root = "../Nintaco_bin_2020-05-01/states/"
//        val root = "/Users/greg/dev/zelda/Nintaco_bin_2020-05-01/states/"
        d { " master plan ${plan.masterPlan.toStringAll()}" }
        val loadZelda by RunOnceLambda {
            d { " load zelda" }
//            api.quickLoadState(1)
            api.loadState("$root/${plan.startPath}")
        }
        val setSpeed by RunOnceLambda {
            d { " set speed" }
            api.setSpeed(400)
        }
        api.addFrameListener {
            renderFinished()
            loadZelda
        }
        api.addStatusListener { message: String -> statusChanged(message) }
        api.addActivateListener {
            apiEnabled()
            setSpeed
        }
        api.addDeactivateListener { apiDisabled() }
        api.addStopListener { dispose() }
        api.run()

//        d { " load zeldaaaa"}
//        api.open(zelda2)
//        val t = Thread( {
//            api.run()
//        })
//        t.start()
//        d { " load zeldaaaa2"}
//        api.open(zelda2)
        // run at 400% always for now
//        api.setSpeed(400)
//        api.isPaused = true
//        api.stepToNextFrame()
    }

    private fun loadSave() {
//        api.loadState()
    }

    private fun apiEnabled() {
        d { "apiEnabled" }
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
            SPRITE_SIZE, sprite
        )
        strWidth = api.getStringWidth(STRING, false)
        strX = 0 //(256 - strWidth) / 2
        strY = (240 - 8) - 40  /// 2 // interesting that's size

        api.createSprite(
            SPRITE_ID,
            SPRITE_SIZE,
            SPRITE_SIZE, sprite
        )

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

    fun makePlan(): MasterPlan {
        // make sure to reset any state here
        frameStateUpdater.reset()
        return ZeldaPlan.makeMasterPlan(hyrule, hyrule.mapCellsObject, hyrule.levelMap)
    }

    val plan = PlanRunner(::makePlan, api)

    var previousLink: FramePoint = FramePoint()
    val collect = SkipLocationCollector()

    private var setEquipmentCt = 200

    private fun getAction(currentFrame: Int, currentGamePad: GamePad): GamePad {
        frameStateUpdater.updateFrame(currentFrame, currentGamePad)
        if (collectSkip) {
            val link = frameStateUpdater.getLink()
            collect.collect(link, previousLink)
            previousLink = link
            d { collect.toString() }
            return GamePad.None
        }
        if (unstick > 0) {
            unstick--
            return if (forcedDirection == GamePad.None) {
                GamePad.randomDirection()
            } else {
                forcedDirection
            }
        }

        // filters..

        if (frameStateUpdater.state.frameState.isDoneScrolling) {
            d { " * clear history" }
            frameStateUpdater.state.clearHistory()
            // then skip action on this frame
            return GamePad.None
        }
        if (frameStateUpdater.state.frameState.isScrolling) {
            return GamePad.None
        }

        if (addKey) {
            frameStateUpdater.addKey()
            addKey = false
        }
        if (addRupee) {
            frameStateUpdater.addRupee()
            addRupee = false
        }
//        fillBombs()
        refillIfOut()
        if (setEquipmentCt > 0 && addEquipment) {
            d { " Set equip" }
//            frameStateUpdater.setSword(ZeldaItem.MagicSword)
            frameStateUpdater.setLadderAndRaft(true)
            frameStateUpdater.setRedCandle()
            frameStateUpdater.setHaveWhistle()
            frameStateUpdater.setBait()
            frameStateUpdater.setLetter()
            frameStateUpdater.setArrow()
            frameStateUpdater.fillTriforce()
//            frameStateUpdater.setRing(ZeldaItem.RedRing)

            frameStateUpdater.fillHearts()
            setEquipmentCt--
        }
        if (frameStateUpdater.state.frameState.clockActivated) {
            frameStateUpdater.deactivateClock()
        }

//        plan.next(frameStateUpdater.state)

        val nextGamePad = plan.next(frameStateUpdater.state)

        frameStateUpdater.state.previousGamePad = nextGamePad

        d { plan.toString() }
        if (ZeldaBot.draw) {
            with(frameStateUpdater.state) {
                val currentCell = currentMapCell
                val locCoordinates = "${frameState.level}: ${frameState.mapLoc} : ${currentCell.mapData.name.take(10)}"
                d {
                    "current --> " +
                            "$locCoordinates " +
//                            " current action: ${plan.action?.name ?: ""} "
                            " target ${plan.target()} " + "link " +
                            "${frameState.link.point}"
                }
                val tenth = this.frameState.tenth
                val dir = this.frameState.link.dir.name.first()
                val damage = this.frameState.inventory.heartCalc.damageNumber()
                val st = plan.action?.name?.first() ?: ""
                try {
                    drawIt(plan.target(), "$locCoordinates $link $st t: $tenth d: $damage")
                } catch (e: Exception) {
                    d { "ERROR $e"}
                }
            }
        }
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

    private fun drawDroppedItem(point: FramePoint, text: String) {
        val pt = point.toScreenY
        api.drawString("$text", pt.x, pt.y, false)
    }

    private fun refillIfOut() {
        fillBombs()

        if (frameStateUpdater.state.frameState.inventory.numRupees!! < 250) {
            api.writeCPU(Addresses.numRupees, 252)
        }
    }

    private fun refillKeys() {
        if (frameStateUpdater.state.frameState.inventory.numKeys == 0) {
            api.writeCPU(Addresses.numKeys, 2)
        }
    }

    private fun fillBombs() {
        if (frameStateUpdater.state.frameState.inventory.numBombs!! <= 2) {
            api.writeCPU(Addresses.numBombs, 8)
        }
    }

    private fun renderFinished() {
        val currentFrame = api.frameCount

        d { "execute ### $currentFrame" }
        monitor.update(frameStateUpdater.state, plan)

        // fill hearts
        // not reliable enough
        val life = frameStateUpdater.state.frameState.inventory.heartCalc.lifeInHearts2()
        if (life <= 2.5) {
            d { "fill hearts $life" }
            frameStateUpdater.fillHearts()
        } else {
            d { "fill hearts $life" }
        }

        val act = doAct && !collectSkip //currentFrame % 3 == 0
        if (act) {
            currentGamePad = getAction(currentFrame, currentGamePad)
            d { " action: at ${frameStateUpdater.state.frameState.link.point} do -> $currentGamePad previous ${frameStateUpdater.state.previousMove.move}" }

            val toRelease = mutableSetOf(GamePad.MoveRight, GamePad.MoveUp, GamePad.MoveLeft, GamePad.MoveDown)
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
        } else {
            d { " no action" }
            currentGamePad = getAction(currentFrame, currentGamePad)
        }
    }

    companion object {
        private const val STRING = "####"
        private const val SPRITE_ID = 123
        private const val SPRITE_ID_2 = 456
        private const val SPRITE_SIZE = 2
        var hasLadder = false
        var doAct = true
        var draw = true
        var addKey = false
        var addRupee = false
        var unstick = 0
        var forcedDirection = GamePad.None
        var addEquipment: Boolean = false

        @JvmStatic
        fun startIt(monitor: ZeldaMonitor): ZeldaBot {
            ApiSource.initRemoteAPI("localhost", 9999)
            val bot = ZeldaBot(monitor)
            bot.launch()
            return bot
        }

//        @JvmStatic
//        fun startActionTest(monitor: ZeldaMonitor) {
//            ApiSource.initRemoteAPI("localhost", 9999)
//            // which save state to load
//            // which action to start
//            ZeldaBot(monitor).launch()
//        }

        @JvmStatic
        fun main(args: Array<String>) {
            println("API enabled GREG kotlin")
            println("START")
            if (args.isNotEmpty()) {
                System.out.println("start remote")
                ApiSource.initRemoteAPI("localhost", 9999)
            } else {
                ApiSource.initRemoteAPI("localhost", 9999)
                System.out.println("start local")
            }
            ZeldaBot(NoOp()).launch()
        }
    }

    interface ZeldaMonitor {
        fun update(state: MapLocationState, plan: PlanRunner)
    }

    class NoOp : ZeldaMonitor {
        override fun update(state: MapLocationState, plan: PlanRunner) {
            // todo
        }
    }
}

