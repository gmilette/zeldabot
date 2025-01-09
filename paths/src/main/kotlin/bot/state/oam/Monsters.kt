package bot.state.oam

import bot.state.Agent
import bot.state.map.Direction
import bot.state.oam.Monsters.add
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
    // valid monster colors
    val color:Set<Int> = setOf(MonsterColor.red, MonsterColor.blue, MonsterColor.grey, MonsterColor.other),
    /**
     * the tiles representing the monster
     */
    val tile: Set<Int> = emptySet(),
    val affectedByBoomerang: Boolean = true,
    val arrowKillable: Boolean = false,
    val overworld: Boolean = true,
    val avoidFront: Boolean = false,
    val type: MutableMap<Int, MonsterItemsType> = mutableMapOf()
) {
    fun typeA(color: Int): Monster {
        this.type[color] = MonsterItemsType.A
        return this
    }
    fun typeB(color: Int): Monster {
        this.type[color] = MonsterItemsType.B
        return this
    }
    fun typeD(color: Int): Monster {
        this.type[color] = MonsterItemsType.D
        return this
    }
    fun typeD(): Monster {
        for (color in MonsterColor.all()) {
            this.type[color] = MonsterItemsType.D
        }
        return this
    }
//    fun typeB() = this.copy(type = MonsterItemsType.B)
//    fun typeC() = this.copy(type = MonsterItemsType.C)
//    fun typeD() = this.copy(type = MonsterItemsType.D)
    fun inL() = this.copy(overworld = false)
    fun immuneToB() = this.copy(affectedByBoomerang = false)
    fun arrowKillable() = this.copy(arrowKillable = true)
    fun avoidFront() = this.copy(avoidFront = true)
    operator fun plus(other: Monster): Monster {
        return Monster("${this.name}_${other.name}",
            tile = this.tile + other.tile,
            color = this.color + other.color,
            affectedByBoomerang = this.affectedByBoomerang || other.affectedByBoomerang,
            overworld = this.overworld,
            arrowKillable = this.arrowKillable || other.arrowKillable,
            avoidFront = this.avoidFront || other.avoidFront
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

object MonstersOverworld {
    val armos = Monster(name = "statue",
        tile = setOf(0xa0, 0xa4, 0xa6),
        // red
        )
    val leever = Monster(name = "undergroundguy",
        tile = setOf(0xc2, 0xc4), //0xc0 is underground, but it's not boomerangable yet
        color = blueAndRed)
    val lynel = Monster(name = "swordshooter",
        tile = setOf(0xD0, 0xD2, 0xD4, 0xD6, 0xD8, 0xCE), //, 0xCE), // not sure CE is there
        color = blueAndRed)
        .avoidFront()
        .typeD(MonsterColor.red)
        .typeD(MonsterColor.blue)
    val octorok = Monster(name = "overworldgrunt",
        tile = setOf(0xb2, 0xb4, 0xb6, 0xb8, 0xba, 0xb0),
        color = blueAndRed,
    ).typeA(MonsterColor.red).typeB(MonsterColor.blue)
    val tektite = Monster(name = "spider",
        tile = setOf(0xcc, 0xca),
        color = blueAndRed) // confirm
    val ghini = Monster(name = "worldghost")
    val moblin = Monster(name = "arrowguy",
        tile = setOf(0xf0, 0xf2, 0xf4, 0xf6, 0xf8, 0xfa, 0xfe, ), // need to double check
        color = redAndGrey // i dont think it can. be blue
    )
    val peahat = Monster(name = "spin",
        tile = setOf(0xc6),
        color = red).immuneToB()
    val zora = Monster(name = "waterguy",
        tile = setOf(0xbc, 0xbe, 0xEC, 0xEE)
    ).immuneToB()

    val all: List<Monster> = listOf(zora, peahat, moblin, ghini, tektite, octorok, lynel, leever, armos)

    val toHuntForBombs: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(octorok)
        .add(moblin)

    val toHuntForBombsTiles: Set<Int> = toHuntForBombs.values.flatMap { it.tile }.toSet()
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

    fun all() = listOf(1,2,3,4)
}

val coin = 0x01
enum class MonsterItemsType(
    val sequence: List<Int>
) {
    A(listOf(coin, heart, coin, fairy, coin, heart, heart, coin, coin, heart)),
    B(listOf(bomb, coin, clockTile, coin, heart, bomb, coin, bomb, heart, heart)),
    C(listOf(coin, heart, coin, bigCoin, heart, clockTile, coin, coin, coin, bigCoin)),
    D(listOf(heart, fairy, coin, heart, fairy, heart, heart, heart, coin, heart)), None(emptyList())
}

object Monsters {
    val levelsWithBoomerangAndSword = setOf(7)
    val levelsWithBoomerang = setOf(1, 2, 7)
    val levelsWithWizzrobes = setOf(6, 9)
    val overworldLevel = setOf(0)
    val levelsWithNotSword = levelsWithWizzrobes + levelsWithBoomerang

    fun damaged(level: Int, tileAttribute: TileAttribute) =
        lookup(level)[tileAttribute.tile]?.let {
            tileAttribute.color !in it.color
        } ?: false
//        lookup[tileAttribute.tile]?.let {
//            tileAttribute.color !in it.color
//        } ?: false

    private fun MutableMap<Int, Monster>.addAll(monsters: List<Monster>): MutableMap<Int, Monster> {
        this.add(*(monsters.toTypedArray()))
        return this
    }

    fun MutableMap<Int, Monster>.add(vararg monsters: Monster): MutableMap<Int, Monster> {
        for (monster in monsters) {
            for (t in monster.tile) {
                this[t] = monster
            }
        }
        return this
    }
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
        tile = setOf(0xbe, 0xb6, 0xBA, 0xb4, 0xac, 0xb0, 0xB8, 0xBC))
        .avoidFront()
        .immuneToB().inL()
    val gel = Monster(name = "babysqui  shy").inL()
    val gibdo = Monster(name = "mummy",
        tile = setOf(0xa6, 0xa4),
        color = blue
    ).inL()
    val goriya = Monster(name = "boomerangguy",
        color = blueAndRed, // guess
        tile = setOf(0xbe, 0xb6, 0xBA, 0xb4, 0xac, 0xb0, 0xB8, 0xBC)).inL()
    val keese = Monster(name = "bat", tile=setOf(0x9c, 0x9a),
        color = blueAndRed).inL() // 1 / 41
    val lanmoia = Monster(name = "eyeworm",
        color = blueAndRed).inL() // confirm
    val likelike = Monster(name = "pancake",
        tile = setOf(0xa6, 0xa4),
        color = red, // red for the pancake
    ).inL()
    val digdogger = Monster(name = "whistleenemy").inL()
    val dodongo = Monster("rhino",
        tile = setOf(0xf8, 0xf4, 0xf6,
            0xe0, 0xe2, 0xe8, 0xea,
            0xf2, 0xf8, 0xfe, 0xfa, 0xfc, ))
    val dragon = Monster(name = "dragon",
        tile = setOf(0xCA, 0xC2, 0xc8, 0xcc, 0xce, 0xc0, 0xdc, 0xda, 0xd4, 0xd2, 0xd8, 0xd0, 0xd6, 0xc6, 0xde),
        color = grey
    ).immuneToB().inL()
    val manhandla = Monster(name = "star").immuneToB().inL()
    val aquamentus = Monster(name = "dragon").immuneToB().inL().typeD()
    val ganon = Monster(name = "ganon",
        tile = setOf(
            0xD0, 0xD2, 0xD4, 0xd6, 0xd8, 0xda, 0xde,
            0xe0, 0xe2, 0xe4, 0xe6, 0xe8
        ),
        color = grey // always 3
    ).immuneToB().inL().typeD()
    val gleeok = Monster(name = "hydradragon",
        tile = setOf(0xCA, 0xC2, 0xc8, 0xcc, 0xce, 0xc0, 0xdc, 0xda, 0xd4, 0xd2, 0xd8, 0xd0, 0xd6, 0xc6, 0xde),
        color = grey
    ).immuneToB().inL().typeD()
    val gohma = Monster(name = "spider").immuneToB().inL().typeD()
    val moldorm = Monster(name = "cirleworm",
        tile = setOf(0x9E, 0xA0),
        color = blueAndRed).immuneToB().inL()
    val patra = Monster(name = "circleEnemy",
        tile = setOf(0xfe, 0xfc, //outside
            0xf8, 0xfa), // inside
        color = blueAndRedAndGrey
    ).immuneToB().inL()
    val polsVoice = Monster(name = "bunny",
        tile = setOf(0xa2, 0xa0),
        color = setOf(MonsterColor.other)
    ).inL().arrowKillable() // .immuneToB(), let us shoot boomerang too
    val rope = Monster("fastWorm",
        tile = setOf(0xa0, 0xa2, 0xa4, 0xa6),
        color = red).inL()
    val stalfos = Monster("skeleton",
        color = red,
        // 0,3,1 are damaged
    ).inL()
    val vire = Monster("batparent").inL()
    val wallmaster = Monster("grabby").inL()
    val zol = Monster("squishy",
        color = setOf(MonsterColor.grey),
        tile = setOf(0xaa, 0xa8)).inL()
    val darknutGroup = darknut + wizzrobe + goriya
    val zolAndStalfos = zol + stalfos
    val gibdoOrLikeLike = gibdo + likelike
    // It's all these!! ahhhhh
    val ropeOrPolsVoice = rope + polsVoice + gibdo + likelike

    val lookupBase: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(zolAndStalfos)
        .add(gibdoOrLikeLike)
        .add(patra)
        .add(MonstersOverworld.leever)
        .add(dragon)
//        .add(ropeOrPolsVoice)

    val lookupOverworld: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .addAll(MonstersOverworld.all)

    val lookup: Map<Int, Monster> = lookupBase + mutableMapOf<Int, Monster>()
        .add(darknutGroup)

    private val lookupLevel1: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(keese, gel, stalfos, goriya, wallmaster, aquamentus)

    private val lookupLevel2: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(keese, rope, gel, goriya, moldorm, dodongo)

    private val lookupLevel3: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(keese,zol,darknut,manhandla)

    private val lookupLevel4: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(keese, vire, likelike, zol, manhandla, gleeok)

    private val lookupLevel5: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(keese, zol, gibdo, polsVoice, darknut, dodongo, digdogger)

    private val lookupLevel6: Map<Int, Monster> = lookupBase + mutableMapOf<Int, Monster>()
        .add(wizzrobe, zol, vire, likelike, keese, gleeok, gohma) // bubble, blade

    private val lookupLevel7: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(goriya, keese, moldorm, rope, stalfos, wallmaster, digdogger, dodongo, aquamentus)

    private val lookupLevel8: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(darknut, gibdo, keese, polsVoice, zol, gohma, manhandla)

    private val lookupLevel9: Map<Int, Monster> = mutableMapOf<Int, Monster>()
        .add(gel, keese, lanmoia, likelike, vire, wizzrobe, zol, patra, ganon)

    fun lookup(level: Int): Map<Int, Monster> =
        when (level) {
            0 -> lookupOverworld
            1 -> lookupLevel1
            2 -> lookupLevel2
            3 -> lookupLevel3
            4 -> lookupLevel4
            5 -> lookupLevel5
            6 -> lookupLevel6
            7 -> lookupLevel7
            8 -> lookupLevel8
            9 -> lookupLevel9
            else -> lookup
        }
}

private val h0 = 0x00
private val h1 = 0x01
private val h2 = 0x02
private val h3 = 0x03
fun h123() = setOf(h0, h1, h2, h3)
fun h023() = setOf(h0, h2, h3)
private val typeA = MonsterItemsType.A
private val typeB = MonsterItemsType.B
private val typeC = MonsterItemsType.C
private val typeD = MonsterItemsType.D

private val grey = setOf(MonsterColor.grey)
private val red = setOf(MonsterColor.red)
private val blue = setOf(MonsterColor.blue)
private val blueAndRed = setOf(MonsterColor.blue, MonsterColor.red)
private val redAndGrey = setOf(MonsterColor.red, MonsterColor.grey)
private val blueAndRedAndGrey = setOf(MonsterColor.blue, MonsterColor.red, MonsterColor.grey)
private val unknown = setOf(MonsterColor.red, MonsterColor.blue, MonsterColor.grey, MonsterColor.other)

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

//val projectiles = setOf(
//    flame1,
//    flame2,
//    144, 142, // sun
//    40, orbProjectile, // ganons
//    arrowTipShotByEnemy,
//    arrowTipShotByEnemy2,
//    fire,
//    brownBoomerang, // but it is also an item to be gotten, not avoided, oy!
//    brownBoomerangSpin,
//    brownBoomerangSpinBendFacingUp,
//    trap, // trap,
//    dragon4FlamingHead,
//    spinCircleEnemy,
//    ghostProjectileUpDown,
//    ghostProjectileLeft1,
//    ghostProjectileLeft2,
//    rockProjectile.tile
//)

const val ghostProjectileUpDown = (0x7a).toInt() // 1 and 41, 02/42, down 81,c1, 83, c3, 82,c2
const val ghostProjectileLeft1 = (0x7e).toInt() // right is 03
const val ghostProjectileLeft2 = (0x7c).toInt() // right is 03
val rockProjectileTile = (0x9e).toInt()

object Projectiles {
    // list all the projectiles
    val bubble = Monster().unblockable()
    val bolder = Monster().unblockable()
    val arrow = Monster().byShield()
    val wizardWand = Monster(
        tile = setOf(ghostProjectileLeft1,
            ghostProjectileLeft2,
            ghostProjectileUpDown
        )
    ).byMagic()
    val rock = Monster(
        tile = setOf(rockProjectileTile)
    ).byShield()
    val trap = Monster().unblockable()
    val boomerang = Monster().byShield()
    //
    val orb = Monster().byMagic()
    // shot by dragon
    val dragonOrb = Monster().unblockable()
    // from candle
    val fire = Monster().unblockable()
}

fun Monster.unblockable(): Monster {
    return this
}

fun Monster.byShield(): Monster {
    return this
}

fun Monster.byMagic(): Monster {
    return this
}