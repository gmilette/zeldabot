package bot.plan.gastar

import bot.state.*
import bot.state.map.Direction
import bot.state.map.horizontal
import bot.state.map.pointModifier
import bot.state.map.vertical
import util.Map2d
import util.d

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

    private val horizontal: List<Direction> = listOf(Direction.Left, Direction.Right)
    private val vertical: List<Direction> = listOf(Direction.Up, Direction.Down)

    private fun okDirections(from: FramePoint, last: Direction? = null, dist: Int): List<Direction> {
        val all = Direction.values().toList()
        if (last == null) return all
        if (true) return all
        // wrong, you still cannot to left or right

        val dirs = when (last) {
            Direction.Left,
            Direction.Right -> {
                if (from.onHighwayX || dist > 1) {
                    all
                } else {
//                    val corner = corner(from, last)
//                    if (corner != null) {
//                        d { " CORNERy $corner"}
//                    }
                    horizontal
                }
            }
            Direction.Up,
            Direction.Down -> {
                if (from.onHighwayY || dist > 1) {
                    all
                } else {
                    // if corner add
//                    val corner = corner(from, last)
//                    if (corner != null) {
//                        d { " CORNER $corner"}
//                    }
//                    corner?.let {
//                        vertical + it.direction!!
//                    } ?: vertical
                    vertical
                }
            }
        }
        return dirs
    }

    // need to know the previous point
    fun neighbors(point: FramePoint, direction: Direction? = null, dist: Int = 1): List<FramePoint> {
        val neigh = mutableListOf<FramePoint>()

        val validDirections = okDirections(point, direction, dist)
        if (GStar.DEBUG) {
            d { " valid directions ${validDirections.size} = $validDirections" }
        }

        for (direction in validDirections) {
            //val next = SkipLocations.getNext(point, direction)
            val next = direction.pointModifier()(point)
//                if (next.distTo(this) > 1) {
//                    d { "skip loc" }
//                }
            if (GStar.DEBUG) {
                d { " test passable $next $direction ${next.onHighwayYAlmost}" }
                d { "rb ${passable.get(next.justRightEndBottom)}"}
                d { "re ${passable.get(next.justRightEnd)}"}
                d { "lb ${passable.get(next.justLeftBottom)}"}
                d { "l ${passable.get(next)}"}
            }
//            logPassable(next)
            if (passableAndWithin(next)) { // || next.isCorner
//                d { " passable! ${neigh.size}"}
                neigh.add(next)
                // but also add an additional neighbor for using the corner
                // then gstar will selection it randomly
                if (next.onHighwayYAlmost && direction.horizontal) {
                    // it's going to go down, but the direction is right/left
                    val realMoveTo = point.down.addDirection(direction)
                    //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                    neigh.add(realMoveTo)
                }

                if (next.onHighwayYAlmostBeyond && direction.horizontal) {
                    // 104, 113 -> if  here you went one to low, and you want to go back up, you could go right to go up
                    // 104, 112
                    val realMoveTo = point.up.addDirection(direction)
                    neigh.add(realMoveTo)
                }

                if (next.onHighwayXAlmost && direction.vertical) {
                    // the way up is blocked, it can go up, but link is actually going to
                    // go right instead to get around it and get on the highway
//                // it's going to go right, but the direction is up/down
                    val realMoveTo = point.right.addDirection(direction)
                    //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                    neigh.add(realMoveTo)
                }
                if (next.onHighwayXAlmostBeyond && direction.vertical) {
                    // copy and paste
                    val realMoveTo = point.left.addDirection(direction)
                    neigh.add(realMoveTo)
                }
                // do left too
            } else if (next.onHighwayYAlmost && direction.horizontal) {
                // it's going to go down, but the direction is right/left
                val realMoveTo = point.down.addDirection(direction)
                //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
                // like when
            } else if (next.onHighwayYAlmostBeyond && direction.horizontal) {
                // 104, 113 -> if  here you went one to low, and you want to go back up, you could go right to go up
                // 104, 112
                val realMoveTo = point.up.addDirection(direction)
//                d { " onHighwayYAlmostBeyond point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
            } else if (next.onHighwayXAlmost && direction.vertical) {
                // the way up is blocked, it can go up, but link is actually going to
                // go right instead to get around it and get on the highway
//                // it's going to go right, but the direction is up/down
                val realMoveTo = point.right.addDirection(direction)
                //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
            } else if (next.onHighwayXAlmostBeyond && direction.vertical) {
                // copy and paste
                val realMoveTo = point.left.addDirection(direction)
                neigh.add(realMoveTo)
            }
            else {
//                d { " passable NOT"}
                // going from 143 to 144
                // check corners
                val corner = corner(point, direction)
                if (GStar.DO_CORNERS && corner != null) {
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

    private fun onHighwayAndWithin(point: FramePoint) =
        point.onHighway && point.within()

    private fun passableAndWithin(point: FramePoint) =
        blockPassableMeta(point) && point.within()
        //blockPassable(point) && point.within()
//        blockPassableHalf(point) && point.within()

    fun corner(point: FramePoint, direction: Direction): FramePoint? {
        // direction point is going
        val modifier = direction.pointModifier()
        return if (direction.vertical) {
            // try left right
            framePoint(point, Direction.Right.pointModifier(), modifier) ?:
            framePoint(point, Direction.Left.pointModifier(), modifier)
        } else {
            framePoint(point, Direction.Up.pointModifier(), modifier) ?:
            framePoint(point, Direction.Down.pointModifier(), modifier)
            null
        }
    }

    private fun framePoint(point: FramePoint, dirModifier: (FramePoint) -> FramePoint, modifier: (FramePoint) -> FramePoint):
            FramePoint? {
        val h2 = dirModifier(dirModifier(point))
        val hDir = modifier(h2)
        return if (passableAndWithin(hDir)) {
            h2
        } else {
            val h1 = dirModifier(point)
            val hDir1 = modifier(h1)
            if (passableAndWithin(hDir1)) {
                h1
            } else {
                null
            }
        }
    }

    private fun framePointo(point: FramePoint, horizModifier: (FramePoint) -> FramePoint, modifier: (FramePoint) -> FramePoint):
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
            // if the point
            !halfPassable || (point.x > 208 && isLevel) -> {
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
                if (passable.empty) {
                    d { " Passable empty "}
                }
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
    private fun blockPassable(point: FramePoint): Boolean {
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