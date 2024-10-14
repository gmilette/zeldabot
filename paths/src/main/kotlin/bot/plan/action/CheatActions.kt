package bot.plan.action

import bot.state.MapLocationState

class CheatGetBombs : Action {
    override fun complete(state: MapLocationState): Boolean = (true).also {
        state.frameState.inventory.setBombs()
    }
}

class CheatRupee : Action {
    override fun complete(state: MapLocationState): Boolean = (true).also {
        state.frameState.inventory.addRupee(state.frameState)
    }
}
