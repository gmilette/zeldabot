package bot.plan.action

import androidx.compose.ui.res.useResource
import bot.state.*
import bot.state.map.Objective
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

class SaveItemAction: Action {
    var currentItem: Int = -1
    override fun complete(state: MapLocationState): Boolean {
        currentItem = state.frameState.inventory.selectedItem
        d{ " potion save item $currentItem"}
        return true
    }
}

fun eitherPotion(): Action = completeIfPotionFull(eitherPoint(
    Objective.ItemLoc.Left.point,
    Objective.ItemLoc.Right.point
) { state ->
    state.frameState.inventory.hasHalfPotion.also {
        // just make sure you have enough rupees
        cheatPotion().nextStep(state)
    }
}
)

fun completeIfPotionFull(wrapped: Action): Action = CompleteIf(wrapped) {
    frameState.inventory.hasFullPotion
}

class CompleteIfGameModeNormal : Action {
    override fun complete(state: MapLocationState): Boolean =
        state.frameState.gameMode == 5

    override fun nextStep(state: MapLocationState): GamePad {
        d { " GAME MODE is ${state.frameState.gameMode}"}
        return super.nextStep(state)
    }
}

fun makeUsePotionAction(): OneTimeActionSequence {
    val save = SaveItemAction()
    return OneTimeActionSequence(
        listOf(
            save,
            // move inside level because you cannot use a potion in the entrance
            CompleteIf(goInward()) { frameState.link.point.isInLevelMap },
            SwitchToItemConditionally(Inventory.Selected.potion),
            // wait until the screen scrolls down, but not too long
//            CompleteIfGameModeNormal(),
            GoIn(80, GamePad.None),        // use it
            UseItem(),
            // i dont think we need this if inventory thing keeps pressing statr
//            GoIn(800, GamePad.None), //500 ok for 8
            SwitchToItemConditionally(inventoryPosition = { save.currentItem }),
            //SwitchToItemConditionally(inventoryPosition = { 0 }),
            GoIn(20, GamePad.None)
        ), tag = "use potion")
}

class UsePotion : Action {
    private val usePotion: OneTimeActionSequence = makeUsePotionAction()

    val done: Boolean
        get() = usePotion.done && usePotion.hasBegun

    val hasBegun: Boolean
        get() = usePotion.hasBegun

    // don't complete
    override fun complete(state: MapLocationState): Boolean =
        usePotion.complete(state) && done

    override fun nextStep(state: MapLocationState): GamePad {
        d { " potion nextstep potion: ${usePotion.complete(state)} done=${done}" }
        return if (complete(state)) {
            d { " potion complete "}
            GamePad.None
        } else {
            d { " use potion"}
            usePotion.nextStep(state)
        }
    }
}

object PotionUsageReasoner {
    val USE_POTION_HEART_LIMIT = 2.0
    val USE_POTION_MINIMIM_LIMIT = 7.0

    var diversionNeed: DiversionState = DiversionState.noNeed

    fun shouldUsePotion(state: FrameState): Boolean {
        val damage = state.inventory.heartCalc.damageInHearts()
        val heart = state.inventory.heartCalc.lessHearts()

        val almostDead = (state.inventory.heartCalc.heartContainers() - heart) <= USE_POTION_HEART_LIMIT
        //val almostDead by lazy { state.inventory.heartCalc.lifeInHearts() <= 8.25f }
        val full by lazy { state.inventory.heartCalc.full(state) }
//        val full = false
        val haveEnough by lazy { (state.inventory.heartCalc.heartContainers() - heart) > USE_POTION_MINIMIM_LIMIT }
        val havePotion by lazy { state.inventory.hasPotion }
        val isLevel = state.isLevel
        val message = when {
            almostDead -> "almost dead"
            havePotion -> "have potion"
            isLevel -> "in level"
            else -> ""
        }

//        d { " life $message ${state.inventory.heartCalc.toString()} d:$damage $almostDead $havePotion $full heart: $heart "}
//        state.inventory.heartCalc.logData()

        return isLevel && almostDead && havePotion
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