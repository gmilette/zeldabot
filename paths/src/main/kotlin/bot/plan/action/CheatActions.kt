package bot.plan.action

import bot.state.GamePad
import bot.state.MapLocationState
import util.LogFile

private val file = LogFile("cheatLog", addDate = true)

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
        file.write(state.frameState.level, state.frameState.mapLoc, state.frameState.currentFrame, "key", "add key", state.frameState.inventory.numKeys)
        state.frameState.inventory.addKey()
    }
}

fun cheatBombs() : Action  = ActionOnce { state ->
    if (state.frameState.inventory.numBombs < 4) {
        file.write(state.frameState.level, state.frameState.mapLoc, state.frameState.currentFrame, "bomb", "set bomb to 4", state.frameState.inventory.numBombs)
    }
    state.frameState.inventory.setBombs()
}

fun cheatRupee(need: String, enoughFor: Int = 100) : Action  = ActionOnce { state ->
    executeRupeeCheat(state, enoughFor, need)
}

fun cheatLetter() : Action  = ActionOnce { state ->
    if (!state.frameState.inventory.hasLetter) {
        file.write(state.frameState.level, state.frameState.mapLoc, state.frameState.currentFrame, "letter", "set letter", state.frameState.inventory.hasLetter)
        state.frameState.inventory.setLetter()
    }
}

fun cheatPotion() : Action  = ActionOnce { state ->
    val (need, what) = when {
        !state.frameState.inventory.hasPotion -> 68 to "red"
        state.frameState.inventory.hasHalfPotion -> 40 to "blue"
        else -> 0 to "none"
    }
    if (need > 0) {
        executeRupeeCheat(state, need, what)
    }
}

fun executeRupeeCheat(state: MapLocationState, enoughFor: Int = 100, need: String) {
    if (state.frameState.inventory.numRupees < enoughFor) {
        file.write(state.frameState.level, state.frameState.mapLoc, state.frameState.currentFrame, need, "set rupee to $enoughFor", state.frameState.inventory.numRupees)
        state.frameState.inventory.addRupeeIfNeed(enoughFor)
    }
}

val AddKey
    get() = cheatGetKey()
val CheatBombs
    get() = cheatBombs()
val EnoughForRing
    get() = cheatRupee("ring", 250)
val EnoughForPotion
    get() = cheatPotion()
val HaveLetter
    get() = cheatLetter()
val EnoughForArchery
    get() = cheatRupee("archery", 75)
val EnoughForArrow
    get() = cheatRupee("arrow", 88)
val EnoughForBait
    get() = cheatRupee("bait", 80)
