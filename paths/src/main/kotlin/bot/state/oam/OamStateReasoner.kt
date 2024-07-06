package bot.state.oam

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.MovingDirection
import bot.state.map.stats.MapStatsTracker
import nintaco.api.API
import org.jheaps.annotations.VisibleForTesting
import util.d

/**
 * reason about the sprites
 */
class OamStateReasoner(
    private val isOverworld: Boolean,
    private val api: API,
    private val mapStatsTracker: MapStatsTracker = MapStatsTracker(),
    private val combine: Boolean = true
) {
    private val sprites: List<SpriteData>
    private var spritesUncombined: List<SpriteData> = emptyList()
    private var spritesRaw: List<SpriteData> = emptyList()

    var ladderSprite: Agent? = null
    var direction: Direction = Direction.None
    var damaged: Boolean = false

    init {
        sprites = readOam()
    }

    val DEBUG = false

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

    fun agentsUncombined(): List<Agent> =
        spritesUncombined.map { it.toAgent() }

    fun agentsRaw(): List<Agent> =
        spritesRaw.map { it.toAgent() }

    // calculate isDamaged here
    private fun SpriteData.toAgent(): Agent {
        val tileAttribute = tile to attribute
        val damaged = DamagedLookup.isDamaged(tile, attribute, isOverworld)
//        val damaged = mapStatsTracker.isDamaged(tile, attribute)
        val blockable = calcBlockable(tile, tileAttribute)
        val state = toState(damaged)
        // could look up the direction based on tile and sprite
        // arrow
        // wizard
        // diagonal
        // boulder -> down in block of 4 pattern
        var movingDirection: MovingDirection = MovingDirection.UNKNOWN_OR_STATIONARY
        val findDir = if (state == EnemyState.Projectile) {
            val found = ProjectileDirectionLookup.findDir(tileAttribute)
            if (found == Direction.None) {
                movingDirection = mapStatsTracker.calcDirection(point, state, tile)
                movingDirection.toDirection()
            } else {
                movingDirection = MovingDirection.from(found)
                found
            }
        } else {
            Direction.None
        }
        if (state == EnemyState.Projectile) {
            d { " Move dir for d:$damaged ${tileAttribute.toHex()} $point is ${movingDirection.toArrow()} and ${findDir.toArrow()}" }
        }
        return Agent(
            index = index, point = point,
            dir = findDir,
            state = toState(damaged), tile = tile, attribute = attribute,
            tileByte = tile.toString(16), attributeByte = attribute.toString(16),
            damaged = damaged,
            blockable = blockable,
            moving = movingDirection
        )
    }

    private fun calcBlockable(tile: Int, tilePair: Pair<Int, Int>): Blockable =
        when {
            EnemyGroup.projectilePairsUnblockable.contains(tilePair) -> Blockable.No
            EnemyGroup.projectileBlockable.contains(tile) -> Blockable.WithSmallShield
            EnemyGroup.projectileMagicShieldBlockable.contains(tile) -> Blockable.WithMagicShield
            else -> Blockable.No
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

        if (DEBUG || true) {
            d { " alive sprites AFTER delete" }
            mutable.forEachIndexed { index, sprite ->
                d { "$index: $sprite" }
            }
        }


        return mutable
    }

    private fun SpriteData.toState(damaged: Boolean): EnemyState =
        when {
            this.hidden -> EnemyState.Dead
            isLoot -> EnemyState.Loot
            isProjectile -> EnemyState.Projectile
            // treat as projectile for now
            damaged -> EnemyState.Projectile
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
        return SpriteData(at / 4, FramePoint(x, y - MapConstants.yAdjust), tile, attrib, combine = combine)
    }

    private fun readOam(): List<SpriteData> {
        spritesRaw = (0..63).map {
            readOam(0x0001 * (it * 4))
        }

        val dirDamage = LinkDirectionFinder.direction(spritesRaw)
        direction = dirDamage.direction
        damaged = dirDamage.damaged

        setLadder(spritesRaw)

        d { " sprites ** alive ** ${spritesRaw.filter { !it.hidden }.size} dir ${direction}" }
        // ahh there are twice as many sprites because each sprite is two big
        val alive = spritesRaw.filter { !it.hidden }
        if (DEBUG || true) {
            d { " alive sprites" }
            alive.forEachIndexed { index, sprite ->
                d { "$index: $sprite" }
            }
        }

        if (DEBUG) {
            d { " sprites" }
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
            sp.toAgent()
        } else {
            null
        }
    }
}
fun Agent.isGannonTriforce(): Boolean =
    tile to attribute == triforceTile2Pair

data class SpriteData(
    val index: Int,
    val point: FramePoint,
    val tile: Int,
    val attribute: Int,
    val tileByte: String = tile.toString(16),
    val attributeByte: String = attribute.toString(16),
    val combine: Boolean = true
) {
    val tilePair = tile to attribute

    val hiddenOrLink: Boolean = point.y >= 248 || attribute == 32 ||
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

    // keep
    // Debug: (Kermit) 49: SpriteData(index=49, point=(177, 128), tile=160, attribute=2) None
    val hidden: Boolean = point.y >= 248 || attribute == 32 || (!EnemyGroup.keepPairs.contains(tilePair) && EnemyGroup.ignore.contains(tile)) ||
//            ( (combine && tile != rhinoUpLeft.tile) && EnemyGroup.ignore.contains(tile)) ||
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
            || (combine && (tile == rhinoTail || tile == rhinoMid))

    val isLoot = !hidden && (EnemyGroup.loot.contains(tile) || EnemyGroup.lootPairs.contains(tilePair))

    val isProjectile = !hidden && (EnemyGroup.projectiles.contains(tile) || EnemyGroup.projectilePairs.contains(tilePair))

    val projectileType = when {
        EnemyGroup.projectileMagicShieldBlockable.contains(tile) -> EnemyStates.Projectile.BlockableWithMagicShield
        EnemyGroup.projectileUnblockable.contains(tile) -> EnemyStates.Projectile.Unblockable
        EnemyGroup.projectilePairsUnblockable.contains(tilePair) -> EnemyStates.Projectile.Unblockable
        else -> EnemyStates.Projectile.Blockable
    }
}



