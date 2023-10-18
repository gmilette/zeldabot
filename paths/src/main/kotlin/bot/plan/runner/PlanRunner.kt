package bot.plan.runner

import bot.ZeldaBot
import bot.plan.action.Action
import bot.plan.action.DoNothing
import bot.plan.action.moveHistoryAttackAction
import bot.state.Addresses
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.destination.ZeldaItem
import nintaco.api.API
import util.d

class PlanRunner(private val makePlan: () -> MasterPlan, private val api: API) {
    var action: Action? = null
        private set
    private lateinit var runLog: RunActionLog
    lateinit var masterPlan: MasterPlan
    lateinit var startPath: String

//    private val target = "afterLev4"
//    private val target = "level7"
    private val target = Experiments.current

    init {
//        run(name = "level1drag")
//        run(name = "level2Boom")
//        run(name = "level6start")
//        run(name = "level8")
//        run(name = "level1L") // with ladder
//        run(name = "level1drag")
//        run(name = "level1Ladder") // with ladder
//        run(name = "level1")
//        run(name = "level3")
//        run(name = "level5") // with ladder
//        run(name = "afterLev4")
//        run(name = "all")
        run(name = "gannon")
//        run(name = "level9") // with ladder
    }

    private fun rerun() {
        run(load = true, name = target)
    }

    private fun run(load: Boolean = false, name: String) {
        val experiments = Experiments(makePlan())
        val ex = experiments.ex(name)
        d { "  run experiment ${ex.name}"}
        ZeldaBot.addEquipment = ex.addEquipment
        masterPlan = ex.plan
        startPath = ex.startSave
        action = withDefaultAction(masterPlan.skipToStart())
        runLog = RunActionLog(ex.name)
        if (load) {
            d { "reload" }
            val root = "../Nintaco_bin_2020-05-01/states/"
            api.loadState("$root/${startPath}")
        }
        setSword(ex.sword)
    }

    private fun setSword(item: ZeldaItem) {
        d  { " SET SWORD $item"}
        when (item) {
            ZeldaItem.WoodenSword -> api.writeCPU(Addresses.hasSword, 1)
            ZeldaItem.WhiteSword -> api.writeCPU(Addresses.hasSword, 2)
            ZeldaItem.MagicSword -> api.writeCPU(Addresses.hasSword, 3)
            else -> {
                api.writeCPU(Addresses.hasSword, 0)
            }
        }
    }

    private fun withDefaultAction(action: Action) = moveHistoryAttackAction(action)

    fun next(state: MapLocationState): GamePad {
        val action = action ?: return GamePad.None
        runLog.frameCompleted(state)

        if (action.complete(state)) {
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
        if (masterPlan.complete) {
            d { " complete "}
            runLog.logFinalComplete(state )
            rerun()
        } else {
            action = withDefaultAction(masterPlan.pop())
            action?.reset()
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