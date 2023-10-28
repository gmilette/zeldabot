package bot.plan.zstar

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import util.Map2d

object NearestSafestPoint {
    fun mapNearest(state: MapLocationState, pts: List<FramePoint>) =
        pts.flatMap { nearestSafePoints(it, state.currentMapCell.zstar.costsF, state.currentMapCell.zstar.passable) }

    fun nearestSafePoints(point: FramePoint, costs: Map2d<Int>, passable: Map2d<Boolean>): List<FramePoint> =
        Direction.values().map {
            nearestSafePoint(point, it, costs, passable)
        }

    private fun nearestSafePoint(point: FramePoint, direction: Direction, costs: Map2d<Int>, passable: Map2d<Boolean>): FramePoint {
        var pt = point
        val modifier = direction.pointModifier()
        var ct = 0
        while(pt.isOnMap && !pt.isSafe(costs, passable) && ct < MapConstants.oneGrid) {
            pt = modifier(pt)
            ct++
        }
        return pt
    }

    private fun FramePoint.isSafe(costs: Map2d<Int>, passable: Map2d<Boolean>): Boolean =
        passable.get(this) && costs.get(this) <= 10
}