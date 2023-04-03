package bot.state

import nintaco.api.API
import util.d

class OamStateReasoner(
    private val api: API
) {
    val DEBUG = false
    // SpriteData(index=17, point=(160, 120), tile=164, attribute=66)
    val pancake = 164
    val tileMap = mapOf(
        // 122
        // 188
        // 190
        (0xD6) to "gannon",
        (0xD4) to "gannon",
        (0xD2) to "gannon",
        (0xE6) to "gannon",
        (0xE4) to "gannon",
        (0xE2) to "gannon",
        (0xE0) to "gannon",
        (0xD0) to "gannon",
        164 to "pancake",
        (0x96).toInt() to "trap",
        (0xF8).toInt() to "circleenemycenter",
        (0xFA).toInt() to "circleenemycenter",
        (0xFC).toInt() to "circleenemy",
        (0x9C).toInt() to "bat",
        (0x9A).toInt() to "bat",
        170 to "squshy", // left and right
        246 to "rhino",
        244 to "rhino",
//        226 to "rhino head",
        124 to "ghostripple",
        126 to "ghostripple",
        142 to "sun", // attrib 1
        144 to "sun", // attrib 65, atrib 67 whiteish color
        184 to "blueghostright", // back attrib 65 // might be different for left facing ghost
        186 to "blueghostright" // right, attrib 1
    )

    private val sprites: List<SpriteData> = readOam()

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
        sprites.map {
            Agent(index = it.tile, point = it.point, state = it.toState(), hp = it.tile, droppedId = it.attribute)
        }

    private fun combine() {
        val xMap = sprites.associateBy { it.point.x }
        // if any of the sprites dont have a match
        sprites.filter { xMap.containsKey(it.point.x + 8) }
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
        val sprites = (0..63).map {
            readOam(0x0001 * (it * 4))
        }
        // Y coord
        // Tile
        // Attribute
        // X coord
        if (DEBUG) {
            d { " sprites" }
            sprites.forEachIndexed { index, sprite ->
                d { "$index: $sprite" }
            }
        }

        d { " sprites** alive ** ${sprites.filter { !it.hidden }.size}" }
        // ahh there are twice as many sprites because each sprite is two big
        sprites.filter { !it.hidden }.forEachIndexed { index, sprite ->
            d { "$index: $sprite" }
        }

        return sprites
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
    val hidden: Boolean = point.y >= 248 || attribute == 32 || notEnemy.contains(tile)
            //|| point.y < 60  dont need that because the y coordinate is adjusted
            //|| projectiles.contains(tile) //|| loot.contains(tile) // should be separate
            || ( (tile == 248 || tile == 250) && point.y == 187) // spinny guy
            || ( (tile == 52) && point.y == 187) // could be just any 187 point should be considered dead
            || ( (tile == 142 || tile == 144) && point.y == 187)
            || ( (tile == 164) && point.y == 187)
            || point.y >= 187 // this keeps coming up, make sense ,because we translated it 61
            || point.y < 0
    val isLoot = !hidden && loot.contains(tile)

    val isProjectile = !hidden && projectiles.contains(tile)
    // it doesn't solve the pancake problem

    companion object {
        val notEnemy = setOf(
            18, 16, // link shield shite
            12, 14, // facing up link
            4, 6, 8, // link // 0
            0, 2, // facing left link
            88, 10, // mor elink
            90, // brown link small shield
            20, 22, // i think link or maybe movable block
            (0x80).toInt(), // link sword
            (0x82).toInt(), // link sword
            84, // link i think
            32, // link's sword
            96, // link again
            (0x1A).toInt(), (0x18).toInt(), // link about to attack
            //
            (0x82).toInt(), // sword hilt
            (0x84).toInt(), // sword point
            90, // not sure what it is maybe link or his sword
            62, // blinking this
            48, // sword
            116, // bomb
            52, // bomb
            48, // swirly thing
            112, // bomb smoke
            114, // i think bomb smoke

            //164, // not sure, that is a pancake
            160,
            ladder,
            wizard,
            flame1,
            flame2,
//            oldWoman
        )
        val projectiles = setOf(
            124, 126, // ghost attack
            144, 142, // sun
            40, 68, // ganons
            (0x44).toInt(), // fire from go to next room
            fire,
            (0x96).toInt() // trap
        )

        val loot = setOf(
            clockTile,
            silverArrow,
            redring,
            brownBoomerang,
            bow,
            map,
            bigHeart,
            50,
            243, // heart
            (0x2E).toInt(),
//            triforceDirt,
//            triforceDirt2,
            triforceTile,
            triforceTile2
        )
    }
}

val fire = (0x5e).toInt()
val ladder =(0x76).toInt() //118

// final triforce
val triforceTile = (0xF4).toInt()
val triforceTile2 = (0xF2).toInt()
val triforceDirt = (0xEC).toInt() // 236
val triforceDirt2 = (0xFA).toInt()

val clockTile = (0x66).toInt()
val silverArrow = (0x28).toInt()
val redring = (0x46).toInt()
val brownBoomerang = (0x36).toInt()
val bow = (0x2A).toInt()
val map = (0x4C).toInt()
val bigHeart = (0x68).toInt() //104
val wizard = (0x98).toInt() // 152
val flame1 = (0x5C).toInt() //92
val flame2 = (0x5E).toInt() //94
val oldWoman = (0x9a).toInt()
val potion = (0x99).toInt() //?

enum class Tile(id: Int) {
    Gem(50),
    Key((0x2E).toInt())
}

