package bot.state.map.destination

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
    Wand,
    Triforce
}

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
