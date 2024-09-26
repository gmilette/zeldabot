package bot.state

import bot.plan.action.LootKnowledge
import bot.state.map.MapConstants
import bot.state.map.destination.ZeldaItem
import bot.state.map.destination.zeldaItemsRequired
import nintaco.api.API
import nintaco.api.ApiSource
import util.d

data class FrameState(
    val api: API = ApiSource.getAPI(),
    val enemies: List<Agent>,
    val enemiesUncombined: List<Agent>,
    val enemiesRaw: List<Agent>,
    val level: Int,
    val mapLoc: MapLoc,
    val link: Agent,
    val ladder: Agent?,
    val seenBoomerang: Boolean,
    val inventory: Inventory = Inventory(api)
) {
    val numBombs: Int = inventory.numBombs
    val life: Double = inventory.heartCalc.lifeInHearts()
    val damageNumber: Int = inventory.heartCalc.damageNumber()

    /**
     *  0=Title/transitory    1=Selection Screen
    5=Normal              6=Preparing Scroll
    7=Scrolling           4=Finishing Scroll;
    E=Registration        F=Elimination
     */
    val gameMode: Int by lazy { api.readCPU(Addresses.gameMode) }
    val tenth: Int by lazy { api.readCPU(Addresses.tenthEnemyCount) }
    val clockActivated: Boolean by lazy { api.readCpuB(Addresses.clockActivated) }
    private val swordUseCountdown: Int by lazy { api.readCPU(Addresses.swordUseCountdown) }

    val isLevel = level != MapConstants.overworld

    val isDead: Boolean
        get() = gameMode == 8

    val canUseSword: Boolean = swordUseCountdown == 0
    val isScrolling: Boolean
        get() = gameMode == 7 || gameMode == 6 || gameMode == 4

    val isDoneScrolling: Boolean
        get() = gameMode == 4

    val ladderDeployed: Boolean
        get() = ladder != null && ladder.point.y >= 0 //on selection screen it is above normal items

    fun enemiesClosestToLink(stateOfEnemy: EnemyState = EnemyState.Alive): List<Agent> {
        return enemies.filter {
            it.state == stateOfEnemy
        }.sortedBy {
            it.point.distTo(link.point)
        }
    }

    fun heartsClosestToLink(): List<Agent> {
        return enemies.filter {
            it.state == EnemyState.Loot && it.tile in LootKnowledge.heartSet
        }.sortedBy {
            it.point.distTo(link.point)
        }
    }

    fun logEnemies() {
        d { " remaining enemies: ${enemies.size}" }
        for (enemy in enemiesSorted) {
            d { " remaining enemy $enemy" }
        }
    }

    fun logAliveEnemies() {
        d { " remaining enemies: ${enemies.size}" }
        for (enemy in enemiesSorted.filter { it.state == EnemyState.Alive }) {
            d { " remaining enemy $enemy" }
        }
    }

    val enemiesSorted: List<Agent>
        get() = enemies.sortedBy { it.point.distTo(link.point) }

    fun withEnemyAt(pt: FramePoint): FrameState {
        return this.copy(enemies = enemies.toMutableList() + Agent(
            0, pt
        ))
    }
}

private fun API.readCpuB(address: Int): Boolean =
    readCPU(address) != 0

data class Inventory(
    val api: API = ApiSource.getAPI()
) {
    val selectedItem by lazy { api.readCPU(Addresses.selectedItem) }
    val numBombs by lazy { api.readCPU(Addresses.numBombs) }
    val numRupees by lazy { api.readCPU(Addresses.numRupees) }
    val numKeys by lazy { api.readCPU(Addresses.numKeys) }
    val hearts by lazy { api.readCPU(Addresses.heartContainers) }
    val damage by lazy { api.readCPU(Addresses.heartContainersHalf) }

    val inventoryItems = InventoryItems(api)
    val heartCalc = HeartsStateCalculator(this)
    val items: Set<ZeldaItem> by lazy { InventoryReader.readInventory(api) }

    val hasPotion: Boolean
        get() = items.contains(ZeldaItem.Potion) || items.contains(ZeldaItem.SecondPotion)

    val hasFullPotion: Boolean
        get() = items.contains(ZeldaItem.SecondPotion)

    val hasLetter: Boolean
        get() = items.contains(ZeldaItem.Letter)

    val percentComplete: Float
        get() = items.size.toFloat() / zeldaItemsRequired.size.toFloat()

    val missingItems: Set<ZeldaItem>
        get() = zeldaItemsRequired - items

    val acquiredItems: Set<ZeldaItem>
        get() = zeldaItemsRequired.intersect(items)

    val hasCandle: Boolean
        get() = items.contains(ZeldaItem.BlueCandle) || items.contains((ZeldaItem.RedCandle))

    object Selected {
        val boomerang = 0
        val bomb = 1
        val arrow = 2
        val candle = 4
        val whistle = 5
        val bait = 6 //?
        val potion = 7
        val letter = 15
        val wand = 8 //?
    }
}


