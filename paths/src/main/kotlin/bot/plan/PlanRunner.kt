package bot.plan

import bot.GamePad
import bot.state.FramePoint
import bot.state.FrameState
import bot.state.MapLocationState
import bot.state.MasterPlan

class PlanRunner(private val masterPlan: MasterPlan) {
    var action = masterPlan.pop()

    // currently does this
    fun next(state: MapLocationState): GamePad {
        // update plan
        // if actions are
        if (action.complete(state)) {
            advance()
        }

        return action.nextStep(state)
    }

    private fun advance() {
        action = masterPlan.pop()
    }

    fun target(): FramePoint {
        return action.target()
    }
}