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
        val bomb20 = DestType.SecretToEverybody(20, entry = EntryType.Bomb)
        val walk100 = DestType.SecretToEverybody(100)
        val forest100South = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Down))
        val secretForest30NorthEast = DestType.SecretToEverybody(30, EntryType.Statue)
        val bombSecret30North  = DestType.SecretToEverybody(30, EntryType.Bomb)
        val fire100SouthBrown = DestType.SecretToEverybody(100, EntryType.Fire(from = Direction.Left)) // or right
        val fire30GreenSouth = DestType.SecretToEverybody(30, EntryType.Fire(from = Direction.Left))
        val level2secret10 = DestType.SecretToEverybody(10, EntryType.Statue)
        val bombHeartNorth = DestType.Heart(entry = EntryType.Bomb)
        val bombHeartSouth = DestType.Heart(entry = EntryType.Bomb)
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
        // option: level 2 left squishy, or level 8 right grab(is it a grab?)
        // plan
        //2: 2 keys (skip snake?, skip left squishy)
        //1: 4 keys: use 4, get 6, (get all - could skip boomerang)
        //3: 5 keys: get 2, use 1
        //4: 5 keys: get 2, use 2 (initial squishies)
        //5: 4 keys: get 1, use 2 // don't waste getting all the keys, just the one inside the zombie
        //6: 0 key: get 0, use 4 (if you have 4 all good)
        //8: 0 keys: get 2, use 2 (forced one, go right grab)
        // total 20 keys needed only
        LevelData(
            level = 1,
            // 1: left start
            // 2: right start
            numKeysIn = 6,
            numKeysNeeded = 4 // only if use a bomb
        ),
        LevelData(
            level = 2,
            // 1: (mid, fast) right from start
            // 2: off path left
            // 3: (mid, fast)
            // 4: snake thing
            numKeysIn = 4, //visual 4 (3 if you skip snake)
            numKeysNeeded = 0 // no keys needed is confirmed
        ),
        // strategy, get first 2 keys, and up key for a total of 3
        LevelData(
            level = 3,
            numKeysIn = 4, // 2 not on path
            numKeysNeeded = 1 // if you have bombs you can skip 1 key// visual
        ),
        LevelData(
            level = 4,
            // 1: initial left, kill bats
            // 2: up 2, grab
            // 3: before water, kill squishies
            // 4: kill bats?, bomb up from coin
            numKeysIn = 4,
            numKeysNeeded = 2 // visual, confirmed
        ),
        LevelData(
            level = 5,
            // 1: (mid) kill right bunnies (? kill?
            // 2: (easy) inside mummy grab
            // 3: (high) have to kill zombie to get in, not worth it
            // 4: (mid) kill zombie
            // 5: (mid) rabbits, zombies
            // 6: (mid), zombies
            numKeysIn = 6,
            numKeysNeeded = 2 //visual
        ),
        LevelData(
            level = 6,
            // 1: (mid) start, ghosts
            // 2: (difficult, but no choice bats)
            // 3: (mid, hard) ghosts
            // 4: (mid, off path) ghosts
            numKeysIn = 4,
            numKeysNeeded = 4 //visual YES
        ),
        LevelData(
            level = 7,
            //1: (mid, off path): fight fast guys and fireball
            //2: (mid, waaay off path): Skeletons way beyond where we normally go
            //3: (mid, off path): fight boomerang guys and fireball
            numKeysIn = 3,
            // before bait
            // after bait (could skip by bombing and wasting time)
            // after red candle
            numKeysNeeded = 3
        ),
        LevelData(
            level = 8,
            // 1: grab? right from start
            // 2: (mid, no choice)
            // 3: far off near boss
            // 4: left from no choice, is it a grab?
            // 5: left, then left from no choice, kill sword guys
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