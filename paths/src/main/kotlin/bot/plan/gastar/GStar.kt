package bot.plan.gastar

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import util.Map2d
import util.d
import java.util.*

class GStar(
    var passable: Map2d<Boolean>,
    halfPassable: Boolean = true,
    isLevel: Boolean = false
) {
    companion object {
        var DEBUG = false
        private val DEBUG_DIR = false
        val DEBUG_ONE = false
        var MAX_ITER = 10000
        var SHORT_ITER = MAX_ITER // 5000 // not sure if i should use
        val LIMIT_ITERATIONS = false
        val DO_CORNERS = true
        val DO_ADJUST = false
        val DO_MAKE_CORNERS = false

        val nearEnemyCost = 10000
        val onEnemyCost = 100000
        val MaxCostAvoidEnemy = onEnemyCost
        val MaxCostAvoidEnemyNear = nearEnemyCost
    }

    private var iterCount = 0
    // return to this map
//    private val initialMap: Map2d<Int> = passable.map { if (it) 1 else 99999 }
    // if it is on a divisible by 16 area 1, if not it is a 10
//    private val initialMap: Map2d<Int> = passable.map { 1 }

    // this really helps keep zelda on track, it's a little strict though walking half way
    // is also acceptable
    // the game is constantly trying to make link travel on one of these paths
    private val highwayDivisor = 8

    // try x,y
    private val initialMap: Map2d<Int> =
        passable.mapXy { y, x -> if (x % highwayDivisor == 0 || y % highwayDivisor == 0) 1 else 1000 }

    // f values
    private val initialPassable = passable
    var costsF: Map2d<Int> = initialMap.copy()

    private val neighborFinder = NeighborFinder(passable, halfPassable, isLevel)

    fun reset() {
        costsF = initialMap.copy()
    }

    fun resetPassable() {
        passable = initialPassable.copy()
        neighborFinder.passable = passable
    }

    val ENEMY_COST = 1000

    fun setEnemy(from: FramePoint, point: FramePoint, size: Int = 16) {
        // make cost relative to the distance??? so it's not all the same badness
        costsF.modify(
            from,
            FramePoint(point.x - MapConstants.oneGrid, point.y - MapConstants.oneGrid),
            size
        ) { dist, current ->
            current + (100 * (MapConstants.twoGrid - dist)) // the closer the higher
        }

        // the actual enemy should have very very high cost
        costsF.modify(from, point, MapConstants.oneGrid) { dist, current ->
            current + onEnemyCost
        }
    }

    fun setNearEnemy(from: FramePoint, point: FramePoint) {
        costsF.modify(from, point, MapConstants.oneGrid) { dist, current ->
            current + nearEnemyCost
        }
    }

    fun setEnemyBig(from: FramePoint, point: FramePoint, size: Int = 16) {
        // TODO: some enemies are smaller than 16!
        // why a little less?
        costsF.modify(from, point.upLeftOneGrid, MapConstants.twoGrid) { dist, current ->
            current + nearEnemyCost
        }

        // actual enemy higher cost then around the enemy
        costsF.modify(from, point, MapConstants.oneGrid) { dist, current ->
            current + onEnemyCost
        }
    }

    val totalCosts = mutableMapOf<FramePoint, Int>()
    val distanceToGoal = mutableMapOf<FramePoint, Int>()

    private val avoid = mutableListOf<FramePoint>()
    fun clearAvoid() {
        avoid.clear()
    }

    fun route(start: FramePoint, beforeStart: FramePoint? = null, target: FramePoint, makePassable: List<FramePoint> = emptyList()): List<FramePoint> {
        return route(start, listOf(target), pointBeforeStart = beforeStart, enemies = emptyList(),
            forcePassable = makePassable)
    }

    fun setEnemyCosts(from: FramePoint, enemies: List<FramePoint> = emptyList()) {
        reset()
        for (enemy in enemies) {
            d { " set enemy cost for $enemy"}
            setEnemyBig(from, enemy)
        }
        ///???
//        setForcePassable(enemies, setTo = false)
    }

    private fun setForcePassable(passableSpot: List<FramePoint> = emptyList(), setTo: Boolean = true) {
        if (passableSpot.isNotEmpty()) {
            d { " force passable $passableSpot" }
            for (spot in passableSpot) {
                // make it slighly
                passable.modifyTo(spot, MapConstants.oneGrid, setTo)
            }
        }
    }

    // if link cannot get to the target, instantly return an empty route.
    // like if they do not fit
    // if the cost of the path exceeds a maximum, also just dodge and wait
    // instead link should dodge
    // if the created route doesn't actually go to the target, just return an empty one

    // could return a RouteResult with boolean "found target"


    fun route(
        start: FramePoint,
        targets: List<FramePoint>,
        pointBeforeStart: FramePoint? = null,
        enemies: List<FramePoint> = emptyList(),
        avoidNearEnemy: List<FramePoint> = emptyList(),
        forcePassable: List<FramePoint> = emptyList(),
        maximumCost: Int = MaxCostAvoidEnemyNear
    ): List<FramePoint> {
        val nearEnemies = enemies.isNotEmpty()
        val maxIter = if (nearEnemies) SHORT_ITER else MAX_ITER
        d {"Plan: iter = $maxIter enemies ${enemies.size}"}
        resetPassable()
        // only if inside a radius
        setEnemyCosts(start, enemies)
        for (nearEnemy in avoidNearEnemy) {
            setNearEnemy(start, nearEnemy)
        }
        setForcePassable(forcePassable)
//        if (forcePassable.isNotEmpty()) {
//            d {" force passable "}
//            passable.write("forcePassable.csv") { v, x, y ->
//                when {
//                    v -> "."
//                    forcePassable[0].isInGrid(FramePoint(x, y)) -> "L"
//                    else -> "X"
//                }
//            }
//        }

        val closedList = mutableSetOf<FramePoint>()
        val cameFrom = mutableMapOf<FramePoint, FramePoint>()

        val target = targets.toList()

        val openList: PriorityQueue<FramePoint> = PriorityQueue<FramePoint> { cell1, cell2 ->
            //Compares 2 Node objects stored in the PriorityQueue and Reorders the Queue according to the object which has the lowest fValue
//            val cell1Val = totalCosts[cell1] ?: 0
//            val cell2Val = totalCosts[cell2] ?: 0
            // distance that makes link just go straight into death, ignoring cost why?
//            val cell1Val = distanceToGoal[cell1] ?: 0
//            val cell2Val = distanceToGoal[cell2] ?: 0

            val cell1Val = (totalCosts[cell1] ?: 0) + (distanceToGoal[cell1] ?: 0)
            val cell2Val = (totalCosts[cell2] ?: 0) + (distanceToGoal[cell2] ?: 0)

//            d { "sort ${cell1} ${cell2} $cell1Val $cell2Val"}
            if (cell1Val < cell2Val) -1 else if (cell1Val > cell2Val) 1 else 0
        }

        val costFromStart = mutableMapOf(start to 0)

        var point = FramePoint(0, 0)
        openList.add(start)
        iterCount = 0
//        while (true && iterCount < MAX_ITER) {
        while (iterCount < maxIter) {
            iterCount++
            if (DEBUG) {
                d { " ****** ITERATION $iterCount open ${openList.size} ****** " }
            }
            if (DEBUG) {
                openList.forEach {
                    d {
                        " open: ${it.x}, ${it.y} cost ${totalCosts[it]} to " +
                                "goal" +
                                " " +
                                "${distanceToGoal[it]}"
                    }
                }
            }
            // 6.5%
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

            // I think this is it
            val previousPoint = if (iterCount == 1 && pointBeforeStart != null) pointBeforeStart else cameFrom[point]
            val dir = previousPoint?.let {
                directionToDir(it, point)
            } ?: null
            val dist = previousPoint?.let { prev ->
                pointBeforeStart?.let { st ->
                    prev.distTo(pointBeforeStart)
                }
            } ?: null
            if (DEBUG) {
                d { "from prev=$previousPoint to $point ${if (point.onHighway) "*" else ""} dir $dir" }
            }

            val neighbors = (neighborFinder.neighbors(point, dir, dist ?: 0) - closedList - avoid).shuffled()
            for (toPoint in neighbors) {
                // raw cost of this cell
                val cost = costsF.get(toPoint)
                //val cost = getQuadCost(toPoint)
                // add to the cost of the point
                val parentCost = costFromStart[point] ?: 99999
                // route to get to this point including parent
                val pathCost = cost + parentCost

                val costToGoal = toPoint.minDistToAny(target)
                val totalCost = costToGoal + pathCost

                if (DEBUG) {
                    d {
                        " neighbor cost ${toPoint.x} ${toPoint.y} ${toPoint.direction ?: "n"} = $totalCost parent " +
                                "$parentCost toGoal = $costToGoal"
                    }
                }
                val costS = costFromStart.getOrDefault(toPoint, Int.MAX_VALUE)
//                d {" cost: $cost $costS"}
                if (cost < costS && cost < maximumCost) {
                    distanceToGoal[toPoint] = costToGoal
                    costFromStart[toPoint] = pathCost
                    totalCosts[toPoint] = totalCost
                    cameFrom[toPoint] = point
                    if (!openList.contains(toPoint)) {
                        openList.add(toPoint)
                    }
                }
//                } else {
//                    if (DEBUG) {
//                        d { " not passible $it" }
//                    }
//                }
            }

            if (DEBUG_ONE) {
                break
            }
        }

        if (DEBUG) {
            d { " ****** DONE $iterCount ****** " }
        }
        // todo: actually should pick the best path so far..
        return generatePath(target, cameFrom, point)
    }

    private fun getQuadCost(point: FramePoint): Int =
        costsF.get(point) + costsF.get(point.justRightEnd) + costsF.get(point.justLeftDown) + costsF.get(point.justRightEndBottom)

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
            Direction.None -> true //
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

    private fun generatePath(
        targets: List<FramePoint>, cameFrom: Map<FramePoint,
                FramePoint>, lastExplored: FramePoint
    ): List<FramePoint> {
        val target = targets.firstOrNull { cameFrom.containsKey(it) }

        // last explored is a problem
        var current = target ?: lastExplored

        val path = mutableListOf(current)
//        d { " came froms "}
//        for (entry in cameFrom) {
//            d { " ${entry.key} -> ${entry.value}"}
//        }
        while (cameFrom.containsKey(current)) {
            current = cameFrom.getValue(current)
            path.add(0, current)
        }
        if (DEBUG) {
            if (!cameFrom.containsKey(target)) {
                d { "no target use $lastExplored looked for $target" }

                cameFrom.forEach { t, u ->
                    d { " $t -> $u" }
                }
                d { " targets " }
                for (target in targets) {
                    d { " targ $target" }
                }
            }
            d { " start " }
            path.forEach {
                d { "${it.x},${it.y} c ${totalCosts[it]}" }
            }
            d { " end " }
        }
        val pathAdjusted = path.toMutableList()
        if (DO_ADJUST) {
            adjustCorner(pathAdjusted)
        }

        return pathAdjusted.also {
            // this doesn't work well
//            if (it.size > 2) {
//                d { " avoid ${avoid}"}
//                avoid.add(0, it[1])
//            }
//            if (avoid.size > 8) {
//                avoid.removeLast()
//            }
            if (DEBUG) {
                d {
                    it.fold("") { sum, e -> "$sum -> ${e.x},${e.y}${if (e.onHighway) "*" else ""}${if (e.isTopRightCorner) "C" else ""} ${e.direction ?: ""}" }
                        .toString()
                }
            }
        }
    }
}

//-> 128,119*C  -> 128,120*  -> 129,120*  -> 130,120*  -> 131,120*  -> 132,120*
fun adjustCorner(path: MutableList<FramePoint>) {
    if (path.size < 2) return
    val corner = path[0]
    if (!corner.isTopRightCorner) return
    d { "THE CORNER $corner" }
    for (pt in path.take(5)) {
        d { " was pt $pt" }
    }
    path.set(1, path[0].right)
    path.set(2, path[0].right.right)
}