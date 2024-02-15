package bot.plan.action

import bot.state.*
import bot.state.map.*
import org.jheaps.annotations.VisibleForTesting
import util.Geom
import util.Map2d

object AttackLongActionDecider {

    fun shouldShootSword(state: MapLocationState): Boolean {
        val swordIsFlying = false
        val canShoot = true //state.frameState HeartCalculator.isFull()
        // draw ray from attack point. Does it hi
        return (canShoot && !swordIsFlying && targetInLongRange(state))
    }

    fun shouldBoomerang(state: MapLocationState): Boolean {
        val shouldShoot = state.frameState.enemies.isNotEmpty()
        val canShoot = false // is boomerang active
        val boomerangeIsFlying = false
        return (canShoot && !boomerangeIsFlying && targetInLongRange(state))
        // there are enemies or loot (allow shooting at loot)
        // if boomerang is active
        // if there are enemies that can be affected by boomerang
        // ex. sword guy, ghost cannot be
        // if enemy can be kill by it, always shoot
        // if it is an enemy that can be stunned, observe cooldown shot
        // if I want to shoot but the boomerang is not active
    }

    // boomerang algorithm
    // since enemies are fro

    private fun targetInLongRange(state: MapLocationState): Boolean {
        val dir = state.frameState.link.dir
        val link = state.link

        // assume go right
        val midLink = link.justDownFourth

        val endReach = rayFrom(state.currentMapCell.passable, midLink, dir)
        return firstEnemyIntersect(state, endReach, dir) != null
    }

    private fun firstEnemyIntersect(state: MapLocationState, to: FramePoint, dir: Direction): Agent? {
        //     0  link
        //     +4 top sword
        // -->
        //     +12 bottom sword (account for size of sword or boomerang)

        // depends on direction
        val link = state.link
        val swordRectangle = swordRectangle(link, to, dir)

//        // don't shoot at front of sword guy
//        return state.aliveEnemies.affectedByBoomerang().filter {
//            swordRectangle.intersect(it.point.toRect())
//        }.minByOrNull {
//            it.point.distTo(link)
//        }
        return state.aliveEnemies.affectedByBoomerang().firstOrNull {
            swordRectangle.intersect(it.point.toRect())
        }
    }

    @VisibleForTesting
    fun swordRectangle(link: FramePoint, to: FramePoint, dir: Direction) = when {
        dir.horizontal -> Geom.Rectangle(link.justDownFourth, link.justDownThreeFourth.withX(to.x))
        dir.vertical -> Geom.Rectangle(link.justRightFourth, link.justRightThreeFourth.withY(to.y))
        else -> link.toRect()
    }

    private fun List<Agent>.affectedByBoomerang() =
        this.filter { it.affectedByBoomerang() }

    // enemy group for immunie to boomerang
    private fun Agent.affectedByBoomerang() = true
//        this.tile !in immuneToBoomerang

    private fun rayFrom(map: Map2d<Boolean>, point: FramePoint, dir: Direction): FramePoint {
        var farthestPoint = point.copy()
        val modifier = dir.pointModifier()
        while (map.get(point) && farthestPoint.isOnMap) {
            farthestPoint = modifier(point)
        }
        return point
    }

    fun shouldActivateBoomerang(state: MapLocationState) {
        // there are enemies or loot that are affected by boomerang
        // have the boomerang
    }

    private val MapLocationState.boomerangeActive: Boolean
        get() = this.frameState.inventory.selectedItem == Inventory.Selected.boomerang

}