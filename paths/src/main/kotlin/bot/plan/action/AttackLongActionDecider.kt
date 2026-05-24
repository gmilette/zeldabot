package bot.plan.action

import bot.state.*
import bot.state.dir
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import bot.state.oam.*
import org.jheaps.annotations.VisibleForTesting
import util.*

object AttackLongActionDecider {
    val DEBUG = false

    fun isInsideEnoughToShoot(state: MapLocationState) =
        if (state.frameState.isLevel) {
            state.link.isInLevelMap
        } else {
            true
        }

    fun ableToShoot(state: MapLocationState): Boolean {
        val swordIsFlying by lazy {
            state.frameState.projectileStatus.swordIsFlying
        }
        val canShoot = state.frameState.inventory.heartCalc.full(state)
        val isInEnoughToShoot = isInsideEnoughToShoot(state)
        if (swordIsFlying) {
            d { " SWORD IS FLYING by sprite -->-->-->-->"}
        }

        d { "should shoot sword can=$canShoot fly=$swordIsFlying ${isInEnoughToShoot.ifFalse("not in enough")} "}
        return (canShoot && !swordIsFlying && isInEnoughToShoot)
    }

    fun shouldShootSword(state: MapLocationState, targets: List<FramePoint>): Boolean {
        return ableToShoot(state) && targetInLongRange(state, targets)
    }

    fun shouldBoomerang(state: MapLocationState, targets: List<FramePoint>): Boolean {
        d { "X-> should boomerang targets=$targets ${state.frameState.projectileStatus.status()}" }
        // includes loot
        val shouldShoot = targets.isNotEmpty()
        var canShoot = false
        val boomerangIsFlying = when {
            (state.boomerangActive) -> {
                canShoot = true
                state.frameState.projectileStatus.boomerangInFlight
            }
            (state.wandActive) -> {
                canShoot = true
                state.frameState.projectileStatus.weaponInFlight
            }
            (state.arrowActive) -> {
                canShoot = state.frameState.inventory.hasRupees
                state.frameState.projectileStatus.weaponInFlight
            }
            else -> false
        }
        val inRange by lazy { targetInLongRange(state, targets) }
        d { "Shoot boomerang $shouldShoot can=$canShoot flying=$boomerangIsFlying range=$inRange"}
        return (shouldShoot && canShoot && !boomerangIsFlying && inRange)
    }

    fun shouldWand(state: MapLocationState, targets: List<FramePoint>): Boolean {
        d { "X-> should wand" }
        // only alive enemies
        // should use the targets list for this
        val shouldShoot = state.aliveEnemies.isNotEmpty()
        val canShoot = state.wandActive
        val inRange by lazy { targetInLongRange(state, emptyList()) }
        d { "Shoot want $shouldShoot can=$canShoot"} // range=$inRange" }
        return (shouldShoot && canShoot && inRange)
    }

    fun targetInLongRange(passable: Map2d<Boolean>, from: FramePoint, targets: List<FramePoint>): Boolean {
        return firstEnemyIntersect(longRectangle(passable, from, from.direction ?: Direction.None), targets) != null
    }

    fun targetInLongRange(state: MapLocationState, targets: List<FramePoint>): Boolean {
        return firstEnemyIntersect(longRectangle(state.currentMapCell.passable,  state.link, state.frameState.link.dir), targets) != null
    }

    fun longRectangle(passable: Map2d<Boolean>, link: FramePoint, dir: Direction): Geom.Rectangle {
        // assume go right
        val midLink = link.justDownFourth
        val bottomLink = link.justDownThreeFourth
        val endReach = rayFrom(passable, bottomLink, dir)
        if (DEBUG) {
            d { " end reach is $endReach $dir" }
        }
        val swordRectangle = swordRectangle(link, endReach, dir)
        if (DEBUG) {
            d { " $link intersect sword rect $swordRectangle" }
        }
        return swordRectangle
    }

    private fun firstEnemyIntersect(
        swordRectangle: Geom.Rectangle,
        targets: List<FramePoint>
    ): FramePoint? {
        //     0  link
        //     +4 top sword
        // -->
        //     +12 bottom sword (account for size of sword or boomerang)

        // depends on direction
        if (DEBUG) {
            for (aliveEnemy in targets) {
                val inter = swordRectangle.intersect(aliveEnemy.toRect())
                d { "long intersect $aliveEnemy $inter" }
            }
        }

        return targets.firstOrNull {
            swordRectangle.intersect(it.toRect())
        }
    }

    @VisibleForTesting
    fun swordRectangle(link: FramePoint, to: FramePoint, dir: Direction) = when (dir) {
//        Direction.Left -> Geom.Rectangle(link.justDownFourth.withX(to.x), link.justDownThreeFourth)
//        Direction.Right -> Geom.Rectangle(link.justDownFourth, link.justDownThreeFourth.withX(to.x))
//        Direction.Down -> Geom.Rectangle(link.justRightFourth, link.justRightThreeFourth.withY(to.y))
//        Direction.Up -> Geom.Rectangle(link.justRightFourth.withY(to.y), link.justRightThreeFourth)
        Direction.Left -> Geom.Rectangle(link.justDown6.withX(to.x), link.justDownLast6)
        Direction.Right -> Geom.Rectangle(link.justDown6, link.justDownLast6.withX(to.x))
        Direction.Down -> Geom.Rectangle(link.justRight6, link.justRightLast6.withY(to.y))
        Direction.Up -> Geom.Rectangle(link.justRight6.withY(to.y), link.justRightLast6)
        else -> link.toRect()
    }

    private fun rayFrom(map: Map2d<Boolean>, point: FramePoint, dir: Direction): FramePoint {
        if (dir == Direction.None) return FramePoint()
        var farthestPoint = point.copy()
        val modifier = dir.pointModifier()
        while (map.getOr(point, false) && farthestPoint.isOnMap) {
            farthestPoint = modifier(farthestPoint)
        }
        return farthestPoint
    }

    fun shouldActivateBoomerang(state: MapLocationState) {
        // there are enemies or loot that are affected by boomerang
        // have the boomerang
    }

    fun inStrikingRange(point: FramePoint, enemies: List<FramePoint>): Boolean {
        return false
    }
}

val MapLocationState.boomerangActive: Boolean
    get() = this.frameState.inventory.selectedItem == Inventory.Selected.boomerang

val MapLocationState.wandActive: Boolean
    get() = this.frameState.inventory.selectedItem == Inventory.Selected.wand

val MapLocationState.arrowActive: Boolean
    get() = this.frameState.inventory.selectedItem == Inventory.Selected.arrow

fun Agent.arrowKillable(level: Int) =
    Monsters.lookup(level)[tile]?.arrowKillable ?: true

fun Agent.affectedByBoomerang(level: Int) =
    Monsters.lookup(level)[tile]?.affectedByBoomerang ?: true // most monsters can be boomeranged AND loot

fun Agent.lootNeeded(state: MapLocationState) = LootKnowledge.neededValuable(state, this)
