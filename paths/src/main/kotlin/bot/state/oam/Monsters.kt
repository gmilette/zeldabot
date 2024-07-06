package bot.state.oam

import bot.state.Agent
import bot.state.map.Direction

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

    fun dirFront(agent: Agent): Direction? =
        when {
            agent.tile in up -> Direction.Up
            agent.tile in down -> Direction.Down
            agent.tile in left -> Direction.Left
            agent.tile in right -> Direction.Right
            else -> null
        }
}

// also the boomerang guys
val swordDir = DirectionMap(
    up = setOf(0xb4, //02, 42
        0xb6), //42x
    down = setOf(0xac, 0xb0, 0xB2), //02
    left = setOf(0xbe, //42
//        0xBC, // was left
        0xba
    ), //42
    right = setOf(0xB8, //02
        0xBC, // was left
        0xBA), //02
)

data class DirectionMapGhosts(
    val up: Set<TileAttribute> = setOf(),
    val left: Set<TileAttribute> = setOf(),
    val rightOrDown: Set<TileAttribute> = setOf(),
) {
    val all = up + left + rightOrDown

    fun dirFront(agent: Agent): Direction? =
        when {
            agent.tileAttrib in up -> Direction.Up
            agent.tileAttrib in rightOrDown -> Direction.Right
            agent.tileAttrib in left -> Direction.Left
            else -> null
        }
}

private val one = 0x01
private val fone = 0x41
private val ftwo = 0x42

val ghostDir = DirectionMapGhosts(
    // could be diagonal too, but if diagonal, they aren't going to shoot
    up = setOf(
        0xbe to one, //b01
        0xbc to one//b41, b01
    ),
    // left can't be down
    left = setOf(
        0xb6 to fone, //b,r 41, 42y
        0xba to fone, //yellow, 42, yep, b41
        0xb8 to fone,
        0xb6 to ftwo, //b,r 41, 42y
        0xba to ftwo, //yellow, 42, yep, b41
        0xb8 to ftwo
    ), //attrib 41
    // left or right could be down!
    // right or down
    rightOrDown = setOf(
        0xb6 to one,
        0xba to one
    ), //02, 012
)

object Monsters {
    val boomerang = Monster(
        parts = setOf(0xb6, 0xb4),
        normal = setOf(),
        damaged = setOf(0x01, 0x43))
}

typealias TileAttribute = Pair<Int, Int>
val TileAttribute.tile: Int
    get() = first

val TileAttribute.attribute: Int
    get() = second

fun TileAttribute.matches(other: TileAttribute) =
    this.first == other.first && this.second == other.second

fun TileAttribute.toHex() =
    "${tile.toString(16)} (${attribute.toString(16)}) "

fun TileAttribute.toSmallString(): String = "${tile}_$attribute"

// need

object MonsterDirection {
    // boomerang guy has
    val damagedAttribute = setOf(1, 2, 3, 40, 43)
}