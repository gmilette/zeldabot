package bot.plan.action

import bot.state.*
import sequence.ZeldaItem
import util.d
import kotlin.random.Random


class SwitchToItem(private val inventoryPosition: Int = Inventory.Selected.candle) : Action {

    private var pressCountdown = 0
    private var last: GamePad = GamePad.None

    override fun complete(state: MapLocationState): Boolean =
        selectedItem(state)// && pressCountdown <= 0

    private fun selectedItem(state: MapLocationState): Boolean =
        state.frameState.inventory.selectedItem == inventoryPosition // how does this map

    private fun directionToSelection(state: MapLocationState): GamePad =
        if (state.frameState.inventory.selectedItem < inventoryPosition) {
            GamePad.MoveRight
        } else {
            GamePad.MoveLeft
        }

    override fun nextStep(state: MapLocationState): GamePad {
        d {
            " selecting item selected=${selectedItem(state)} state=${
                state.frameState.inventory
                    .selectedItem
            }"
        }
        // this is weird, link cannot just keep pressing left for right, it has to
        // press none inbetween sometimes. Whatever this works
        return if (selectedItem(state) || Random.nextBoolean()) {
            GamePad.None
        } else {
            directionToSelection(state)
        }
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun path(): List<FramePoint> {
        return emptyList()
    }

    override val name: String
        get() = "SwitchToItem to ${inventoryPosition}"
}

class SwitchToItemConditionally(private val inventoryPosition: Int = Inventory.Selected.candle) : Action {
    private val switchSequence = mutableListOf(
        GoIn(2, GamePad.Start),
        GoIn(30, GamePad.None),
        SwitchToItem(inventoryPosition),
        GoIn(100, GamePad.None),
        GoIn(2, GamePad.Start),
        GoIn(30, GamePad.None),
    )

    private val positionShoot = OrderedActionSequence(switchSequence)


    override fun complete(state: MapLocationState): Boolean {
        return positionShoot.done || startedWithItem
    }

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    private var firstStep = true
    private var startedWithItem = false

    override fun nextStep(state: MapLocationState): GamePad {
        d {"SwitchToItemConditionally"}
        return if (firstStep && state.frameState.inventory.selectedItem == inventoryPosition) {
            d {"Already have"}
            startedWithItem = true
            GamePad.None
        } else {
            firstStep = false
            positionShoot.nextStep(state)
        }
    }

    override val name: String
        get() = "SwitchToItemConditionally ${inventoryPosition}"
}

class UseItem(private val usedTimes: Int = 5) : Action {
    private var used = 3
    override fun complete(state: MapLocationState): Boolean =
        used > usedTimes

    override fun nextStep(state: MapLocationState): GamePad {
        return GamePad.B.also {
            used++
        }
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun path(): List<FramePoint> {
        return emptyList()
    }

    override val name: String
        get() = "Use item $used"
}

class WaitForOldWoman : Action {
    override fun complete(state: MapLocationState): Boolean {
        return state.frameState.hasOldWoman
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return GamePad.None
    }

    override val name: String
        get() = "WaitForOldWoman"
}

private val FrameState.hasOldWoman
    get() = enemies.any { it.tile == oldWoman}

private val FrameState.hasPotion
    get() = enemies.any { it.tile == potion}


// any time enter shop add this action just in case
class ShowLetterConditionally : Action {
    private val switchSequence = mutableListOf(
        GoIn(120, GamePad.MoveUp),
        GoIn(50, GamePad.None),
//        WaitForPotionOrOldWoman(),
        WaitForOldWoman(),
//        SwitchToItem(Inventory.Selected.potionletter),
        SwitchToItemConditionally(Inventory.Selected.letter),
//        GoIn(50, GamePad.None),
        UseItem(),
        // wait for the lady to display the potions
        // need these both for some reason
        GoIn(250, GamePad.None),
        GoIn(100, GamePad.None),
    )

    private var frameCt = 0

    private val sequence = OrderedActionSequence(switchSequence, restartWhenDone = false)

    override fun complete(state: MapLocationState): Boolean {
        // if there is a potion in the inventory skip,
        // if there is just a letter then we need to show it I think
        return (sequence.done) || (state.frameState.inventory.hasPotion)
    }

    override fun target(): FramePoint {
        return sequence.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {"ShowLetterConditionally"}
        frameCt++
        return sequence.nextStep(state)
     }

    override val name: String
        get() = "ShowLetterConditionally current: ${sequence.currentAction?.name ?: "no action"}"
}