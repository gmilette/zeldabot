package bot.plan.runner

import bot.state.GamePad
import bot.plan.action.Action
import bot.plan.action.DoNothing
import bot.plan.action.MapAwareAction
import bot.plan.action.moveHistoryAttackAction
import bot.state.FramePoint
import bot.state.MapLocationState
import util.d

class PlanRunner(val masterPlan: MasterPlan) {
    var action: Action
    private val runLog = RunLog()

    init {
        action = withDefaultAction(masterPlan.skipToStart())
    }

    private fun withDefaultAction(action: Action) = moveHistoryAttackAction(action)

    // currently does this
    fun next(state: MapLocationState): GamePad {
        // update plan
        // if actions are
        if (action.complete(state)) {
            runLog.advance(action, state)
            advance()
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
    fun advance() {
        action = withDefaultAction(masterPlan.pop())
        action.reset()
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