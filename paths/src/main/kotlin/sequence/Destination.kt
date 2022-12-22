package sequence

object Dest {
    val levels = listOf(
        DestType.LEVEL(1),
        DestType.LEVEL(2),
        DestType.LEVEL(3),
        DestType.LEVEL(4),
        DestType.LEVEL(5),
        DestType.LEVEL(6),
        DestType.LEVEL(7),
        DestType.LEVEL(8),
        DestType.LEVEL(9, EntryType.Bomb)
    )

    fun level(number: Int) = levels[number - 1]

    class Items {
        val heartBomb = DestType.ITEM(ZeldaItem.BombHeart)
        val secret20Bomb = DestType.ITEM(ZeldaItem.BombHeart)
    }

    val itemLookup = mutableMapOf<ZeldaItem, DestType>().also { map ->
        ZeldaItem.values().forEach {
            map[it] = DestType.ITEM(it)
        }
    }

    fun item(item: ZeldaItem): DestType =
        itemLookup[item] ?: DestType.ITEM(ZeldaItem.None)

    class Secrets {
        val bomb20 = DestType.SECRET_TO_EVERYBODY(20, entry = EntryType.Bomb)
        val walk100 = DestType.SECRET_TO_EVERYBODY(100, entry = EntryType.Bomb)
    }

//    2nd Potion
}

sealed class DestType {
    data class LEVEL(val which: Int, val entry: EntryType = EntryType.Walk) :
        DestType()
    data class ITEM(val name: ZeldaItem, val entry: EntryType = EntryType.Walk)
        : DestType()
    data class SHOP(val shopType: ShopType = ShopType.C, val entry: EntryType
    = EntryType.Walk) : DestType()
    object WOMAN : DestType()
    object MONEY_GAME : DestType()
    object Fairy : DestType()
    object Travel : DestType()
    data class SECRET_TO_EVERYBODY(
        val
        rupees: Int,
        val
        entry: EntryType = EntryType.Walk, val name: String = "secret_$rupees"
    ) : DestType()
}

enum class ShopType {
    A, B, C, Woman, BlueRing
}

enum class EntryType {
    Walk, Fire, Bomb, Statue, WhistleWalk
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