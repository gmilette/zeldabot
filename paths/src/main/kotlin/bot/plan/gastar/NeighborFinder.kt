package bot.plan.gastar

import bot.state.*
import bot.state.map.Direction
import bot.state.map.pointModifier
import bot.state.map.vertical
import util.Map2d
import util.d
import java.lang.Exception

// list of passible nodes
//    private val FramePoint.neighbors
//        get() = listOf(this.up, this.down, this.left, this.right).filter {
//            it.x > 0 && it.x < passible.maxX && it.y > 0 && it.y <
//                    passible.maxY && passible.get(it) }

class NeighborFinder(
    private val passable: Map2d<Boolean>,
    private val halfPassable: Boolean = true,
    /**
     * inside the level, link cannot move halfway into any part of the top two grids
     */
    private val isLevel: Boolean = false
) {
    fun neighbors(point: FramePoint): List<FramePoint> {
        val neigh = mutableListOf<FramePoint>()
        for (direction in Direction.values()) {
            //val next = SkipLocations.getNext(point, direction)
            val next = direction.pointModifier()(point)
//                if (next.distTo(this) > 1) {
//                    d { "skip loc" }
//                }
            if (GStar.DEBUG) {
                d { " test $next $direction" }
                d { "rb ${passable.get(next.justRightEndBottom)}"}
                d { "re ${passable.get(next.justRightEnd)}"}
                d { "lb ${passable.get(next.justLeftBottom)}"}
                d { "l ${passable.get(next)}"}
            }
//            logPassable(next)
            if (passableAndWithin(next)) {
//                d { " passable! ${neigh.size}"}
                neigh.add(next)
            } else {
//                d { " passable NOT"}
                // going from 143 to 144
                // check corners
                val corner = corner(point, direction)
                if (corner != null) {
                    if (GStar.DEBUG) {
                        d { " add corner $corner $direction" }
                    }
                    neigh.add(corner.addDirection(direction))
                }
                if (GStar.DEBUG) {
                    d { " corner $point $next $direction not passable" }
                }
            }
        }
        return neigh
    }

    private fun logPassable(point: FramePoint) {
        d { "point $point"}
        d { "mid ${passable.get(point.justMid)}"}
        d { "mid end ${passable.get(point.justMidEnd)}"}
        d { "right end ${passable.get(point.justRightEndBottom)} ${point.justRightEndBottom}"}
        d { "bottom left ${passable.get(point.justLeftBottom)}"}
    }

    private fun passableAndWithin(point: FramePoint) =
        blockPassableMeta(point) && point.within()
        //blockPassable(point) && point.within()
//        blockPassableHalf(point) && point.within()

    fun corner(point: FramePoint, direction: Direction): FramePoint? {
        val modifier = direction.pointModifier()
        return if (direction.vertical) {
            // try left right
            framePoint(point, Direction.Right.pointModifier(), modifier) ?:
            framePoint(point, Direction.Left.pointModifier(), modifier)
        } else {
            // horizontal
            framePoint(point, Direction.Up.pointModifier(), modifier) ?:
            framePoint(point, Direction.Down.pointModifier(), modifier)
            null
        }
    }

    private fun framePoint(point: FramePoint, horizModifier: (FramePoint) -> FramePoint, modifier: (FramePoint) -> FramePoint):
            FramePoint? {
        val h2 = horizModifier(horizModifier(point))
        val hDir = modifier(h2)
        return if (passableAndWithin(hDir)) {
            h2
        } else {
            val h1 = horizModifier(point)
            val hDir1 = modifier(h1)
            if (passableAndWithin(hDir1)) {
                h1
            } else {
                null
            }
        }
    }

    private fun blockPassableMeta(point: FramePoint): Boolean {
        // levels have special rules for exits
        // todo: add more logic here
//        d { " passable $point ${passable.get(point)} $halfPassable"}
        return when {
            !halfPassable -> {
                blockPassable(point)
            }
            // check point end end
            // was <= 32
            // (isLevel && point.y <= 24)
            (isLevel && point.y <= 32) && (!passable.get(point) || !passable.get(point.justRightEnd)) -> {
                if (GStar.DEBUG) {
                    d { " failed cuz y (pt: $point) < 32 ${passable.get(point)} ${passable.get(point.justRightEnd)}" }
                }
                false
            }
            else -> {
                blockPassableHalf(point)
//                return try {
//                blockPassableHalf(point)
//                } catch(e: Exception) {
//                    d { "errr " + passable.map.size}
//                    return false
//                }
            }
        }
    }

    /**
     * top
     */
    fun blockPassable(point: FramePoint): Boolean {
        return passable.get(point) && passable.get(point.justRightEndBottom)
                && passable.get(point.justRightEnd)
                && passable.get(point.justLeftBottom)
    }

    /**
     * test at mid
     */
    private fun blockPassableHalf(point: FramePoint): Boolean {
        return passable.get(point.justMid)
                && passable.get(point.justMidEnd)
                && passable.get(point.justRightEndBottom)
                && passable.get(point.justLeftBottom)
    }

    private fun FramePoint.within() =
        x > -1 && x < passable.maxX && y > -1 && y <
                passable.maxY
}