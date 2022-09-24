package bot.plan.gastar

import bot.GamePad
import bot.state.*
import util.Map2d
import util.d
import java.util.*

class GStar(
    private val passable: Map2d<Boolean>,
) {
    private val DEBUG = false
    private val DEBUG_DIR = false
    private val DEBUG_ONE = false
    private val MAX_ITER = 300

    private var iterCount = 0
    // return to this map
    private val initialMap: Map2d<Int> = passable.map { if (it) 1 else 99999 }
    // f values
    private var costsF: Map2d<Int> = initialMap.copy()

    fun reset() {
        costsF = initialMap.copy()
    }

    val ENEMY_COST = 1000

    fun setEnemy(point: FramePoint, size: Int = 16) {
        costsF.modify(point, size) {
            ENEMY_COST
        }
        // then add some cost to points around it
    }

    val totalCosts = mutableMapOf<FramePoint, Int>()
    val distanceToGoal = mutableMapOf<FramePoint, Int>()

    fun route(start: FramePoint, target: FramePoint): List<FramePoint> {
        val closedList = mutableSetOf<FramePoint>()
        val cameFrom = mutableMapOf<FramePoint, FramePoint>()

        val openList: PriorityQueue<FramePoint> = PriorityQueue<FramePoint> { cell1, cell2 ->
            //Compares 2 Node objects stored in the PriorityQueue and Reorders the Queue according to the object which has the lowest fValue
//            val cell1Val = totalCosts[cell1] ?: 0
//            val cell2Val = totalCosts[cell2] ?: 0
            val cell1Val = distanceToGoal[cell1] ?: 0
            val cell2Val = distanceToGoal[cell2] ?: 0
//            d { "sort ${cell1} ${cell2} $cell1Val $cell2Val"}
            if (cell1Val < cell2Val) -1 else if (cell1Val > cell2Val) 1 else 0
        }

        val costFromStart = mutableMapOf(start to 0)

        openList.add(start)
        iterCount = 0
        while (true && iterCount < MAX_ITER) {
            iterCount++
            if (DEBUG) {
                d { " ****** ITERATION $iterCount ****** "}
            }
            if (DEBUG) {
                openList.forEach {
//                    d { " open: ${it.x}, ${it.y} cost ${totalCosts[it]} to " +
//                            "goal" +
//                            " " +
//                            "${distanceToGoal[it]}" }
                }
            }
            val point = openList.poll() ?: break

            if (DEBUG) {
                d { " explore $point" }
            }

            if (point == target) {
                if (DEBUG) {
                    d { " explore found! $point" }
                }
                closedList.add(point)
                break
            }

            closedList.add(point)

            (point.neighbors - closedList).forEach {
                if (passable.get(it)) {
//                if (passableFrom(point, it)) {
                    // raw cost of this cell
                    val cost = costsF.get(it)
                    // add to the cost of the point
                    val parentCost = costFromStart[point] ?: 99999
                    // route to get to this point including parent
                    val pathCost = cost + parentCost

                    val costToGoal = it.distTo(target)
                    val totalCost = costToGoal + pathCost

                    if (DEBUG) {
                        d {
                            " neighbor cost ${it.x} ${it.y} = $totalCost parent " +
                                    "$parentCost toGoal = $costToGoal"
                        }
                    }
                    if (cost < costFromStart.getOrDefault(it, Int.MAX_VALUE)) {
                        distanceToGoal[it] = costToGoal
                        costFromStart[it] = pathCost
                        totalCosts[it] = totalCost
                        cameFrom[it] = point
                        if (!openList.contains(it)) {
                            openList.add(it)
                        }
                    }
                } else {
                    if (DEBUG) {
                        d { " not passible $it" }
                    }
                }
            }

            if (DEBUG_ONE) {
                break
            }
        }

        return generatePath(target, cameFrom)
    }

    private fun passableFrom(from: FramePoint, to: FramePoint): Boolean {
        // which direction is it

        val dir = directionToDir(from, to)
        return when (dir) {
            Direction.Right -> passable.get(from.rightEnd) && passable.get(from.rightEndDown)
            Direction.Left -> passable.get(to) && passable.get(from.leftDown)
            // should be
            // just the to
            Direction.Down -> passable.get(from.downEnd)
                    && passable.get(from.downEndRight)
            Direction.Up -> {
                passable.get(from.upEnd) && passable.get(from.upEndRight)
            }
        }
        //* Can't go down if y + 16, is impassible
        //* can't go right if x + 16 is impassible
        //* can't go up if y + 8 - 1 is impassible (middle of link)
        //* can't go left if x-1 is impassible
//        if (from.rightEnd)
    }

    private fun directionToDir(from: FramePoint, to: FramePoint): Direction {
        return when {
            from.x == to.x -> {
                if (from.y < to.y) Direction.Down else Direction.Up
            }
            from.y == to.y -> {
                if (from.x < to.x) Direction.Right else Direction.Left
            }
            else -> Direction.Left
        }
    }


    private fun directionTo(from: FramePoint, to: FramePoint): GamePad {
        return when {
            from.x == to.x -> {
                if (from.y < to.y) GamePad.MoveDown else GamePad.MoveUp
            }
            from.y == to.y -> {
                if (from.x < to.x) GamePad.MoveRight else GamePad.MoveLeft
            }
            else -> GamePad.MoveLeft
        }
    }

    private fun generatePath(target: FramePoint, cameFrom: Map<FramePoint,
            FramePoint>): List<FramePoint> {
        val path = mutableListOf(target)
        var current = target

        while (cameFrom.containsKey(current)) {
            current = cameFrom.getValue(current)
            path.add(0, current)
        }
        if (DEBUG) {
            d { " start " }
            path.forEach {
                d { "${it.x},${it.y} c ${totalCosts[it]}" }
            }
            d { " end " }
        }

        return path.toList().also {
            if (DEBUG) {
                d {
                    it.fold("") { sum, e -> "$sum -> ${e.x},${e.y}" }
                        .toString()
                }
            }
        }
    }

    // list of passible nodes
//    private val FramePoint.neighbors
//        get() = listOf(this.up, this.down, this.left, this.right).filter {
//            it.x > 0 && it.x < passible.maxX && it.y > 0 && it.y <
//                    passible.maxY && passible.get(it) }

    private val FramePoint.neighborsz: List<FramePoint>
        get() {
            val neigh = mutableListOf<FramePoint>()
//            // go up
//                Direction.Right -> passible.get(from.rightEnd) && passible.get(from.rightEndDown)
//                Direction.Left -> passible.get(to) && passible.get(from.leftDown)
//                // should be
//                // just the to
//                Direction.Down -> passible.get(from.downEnd)
//                        && passible.get(from.downEndRight)
//                Direction.Up -> {
//                }
            if (passable.get(this.upEnd) && passable.get(this.upEndRight)) {
                if (this.up.within()) {
                    if (DEBUG_DIR) d { " up "}
                    neigh.add(this.up)
                }
            } else {
                if (DEBUG_DIR) d { " no up "}
            }
            if (passable.get(this.downEnd) && passable.get(this.downEndRight)) {
                if (this.down.within()) {
                    if (DEBUG_DIR)  d { " down "}
                    neigh.add(this.down)
                }
            } else {
                if (DEBUG_DIR) d { " no down ${passable.get(this.downEnd)}"}
            }
            if (passable.get(this.left) && passable.get(this.leftDown)) {
                if (this.left.within()) {
                    if (DEBUG_DIR) d { " left "}
                    neigh.add(this.left)
                }
            } else {
                if (DEBUG_DIR) d { " no left "}
            }
            if (passable.get(this.rightEnd) && passable.get(this.rightEndDown)) {
                if (this.right.within()) {
                    if (DEBUG_DIR) d { " right "}
                    neigh.add(this.right)
                }
            } else {
                if (DEBUG_DIR) d { " no right end ${this.rightEnd} end down ${this.rightEndDown}"}
            }

            return neigh
        }

    private val FramePoint.neighbors: List<FramePoint>
        get() {
            val neigh = mutableListOf<FramePoint>()
            val up = this.up
            if (blockPassable(up) && up.within()) {
                neigh.add(up)
            }
            val down = this.down
            if (blockPassable(down) && down.within()) {
                neigh.add(down)
            }
            val right = this.right
            if (blockPassable(right) && right.within()) {
                neigh.add(right)
            } else {
                if (DEBUG) {
                    d { " r $right not passable" }
                }
            }
            val left = this.left
            if (blockPassable(left) && left.within()) {
                neigh.add(left)
            }
            return neigh
        }

    private fun blockPassable(point: FramePoint): Boolean {
            return passable.get(point) && passable.get(point.justRightEndBottom)
                    && passable.get(point.justRightEnd)
                    && passable.get(point.justLeftBottom)
        }

        private fun FramePoint.within() =
            x > -1 && x < passable.maxX && y > -1 && y <
                    passable.maxY
}