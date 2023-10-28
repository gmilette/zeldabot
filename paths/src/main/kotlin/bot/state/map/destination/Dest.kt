package bot.state.map.destination

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
        DestType.Level(9, EntryType.Bomb)
    )

    fun level(number: Int) = levels[number - 1]

    private val itemLookup = mutableMapOf<ZeldaItem, DestType>().also { map ->
        ZeldaItem.values().forEach {
            map[it] = DestType.Item(it)
        }
    }

    fun item(item: ZeldaItem): DestType =
        itemLookup[item] ?: DestType.Item(ZeldaItem.None)

    object Secrets {
        val bomb30Start = DestType.SecretToEverybody(30, entry = EntryType.Bomb)
        val bomb20 = DestType.SecretToEverybody(20, entry = EntryType.Bomb)
        val walk100 = DestType.SecretToEverybody(100)
        val forest100South = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Down))
        val secretForest30NorthEast = DestType.SecretToEverybody(30, EntryType.Statue)
        val bombSecret30North = DestType.SecretToEverybody(30, EntryType.Bomb)
        val fire100SouthBrown = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Right)) // or right
        val fire30GreenSouth = DestType.SecretToEverybody(30, EntryType.Fire(from = Direction.Left))
        val level2secret10 = DestType.SecretToEverybody(10, EntryType.Statue)
        val bombHeartNorth = DestType.Heart(entry = EntryType.Bomb)
        val bombHeartSouth = DestType.Heart(entry = EntryType.Bomb)
    }

    object Heart {
        val fireHeart = DestType.Heart(entry = EntryType.Fire(from = Direction.Up))
        val raftHeart = DestType.Heart(entry = EntryType.Walk())
        val ladderHeart = DestType.Heart(entry = EntryType.WalkInLadder)
    }

    object Shop {
        val blueRing = DestType.Shop(ShopType.BlueRing, EntryType.Statue)
        val candleShopMid = DestType.Shop(ShopType.C, EntryType.Walk())
        val arrowShop = DestType.Shop(ShopType.B, EntryType.Walk())
        val eastTreeShop = DestType.Shop(ShopType.C, EntryType.Fire(from = Direction.Left))
        val potionShopForest = DestType.Shop(ShopType.Potion, EntryType.Fire(from = Direction.Left))
        val potionShopWest = DestType.Shop(ShopType.Potion, EntryType.Walk(requireLetter = true))

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
    object Fairy : DestType()
    object Travel : DestType()
    class Heart(entry: EntryType = EntryType.Walk()) : DestType(entry, "heart by ${entry.name}")
    class SecretToEverybody(
        val rupees: Int = -1,
        entry: EntryType = EntryType.Walk(),
    ) : DestType(entry, "secret_$rupees by ${entry.name}")
}

enum class ShopType {
    A, B, C, Potion, BlueRing
}

sealed class EntryType(val name: String) {
    data class Walk(val requireLetter: Boolean = false) : EntryType("walk")
    object WalkIn : EntryType("walkIn")
    // requires special handling because of the laddder
    object WalkInLadder : EntryType("walkInLadder")
    object Bomb : EntryType("bomb")
    data class Fire(val from: Direction = Direction.Down) : EntryType("fire from $from")
    data class Push(val from: Direction = Direction.Up) : EntryType("push from $from")
    object Statue : EntryType("statue")
    object WhistleWalk : EntryType("whistlewalk")
}
