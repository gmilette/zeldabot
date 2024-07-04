package bot.plan.runner

import bot.DirectoryConstants
import bot.plan.action.Action
import bot.plan.action.DoNothing
import bot.plan.action.moveHistoryAttackAction
import bot.state.*
import nintaco.api.API
import util.d
import java.io.File

typealias PlanMaker = () -> MasterPlan

class PlanRunner(private val makePlan: PlanMaker, private val api: API) {
    var action: Action? = null
        private set
    lateinit var runLog: RunActionLog
    lateinit var masterPlan: MasterPlan
    lateinit var startPath: String

    private val experiments = Experiments(makePlan)

    fun getExp(name: String): Experiment {
        return experiments.experiments[name] ?: experiments.evaluation[name] ?: experiments.default
    }

    //    private val target = "afterLev4"
//    private val target = "level7"
    private val target: Experiment
        get() = experiments.default
//        get() = experiments.evaluation["level2Bomb6wws"] ?: experiments.current

    private var runCt = 0
    private var runSetupCt = 0

    init {
//        if (runCt % 10 == 0) {
//            experiments.experimentIncrement++
//        }
//        val runIt: Experiment =
//        runIt("level2rhino")
//        runIt("level25h")
        runIt("all")
//        runLoc(true,94, 0) // near start
//        runLoc(true,120, 0) // near start
//        runLoc(true,10, 0) // near start
//        runLoc(true,26, 0) // near start

//        val runIt: Experiment = getExp("allBoom")
//        runIt(ex = runIt)
//        run(name = "level1drag")
//        run(name = "level2Boom")
//        run(name = "level6start")
//        run(name = "level8")
//        run(name = "level1L") // with ladder
//        run(name = "level1drag")
//        run(name = "level1Ladder") // with ladder
//          run(name = "level1")
//        run(name = "level1dodge")
//        run(name = "level2dodge")
//        run(name = "overworlddodge")
//        run(name = "level1dodgeb")

//        run(name = "level3")
//        run(name = "level5") // with ladder
//        runLoc(true, 120, 6)
//        run(name = "afterLev4")
//        runIt("level2w")
//         run(name = "all")
//        runLoc(true,121, 0) // near start
//        runLoc(true,26, 0) // near start

//            runLoc(true,91, 0)
//        run(name = "level7"
//        run(name = "go to level 9")
//        run(name = "level2rhinoAfter")
//        run(name = "gannon")
//        run(name = "level9") // with ladder
//        run(name = "ladder_heart")
//        runLoc(true,5, 0)
//        run(name = "level1")
//        run(name = "level2w")
//        runLoc(true,62, 2) // sand
//        runLoc(true,94, 2) // before boomerang
//        runLoc(true,35, 1) // before bow
//        runLoc(true,35+16+16, 1)
//        runLoc(true,69, 1) // dragon
//        runLoc(true,35+16+16, 1)
//        runLoc(true,87, 5)
//        runLoc(true,48, 4)
//        runLoc(true,50, 4) // push ladder
//        runLoc(true,16, 4) // push drag
//        runLoc(true,24, 7)
//        runLoc(true,76, 3)
//        runLoc(true,91, 3) //sword guy
//        runLoc(true,107, 3) // right stair
//        runLoc(true,5, 5)
//        runLoc(true,6, 5)
//        runLoc(true,63, 8)
//        runLoc(true,104, 0) // near start
//        runLoc(true,45, 0) // forest statue
//        runLoc(true,61, 0) // forest statue
//        runLoc(true,52, 0) // forest statue
//        runLoc(true,36, 0) // forest statue
//        runLoc(true,99-16, 0) // forest statue
//        runLoc(true,69+16+16, 0) // going to 13
    }

    private fun rerun() {
        runIt(load = true, ex = target)
    }

    private fun runLoc(load: Boolean = false, mapLoc: Int, level: Int = 0) {
        val plan = makePlan()
//        val ex = experiments.ex(name)
//        d { "  run experiment ${ex.name}"}
//        ZeldaBot.addEquipment = ex.addEquipment
        masterPlan = plan
//        val root = "../Nintaco_bin_2020-05-01/states/mapstate_${level}_${mapLoc}.save"

        val root = "mapstate/mapstate_${level}_${mapLoc}.save"

        d { " map state $root"}
        startPath = root
        action = withDefaultAction(masterPlan.skipToLocation(mapLoc, level))
        runLog = RunActionLog("mapstate_${level}_${mapLoc}", target)
    }

    private fun runIt(ex: String) {
        runIt(true, getExp(ex))
    }

    private fun runIt(load: Boolean = false, ex: Experiment) {
        d { "  run experiment ${ex.name} load=$load"}

//        val ex = experiments.getExp()
//        ZeldaBot.addEquipment = ex.addEquipment
        masterPlan = ex.plan()
        startPath = ex.startSave
        masterPlan.reset()
        action = withDefaultAction(masterPlan.skipToStart())
        d { " START AT ${action?.name}"}
        runLog = RunActionLog(ex.name, ex)
        if (load) {
            d { "reset" }
            Thread( {
                api.reset()
            }).start()
            Thread.sleep(100)
            d { "reload" }
            val root = DirectoryConstants.states
            api.loadState("$root/${startPath}")
        }
        runCt++
        runSetupCt = 0
    }

    fun runSetup(manipulator: StateManipulator) {
        d { " run setup "}
        if (runSetupCt > 20) {
            return
        }
//        api.setSpeed(400)
        runSetupCt++
        val ex = this.target
        d { " set sword to ${ex.sword} hearts to ${ex.hearts}"}
        manipulator.setSword(ex.sword)
        ex.hearts?.let {
            manipulator.setHearts(it)
        }
        manipulator.clearRupee()
        manipulator.setRing(ex.ring)
        manipulator.setBombs(ex.bombs)
        if (ex.shield) {
            manipulator.setMagicShield()
        }
        manipulator.setBoomerang(ex.boomerang)
        manipulator.setKeys(ex.keys)
    }

    private fun withDefaultAction(action: Action) = moveHistoryAttackAction(action)

    fun next(state: MapLocationState): GamePad {
        val action = action ?: return GamePad.None
        runLog.frameCompleted(state)

        if (action.complete(state) || state.frameState.isDead) {
            runLog.advance(action, state)
            advance(state)
        }

        return try {
            action.nextStep(state)
        } catch (e: Exception) {
            DoNothing().nextStep(state)
        }
    }

    /**
     * advance to the next step
     */
    private fun advance(state: MapLocationState) {
        if (masterPlan.complete || state.frameState.isDead) {
            d { " complete "}
            runLog.logFinalComplete(state, masterPlan)
            rerun()
        } else {
            d { " complete action ${action?.javaClass?.name ?: ""}"}
            completedAction(state, action)
            action = withDefaultAction(masterPlan.pop())
            action?.reset()
        }
    }

    private fun completedAction(state: MapLocationState, action: Action?) {
        val stateName = "${state.frameState.level}_${state.movedTo}"
        val root = DirectoryConstants.states
        val stateFileName = "$root/mapstate/mapstate_${stateName}.save"
        if (!File(stateFileName).exists()) {
            d { "Saved a state $stateFileName" }
            api.saveState(stateFileName)
            api.saveScreenshot()
        } else {
            d { "Already saved $stateFileName" }
        }
    }

    fun afterThis() = masterPlan.next()

    fun afterAfterThis() = masterPlan.nextAfter()

    fun target(): FramePoint {
        return action?.target() ?: FramePoint(1,1)
    }

    fun targets(): List<FramePoint> {
        return action?.targets() ?: emptyList()
    }

    fun path(): List<FramePoint> {
        d { " path for action ${action?.name ?: ""}"}
        return action?.path() ?: emptyList()
    }

    override fun toString(): String {
        return "*** ${action?.name ?: ""}: Plan: $masterPlan"
    }
}