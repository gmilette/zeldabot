package sequence

import bot.state.map.Direction
import bot.state.map.Objective

object Dest {
    val levels = listOf(
        DestType.Level(1),
        DestType.Level(2),
        DestType.Level(3),
        DestType.Level(4),
        DestType.Level(5),
        DestType.Level(6),
        DestType.Level(7, entry = EntryType.WhistleWalk),
        DestType.Level(8, entry = EntryType.Fire(from = Direction.Right)),
        DestType.Level(9, EntryType.Bomb)
    )

    fun level(number: Int) = levels[number - 1]

    class Items {
        val heartBomb = DestType.Item(ZeldaItem.BombHeart)
        val secret20Bomb = DestType.Item(ZeldaItem.BombHeart)
    }

    val itemLookup = mutableMapOf<ZeldaItem, DestType>().also { map ->
        ZeldaItem.values().forEach {
            map[it] = DestType.Item(it)
        }
    }

    fun item(item: ZeldaItem): DestType =
        itemLookup[item] ?: DestType.Item(ZeldaItem.None)

    object Secrets {
        val bomb20 = DestType.SecretToEverybody(20, entry = EntryType.Bomb)
        val walk100 = DestType.SecretToEverybody(100)
        val forest100South = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Down))
        val secretForest30NorthEast = DestType.SecretToEverybody(30, EntryType.Statue)
        val bombSecret30North  = DestType.SecretToEverybody(30, EntryType.Bomb)
        val bombHeartNorth = DestType.Heart(entry = EntryType.Bomb)
        val bombHeartSouth = DestType.Heart(entry = EntryType.Bomb)
        val fire100SouthBrown = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Left)) // or right
        val fire30GreenSouth = DestType.SecretToEverybody(30, EntryType.Fire(from = Direction.Left))
        val level2secret10 = DestType.SecretToEverybody(10, EntryType.Statue)
    }

    object Heart {
        val fireHeart = DestType.Heart(entry = EntryType.Fire(from = Direction.Up))
        val raftHeart = DestType.Heart(entry = EntryType.Walk())
        val ladderHeart = DestType.Heart(entry = EntryType.WalkIn)
    }

    object Shop {
        val blueRing = DestType.Shop(ShopType.BlueRing, EntryType.Statue)
        val candleShopMid = DestType.Shop(ShopType.C, EntryType.Walk())
        val arrowShop = DestType.Shop(ShopType.B, EntryType.Walk())
        val eastTreeShop = DestType.Shop(ShopType.C, EntryType.Fire(from = Direction.Left))
        val potionShopForest = DestType.Shop(ShopType.Potion, EntryType.Fire(from = Direction.Left))
        val potionShopWest = DestType.Shop(ShopType.Potion, EntryType.Walk())

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
    object None: DestType(name = "none")
    class Level(val which: Int, entry: EntryType = EntryType.Walk()) :
        DestType(entry, "level $which")
    class Item(val item: ZeldaItem, entry: EntryType = EntryType.Walk())
        : DestType(entry, "item ${item.name} by ${entry.name}")
    class Shop(val shopType: ShopType = ShopType.C, entry: EntryType = EntryType.Walk()) : DestType(entry,
        name = "Shop ${shopType.name}")
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
    data class Walk(val requireLetter: Boolean = false): EntryType("walk")
    object WalkIn: EntryType("walkIn")
    object Bomb: EntryType("bomb")
    data class Fire(val from: Direction = Direction.Down): EntryType("fire from $from")
    data class Push(val from: Direction = Direction.Up): EntryType("push from $from")
    object Statue: EntryType("statue")
    object WhistleWalk: EntryType("whistlewalk")
}

data class LevelData(
    val level: Int,
    val numKeysIn: Int,
    val numKeysNeeded: Int
)

object LevelsData {
    val levels = listOf(
        // total 20 keys needed only
        LevelData(
            level = 1,
            numKeysIn = 5,
            numKeysNeeded = 4 // only if use a bomb
        ),
        LevelData(
            level = 2,
            numKeysIn = 3,
            numKeysNeeded = 0 // no keys needed is confirmed
        ),
        LevelData(
            level = 3,
            numKeysIn = 4,
            numKeysNeeded = 1 // if you have bombs you can skip keys 2 // visual
        ),
        LevelData(
            level = 4,
            numKeysIn = 2,
            numKeysNeeded = 2 // visual
        ),
        LevelData(
            level = 5,
            numKeysIn = 2,
            numKeysNeeded = 3 //?
        ),
        LevelData(
            level = 6,
            numKeysIn = 3,
            numKeysNeeded = 4 //?
        ),
        LevelData(
            level = 7,
            numKeysIn = 4,
            numKeysNeeded = 3 //ish
        ),
        LevelData(
            level = 8,
            numKeysIn = 5,
            numKeysNeeded = 2 // 2 needed before getting the magic key
        ),
        LevelData(
            level = 9,
            numKeysIn = 1,
            numKeysNeeded = 0
        ),
    )
}

//todo if you got the white sword, do you still have the wooden?
val zeldaItemsRequired = setOf<ZeldaItem>(
    ZeldaItem.MagicSword,
    ZeldaItem.RedRing,
    ZeldaItem.MagicalBoomerang,
    ZeldaItem.BookOfMagic,
    ZeldaItem.SilverArrow,
    ZeldaItem.Bow,
    ZeldaItem.Raft,
    ZeldaItem.Ladder,
    ZeldaItem.Letter,
    ZeldaItem.RedCandle,
    ZeldaItem.Whistle,
    ZeldaItem.PowerBracelet,
    ZeldaItem.MagicKey,
    ZeldaItem.Potion,
    ZeldaItem.Wand
)

enum class ZeldaItem {
    None,
    // named
    BombHeart,
    RaftHeartEntry,
    RaftHeart,
    // built in
    WoodenSword,
    WhiteSword,
    MagicSword,
    MagicShield,
    BlueRing,
    RedRing,
    Boomerang,
    MagicalBoomerang,
    Bomb,
    BookOfMagic,
    Arrow,
    SilverArrow,
    Bow,
    Raft,
    Ladder,
    Letter,
    BlueCandle,
    RedCandle,
    Whistle,
    PowerBracelet,
    MagicKey,
    Food,
    Potion,
    SecondPotion,
    Wand
}