package bot.plan.action

import bot.state.*
import bot.state.map.MapConstants
import bot.state.map.toGamePad
import bot.state.oam.oldWoman
import bot.state.oam.potion
import nintaco.api.API
import nintaco.api.ApiSource
import util.d
import kotlin.random.Random


class SwitchToItem(private val inventoryPosition: () -> Int = { Inventory.Selected.candle }) : Action {

    private var pressCountdown = 0
    private var last: GamePad = GamePad.None

    override fun complete(state: MapLocationState): Boolean =
        selectedItem(state)// && pressCountdown <= 0

    private fun selectedItem(state: MapLocationState): Boolean =
        state.frameState.inventory.selectedItem == inventoryPosition() // how does this map

    private fun directionToSelection(state: MapLocationState): GamePad {
        // doesn't work if you have the letter
        val item = state.frameState.inventory.selectedItem

        // for purposes of determining which way to move the cursor
        // use the potion position
        val selected = if (item == Inventory.Selected.letter) {
            Inventory.Selected.potion
        } else {
            item
        }

        return  if (selected < inventoryPosition()) {
            GamePad.MoveRight
        } else {
            GamePad.MoveLeft
        }
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {
            " selecting item selected=${selectedItem(state)} state=${
                state.frameState.inventory
                    .selectedItem
            } try to select ${inventoryPosition()}"
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
        get() = "SwitchToItem to ${inventoryPosition()}"
}

class SwitchToItemConditionally(private val inventoryPosition: () -> Int = { Inventory.Selected.candle }) : Action {
    constructor(inventory: Int) : this({ inventory })

    private val switchSequence = mutableListOf(
        // to do repeat pressing start then waiting
//        GoIn(2, GamePad.Start),
//        GoIn(30, GamePad.None),
        OnceAction(HitStartUntilItemScreenChanges()),
        OnceAction(WaitUntilDisplayingInventory()),
        SwitchToItem(inventoryPosition),
        GoIn(100, GamePad.None),
        GoIn(2, GamePad.Start),
        GoIn(20, GamePad.None), // was 10
        OnceAction(WaitUntilItemMenuClosed()),
        // sometimes is just skip the killall
        GoIn(10, GamePad.None),
    )

    private val positionShoot = OrderedActionSequence(switchSequence, restartWhenDone = false)

    override fun complete(state: MapLocationState): Boolean {
        return (positionShoot.done || startedWithItem || !haveItem).also { d { "SwitchToItemConditionally complete $it" } }
    }

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    private var firstStep = true
    private var startedWithItem = false
    private var haveItem = true

    override fun nextStep(state: MapLocationState): GamePad {
        d {"SwitchToItemConditionallyZ selected: ${state.frameState.inventory.selectedItem} try to select ${inventoryPosition()} complete ${complete(state)} positionShoot ${positionShoot.stepName}"}
        return if (firstStep && state.frameState.inventory.selectedItem == inventoryPosition()) {
            d {"Already have"}
            startedWithItem = true
            GamePad.None
        } else if (firstStep && !haveItem(state)) {
            haveItem = false
            GamePad.None
        } else {
            firstStep = false
            positionShoot.nextStep(state)
        }
    }

    private fun haveItem(state: MapLocationState): Boolean =
        inventoryPositionToHave(inventoryPosition(), state)

    private fun inventoryPositionToName(position: Int): String =
        when (position) {
            0 -> "Boomerang"
            1 -> "Bomb"
            2 -> "Arrow"
            4 -> "Candle"
            5 -> "Whistle"
            6 -> "Bait"
            7 -> "Potion"
            15 -> "letter"
            8 -> "Wand"
            else -> position.toString()
        }

    private fun inventoryPositionToHave(position: Int, state: MapLocationState): Boolean =
        // doesn't need to have all the potions really
        when (position) {
            0 -> state.frameState.inventory.hasBoomerang
            1 -> true // state.frameState.inventory.numBombs > 0
            2 -> true
            4 -> true // state.frameState.inventory.hasCandle
            5 -> true
            6 -> true
            7 -> state.frameState.inventory.hasPotion
            15 -> true //state.frameState.inventory.hasLetter
            8 -> state.frameState.inventory.hasWand
            else -> true
        }

    override val name: String
        get() = "SwitchToItemConditionally to ${inventoryPositionToName(inventoryPosition())}"
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
    get() = enemies.any { it.tile == oldWoman }

class OnceAction(action: Action) : WrappedAction(action) {
    private var completed = false

    override fun complete(state: MapLocationState): Boolean {
        completed = completed || wrapped.complete(state)
        d { "OnceAction complete ${wrapped.name} $completed"}
        return completed
    }
}

class WaitUntilItemMenuClosed : Action {
    override fun complete(state: MapLocationState): Boolean {
        // waiting for it to go to zero i think
        return !state.frameState.inventory.isItemMenuOpenOrOpening.also {
            if (!it) {
                d { " waiting item menu to close current itemMenu = ${state.frameState.inventory.itemMenu} ${state.framesOnScreen}"}
            } else {
                d { " it is closed! "}
            }
        }
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return GamePad.None
    }

    override val name: String
        get() = "WaitUntilItemMenuClosed"
}

class HitStartUntilItemScreenChanges : Action {
    private var frames = 0
    override fun complete(state: MapLocationState): Boolean {
        return state.frameState.inventory.isItemMenuChanging
    }

    override fun nextStep(state: MapLocationState): GamePad {
        frames++
        return if (state.frameState.inventory.isItemMenuChanging) {
            d { " hit start until not changing"}
            GamePad.None
        } else {
            val r = frames % 10
            if (r < 3) {
                d { " hit start until START" }
                GamePad.Start
            } else if (r < 6) {
                GamePad.None
            } else if (r < 9) {
                d { " hit start until random"}
                state.bestDirection().toGamePad()
            } else {
                GamePad.None
            }
        }
    }

    override val name: String
        get() = "HitStartUntilItemScreenChanges"
}

class WaitUntilDisplayingInventory : Action {
    override fun complete(state: MapLocationState): Boolean {
        return state.frameState.inventory.isItemMenuOpen
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return GamePad.None
    }

    override val name: String
        get() = "WaitUntilDisplayingInventory"
}

class WaitUntilDisplayingInventoryByPixels : Action {
    private val allPixels by lazy { IntArray(256 * 240) }

    private fun isShowingSelectInventory(): Boolean {
        // somehow get this value via API
        ApiSource.getAPI().getPixels(allPixels)
        val count = allPixels.count { it == 0 }
        d { " showing count $count"}
        return count < 60 // was == 40
    }

    override fun complete(state: MapLocationState): Boolean {
        return isShowingSelectInventory().also {
            if (!it) {
                d { " waiting to show inventory "}
            }
        }
    }

    override fun nextStep(state: MapLocationState): GamePad {
        // maybe hit start while waiting
        return GamePad.None
    }

    override val name: String
        get() = "WaitUntilDisplayingInventory"
}

private val FrameState.hasPotion
    get() = enemies.any { it.tile == potion }


// any time enter shop add this action just in case
class ShowLetterConditionally : Action {
    private val switchSequence = mutableListOf(
        GoInConsume(MapConstants.twoGrid, GamePad.MoveUp),
        GoIn(50, GamePad.None),
//        WaitForPotionOrOldWoman(),
        WaitForOldWoman(),
//        SwitchToItem(Inventory.Selected.potionletter),
        SwitchToItemConditionally(Inventory.Selected.letter),
        // the ait until the menu item is closed doesn't work here
        // so do regular wait
        GoIn(100, GamePad.None),
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