package bot.state

import bot.state.map.Direction
import nintaco.api.API
import org.jheaps.annotations.VisibleForTesting
import util.d



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
    val parts:Set<Int> = setOf(0xb6, 0xb4),
    val damaged:Set<Int> = setOf(0x01, 0x43)
)

object Monsters {
    val boomerang = Monster(setOf(0xb6, 0xb4), setOf(0x01, 0x43))
}

// need

object MonsterDirection {
    // boomerang guy has
    val damagedAttribute = setOf(1, 2, 3, 40, 43)
}

object LinkDirection { //14,16 attrib 01
    val damagedAttribute = setOf(41, 42, 43)

    private val down = setOf(0x16, 0x14, 0x5A, 0x08, 0x5A, 0x58, 0x0A, 0x60) //5A, attribute 00 is down. 60, attribute 00, 08 attribute 40
    private val up = setOf(0x0C, 0x0E, 0x18, 0x1A) //43 was hit (dark color), //41 was hit light color
    //42 is hit
    private val right = setOf((0x02).toInt(), (0x00).toInt(), (0x06).toInt(), (0x04).toInt()) // attribute 00, could be 02 with attrib 00
    private val left = setOf((0x02).toInt(), (0x00).toInt(), (0x10).toInt(), (0x12).toInt()) // attribute 40 or 43

    private val downInt = down.map { it.toInt() }
    private val upInt = up.map { it.toInt() }
    private val rightInt = right.map { it.toInt() }
    private val leftInt = left.map { it.toInt() }

    data class DirectionDamage(val direction: Direction, val damaged: Boolean)

    fun direction(sprites: List<SpriteData>): DirectionDamage {
        val linkMatch = sprites.firstOrNull { it.toDir() != Direction.None }
        val dir = linkMatch?.toDir()
        val isDamaged = damagedAttribute.contains(linkMatch?.attribute)
        d { "link match $linkMatch $dir damaged $isDamaged"}
//        if (linkMatch == null) {
//            d { " sprites link!" }
//            sprites.forEachIndexed { index, sprite ->
//                d { "$index: $sprite ${LinkDirection.dirFor(sprite)}" }
//            }
//        }
        return DirectionDamage((dir ?: Direction.None), isDamaged)
    }

    fun dirFor(data: SpriteData): Direction = data.toDir()

    private fun SpriteData.toDir(): Direction =
        when {
            downInt.contains(tile) && attribute == 0 -> Direction.Down
            upInt.contains(tile) -> Direction.Up
            leftInt.contains(tile) ||
                rightInt.contains(tile) -> {
                if (attribute == 40 || attribute == 64 || attribute == 43) Direction.Left else Direction.Right
            }
            else -> Direction.None
//                .also {
//                d { " tile is null $tile ${leftInt} ${rightInt} $downInt $upInt"}
//            }
        }
}

class OamStateReasoner(
    private val api: API
) {
    private val sprites: List<SpriteData>

    var ladderSprite: Agent? = null
    var direction: Direction = Direction.None
    var damaged: Boolean = false

    init {
        sprites = readOam()
    }

    val DEBUG = true

    val alive: List<SpriteData>
        get() {
            return sprites.filter { !it.hidden }
        }

    val loot: List<SpriteData>
        get() {
            return sprites.filter { it.isLoot }
        }

    val allDead: Boolean
        get() = alive.isEmpty()

    fun agents(): List<Agent> =
        sprites.map { it.toAgent() }

    private fun SpriteData.toAgent(): Agent =
        Agent(index = tile, point = point, state = toState(), hp = tile, droppedId = attribute)

    //combine these
    //Debug: (Kermit)  enemy Agent(index=176, point=(128, 55), dir=None, state=Alive, countDown=0, hp=176, projectileState=NotProjectile, droppedId=1)
    //Debug: (Kermit)  enemy Agent(index=178, point=(136, 55), dir=None, state=Alive, countDown=0, hp=178, projectileState=NotProjectile, droppedId=1)

    @VisibleForTesting
    fun combine(toCombine: List<SpriteData>): List<SpriteData> {
        val xMap = toCombine.associateBy { it.point.x }

        // can delete, because there is a sprite 8pxs to left that is the same
        val toDelete = toCombine
            // keep all the projectiles because most are just small
//            .filter { !SpriteData.projectiles.contains(it.tile) }
            .filter {
                val matched = xMap[it.point.x - 8]
                // tiles do not always match
//                val delete = matched?.tile == it.tile && matched.point.y == it.point.y
                val delete = matched?.point?.y == it.point.y
                if (DEBUG && delete) {
                    d { "! delete ${matched?.point}, copy of ${it.point}" }
                }
                delete
        }

        val mutable = toCombine.toMutableList()
        for (spriteData in toDelete) {
            if (DEBUG) {
                d { "! remove $spriteData" }
            }
            mutable.remove(spriteData)
        }

        return mutable
    }

    private fun SpriteData.toState(): EnemyState =
        when {
            this.hidden -> EnemyState.Dead
            isLoot -> EnemyState.Loot
            isProjectile -> EnemyState.Projectile
            else -> EnemyState.Alive
        }

    // this isn't real
    // 21: SpriteData(index=21, point=(74, 23), tile=62, attribute=0, hidden=false)
    // there are always 2 sprites on for each enemy
    // one is at x, other is at x+8, same attribute
    // to translate to current coordinates
    // use the lower x value
    // then subtract 61 from the y, value

    private fun readOam(at: Int): SpriteData {
        val x = api.readOAM(at + 0x0003)
        val y = api.readOAM(at)
        val tile = api.readOAM(at + 0x0001)
        val attrib = api.readOAM(at + 0x0002)
        return SpriteData(at / 4, FramePoint(x, y - 61), tile, attrib)
    }

    private fun readOam(): List<SpriteData> {
        val spritesRaw = (0..63).map {
            readOam(0x0001 * (it * 4))
        }

        val dirDamage = LinkDirection.direction(spritesRaw)
        direction = dirDamage.direction
        damaged = dirDamage.damaged

        val ladders = spritesRaw.filter { it.tile == ladder }
        ladderSprite = if (ladders.isNotEmpty()) {
            val sp = if (ladders.size == 1) {
                ladders.first()
            } else {
                if (ladders[0].point.x < ladders[1].point.x) {
                    ladders[0]
                } else {
                    ladders[1]
                }
            }
            sp.toAgent()
        } else {
            null
        }

        d { " sprites** alive ** ${spritesRaw.filter { !it.hidden }.size} dir ${direction}" }
        // ahh there are twice as many sprites because each sprite is two big
        spritesRaw.filter { !it.hidden }.forEachIndexed { index, sprite ->
            d { "$index: $sprite" }
        }

        if (DEBUG) {
            d { " sprites" }
            spritesRaw.forEachIndexed { index, sprite ->
                d { "$index: $sprite ${LinkDirection.dirFor(sprite)}" }
            }
        }

        return combine(spritesRaw)
    }
}
fun Agent.isGannonTriforce(): Boolean =
    tile == triforceTile

data class SpriteData(
    val index: Int,
    val point: FramePoint,
    val tile: Int,
    val attribute: Int
    // or > 239
) {
    val tilePair = tile to attribute

    val hidden: Boolean = point.y >= 248 || attribute == 32 || ignore.contains(tile) ||
            ignorePairs.contains(tilePair) // does this work?
            //|| point.y < 60  dont need that because the y coordinate is adjusted
            //|| projectiles.contains(tile) //|| loot.contains(tile) // should be separate
            || ( (tile == 248 || tile == 250) && point.y == 187) // spinny guy
            // tile 52 is a bomb
            || ( (tile == 52) && point.y == 187) // could be just any 187 point should be considered dead
            || ( (tile == 142 || tile == 144) && point.y == 187)
            || ( (tile == 164) && point.y == 187)
            || point.y >= 187 // this keeps coming up, make sense ,because we translated it 61
            || point.y < 0
    val isLoot = !hidden && loot.contains(tile)

    val isProjectile = !hidden && projectiles.contains(tile)
    // it doesn't solve the pancake problem

    companion object {
        val ignore = setOf(
            18, 16, // link shield shite
            12, 14, // facing up link
            4, 6, 8, // link // 0
            0, 2, // facing left link
            88, 10, // mor elink
            90, // brown link small shield
            20, 22, // i think link or maybe movable block
            (0x80).toInt(), // link sword
            (0x82).toInt(), // link sword
            84, // link i think //(0x54).toInt(), // link with shield
            86, // link i think //(0x54).toInt(), // link with shield
            32, // link's sword
            96, // link again
            largeShield,
            (0x58).toInt(),
            (0x1A).toInt(), (0x18).toInt(), // link about to attack
            //
            (0x82).toInt(), // sword hilt
            (0x84).toInt(), // sword point
            90, // not sure what it is maybe link or his sword
            62, // blinking this
            48, // sword
            48, // swirly thing
            bombSmoke, // yea but then it attacks its own bomb// 112, // bomb smoke, removed it so I can attack the monsters
            114, // i think bomb smoke

            //164, // not sure, that is a pancake
            160,
            ladder,
            wizard,
            flame1,
            flame2,
            dragon4Body,
            dragon4BodyFoot,
            dragon4BodySpine,
//            triforceDirt,
            triforceDirt2,
//            oldWoman
//            dragonFeet,
//            dragonFeet2,
//            dragonTail,
//            dragonBody,
//            rhinoTail, // but it also the dragon's head
//            rhinoMid,
            // keep for now, but I have to make link not attack sometimes
//            secretEverybodyMonsterOrCircleEnemyLeft,
//            secretEverybodyMonsterOrCircleEnemyRight
        )
        val projectiles = setOf(
            124, 126, // ghost attack
            144, 142, // sun
            40, orbProjectile, // ganons
            arrowTipShotByEnemy,
            fire,
            brownBoomerang, // but it is also an item to be gotten, not avoided, oy!
            (0x96).toInt(), // trap,
            boulder, boulder2, boulder3, boulder4,
            dragon4FlamingHead,
            spinCircleEnemy
        )

        val largeProjectiles = setOf(
            124, 126, // ghost attack
            144, 142, // sun
            fire,
            (0x96).toInt(), // trap,
            boulder, boulder2, boulder3, boulder4,
            spinCircleEnemy
        )

        // gannon stuff
        // pile: EC, EA
        // triforce: F2, F4
        
        val ignorePairs = setOf(
            triforceDirtPair
        )

        val loot = setOf(
            compass,
            clockTile,
            silverArrow,
            redring,
//            brownBoomerang,
            bow,
            map,
            masterKey,
            raft,
            bigHeart,
            fairy,
            fairy2,
            magicBook,
            rod,
            powerBracelet,
            whistle,
            candle,
            120, // not sure maybe something on triforce level , 78
            50,
            243, // heart
            key, //key
            116, // bomb
            bomb, // bomb
            triforceNormal,
//            triforceDirt, // why?
//            triforceDirt2,
            triforceTile,
            triforceTile2
        )
    }
}

object TileInfo {
    val longWaitEnemies = setOf(
        184, 186, // ghost
        ghost,
        rhinoHeadDown, rhinoHeadDown2, rhinoHeadMouthOpen, rhinoHead2,
        bat,
        254, 248 // the circle monster because I dont know why im stuck here
    )

    const val oldWoman = (0x9a).toInt()
}

val orbProjectile = (0x44).toInt() // fire from go to next room, also projectiles flying around from ship guy
//val grabbyHands = 142
val grabbyHands = (0xAE).toInt() //158
val grabbyHands2 = (0xAC).toInt() // 172

val ghost = (0xBC).toInt()
val bombSmoke = 112
val monsterCloud = (0x70).toInt()
val bomb = 52
val key = (0x2E).toInt() //key

val shopkeeperAndBat = (0x9C).toInt()
val bat = (0x9A).toInt()

val boulder = (0x90).toInt()
val boulder2 = (0x92).toInt()
val boulder3 = (0xEA).toInt() // it is also triforce part sooo... // also rhino piece
val boulder4 = (0xE8).toInt() // also part of circle enemy // also rhino
val boulder4Pair = (0xE8).toInt() to (0x00).toInt() //??
val arrowTipShotByEnemy = (0x88).toInt()
val arrowButtShotByEnemy = (0x86).toInt()

val largeShield = (0x56).toInt()
val candle = (0x26).toInt()
val fire = (0x5e).toInt()
val ladder =(0x76).toInt() //118
val raft = (0x6C).toInt()
val triforceNormal = (0x6E).toInt()
// final triforce
val triforceTile = (0xF4).toInt()
val triforceTile2 = (0xF2).toInt()
val triforceDirt = (0xEC).toInt() // attrib 03 //236 // also part of fourMonster
val triforceDirt2 = (0xFA).toInt() // also circle enemy center
val triforceDirt3 = (0xEA).toInt() // attrib 03

val waterMonster = 0xEC to (0x43)

// verify
val triforceDirtPair = (0xEC).toInt() to (0x03)// attrib 03 //236 // also part of fourMonster

// added from online
// https://www.computerarcheology.com/NES/Zelda/Bank2.html
val magicBook = (0x42).toInt()
val magicSword = (0x48).toInt()
val rod = (0x4A).toInt()
val letter = (0x4C).toInt()
val powerBracelet = (0x4E).toInt()


val silverArrow = (0x28).toInt()
val compass = (0x6A).toInt()
val clockTile = (0x66).toInt()
val bigHeart = (0x68).toInt() //104
val redring = (0x46).toInt()
val brownBoomerang = (0x36).toInt()
val bow = (0x2A).toInt()
val map = (0x4C).toInt()
val fairy = (0x50).toInt()
val fairy2 = (0x52).toInt()
val wizard = (0x98).toInt() // 152
val flame1 = (0x5C).toInt() //92
const val flame2 = (0x5E).toInt() //94
const val oldWoman = (0x9a).toInt()
const val potion = (0x99).toInt() //?
val whistle = (0x24).toInt()
val masterKey = (0x2C).toInt()

val secretEverybodyMonsterOrCircleEnemyLeft = 250 // fa
val secretEverybodyMonsterOrCircleEnemyRight = 248 // f8

// enemies
val dragonHead = (0xC0).toInt()
val dragonNeck = (0xC4).toInt()
// do not attack
// but these are also the pinwheel guys
//val dragonFeet = (0xC6).toInt() //wheel guy
//val dragonFeet2 = (0xCA).toInt() // and spiders
//val dragonTail = (0xC8).toInt() //wheel guy
//val dragonBody = (0xC2).toInt()

val dragon4Head = (0xDC).toInt()
val dragon4Body = (0xD4).toInt() // attrib 3
val dragon4BodyWingRight = (0xD2).toInt()
val dragon4BodyWingLeft = (0xD8).toInt()
val dragon4BodyFoot = (0xD0).toInt() // attrib 3
val dragon4BodySide = (0xD6).toInt()
val dragon4BodySpine = (0xC6).toInt() // attribute 3
val dragon4FlamingHead = (0xDE).toInt()
val rhinoHeadDown = (0xF4).toInt()
val rhinoHeadDown2 = (0xF6).toInt()
val rhinoHeadMouthOpen = (0xE2).toInt()
val rhinoHeadHeadWithMouthOpen = (0xE0).toInt()
val rhinoHeadHeadWithMouthClosed = (0xE8).toInt()
val rhinoHeadMouthClosed = (0xEA).toInt()
val rhinoHead2 = (0xE0).toInt()

val fourMonster = (0xE2).toInt() to (0x41).toInt()
val fourMonster2 = (0xE0).toInt() to (0x41).toInt()
val fourMonsterBody = (0xEC).toInt() to (0x41).toInt()
val fourMonster3 = (0xE8).toInt() to (0xC1).toInt() //??
val fourMonster5 = (0xE8).toInt() to (0x81).toInt() //??

val rhinoTail = (0xDC).toInt()
val rhinoMid = (0xDE).toInt()

val circleMonster = (0xFC).toInt()
val circleMonster2 = (0xFE).toInt()
val circleMonsterCenter = (0xFA).toInt()
val circleMonsterCenter2 = (0xF8).toInt()
val spinCircleEnemy = (0xC8).toInt() // attribute 42 and 2

// enemies
// https://www.computerarcheology.com/NES/Zelda/Bank3.html

//Gem(50),
//Key((0x2E).toInt())

//x96 rhino butt
sealed class Tile(id: Int, attribute: Int? = null) {
    class Loot(id: Int): Tile(id)
    class Enemy(id: Int): Tile(id)
    class Projectile(id: Int): Tile(id)
    class Ignore(id: Int): Tile(id)
}

object Tiles {
    // ignore

    // monsters

    // projectiles


    val compass = Tile.Loot(0x6A)
}

