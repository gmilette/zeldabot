package bot.plan.action

import bot.state.GamePad
import bot.state.MapLocationState
import util.LogFile

private val file = LogFile("cheatLog")

class ActionOnce(private val action: (MapLocationState) -> Unit) : Action {
    private var done = false

    override fun complete(state: MapLocationState): Boolean = done

    override fun nextStep(state: MapLocationState): GamePad {
        if (!done) {
            action(state)
        }
        done = true
        return GamePad.None
    }
}

fun cheatGetKey(): Action = ActionOnce { state ->
    if (state.frameState.inventory.numKeys == 0) {
        file.write(state.frameState.mapLoc, "add key", state.frameState.inventory.numKeys)
        state.frameState.inventory.addKey()
    }
}

fun cheatBombs() : Action  = ActionOnce { state ->
    if (state.frameState.inventory.numBombs < 4) {
        file.write(state.frameState.mapLoc, "set bomb to 4", state.frameState.inventory.numBombs)
    }
    state.frameState.inventory.setBombs()
}

fun cheatRupee(enoughFor: Int = 100) : Action  = ActionOnce { state ->
    if (state.frameState.inventory.numRupees < enoughFor) {
        file.write(state.frameState.mapLoc, "set rupee to $enoughFor", state.frameState.inventory.numRupees)
    }
    state.frameState.inventory.addRupeeIfNeed(enoughFor)
}

val AddKey
    get() = cheatGetKey()
val CheatBombs
    get() = cheatBombs()
val EnoughForRing
    get() = cheatRupee(250)
val EnoughForPotion
    get() = cheatRupee(68)
val EnoughForArchery
    get() = cheatRupee(100)
val EnoughForArrow
    get() = cheatRupee(88)
val EnoughForBait
    get() = cheatRupee(80)
