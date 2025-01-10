package bot.plan.action

import bot.state.MapLocationState

class CheatGetKey : Action {
    override fun complete(state: MapLocationState): Boolean = (true).also {
        state.frameState.inventory.addKey()
    }
}

class CheatGetBombs : Action {
    override fun complete(state: MapLocationState): Boolean = (true).also {
        state.frameState.inventory.setBombs()
    }
}

class CheatRupee(private val enoughFor: Int = 100) : Action {
    override fun complete(state: MapLocationState): Boolean = (true).also {
        state.frameState.inventory.addRupeeIfNeed(enoughFor)
    }
}

val AddKey
    get() = CheatGetKey()
val EnoughForRing
    get() = CheatRupee(250)
val EnoughForPotion
    get() = CheatRupee(68)
val EnoughForArchery
    get() = CheatRupee(100)
val EnoughForArrow
    get() = CheatRupee(88)
val EnoughForBait
    get() = CheatRupee(80)
