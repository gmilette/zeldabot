package bot.plan.runner

import bot.state.GamePad
import bot.plan.action.Action
import bot.plan.action.DoNothing
import bot.plan.action.moveHistoryAttackAction
import bot.state.Addresses
import bot.state.FramePoint
import bot.state.MapLocationState
import nintaco.api.API
import sequence.ZeldaItem
import util.d

class PlanRunner(private val makePlan: () -> MasterPlan, private val api: API) {
    lateinit var action: Action
    private lateinit var runLog: RunActionLog
    lateinit var masterPlan: MasterPlan
    lateinit var startPath: String

    init {
        run { lev2w }
    }

    private fun rerun() {
        run(load = true) { lev2w }
    }

    private fun run(load: Boolean = false, select: Experiments.() -> Experiment) {
        val experiments = Experiments(makePlan())
        val ex = experiments.select()
        setSword(ex.sword)
        masterPlan = ex.plan
        startPath = ex.startSave
        action = withDefaultAction(masterPlan.skipToStart())
        runLog = RunActionLog(ex.name)
        if (load) {
            d { "reload" }
            val root = "../Nintaco_bin_2020-05-01/states/"
            api.loadState("$root/${startPath}")
        }
    }

    fun setSword(item: ZeldaItem) {
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

    // currently does this
    fun next(state: MapLocationState): GamePad {
        runLog.frameCompleted(state)
        // update plan
        // if actions are
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
    fun advance(state: MapLocationState) {
        if (masterPlan.complete) {
            d { " complete "}
            runLog.logFinalComplete(state )
            rerun()
            // record final
        } else {
            action = withDefaultAction(masterPlan.pop())
            action.reset()
        }
    }

    fun afterThis() = masterPlan.next()

    fun afterAfterThis() = masterPlan.nextAfter()

    fun target(): FramePoint {
        return action.target()
    }

    fun path(): List<FramePoint> {
        return listOf()
    }

    override fun toString(): String {
        return "*** ${action.name}: Plan: $masterPlan"
    }
}