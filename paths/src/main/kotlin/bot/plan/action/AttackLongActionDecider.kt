package bot.plan.action

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import org.jheaps.annotations.VisibleForTesting
import util.Geom
import util.Map2d
import util.d

object AttackLongActionDecider {

    fun shouldShootSword(state: MapLocationState): Boolean {
        val swordIsFlying = false
        val full = state.frameState.inventory.heartCalc.full(state.frameState.inventory.inventoryItems.whichRing())
        val canShoot = full //state.frameState HeartCalculator.isFull()
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
        return firstEnemyIntersect(state, longRectangle(state)) != null
    }

    fun longRectangle(state: MapLocationState): Geom.Rectangle {
        val dir = state.frameState.link.dir
        val link = state.link
        // assume go right
        val midLink = link.justDownFourth
        val endReach = rayFrom(state.currentMapCell.passable, midLink, dir)
        d { " end reach is $endReach $dir"}
        val swordRectangle = swordRectangle(link, endReach, dir)
        d { " $link intersect sword rect $swordRectangle"}
        return swordRectangle
    }

    private fun firstEnemyIntersect(state: MapLocationState, swordRectangle: Geom.Rectangle): Agent? {
        //     0  link
        //     +4 top sword
        // -->
        //     +12 bottom sword (account for size of sword or boomerang)

        // depends on direction
        for (aliveEnemy in state.aliveEnemies) {
            val inter = swordRectangle.intersect(aliveEnemy.point.toRect())
            d { "intersect ${aliveEnemy.point} $inter" }
        }

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

    private fun List<Agent>.affectedByBoomerang() =
        this.filter { it.affectedByBoomerang() }

    // enemy group for immunie to boomerang
    private fun Agent.affectedByBoomerang() = true
//        this.tile !in immuneToBoomerang

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

    private val MapLocationState.boomerangeActive: Boolean
        get() = this.frameState.inventory.selectedItem == Inventory.Selected.boomerang

}