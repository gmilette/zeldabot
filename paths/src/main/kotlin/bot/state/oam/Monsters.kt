package bot.state.oam

import bot.state.Agent
import bot.state.map.Direction
import nintaco.util.BitUtil

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
    val name: String = "",
//    // sprite ids
//    val parts:Set<Int> = setOf(0xb6, 0xb4),
    // attributes that indicate damage
    val damaged:Map<Int, Set<Int>> = emptyMap(),
    val damagedA:Set<Int> = emptySet(),
    // attributes that mean its read, otherwise assume blue
    val red:Set<Int> = emptySet(),
    val blue:Set<Int> = emptySet(),
//    val colors:Set<Int> = emptySet(),
    // valid monster colors
    val color:Set<Int> = emptySet(),
    /**
     * the tiles representing the monster
     */
    val tile: Set<Int> = emptySet(),
    val affectedByBoomerang: Boolean = true,
    val overworld: Boolean = true
) {
    fun inL() = this.copy(overworld = false)
    fun immuneToB() = this.copy(affectedByBoomerang = false)
    operator fun plus(other: Monster): Monster {
        return Monster("${this.name}_${other.name}",
            tile = this.tile + other.tile,
            color = this.color + other.color,
            affectedByBoomerang = this.affectedByBoomerang || other.affectedByBoomerang,
            overworld = this.overworld
        )
    }
}


val waterMonster = Monster(tile = setOf(0x0EE0, 0x0EC0))

data class DirectionMap(
    val up: Set<Int> = setOf(),
    val down: Set<Int> = setOf(),
    val left: Set<Int> = setOf(),
    val right: Set<Int> = setOf(),
) {
    val all = up + down + left + right

    fun dirFront(agent: Agent): Direction? = dirFront(agent.tile)

    fun inAny(tile: Int): Boolean = tile in all

    fun dirFront(tile: Int): Direction? =
        when (tile) {
            in up -> Direction.Up
            in down -> Direction.Down
            in left -> Direction.Left
            in right -> Direction.Right
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

object MonstersOverworld {
    val armos = Monster(name = "statue")
    val leever = Monster(name = "undergroundguy")
    val lynel = Monster(name = "swordshooter")
    val octorok = Monster(name = "overworldgrunt")
    val tektite = Monster(name = "spider")
    val ghini = Monster(name = "worldghost")
    val moblin = Monster(name = "arrowguy")
    val peahat = Monster(name = "spin",
        tile = setOf())
    val zora = Monster(name = "waterguy",
        tile = setOf(0xbc, 0xbe, 0xEC, 0xEE)
    ).immuneToB()
}

//val swordDir = DirectionMap(
//    up = setOf(0xb4, //02, 42
//        0xb6), //42x
//    down = setOf(0xac, 0xb0, 0xB2), //02
//    left = setOf(0xbe, //42
////        0xBC, // was left
//        0xba
//    ), //42
//    right = setOf(0xB8, //02
//        0xBC, // was left
//        0xBA), //02
//)

object MonsterColor {
    val blue = 1
    val red = 2
    val grey = 3
    val other = 0

    fun name(color: Int): String = when (color) {
        grey -> "G"
        blue -> "B"
        red -> "R"
        else -> "O"
    }
}
//enum class MonsterColor(color: Int, colorT: Int) {
//    Blue(1, 20), //01 //
//    Red(2, 24),  //10
//    Other(0, 16),//00
//    Grey(3, 28)  //11 // like squishy
//}

object Monsters {
    val levelsWithBoomerangAndSword = setOf(7) // 7 also has swords
    val levelsWithBoomerang = setOf(1, 2) // 7 also has swords
    val levelsWithWizzrobes = setOf(6, 9)
    val levelsWithNotSword = levelsWithWizzrobes + levelsWithBoomerang

    fun damaged(tileAttribute: TileAttribute) =
        lookup[tileAttribute.tile]?.let {
            tileAttribute.color !in it.color
        } ?: false

//        lookup[tileAttribute.tile]?.let {
//            tileAttribute.attribute in it.damagedA
//        } ?: false

    fun MutableMap<Int, Monster>.add(monster: Monster): MutableMap<Int, Monster> {
        for (t in monster.tile) {
            this[t] = monster
        }
        return this
    }

    val boomerang = Monster()
    //6_56
    // also same as sword guy
    val wizzrobe = Monster(name = "ghost",
        color = blueAndRed,
        // missing some tiles?
        tile = setOf(0xb4, 0xb6, 0xb8, 0xba)).immuneToB().inL()
    // 3_105 red
    // 5_100 blue
    val darknut = Monster("swordguy",
        color = blueAndRed,
        // meed tp remove red
        damagedA = setOf(h0, h3, h64, h67),
        blue = setOf(h65, h1),
        red = setOf(h66, h2),
        // bc might not have a 3
        tile = setOf(0xbe, 0xb6, 0xBA, 0xb4, 0xac, 0xb0, 0xB8, 0xBC)).immuneToB().inL()
    val gel = Monster(name = "babysquixxshy").inL()
    val gibdoOrLikeLike = Monster(name = "mummy/pancake",
        tile = setOf(0xa6, 0xa4),
        color = blueAndRed, // red for the pancake
//        damagedA = h64No5() + h023(),
//        damaged = mapOf(
//            0xa4 to h123(),
//            0xa6 to h64())
    ).inL()
    val goriya = Monster(name = "boomerangguy",
        color = blueAndRed, // guess
        tile = setOf(0xbe, 0xb6, 0xBA, 0xb4, 0xac, 0xb0, 0xB8, 0xBC)).inL()
    val keese = Monster(name = "bat", tile=setOf(0x9c, 0x9a)).inL()
    val lanmoia = Monster(name = "eyeworm").inL()
//    val likelike = Monster(name = "pancake",
//        tile = setOf(0xa6, 0xa4),
//        color = BlueAndRed, // red for the pancake
//    ).inL()
    val manhandla = Monster(name = "star").immuneToB().inL()
    val moldorm = Monster(name = "cirleworm",
        tile = setOf(0x9E, 0xA0),
        color = blueAndRed).immuneToB().inL()
    val patra = Monster(name = "eyecircleAndRhino",
        tile = setOf(0xfe, 0xfc, 0xf8, 0xfa),
        color = blueAndRedAndGrey
    ).immuneToB().inL()
    val polsVoice = Monster(name = "bunny",
        tile = setOf(0x00)
    ).immuneToB().inL()
    val rope = Monster("fastWorm").inL()
    val stalfos = Monster("skeleton",
        color = setOf(MonsterColor.red),
        // 0,3,1 are damaged
    ).inL()
    val vire = Monster("batparent").inL()
    val wallmaster = Monster("grabby").inL()
    val zol = Monster("squishy",
        color = setOf(MonsterColor.grey),
        tile = setOf(0xaa, 0xa8)).inL()
    val zolAndStalfos = zol + stalfos

    val lookup: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(wizzrobe)
        .add(zolAndStalfos)
        .add(darknut)
        .add(gibdoOrLikeLike)
        .add(patra)
}

private val h0 = 0x00
private val h1 = 0x01
private val h2 = 0x02
private val h3 = 0x03
fun h123() = setOf(h0, h1, h2, h3)
fun h023() = setOf(h0, h2, h3)
private val red = setOf(MonsterColor.red)
private val blue = setOf(MonsterColor.blue)
private val blueAndRed = setOf(MonsterColor.blue, MonsterColor.red)
private val blueAndRedAndGrey = setOf(MonsterColor.blue, MonsterColor.red, MonsterColor.grey)

private val h67 = 0x43
private val h66 = 0x42
private val h65 = 0x41
private val h64 = 0x40
fun h64() = setOf(h64, h65, h66, h67)
fun h64No5() = setOf(h64, h66, h67)

typealias TileAttribute = Pair<Int, Int>
val TileAttribute.color: Int
    get() = attribute.monsterColor()

fun TileAttribute.toFlagString(): String =
    "x: $xFlip y: $yFlip color: $color"

val TileAttribute.xFlip: Boolean
    get() = BitUtil.getBitBool(attribute, 6)

val TileAttribute.yFlip: Boolean
    get() = BitUtil.getBitBool(attribute, 7)

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

fun Int.colorOf() =
    0x10 or ((this and 0x03) shl 2)

fun Int.monsterColor() =
    this and 0x03
