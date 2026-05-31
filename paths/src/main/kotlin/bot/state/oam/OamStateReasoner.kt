package bot.state.oam

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.stats.MapStatsTracker
import nintaco.api.API
import nintaco.util.BitUtil
import org.jheaps.annotations.VisibleForTesting
import util.d
import util.e

/**
 * reason about the sprites
 */
class OamStateReasoner(
    private val isOverworld: Boolean,
    private val api: API,
    private val mapStatsTracker: MapStatsTracker = MapStatsTracker(),
    private val combine: Boolean = true,
    private val level: Int = 1,
    private val isGannon: Boolean = false
) {
    val lookup = DirectionByMemoryLookup(api)
    private val sprites: List<SpriteData>
    private var spritesUncombined: List<SpriteData> = emptyList()
    private var spritesRaw: List<SpriteData> = emptyList()

    var ladderSprite: Agent? = null
        private set

    init {
        sprites = readOam()
    }

    private val DEBUG = false

    fun agents(): List<Agent> =
        sprites.map { it.toAgent() }

    fun tilesUncombined(): List<Tile> =
        spritesUncombined.map { it.tile }

    // but also filter anything that isn'
    fun agentsRaw(): List<Tile> =
        spritesRaw.filter { it.point.y < 248 }.map { it.tile }

    /**
     * contains only oam data
      */
    private fun SpriteData.fromSpriteData(): Agent =
        Agent(
            index = index,
            point = point,
            tile = tile,
            attribute = attribute,
            color = color,
        )

    private fun SpriteData.toAgent(): Agent {
        val tileAttribute = tile to attribute

        val memory = lookup.closest(point) ?: return fromSpriteData().also { e { "EMPTY AGENT DETECTED, not memory at point $point" }}

        d(DEBUG) { "memory: $memory"}
        // currently testing this, possibly could use & or || to check that both agree
        val damaged = memory.damaged != 0
        if (damaged) {
            d(DEBUG) { "$tile tis damaged point $point"}
        }
        val hp = memory.hp
        val type = memory.type
        val stunned = memory.stunned
        val maxHp = EnemyMaxHpTable.maxHp(type)
        val memoryIndex = memory.index

        val blockable = calcBlockable(tile, type)
        val state = toState(damaged, isOverworld, isGannon)

        // boulder has a different movement direction, down 4 block type
        if (state == EnemyState.Projectile) {
            d(DEBUG) { " Move dir for tile:${tileAttribute.toHex()} $point is ${memory.point} damaged: $damaged pair: ${toStringIsProjLevel()}" }
        }

        return Agent(
            index = index,
            point = memory.point,
            dir = memory.point.direction ?: Direction.None,
            state = state,
            tile = tile,
            attribute = attribute,
            damaged = damaged,
            blockable = blockable,
            stunnedLeft = stunned,
            moving = memory.move,
            color = color,
            hp = hp,
            maxHp = maxHp,
            type = type
        )
    }

    private fun calcBlockable(tile: Int, type: Int): Blockable =
        when {
            EnemyGroup.projectileUnblockable.contains(tile) || EnemyObjectTypes.projectileObjectTypeUnblockable.contains(type) -> Blockable.No
            EnemyGroup.projectileBlockable.contains(tile) -> Blockable.WithSmallShield
            EnemyGroup.projectileMagicShieldBlockable.contains(tile) -> Blockable.WithMagicShield
            else -> Blockable.No
        }

    private fun calcBlockableFromType(objType: Int): Blockable {
        return when {
            (objType in EnemyObjectTypes.blockableWithoutShield) -> Blockable.WithSmallShield
            // TODO: need to add boulder and other projectiles
            (objType in EnemyObjectTypes.alwaysHarmful) -> Blockable.No
            (objType < 0x53) -> Blockable.No
            else -> Blockable.WithMagicShield
        }
    }

    @VisibleForTesting
    fun combine(toCombine: List<SpriteData>): List<SpriteData> {
        // can delete, because there is a sprite 8pxs to left that is the same
        val toDelete = toCombine
            // keep all the projectiles because most are just small
//            .filter { !SpriteData.projectiles.contains(it.tile) }
            .filter { maybeKeep ->
//                val matched = xMap[it.point.x - 8]
                toCombine.filter { it.point.x == maybeKeep.point.x - 8 }.any { matched ->
                    matched.point.y == maybeKeep.point.y
                }
            }

        val mutable = toCombine.toMutableList()
        for (spriteData in toDelete) {
            if (DEBUG) {
                d { "! remove $spriteData" }
            }
            mutable.remove(spriteData)
        }

        if (DEBUG) {
            d { " alive sprites AFTER delete" }
            mutable.forEachIndexed { index, sprite ->
                d { "$index: $sprite" }
            }
        }

        return mutable
    }

    private fun SpriteData.toState(damaged: Boolean, isOverworld: Boolean, isGannon: Boolean): EnemyState {
        val isSword = this.tile in Monsters.darknut.tile
        val facingLink = false

        return when {
            this.hidden -> EnemyState.Dead
            !isOverworld && isSword && facingLink -> EnemyState.Projectile
            isOverworld && isLoot -> EnemyState.Loot
            isGannon && this.tile in EnemyGroup.triforceTiles -> EnemyState.Loot
            !isOverworld && isLoot -> EnemyState.Loot
            isOverworld && isProjectile -> EnemyState.Projectile
            !isOverworld && isProjectileLevel -> EnemyState.Projectile
            // treat as projectile for now
            damaged -> EnemyState.Projectile
            else -> EnemyState.Alive
        }
    }

    // this isn't real
    // 21: SpriteData(index=21, point=(74, 23), tile=62, attribute=0, hidden=false)
    // there are always 2 sprites on for each enemy
    // one is at x, other is at x+8, same attribute
    // to translate to current coordinates
    // use the lower x value
    // then subtract 61 from the y, value

    fun getBitBool(x: Int, bit: Int): Boolean {
        return BitUtil.getBit(x, bit) == 1
    }

    private fun readOam(at: Int): SpriteData {
        val x = api.readOAM(at + 0x0003)
        val y = api.readOAM(at)
        val tile = api.readOAM(at + 0x0001)
        val attrib = api.readOAM(at + 0x0002)
//        val tileAddress = if (ppu.isSpriteSize8x16()) (((tile and 1) shl 12)
//                or ((tile and 0xFE) shl 4)) else (ppu.getSpritePatternTableAddress()
//                or (tile shl 4))
        // if priority is false, then maybe ignore it
        // OAM data frame

        // damaged ghost is only pallette 28.. the palette 24 could be, but it is also the live ghost, depending
//        val paletteIndex = 0x10 or ((attrib and 0x03) shl 2)
        val color = attrib.monsterColor()
        val priority = getBitBool(attrib, 5)
        val xFlip = BitUtil.getBitBool(attrib, 6)
        val yFlip = BitUtil.getBitBool(attrib, 7)
        return SpriteData(at / 4, FramePoint(x, y - MapConstants.yAdjust), tile, attrib,
            priority = priority, xFlip = xFlip, yFlip = yFlip, color = color, combine = combine, isOverworld = isOverworld)
    }

    private fun readOam(): List<SpriteData> {
        spritesRaw = (0..63).map {
            readOam(0x0001 * (it * 4))
        }

        setLadder(spritesRaw)

        d { " sprites ** alive ** ${spritesRaw.filter { !it.hidden }.size}" }
        // ahh there are twice as many sprites because each sprite is two big
        val alive = spritesRaw.filter { !it.hidden }
        if (DEBUG) {
            d { " alive sprites OAM" }
            alive.forEachIndexed { index, sprite ->
                d { "$index: $sprite" }
            }
        }

        if (DEBUG) {
            d { " raw sprites " }
            spritesRaw.forEachIndexed { index, sprite ->
                d { "$index: $sprite ${LinkDirectionFinder.dirFor(sprite)}" }
            }
        }

        spritesUncombined = alive.toMutableList()
        return if (combine) {
            combine(alive)
        } else {
            // combine only the rhino head
            spritesUncombined
        }
    }

    private fun setLadder(spritesRaw: List<SpriteData>) {
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
            d { "set ladder sprite $sp"}
            sp.toAgent().copy(dir = ladderDirection())
        } else {
            null
        }
    }

    fun ladderDirection(): Direction {
        val ladderSlot = api.readCPU(Addresses.ladderSlot) and 0xFF
        val ladderActive = ladderSlot != 0

        d { "ladderSlot: $ladderSlot ladderActive: $ladderActive"}

        val ladderDirection = if (ladderActive) {
            // ladderSlot is indexed assuming link is the first
            // but lookup assumes it's the first enemy
            lookup.get(ladderSlot-1)?.point?.direction ?: Direction.None
        } else {
            Direction.None
        }

        return ladderDirection
    }
}

data class SpriteData(
    val index: Int,
    val point: FramePoint,
    val tile: Int,
    val attribute: Int = 0,
    val tileByte: String = tile.toString(16),
    val attributeByte: String = attribute.toString(16),
    val priority: Boolean = false, // appears true when the monster is hidden
    val xFlip: Boolean = false,
    val yFlip: Boolean = false,
    val color: Int = 0,
    val combine: Boolean = true,
    val isOverworld: Boolean = false,
) {
    val tilePair = tile to attribute

    val hiddenOrLink: Boolean = point.y >= 248 || attribute == 32 ||
            EnemyGroup.ignorePairs.contains(tilePair)
            || (tile == 32) //link sword
            //|| point.y < 60  dont need that because the y coordinate is adjusted
            //|| projectiles.contains(tile) //|| loot.contains(tile) // should be separate
            || ( (tile == 248 || tile == 250) && point.y == 187) // spinny guy
            // tile 52 is a bomb
            || ( (tile == 52) && point.y == 187) // could be just any 187 point should be considered dead
            || ( (tile == 142 || tile == 144) && point.y == 187)
            || ( (tile == 164) && point.y == 187)
            || point.y >= 187 // this keeps coming up, make sense ,because we translated it 61
            || point.y < 0

    // keep
    // Debug: (Kermit) 49: SpriteData(index=49, point=(177, 128), tile=160, attribute=2) None
    val hidden: Boolean = priority || point.y >= 248 ||
            attribute == 32 ||
            tile == 32 || // link's sword
            (!EnemyGroup.keep.contains(tile) && EnemyGroup.ignoreFor(isOverworld).contains(tile)) ||
            EnemyGroup.ignorePairs.contains(tilePair)
            //|| point.y < 60  dont need that because the y coordinate is adjusted
            //|| projectiles.contains(tile) //|| loot.contains(tile) // should be separate
            || ( (tile == 248 || tile == 250) && point.y == 187) // spinny guy
            // tile 52 is a bomb
            || ( (tile == 52) && point.y == 187) // could be just any 187 point should be considered dead
            || ( (tile == 142 || tile == 144) && point.y == 187)
            || ( (tile == 164) && point.y == 187)
            || point.y >= 187 // this keeps coming up, make sense ,because we translated it 61
            || point.y < 0
            || (!combine && (tile == rhinoTail || tile == rhinoMid))

    val isLoot = !hidden && (EnemyGroup.loot.contains(tile))

    val isProjectile = !hidden && (EnemyGroup.projectiles.contains(tile) || EnemyGroup.projectiles.contains(tile))

    fun toStringIsProj(): String {
        return if (isProjectile) {
             " hidden $hidden enemyGroup: ${EnemyGroup.projectiles.contains(tile)}  pairs: ${EnemyGroup.projectiles.contains(tile)}"
        } else {
            " Not projectile"
        }
    }

    val isProjectileLevel = !hidden && (EnemyGroup.projectilesLevel.contains(tile))

    fun toStringIsProjLevel(): String {
        return if (isProjectileLevel) {
            " hidden $hidden enemyGroup: ${EnemyGroup.projectilesLevel.contains(tile)}"
        } else {
            " Not projectile"
        }
    }
}



