package bot

import bot.plan.ZeldaPlan
import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.plan.runner.MasterPlan
import bot.plan.runner.PlanRunner
import bot.state.*
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.map.MapCell
import bot.state.map.MapConstants
import bot.state.oam.rhinoHeadHeadWithMouthClosed
import bot.state.oam.rhinoHeadHeadWithMouthOpen
import nintaco.api.API
import nintaco.api.ApiSource
import nintaco.api.Colors
import nintaco.api.GamepadButtons
import util.Map2d
import util.RunOnceLambda
import util.d

class ZeldaBot(private val monitor: ZeldaMonitor) {
    private val api: API = ApiSource.getAPI()

    fun launch() {
        startAndSetListeners()

        api.addDeactivateListener { apiDisabled() }
        api.addStopListener { dispose() }
        api.run()
    }

    private fun startAndSetListeners() {
        // this allows the code to programatically start the game at a certain state
        // but it has to happen after the API is ready, so execute on callback from
        // listeners

        val root = "../Nintaco_bin_2020-05-01/states/"
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
            setSpeed
        }
        api.addStatusListener { message: String -> statusChanged(message) }
        api.addActivateListener {
            apiEnabled()
            setSpeed
        }
    }

    private fun apiEnabled() {
        d { "apiEnabled" }
        screenDraw.apiEnabled()
    }

    private fun apiDisabled() {
        println("API disabled")
    }

    private fun dispose() {
        println("API stopped")
    }

    private fun statusChanged(message: String) {
        println("Status message: $message")
    }

    private var currentGamePad = GamePad.MoveRight
    private val hyrule: Hyrule = Hyrule()
    private val screenDraw = ScreenDraw()
    private var setEquipmentCt = 200
    private var frameStateUpdater: FrameStateUpdater = FrameStateUpdater(api, hyrule)
    private val cheater = Cheater(api, frameStateUpdater)
    val plan = PlanRunner(::makePlan, api)

    private fun makePlan(): MasterPlan {
        // make sure to reset any state here
        frameStateUpdater.reset()
        return ZeldaPlan.makeMasterPlan(hyrule, hyrule.mapCellsObject, hyrule.levelMap)
    }

    private fun renderFinished() {
        val currentFrame = api.frameCount

        d { "execute ### $currentFrame" }
        monitor.update(frameStateUpdater.state, plan)

        cheater.refillAndSetItems()

        val act = doAct
        if (act) {
            currentGamePad = getAction(currentFrame, currentGamePad)
            screenDraw.draw()
            d { plan.toString() }
            d { " action: at ${frameStateUpdater.state.frameState.link.point} do -> $currentGamePad previous ${frameStateUpdater.state.previousMove.move}" }

            // not necessary I think
//            val toRelease = mutableSetOf(GamePad.MoveRight, GamePad.MoveUp, GamePad.MoveLeft, GamePad.MoveDown)
//            toRelease.forEach { api.writeGamepad(0, it.toGamepadButton, false) }
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

    private fun getAction(currentFrame: Int, currentGamePad: GamePad): GamePad {
        frameStateUpdater.updateFrame(currentFrame, currentGamePad)

        // this is a debug function so the debug UI can control link
        if (unstick > 0) {
            unstick--
            return if (forcedDirection == GamePad.None) {
                GamePad.randomDirection()
            } else {
                forcedDirection
            }
        }

        if (frameStateUpdater.state.frameState.isDoneScrolling) {
            d { " * done scrolling to new screen" }
            frameStateUpdater.state.clearHistory()
            // then skip action on this frame
            return GamePad.None
        }
        if (frameStateUpdater.state.frameState.isScrolling) {
            return GamePad.None
        }

        val nextGamePad = plan.next(frameStateUpdater.state)

        frameStateUpdater.state.previousGamePad = nextGamePad

        return nextGamePad
    }

    companion object {
        private const val STRING = "####"
        private const val SPRITE_ID = 123
        private const val SPRITE_ID_2 = 456
        private const val SPRITE_SIZE = 6

        // parameters controlled by the debug UI
        var hasLadder = false
        var doAct = true
        var draw = true
        var addKey = false
        var addRupee = false
        var unstick = 0
        var forcedDirection = GamePad.None
        var addEquipment: Boolean = false
        var invincible: Boolean = true
        var maxLife: Boolean = true

        @JvmStatic
        fun startIt(monitor: ZeldaMonitor): ZeldaBot {
            ApiSource.initRemoteAPI("localhost", 9999)
            val bot = ZeldaBot(monitor)
            bot.launch()
            return bot
        }

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isNotEmpty()) {
                println("start remote")
                ApiSource.initRemoteAPI("localhost", 9999)
            } else {
                ApiSource.initRemoteAPI("localhost", 9999)
                println("start local")
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

    /**
     * refil, make link invincible, etc..
     */
    inner class Cheater(api: API, frameStateUpdater: FrameStateUpdater) {
        private val stateManipulator = StateManipulator(api, frameStateUpdater)
        fun refillAndSetItems() {
            // fill hearts
            // not reliable enough
            val life = frameStateUpdater.state.frameState.inventory.heartCalc.lifeInHearts2()
            d { "fill hearts $life" }
            when {
                life <= 4.0 && invincible -> {
                    d { "*fill*" }
                    stateManipulator.fillHearts()
                }
                maxLife -> stateManipulator.fillHearts()
            }

            if (addKey) {
                stateManipulator.addKey()
                addKey = false
            }
            if (addRupee) {
                stateManipulator.addRupee()
                addRupee = false
            }
            refillIfOut()
//            stateManipulator.setSword(ZeldaItem.WoodenSword)
//            stateManipulator.setRing(ZeldaItem.BlueRing)
            if (setEquipmentCt > 0 && addEquipment) {
                d { " Set equip" }
//            frameStateUpdater.setSword(ZeldaItem.MagicSword)
//            frameStateUpdater.setRing(ZeldaItem.BlueRing)
                stateManipulator.setLadderAndRaft(true)
                stateManipulator.setRedCandle()
                stateManipulator.setHaveWhistle()
                stateManipulator.setBait()
                stateManipulator.setLetter()
                stateManipulator.setArrow()
                stateManipulator.fillTriforce()
//            frameStateUpdater.fillHearts()
                setEquipmentCt--
            }
            stateManipulator.deactivateClock()
        }

        private fun refillIfOut() {
            fillBombs()

//            refillRupees()
        }

        private fun refillRupees() {
            if (frameStateUpdater.state.frameState.inventory.numRupees < 250) {
                api.writeCPU(Addresses.numRupees, 252)
            }
        }

        private fun refillKeys() {
            if (frameStateUpdater.state.frameState.inventory.numKeys == 0) {
                api.writeCPU(Addresses.numKeys, 2)
            }
        }

        private fun fillBombs() {
            if (frameStateUpdater.state.frameState.inventory.numBombs <= 2) {
                api.writeCPU(Addresses.numBombs, 8)
            }
        }
    }

    inner class ScreenDraw {
        private val rhinoHeadLeftUp = 0xFA // foot up
        private val rhinoHeadLeftUp2 = 0xFC // foot down
        private val rhinoHeadLeftDown = 0xF6 // foot up
        private val rhinoHeadLeftDown2 = 0xF4 // foot down
        private val head = setOf(
            rhinoHeadHeadWithMouthOpen,
            rhinoHeadHeadWithMouthClosed,
            rhinoHeadLeftUp,
            rhinoHeadLeftUp2,
            rhinoHeadLeftDown,
            rhinoHeadLeftDown2
        )

        private var spriteX = 0
        private var spriteY = 8
        private var spriteVx = 1
        private var spriteVy = 1
        private var strWidth = 0
        private var strX = 0
        private var strY = 200

        fun apiEnabled() {
            val sprite = IntArray(SPRITE_SIZE * SPRITE_SIZE)
            for (y in 0 until SPRITE_SIZE) {
                val Y = y - SPRITE_SIZE / 2 + 0.5
                for (x in 0 until SPRITE_SIZE) {
                    val X = x - SPRITE_SIZE / 2 + 0.5
                    sprite[SPRITE_SIZE * y + x] = if (X * X + Y * Y
                        <= SPRITE_SIZE * SPRITE_SIZE / 4
                    ) Colors.DARK_BLUE else -1
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

        fun draw() {
            if (draw) {
                with(frameStateUpdater.state) {
                    val currentCell = currentMapCell
                    val locName = if (currentCell.mapData.name.isEmpty()) {
                        ""
                    } else {
                        " : ${currentCell.mapData.name.take(10)}"
                    }
                    val locCoordinates =
                        "${frameState.level}:${frameState.mapLoc} $locName" // at $movedTo"
                    d {
                        "current --> " +
                                "$locCoordinates " +
                                " target ${plan.target()} " + "link " +
                                "${frameState.link.point}"
                    }
                    val tenth = this.frameState.tenth
                    val dir = this.frameState.link.dir.name.first()
//                    val damage = this.frameState.inventory.heartCalc.damageNumber()
//                    d: $damage
                    try {
                        drawIt(plan.target(), plan.path(), "$locCoordinates $link t: $tenth d: $dir")
                    } catch (e: Exception) {
                        d { "ERROR $e" }
                    }

                    // draws the right look but the colors are wrong
//                val mapCell = this.hyrule.getMapCell(this.frameState.mapLoc)
                    try {
                        val mapCell = if (this.frameState.isLevel) hyrule.levelMap.cell(
                            this.frameState.level,
                            this.frameState.mapLoc
                        ) else hyrule.getMapCell(this.frameState.mapLoc)
//                        val should = AttackActionDecider.shouldAttack(this)
                        val should = false
                        drawCosts(frameState.link.dir, frameState.link.point, should, mapCell.zstar.costsF.copy())
                    } catch (e: Exception) {
                        d { "ERROR $e" }
                    }

                    for (enemy in frameState.enemies) {
                        api.color = Colors.CYAN
                        val pts = AttackActionDecider.attackPoints(enemy.point)
                        for (pt in pts) {
                            api.drawOval(pt.x, pt.y + MapConstants.yAdjust, 2, 2)
                        }
                    }

                    // only display if hearts are not full, which is always
                    val swords = AttackActionDecider.swordRectangles(link)
                    for (sword in swords) {
                        api.color = Colors.BLACK
                        sword.value.apply {
                            api.drawRect(topLeft.x, topLeft.y + MapConstants.yAdjust, width, height)
                        }
                    }
                    val longAttack = AttackLongActionDecider.longRectangle(this)
                    longAttack.apply {
                        api.color = Colors.GRAY
                        api.drawRect(topLeft.x, topLeft.y + MapConstants.yAdjust, width, height)
                    }

                }
            }
        }

        private fun MapLocationState.rhino(): Agent? =
            // pick the left most head
            frameState.enemies.filter { it.y != 187 && head.contains(it.tile) }.minByOrNull { it.x }

        private fun drawMap(cell: MapCell) {
            for (x in 0..255) {
                for (y in 0..167) {
//                val pt = point.toScreenY
                    if (cell.passable.get(x, y)) {
                        api.drawSprite(SPRITE_ID, x, y + MapConstants.yAdjust)
                    }
                }
            }
        }

        private fun drawCosts(dir: Direction, link: FramePoint, should: Boolean = false, map: Map2d<Int>) {
            map.map.forEachIndexed { y, row ->
                row.forEachIndexed { x, v ->
                    // if it is in the attack grid color it
                    val color = when {
                        // attack grid
//                        (y % 16 % 2 == 0) && (x % 16 % 2 == 0) && attackDirectionGrid.isInHalfFatGrid(FramePoint(x, y), dir.vertical) ->
//                            if (should) Colors.DARK_GREEN else Colors.LIGHT_BLUE

//                        v > 100000 -> Colors.MAGENTA
//                        v > 9000 && (y % 16 % 2 == 0) -> Colors.RED
                        else -> -1
                    }
                    if (color != -1) {
                        api.color = color
                        api.drawOval(x, y + MapConstants.yAdjust, 1, 1)
                    }
                }
            }
        }

        private fun drawIt(point: FramePoint, path: List<FramePoint>, text: String) {
            val pt = point.toScreenY
            api.drawSprite(SPRITE_ID, pt.x, pt.y)
            api.color = Colors.DARK_BLUE
            api.fillRect(strX - 1, strY - 1, strWidth + 2, 9)

            if (path.isNotEmpty()) {
                api.color = Colors.ORANGE
                val screenPath = path.map { it.toScreenY }
                var prev: FramePoint = screenPath.first()
                for (pathPt in screenPath) {
                    api.drawLine(prev.x, prev.y, pathPt.x, pathPt.y)
                    prev = pathPt
                }
            }

            // todo: draw dots on enemies, one color for projectile, one for enemy
            // draw the grid of cost function?

//        api.setColor(Colors.BLUE)
//        api.drawRect(strX - 2, strY - 2, strWidth + 3, 10)
            api.color = Colors.WHITE
            api.drawString("$text", strX, strY, false)
        }
    }
}


