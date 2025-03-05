package bot.plan.zstar

import bot.state.*
import bot.state.map.*
import util.Map2d
import util.d
import kotlin.random.Random

// list of passible nodes
//    private val FramePoint.neighbors
//        get() = listOf(this.up, this.down, this.left, this.right).filter {
//            it.x > 0 && it.x < passible.maxX && it.y > 0 && it.y <
//                    passible.maxY && passible.get(it) }

class NeighborFinder(
    var passable: Map2d<Boolean>,
    private val halfPassable: Boolean = true,
    /**
     * inside the level, link cannot move halfway into any part of the top two grids
     */
    private val isLevel: Boolean = false
) {
    var costF: Map2d<Int> = Map2d(mutableListOf())

    private val horizontal: List<Direction> = Direction.horizontal
    private val vertical: List<Direction> = Direction.vertical

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
            Direction.None -> all
        }
        return dirs
    }

    // need to know the previous point
    fun neighbors(point: FramePoint, direction: Direction? = null, dist: Int = 1, ladderSpec: ZStar.LadderSpec? = null, from: FramePoint? = null): List<FramePoint> {
        val neigh = mutableListOf<FramePoint>()

        //val validDirections = ladderSpec?.directions(point) ?: Direction.all

        val dontTurnAround = true
        val dirMovingIn: Direction = if (dontTurnAround) {
            from?.dirTo(point)?.opposite() ?: Direction.None
        } else {
            Direction.None
        }
        d { " neighbors point: $point dir: $direction from $from dirMovingIn $dirMovingIn" }

        val validDirections = (ladderSpec?.directions(point) ?: Direction.all) - dirMovingIn

//        var validDirections = okDirections(point, direction, dist)
//        if (GStar.DEBUG) {
//            d { " valid directions ${validDirections.size} = $validDirections" }
//        }

        // this is bad idea when link NEEDS to go down first
        // only go up down when you are close to the exit
        // y > 150
        // y < 5
        // x > 240
//        val validDirections = if (point.y > 150 || point.y < 5) {
//            Direction.vertical
//        } else if (point.x > 240 || point.x < 5) {
//            Direction.horizontal
//        } else {
//            Direction.values().toList()
//        }

        for (direction in validDirections) {
            //val next = SkipLocations.getNext(point, direction)
            val next = direction.pointModifier()(point)
//                if (next.distTo(this) > 1) {
//                    d { "skip loc" }
//                }
            //test passable (112, 129) Down false
            if (ZStar.DEBUG) {
                d { " test passable $next $direction onhway=${next.onHighway}" }
                d { "rb ${passable.get(next.justRightEndBottom)}"}
                d { "re ${passable.get(next.justRightEnd)}"}
                d { "lb ${passable.get(next.justLeftBottom)}"}
                d { "l ${passable.get(next)}"}
            }
//            logPassable(next)
            if (passableAndWithin(next)) {
//                d { " passable! ${neigh.size}"}
                // it's pointless to explore a point link cannot walk on. Link can
                // only walk on highways
                if (next.onHighway) {
                    neigh.add(next)
                }

                // but it's possible link could attempt to move before a corner
                // in order to actually move and not get stuck.
                // this is needed because sometimes random pixels are not accessible by link
                sometimes {
                    addCornerMove(next, direction, point, neigh)
                }
            } else {
                sometimes {
                    addCornerMove(next, direction, point, neigh)
                    // why are these different?
//                    addCornerMoveNotPassable(next, direction, point, neigh)
                }
            }
        }
        return neigh
    }

    private fun addCornerMove(
        next: FramePoint,
        direction: Direction,
        point: FramePoint,
        neigh: MutableList<FramePoint>
    ) {
        // but also add an additional neighbor for using the corner
        // then gstar will selection it randomly
        if (next.onHighwayYAlmost && direction.horizontal) {
            // it's going to go down, but the direction is right/left
            // don't add it if the point it outside
            if (point.down.within()) {
                val realMoveTo = point.down.addDirection(direction)
                //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
            }
        }

        if (next.onHighwayYAlmostBeyond && direction.horizontal) {
            // 104, 113 -> if  here you went one to low, and you want to go back up, you could go right to go up
            // 104, 112
            if (point.up.within()) {
                val realMoveTo = point.up.addDirection(direction)
                neigh.add(realMoveTo)
            }
        }

        if (next.onHighwayXAlmost && direction.vertical) {
            // the way up is blocked, it can go up, but link is actually going to
            // go right instead to get around it and get on the highway
    //                // it's going to go right, but the direction is up/down
            if (point.right.within()) {
                val realMoveTo = point.right.addDirection(direction)
                //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
            }
        }

        if (next.onHighwayXAlmostBeyond && direction.vertical) {
            // copy and paste
            if (point.left.within()) {
                val realMoveTo = point.left.addDirection(direction)
                neigh.add(realMoveTo)
            }
        }
    }

    private fun addCornerMoveNotPassable(
        next: FramePoint,
        direction: Direction,
        point: FramePoint,
        neigh: MutableList<FramePoint>
    ) {
        if (next.onHighwayYAlmost && direction.horizontal) {
            // it's going to go down, but the direction is right/left
            if (point.down.within()) {
                val realMoveTo = point.down.addDirection(direction)
                //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
            }
            // like when
        } else if (next.onHighwayYAlmostBeyond && direction.horizontal) {
            // 104, 113 -> if  here you went one to low, and you want to go back up, you could go right to go up
            // 104, 112
            if (point.up.within()) {
                val realMoveTo = point.up.addDirection(direction)
                //                d { " onHighwayYAlmostBeyond point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
            }
        } else if (next.onHighwayXAlmost && direction.vertical) {
            // the way up is blocked, it can go up, but link is actually going to
            // go right instead to get around it and get on the highway
            //                // it's going to go right, but the direction is up/down
            if (point.right.within()) {
                val realMoveTo = point.right.addDirection(direction)
                //d { " onHighwayYAlmost point $point $direction real: $realMoveTo" }
                neigh.add(realMoveTo)
            }
        } else if (next.onHighwayXAlmostBeyond && direction.vertical) {
            // copy and paste
            if (point.left.within()) {
                val realMoveTo = point.left.addDirection(direction)
                neigh.add(realMoveTo)
            }
        } else {
            //                d { " passable NOT"}
            // going from 143 to 144
            // check corners
            val corner = corner(point, direction)
            if (ZStar.DO_CORNERS && corner != null) {
                if (ZStar.DEBUG) {
                    d { " add corner $corner $direction" }
                }
                neigh.add(corner.addDirection(direction))
            }
            if (ZStar.DEBUG) {
                d { " corner $point $next $direction not passable" }
            }
        }
    }

    private fun sometimes(block: () -> Unit) {
        if (true || Random.nextInt(2) == 0) {
//            block()
        }
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

    fun passableAndWithin(point: FramePoint) =
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
                blockPassable(point) // && blockSafe(point)
            }
            // check point end end
            // was <= 32
            // (isLevel && point.y <= 24)
            (isLevel && point.y <= 32) && (!passable.get(point) || !passable.get(point.justRightEnd)) -> {
                if (ZStar.DEBUG) {
                    d { " failed cuz y (pt: $point) < 32 ${passable.get(point)} ${passable.get(point.justRightEnd)}" }
                }
                false
            }
            else -> {
                if (passable.empty) {
                    d { " Passable empty "}
                }
                blockPassableHalf(point) // && blockSafeHalf(point)
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

    private fun blockSafe(point: FramePoint): Boolean {
        return costF.safe(point) && costF.safe(point.justRightEndBottom)
                && costF.safe(point.justRightEnd)
                && costF.safe(point.justLeftBottom)
    }

    fun Map2d<Int>.safe(point: FramePoint): Boolean =
        get(point) < 100
    /**
     * test at mid
     */
    private fun blockPassableHalf(point: FramePoint): Boolean {
        return passable.get(point.justMid)
                && passable.get(point.justMidEnd)
                && passable.get(point.justRightEndBottom)
                && passable.get(point.justLeftBottom)
    }

    private fun blockSafeHalf(point: FramePoint): Boolean {
        return costF.safe(point.justMid)
                && costF.safe(point.justMidEnd)
                && costF.safe(point.justRightEndBottom)
                && costF.safe(point.justLeftBottom)
    }

    private fun FramePoint.within() =
        x > -1 && x < passable.maxX && y > -1 && y <
                passable.maxY
}