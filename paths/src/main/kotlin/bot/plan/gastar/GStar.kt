package bot.plan.gastar

import bot.GamePad
import bot.state.*
import util.Map2d
import util.d
import java.util.*

class GStar(
    private val passable: Map2d<Boolean>,
) {
    companion object {
        var DEBUG = false
        private val DEBUG_DIR = false
        val DEBUG_ONE = false
        val MAX_ITER = 300
        val doSkip = true
    }

    private var iterCount = 0
    // return to this map
    private val initialMap: Map2d<Int> = passable.map { if (it) 1 else 99999 }
    // f values
    private var costsF: Map2d<Int> = initialMap.copy()

    private val neighborFinder = NeighborFinder(passable)

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
        return route(start, listOf(target))
    }

    fun route(start: FramePoint, targets: List<FramePoint>): List<FramePoint> {
        val closedList = mutableSetOf<FramePoint>()
        val cameFrom = mutableMapOf<FramePoint, FramePoint>()

        val target = targets.toList()

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

        var point = FramePoint(0, 0)
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
            point = openList.poll() ?: break

            if (DEBUG) {
                d { " explore $point" }
            }

//            if (point == target) {
            if (target.contains(point)) {
                if (DEBUG) {
                    d { " explore found! $point" }
                }
                closedList.add(point)
                break
            }

            closedList.add(point)

            (neighborFinder.neighbors(point) - closedList).forEach {
                if (passable.get(it)) {
//                if (passableFrom(point, it)) {
                    // raw cost of this cell
                    val cost = costsF.get(it)
                    // add to the cost of the point
                    val parentCost = costFromStart[point] ?: 99999
                    // route to get to this point including parent
                    val pathCost = cost + parentCost

                    val costToGoal = it.minDistToAny(target)
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

        // todo: actually should pick the best path so far..
        return generatePath(target, cameFrom, point)
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

    private fun generatePath(targets: List<FramePoint>, cameFrom: Map<FramePoint,
            FramePoint>, lastExplored: FramePoint): List<FramePoint> {
        val target = targets.firstOrNull { cameFrom.containsKey(it) }
//        var current = target
        var current = target ?: lastExplored

        val path = mutableListOf(current)
        while (cameFrom.containsKey(current)) {
            current = cameFrom.getValue(current)
            path.add(0, current)
        }
        if (DEBUG) {
            if (!cameFrom.containsKey(target)) {
                d { "no target use $lastExplored"}

                cameFrom.forEach { t, u ->
                    d { " $t -> $u"}
                }
            }
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
}