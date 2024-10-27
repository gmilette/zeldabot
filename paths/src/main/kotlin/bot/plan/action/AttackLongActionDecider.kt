package bot.plan.action

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import bot.state.oam.EnemyGroup
import bot.state.oam.Monsters
import bot.state.oam.ProjectileDirectionLookup
import bot.state.oam.toHex
import org.jheaps.annotations.VisibleForTesting
import util.Geom
import util.Map2d
import util.d

object AttackLongActionDecider {
    fun isSwordLink() {
        // if it is moving in same direction as link
        // if it is within 1 grid of link
    }

    fun shouldShootSword(state: MapLocationState, targets: List<FramePoint>): Boolean {
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
            state.frameState.enemiesRaw.any { it.tileAttrib in EnemyGroup.swordProjectile }
        }
        if (swordIsFlying) {
            d { " SWORD IS FLYING -->-->-->-->"}
        } else if (thereIsASword) {
            d { " SWORD IS THERE ------------"}
        }
        val canShoot = full //state.frameState HeartCalculator.isFull()
        return (canShoot && !swordIsFlying && targetInLongRange(state, targets))
    }

    fun shouldBoomerang(state: MapLocationState, targets: List<FramePoint>): Boolean {
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
                false
//                state.frameState.enemies.any { it.tile in ProjectileDirectionLookup.ghostProjectiles }
            }
            else -> false
        }
        val inRange by lazy { targetInLongRange(state, targets) }
        d { "Shoot boomerang $shouldShoot can=$canShoot flying=$boomerangIsFlying "} // range=$inRange" }
        return (shouldShoot && canShoot && !boomerangIsFlying && inRange)
        // when start a level, if enemies can be boomeranged, switch to it, but not if about
        // to switch to bomb
        // should be able to alternate, shooting sword and boomerang
        // there are enemies or loot (allow shooting at loot)
        // if boomerang is active
        // if there are enemies that can be affected by boomerang
        // ex. sword guy, ghost cannot be
        // if enemy can be kill by it, always shoot
        // if it is an enemy that can be stunned, observe cooldown shot
        // if I want to shoot but the boomerang is not active
    }

    fun shouldWand(state: MapLocationState, targets: List<FramePoint>): Boolean {
        // only alive enemies
        // should use the targets list for this
        val shouldShoot = state.aliveEnemies.isNotEmpty()
        val canShoot = state.wandActive
        val inRange by lazy { targetInLongRange(state, emptyList()) }
        d { "Shoot want $shouldShoot can=$canShoot"} // range=$inRange" }
        return (shouldShoot && canShoot && inRange)
    }

    // boomerang algorithm
    // since enemies are fro

//    private fun targetInLongRange(state: MapLocationState): Boolean {
//        return firstEnemyIntersect(state, longRectangle(state), state.aliveEnemies.map { it.point }) != null
//    }

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
            d { "intersect $aliveEnemy $inter" }
        }

//        return state.aliveEnemies.firstOrNull {
//            swordRectangle.intersect(it.point.toRect())
//        }
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

    // enemy group for immunie to boomerang

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

    private val MapLocationState.boomerangActive: Boolean
        get() = this.frameState.inventory.selectedItem == Inventory.Selected.boomerang

    private val MapLocationState.wandActive: Boolean
        get() = this.frameState.inventory.selectedItem == Inventory.Selected.wand

    private val MapLocationState.arrowActive: Boolean
        get() = this.frameState.inventory.selectedItem == Inventory.Selected.arrow
}

fun Agent.affectedByBoomerang(level: Int) =
    Monsters.lookup(level)[tile]?.affectedByBoomerang ?: true // most monsters can be boomeranged AND loot

fun Agent.lootNeeded(state: MapLocationState) = LootKnowledge.neededValuable(state, this)
