package bot.state.map.destination

import bot.state.MapLoc
import bot.state.map.Direction
import bot.state.map.Objective

object Dest {
    private val levels = listOf(
        DestType.Level(1),
        DestType.Level(2),
        DestType.Level(3),
        DestType.Level(4),
        DestType.Level(5),
        DestType.Level(6),
        DestType.Level(7, entry = EntryType.WhistleWalk),
        DestType.Level(8, entry = EntryType.Fire(from = Direction.Left)),
        DestType.Level(9, EntryType.Bomb())
    )

    fun level(number: Int) = levels[number - 1]

    private val itemLookup = mutableMapOf<ZeldaItem, DestType>().also { map ->
        ZeldaItem.values().forEach {
            map[it] = DestType.Item(it)
        }
    }

    fun item(item: ZeldaItem): DestType =
        itemLookup[item] ?: DestType.Item(ZeldaItem.None)

    object Fairy {
        private val greenForestMapLoc: MapLoc = 57
        private val brownForestMapLoc: MapLoc = 67

        val greenForest = DestType.Fairy(greenForestMapLoc)
        val brownForest = DestType.Fairy(brownForestMapLoc)
    }

    object SecretsNegative {
        val forest20NearStart = DestType.SecretToEverybody(20, EntryType.Fire(Direction.Down))
    }

    object Secrets {
        val bomb30Start = DestType.SecretToEverybody(30, entry = EntryType.Bomb())
        val bomb20 = DestType.SecretToEverybody(20, entry = EntryType.Bomb())
        // forest
        val walk100 = DestType.SecretToEverybody(100)
        val forest30NearDesertForest = DestType.SecretToEverybody(30, EntryType.Fire(Direction.Left))
        val forest10Mid = DestType.SecretToEverybody(10, EntryType.Fire(Direction.Left))
        val forest10Mid91 = DestType.SecretToEverybody(10, EntryType.Fire(Direction.Down))
        val forest100South = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Down))
        val secretForest30NorthEast = DestType.SecretToEverybody(30, EntryType.Statue)
        val bombSecret30North = DestType.SecretToEverybody(30, EntryType.Bomb())
        val bombSecret30SouthWest = DestType.SecretToEverybody(30, EntryType.Bomb())
        val forest10BurnBrown = DestType.SecretToEverybody(10, EntryType.Fire(from = Direction.Down))
        val fire100SouthBrown = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Right)) // or right
        val fire30GreenSouth = DestType.SecretToEverybody(30, EntryType.Fire(from = Direction.Left))
        val level2secret10 = DestType.SecretToEverybody(10, EntryType.Statue)
    }

    object Heart {
        val fireHeart = DestType.Heart(entry = EntryType.Fire(from = Direction.Up))
        val raftHeart = DestType.Heart(entry = EntryType.Walk())
        val ladderHeart = DestType.Heart(entry = EntryType.WalkInLadder)
        val bombHeartNorth = DestType.Heart(entry = EntryType.Bomb())
        val bombHeartSouth = DestType.Heart(entry = EntryType.Bomb())
    }

    object Shop {
        val blueRing = DestType.Shop(ShopType.BlueRing, EntryType.Statue)
        val candleShopMid = DestType.Shop(ShopType.C, EntryType.Walk())
        val candleShopEast = DestType.Shop(ShopType.B, EntryType.Walk())
        val arrowShop = DestType.Shop(ShopType.B, EntryType.Walk())
        val eastTreeShop = DestType.Shop(ShopType.C, EntryType.Fire(from = Direction.Left))
        val westTreeShopNearWater = DestType.Shop(ShopType.C, EntryType.Fire(from = Direction.Up))
        val potionShopForest = DestType.Shop(ShopType.Potion, EntryType.Fire(from = Direction.Right))
        val potionShopWest = DestType.Shop(ShopType.Potion, EntryType.Walk(requireLetter = true))
        val potionShopCornerNear1 = DestType.Shop(ShopType.Potion, EntryType.Bomb(requireLetter = true))
        val potionShopTop = DestType.Shop(ShopType.Potion, EntryType.Bomb(requireLetter = true))
        val potionShopLevel6 = DestType.Shop(ShopType.Potion, EntryType.Bomb(requireLetter = true))
        val potionShopLevel9 = DestType.Shop(ShopType.Potion, EntryType.Bomb(requireLetter = true))
        val potionShopNearStart = DestType.Shop(ShopType.Potion, EntryType.Fire(from = Direction.Down))

        object ItemLocs {
            val redPotion = Objective.ItemLoc.Right
            val bluePotion = Objective.ItemLoc.Left
            val magicShield = Objective.ItemLoc.Left
            val bait = Objective.ItemLoc.Right
//            val candle = Objective.ItemLoc.Right
//            val blueRing = Objective.ItemLoc.Right
//            val arrow = Objective.ItemLoc.Right
        }
    }

//    2nd Potion
}


sealed class DestType(val entry: EntryType = EntryType.Walk(), val name: String = "") {
    object None : DestType(name = "none")
    class Level(val which: Int, entry: EntryType = EntryType.Walk()) :
        DestType(entry, "level $which")

    class Item(val item: ZeldaItem, entry: EntryType = EntryType.Walk()) :
        DestType(entry, "item ${item.name} by ${entry.name}")

    class Shop(val shopType: ShopType = ShopType.C, entry: EntryType = EntryType.Walk()) : DestType(
        entry,
        name = "Shop ${shopType.name}"
    )

    object Woman : DestType()
    object MoneyGame : DestType()
    class Fairy(loc: MapLoc) : DestType(EntryType.WalkUp)
    object Travel : DestType()
    class Heart(entry: EntryType = EntryType.Walk()) : DestType(entry, "heart by ${entry.name}")
    class SecretToEverybody(
        rupees: Int = -1,
        entry: EntryType = EntryType.Walk(),
    ) : DestType(entry, "Secret $rupees by ${entry.name}")
    // when inside a level, goal is a triforce
    data class Triforce(val level: Int) : DestType(name = "Triforce")
    object Princess : DestType(name = "Princess")
}

enum class ShopType {
    A, B, C, Potion, BlueRing
}

sealed class EntryType(val name: String) {
    data class Walk(val requireLetter: Boolean = false) : EntryType("walk")
    object WalkUp : EntryType("walkUp")
    object WalkIn : EntryType("walkIn")
    // requires special handling because of the laddder
    object WalkInLadder : EntryType("walkInLadder")
    data class Bomb(val requireLetter: Boolean = false) : EntryType("bomb")
    data class Fire(val from: Direction = Direction.Down) : EntryType("fire from $from")
    data class Push(val from: Direction = Direction.Up) : EntryType("push from $from")
    object Statue : EntryType("statue")
    object WhistleWalk : EntryType("whistlewalk")
}
