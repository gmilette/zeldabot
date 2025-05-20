package bot.plan.action

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import bot.state.oam.*
import org.jheaps.annotations.VisibleForTesting
import util.*

object AttackLongActionDecider {
    fun isInsideEnoughToShoot(state: MapLocationState) =
        if (state.frameState.isLevel) {
            state.link.isInLevelMap
        } else {
            true
        }

    fun shouldShootSword(state: MapLocationState, targets: List<FramePoint>): Boolean {
        d { "X-> should shoot sword" }
        val full = state.frameState.inventory.heartCalc.full(state)
        // sword is more than 1 grid away from link
        // need more work
        val swordIsFlying by lazy {
            for (agent in state.frameState.enemiesRaw.filter {
                it.tileAttrib in EnemyGroup.swordProjectile
            }) {
                d { " raw sword $agent ${agent.point.distTo(state.link)}"}
            }
            state.frameState.enemiesRaw.filter { it.y != 187 && it.y != 248 && it.tileAttrib in EnemyGroup.swordProjectile}
                .minByOrNull { it.point.distTo(state.link) }?.let {
                    it.point.distTo(state.link) > MapConstants.oneGrid
                } ?: false
        }
        val thereIsASword by lazy {
            state.frameState.enemiesRaw.any { it.y != 187 && it.tileAttrib in EnemyGroup.swordProjectile }
        }
        val thereIsAnExplosion by lazy {
            state.frameState.enemiesRaw.any { it.tile == explosion }
        }
        if (swordIsFlying) {
            d { " SWORD IS FLYING -->-->-->-->"}
        } else if (thereIsASword) {
            d { " SWORD IS THERE ------------" }
        }
        if (thereIsAnExplosion) {
            d { " SWORD IS EXPLODING -x-x-x-x-x-x"}
        }
        val canShoot = full //state.frameState HeartCalculator.isFull()
        val isInEnoughToShoot = isInsideEnoughToShoot(state)

        val explosionDist by lazy {
            if (thereIsAnExplosion) {
                // could start shooting before explosion is done.
                // there could be an algorithm to determine how far apart the explosions are
                val dist = MinDistTotalFramesCount()
                val explosions = state.frameState.enemiesRaw.filter { it.tile == explosion }.map { it.point }
                for (pt in explosions) {
                    dist.record(pt)
                }
                val exDist = dist.distance()
                exDist < 180 // 160 // the explosions are pretty far apart by this point, ok to start shooting
            } else {
                false
            }
        }
        d { "should shoot sword $canShoot $swordIsFlying $explosionDist ${isInEnoughToShoot.ifFalse("not in enough")} "}
        return (canShoot && !swordIsFlying && !explosionDist &&isInEnoughToShoot && targetInLongRange(state, targets))
    }

    fun shouldBoomerang(state: MapLocationState, targets: List<FramePoint>): Boolean {
        d { "X-> should boomerang targets=$targets " }
        // includes loot
        val shouldShoot = targets.isNotEmpty()
        var canShoot = false // state.boomerangActive || state.wandActive || state.arrowActive
        val boomerangIsFlying = when {
            (state.boomerangActive) -> {
                canShoot = true
                state.frameState.enemies.any { it.tile in EnemyGroup.boomerangs }
            }
            (state.wandActive) -> {
                canShoot = true
                state.frameState.enemies.any { it.tile in ProjectileDirectionLookup.ghostProjectiles }
            }
            (state.arrowActive) -> {
                canShoot = true
                // need testing
                state.frameState.enemies.any { it.tile in ProjectileDirectionLookup.arrowProjectiles }
            }
            else -> false
        }
        val inRange by lazy { targetInLongRange(state, targets) }
        d { "Shoot boomerang $shouldShoot can=$canShoot flying=$boomerangIsFlying range=$inRange"} // range=$inRange" }
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

    private fun targetInLongRange(state: MapLocationState, targets: List<FramePoint>): Boolean {
        return firstEnemyIntersect(longRectangle(state), targets) != null
    }

    fun longRectangle(state: MapLocationState): Geom.Rectangle {
        val dir = state.frameState.link.dir
        val link = state.link
        // assume go right
        val midLink = link.justDownFourth
        val bottomLink = link.justDownThreeFourth
        val endReach = rayFrom(state.currentMapCell.passable, bottomLink, dir)
        d { " end reach is $endReach $dir" }
        val swordRectangle = swordRectangle(link, endReach, dir)
        d { " $link intersect sword rect $swordRectangle" }
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
        for (aliveEnemy in targets) {
            val inter = swordRectangle.intersect(aliveEnemy.toRect())
            d { "long intersect $aliveEnemy $inter" }
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

fun Agent.affectedByBoomerang(level: Int) =
    Monsters.lookup(level)[tile]?.affectedByBoomerang ?: true // most monsters can be boomeranged AND loot

fun Agent.lootNeeded(state: MapLocationState) = LootKnowledge.neededValuable(state, this)
