package bot.plan.action

import bot.state.*
import util.d

enum class DiversionState {
    noNeed,
    needRupees,
    getPotion,
    usePotion
}

fun UsePotionAction(wrapped: Action): () -> Action = {
    UsePotion()
}

//class CompleteIfHeartsFull(wrapped: Action) : WrappedAction(wrapped) {
//    override fun complete(state: MapLocationState): Boolean =
//        state.frameState.inventory.heartCalc.full(state) || super.complete(state)
//}

fun makeUsePotionAction(): Action =
    OrderedActionSequence(listOf(
        SwitchToItemConditionally(inventoryPosition = Inventory.Selected.potion),
        GoIn(20, GamePad.MoveUp),        // use it
        GoIn(20, GamePad.B),        // use it
        GoIn(20, GamePad.MoveUp),        // use it
        GoIn(20, GamePad.B),        // use it
//        SwitchToItemConditionally(inventoryPosition = Inventory.Selected.potion),
        GoIn(50, GamePad.None), // wait until full
    ), tag = "use potion", shouldComplete = true)


class UsePotionW(wrapped: Action) : WrappedAction(wrapped) {
    private val usePotion = makeUsePotionAction()

    override fun complete(state: MapLocationState): Boolean =
        !PotionUsageReasoner.shouldUsePotion(state.frameState) || wrapped.complete(state)

    // go
    override fun nextStep(state: MapLocationState): GamePad {
        val should = PotionUsageReasoner.shouldUsePotion(state.frameState)
        return if (should) {
            d { "!!need potion!! should use ${should}"}
            usePotion.nextStep(state)
        } else {
            d { "!!need potion!! no need"}
            wrapped.nextStep(state)
        }
    }
}

class UsePotion : Action {
    private val usePotion = makeUsePotionAction()

    override fun complete(state: MapLocationState): Boolean =
//        state.frameState.inventory.heartCalc.full(state)
        (!PotionUsageReasoner.shouldUsePotion(state.frameState) || usePotion.complete(state)).also {
            d { " is complete potion $it ${PotionUsageReasoner.shouldUsePotion(state.frameState)} ${usePotion.complete(state)}"}
        }

    override fun nextStep(state: MapLocationState): GamePad {
        d { " use potion use potion:    ${PotionUsageReasoner.shouldUsePotion(state.frameState)}"}
        if (complete(state)) {
            return GamePad.None
        }
        return usePotion.nextStep(state)
    }
}

object PotionUsageReasoner {
    var diversionNeed: DiversionState = DiversionState.noNeed

    fun shouldUsePotion(state: FrameState): Boolean {
        val damage = state.inventory.heartCalc.damageInHearts()
        val almostDead by lazy { state.inventory.heartCalc.lifeInHearts() <= 8.25f }
        val full by lazy { state.inventory.heartCalc.full(state) }
//        val full = false
        val haveEnough by lazy { state.inventory.heartCalc.heartContainers() >= 8 }
        val havePotion by lazy { state.inventory.hasPotion }
        val isLevel = state.isLevel
        val message = when {
            almostDead -> "almost dead"
            havePotion -> "have potion"
            isLevel -> "in level"
            else -> ""
        }
        d { " life $message ${state.inventory.heartCalc.lifeInHearts()} d:$damage $almostDead $havePotion"}

//        if (haveEnough && almostDead && havePotion) {
//            d { "!!need potion!!"}
//            diversionNeed = DiversionState.usePotion
//        } else {
//            goGet(state)
//        }

        return isLevel && !full && havePotion
    }

    fun usePotion() {
        // remember menu selection
        // open menu
        // select potion
        // use potion
        // restore menu selection
    }

    fun goGet(state: FrameState) {
        val reason = when {
            state.isLevel -> "can't get potion from level"
            state.inventory.hasFullPotion -> "already have potion"
            !state.inventory.hasLetter -> "need letter"
            else -> ""
        }

        if (reason.isNotEmpty()) {
            d { " dont get potion because $reason" }
            diversionNeed = DiversionState.noNeed
        } else {
            return
        }

        // might need to go get $$ for other reasons

        val rupeesNeeded = when {
            state.inventory.hasPotion -> 48
            else -> 68
        }

        diversionNeed = if (state.inventory.numRupees < rupeesNeeded) {
            d { " need to gather some rupees! "}
            // go towards level 2, until have enough rupees
            DiversionState.needRupees
        } else {
            // go towards nearest potion location, and buy it!
            DiversionState.getPotion
        }
    }
}