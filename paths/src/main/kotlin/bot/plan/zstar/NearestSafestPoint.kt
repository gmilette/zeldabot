package bot.plan.zstar

import bot.state.FramePoint
import bot.state.MapLocationState
import bot.state.isOnMap
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import util.Map2d

object NearestSafestPoint {
    fun isMapSafe(state: MapLocationState, pt: FramePoint) =
        pt.isSafe(state.currentMapCell.zstar.costsF, state.currentMapCell.zstar.passable)

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

//        val safePoint = pt.copy()
//
//        // find nearest unsafe point
//        ct = 0
//        while(pt.isOnMap && pt.isSafe(costs, passable) && ct < MapConstants.oneGrid) {
//            pt = modifier(pt)
//            ct++
//        }

        // this is still possible, the safe point is surrounded by unsafe points

        //////
        //Pt//
        //////

        return pt
    }

    private fun FramePoint.isSafe(costs: Map2d<Int>, passable: Map2d<Boolean>): Boolean =
        passable.get(this) && costs.get(this) <= 10
}