package bot.plan.action

import bot.state.MapLocationState

class CheatGetBombs : Action {
    override fun complete(state: MapLocationState): Boolean = (true).also {
        state.frameState.inventory.setBombs()
    }
}

class CheatRupee(val enoughFor: Int = 100) : Action {
    override fun complete(state: MapLocationState): Boolean = (true).also {
        state.frameState.inventory.addRupeeIfNeed(state.frameState, enoughFor)
    }
}

val EnoughForRing
    get() = CheatRupee(250)
val EnoughForPotion
    get() = CheatRupee(68)
val EnoughForArrow
    get() = CheatRupee(88)
val EnoughForBait
    get() = CheatRupee(80)
