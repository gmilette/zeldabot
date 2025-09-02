package bot.plan.action

import bot.plan.runner.MasterPlan
import bot.state.MapLocationState

interface ModifyAction : Action {
    /**
     * can push or pop actions
     */
    fun change(state: MapLocationState, masterPlan: MasterPlan)
}

open class ActionHolder : Action {
    override fun complete(state: MapLocationState): Boolean {
        return true
    }
}

// if not in location push a task to get back to the location
fun IfNeedBombsDo(action: Action): Action {
    return IfDo(action) {
        it.frameState.inventory.numBombs < 8
    }
}

class IfDo(val action: Action, val condition: (MapLocationState) -> Boolean) : ActionHolder(), ModifyAction {
    override fun change(state: MapLocationState, masterPlan: MasterPlan) {
        if (condition(state)) {
            masterPlan.push(action)
        }
    }
}