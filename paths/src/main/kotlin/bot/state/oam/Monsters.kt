package bot.state.oam

//18, 16, // link shield shite
//12, 14, // facing up link
//4, 6, 8, // link // 0
//0, 2, // facing left link
//88, 10, // mor elink
//90, // brown link small shield
//20, 22, // i think link or maybe movable block
//(0x80).toInt(), // link sword
//(0x82).toInt(), // link sword
//84, // link i think //(0x54).toInt(), // link with shield
//86, // link i think //(0x54).toInt(), // link with shield
//32, // link's sword
//96, // link again

// attribute 42 is hit, I think

data class Monster(
    // sprite ids
    val parts:Set<Int> = setOf(0xb6, 0xb4),
    // attributes while monster is in normal state
    val normal: Set<Int> = emptySet(),
    // attributes that indicate damage
    val damaged:Set<Int> = setOf(0x01, 0x43),
    val tile: Set<Int> = emptySet()
)

val waterMonster = Monster(tile = setOf(0x0EE0, 0x0EC0))

data class DirectionMap(
    val up: Set<Int> = setOf(),
    val down: Set<Int> = setOf(),
    val left: Set<Int> = setOf(),
    val right: Set<Int> = setOf(),
) {
    val all = up + down + left + right
}

val swordDir = DirectionMap(
    up = setOf(),
    down = setOf(0xB2), //02
    left = setOf(),
    right = setOf(0xBC, 0xBE), //be is tipattrib 02
)

object Monsters {
    val boomerang = Monster(
        parts = setOf(0xb6, 0xb4),
        normal = setOf(),
        damaged = setOf(0x01, 0x43))
}

typealias TileAttribute = Pair<Byte, Byte>
val TileAttribute.tile: Byte
    get() = first

val TileAttribute.attribute: Byte
    get() = second

// need

object MonsterDirection {
    // boomerang guy has
    val damagedAttribute = setOf(1, 2, 3, 40, 43)
}