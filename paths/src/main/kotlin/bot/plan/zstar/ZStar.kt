package bot.plan.zstar

import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.plan.action.RouteTo
import bot.plan.action.isInGrid
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import util.Map2d
import util.d
import java.util.*

/**
 * a star implementations modified for zelda routing
 */
class ZStar(
    var passable: Map2d<Boolean>,
    halfPassable: Boolean = true,
    isLevel: Boolean = false
) {
    companion object {
        var DEBUG = false
        private val DEBUG_DIR = false
        val DEBUG_ONE = false

        // at 1000, link cant go into the squares
        // at 10000, it is slow
        var MAX_ITER = 10000 // not sure if need more than this
        var SHORT_ITER = MAX_ITER // 5000 // not sure if i should use
        val LIMIT_ITERATIONS = false
        val DO_CORNERS = true
        val DO_MAKE_CORNERS = false

        val nearEnemyCost = 10000
        val onEnemyCost = 100000
        val MaxCostAvoidEnemy = onEnemyCost
        val MaxCostAvoidEnemyNear = nearEnemyCost
        const val ENEMY_COST = 10000
    }

    val maximumCost: Int = MaxCostAvoidEnemyNear

    data class LadderSpec(val horizontal: Boolean, val point: FramePoint) {
        private fun isIn(other: FramePoint) = point.isInGrid(other)

        fun directions(other: FramePoint) = if (false && isIn(other)) {
            if (DEBUG) {
                d { "on ladder $other horiz=$horizontal" }
            }
            if (horizontal) Direction.horizontal else Direction.vertical
        } else {
            if (DEBUG) {
                d { "on ladder no $other not in $point" }
            }
            Direction.all
        }
    }

    private var iterCount = 0

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

    private val totalCosts = mutableMapOf<FramePoint, Int>()
    private val distanceToGoal = mutableMapOf<FramePoint, Int>()
    private val pathSizeToGoal = mutableMapOf<FramePoint, Int>()

    private val avoid = mutableListOf<FramePoint>()


    // if link cannot get to the target, instantly return an empty route.
    // like if they do not fit
    // if the cost of the path exceeds a maximum, also just dodge and wait
    // instead link should dodge
    // if the created route doesn't actually go to the target, just return an empty one
    // could return a RouteResult with boolean "found target"

    val customizer = GridCustomizer()

    data class ZRouteParam(
        val start: FramePoint,
        val targets: List<FramePoint>,
        val pointBeforeStart: FramePoint? = null,
        val enemies: List<FramePoint> = emptyList(),
        val rParam: RouteTo.RoutingParamCommon = RouteTo.RoutingParamCommon(),
    )

    fun route(
        start: FramePoint,
        beforeStart: FramePoint? = null,
        target: FramePoint,
        makePassable: List<FramePoint> = emptyList()
    ): List<FramePoint> {
        return route(
            ZRouteParam(
                start = start, targets = listOf(target), pointBeforeStart = beforeStart, enemies = emptyList(),
                rParam = RouteTo.RoutingParamCommon(
                    forcePassable = makePassable
                ),
            )
        )
    }

    private fun sum(): Int {
        var sum = 0
        for (costRow in costsF.map) {
            sum += costRow.sum()
        }
        return sum
    }

    private fun Map2d<Int>.safe(point: FramePoint): Boolean =
        get(point) < 1000

    fun routeNearestSafe(
        param: ZRouteParam
    ): List<FramePoint> {
        customizer.customize(param)
        return breadthSearch(param.start, param.targets)
    }

    // if safe needed
    // BFS
    // greedy (if fail)
    // astar

    /**
     * find the path that gets closest to the goal
     */
    private fun greedySearch() {

    }

    private fun breadthSearch(current: FramePoint,
                              targets: List<FramePoint>,
                              visited: MutableSet<FramePoint> = mutableSetOf()): List<FramePoint> {
        val toExplore = neighborFinder.neighbors(current).toMutableList()
        val cameFrom = mutableMapOf<FramePoint, FramePoint>()
        var finalPoint = FramePoint()

        d { " targets $targets start $current" }
        var i = 0
        while (toExplore.isNotEmpty()) {
            if (DEBUG) {
                d { "$i: open nodes: ${toExplore.size} : $toExplore" }
            }
            for (nearPoint in toExplore.toMutableList()) {
                if (DEBUG) {
                    d { " -->explore $nearPoint" }
                }
                if (costsF.safe(nearPoint)) {
//                if (nearPoint in targets) {
                    if (DEBUG) {
                        d { " Found end!!" }
                    }
                    finalPoint = nearPoint
                    toExplore.clear()
                    break;
                } else {
                    visited.add(nearPoint)
                    val neighbors = neighborFinder.neighbors(nearPoint) - visited
                    for (neighbor in neighbors) {
                        cameFrom[neighbor] = nearPoint
                    }
                    toExplore.addAll(neighbors)
                }
                i++
            }
        }

        val path = mutableListOf(finalPoint)
        if (DEBUG) {
            d { " came froms " }
            for (entry in cameFrom) {
                d { " ${entry.key} -> ${entry.value}" }
            }
            d { " came from $finalPoint" }
        }

        var lastPoint = finalPoint
        while (cameFrom.containsKey(lastPoint)) {
            lastPoint = cameFrom.getValue(lastPoint)
            path.add(0, lastPoint)
        }

        d { " the final path is $path"}
        return path
    }

    private fun targetAnalysis() {
        // determine
    }

    private fun goalFunction() {

    }

    fun route(
        param: ZRouteParam
    ): List<FramePoint> {
        customizer.customize(param)
        val maxIter = MAX_ITER
        val targets = if (false && param.rParam.mapNearest) {
            param.targets.flatMap { NearestSafestPoint.nearestSafePoints(it, costsF, passable) }
        } else {
            param.targets
        }

        //writePassableFile(param.forcePassable)

        val closedList = mutableSetOf<FramePoint>()
        val cameFrom = mutableMapOf<FramePoint, FramePoint>()

        // really what I want is it to just seek ANY safe point
        var routeToSafe = false

        val startIsSafe = costsF.safe(param.start)

        // testing
        if (param.rParam.findNearestSafeIfCurrentlyNotSafe == true && !startIsSafe) {
            d { " dodge! "}
            return routeNearestSafe(param)
        }

        // can result in NO_ROUTE, if already at target, or if already safe without routing anywhere
        val target = targets.toList().filter { costsF.safe(it) }.ifEmpty {
            routeToSafe = !startIsSafe
            if (routeToSafe) {
                d { " route to safe " }
            } else {
                d { " route to extra points " }
            }
            targets.flatMap { listOf(it.leftOneGrid, it.upOneGrid, it.downOneGrid, it.rightOneGrid,
                it.leftTwoGrid, it.upTwoGrid, it.downTwoGrid, it.rightTwoGrid ) }.filter { costsF.safe(it) }
        }.filter { costsF.safe(it) }
            // if all else fails just go with the original list
            .ifEmpty { targets.toList() }

        // stil getting NO ROUTE
        d { "From safe: $startIsSafe cost = ${costsF.get(param.start)} targets size = ${target.size}"}

        val openList: PriorityQueue<FramePoint> = PriorityQueue<FramePoint> { cell1, cell2 ->
            val cell1Val = (totalCosts[cell1] ?: 0) + (distanceToGoal[cell1] ?: 0)
            val cell2Val = (totalCosts[cell2] ?: 0) + (distanceToGoal[cell2] ?: 0)
            if (cell1Val < cell2Val) -1 else if (cell1Val > cell2Val) 1 else 0
        }

        val costFromStart = mutableMapOf(param.start to 0)
        var pointClosestToGoal = FramePoint()
        var pointClosestToGoalPathSize = Int.MAX_VALUE

        var point = FramePoint(0, 0)
        openList.add(param.start)
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

            // enemy target is always null currently, this is going to route to nearest
            // which is what we want anyway I think
            val done = if (param.rParam.finishWithinStrikingRange) {
                val inLongRange = param.rParam.finishWithinLongStrikingRange &&
                        // hard to do without direction..
                        AttackLongActionDecider.inStrikingRange(point, enemies = param.enemies)
                AttackActionDecider.inStrikingRange(point, enemies = param.enemies)
            } else if (routeToSafe && costsF.safe(point)) {
                true
            } else {
                target.contains(point) // && costsF.safe(point)
            }
            if (done) {
                if (DEBUG) {
                    d { " explore found: $point" }
                }
                closedList.add(point)
                break
            }

            closedList.add(point)

            // I think this is it
            val previousPoint =
                if (iterCount == 1 && param.pointBeforeStart != null) param.pointBeforeStart else cameFrom[point]
            val dir = previousPoint?.let {
                directionToDir(it, point)
            } ?: null
            val dist = previousPoint?.let { prev ->
                param.pointBeforeStart?.let { st ->
                    prev.distTo(param.pointBeforeStart)
                }
            } ?: null
            if (DEBUG) {
                d { "from prev=$previousPoint to $point ${if (point.onHighway) "*" else ""} dir $dir" }
            }

            neighborFinder.costF = costsF
            val neighbors =
                (neighborFinder.neighbors(point, dir, dist ?: 0, param.rParam.ladderSpec) - closedList - avoid).shuffled()
            for (toPoint in neighbors) {
                // raw cost of this cell
                val cost = costsF.get(toPoint)
                //val cost = getQuadCost(toPoint)
                // add to the cost of the point
                val parentCost = costFromStart[point] ?: 99999
                // route to get to this point including parent
                val pathCost = cost + parentCost

                // min distance to link
                val minDistToLong = toPoint.distTo(param.start)
                val minDistToTarget = toPoint.minDistToAny(target)
                val costToGoal = if (routeToSafe) minDistToLong else minDistToTarget
                val totalCost = costToGoal + pathCost

                if (DEBUG) {
                    d {
                        " neighbor cost ${toPoint.x} ${toPoint.y} ${toPoint.direction ?: "n"} = $totalCost parent " +
                                "$parentCost toGoal = $costToGoal"
                    }
                }
                val costS = costFromStart.getOrDefault(toPoint, Int.MAX_VALUE)
//                d {" cost: $cost $costS"}
                //  cost < maximumCost failed attempt to discourage
                // link from walking into enemies
                if (cost < costS) { // && cost < maximumCost) {
                    // todo: prefer short path, so weight path length vs. distance to
//                    pathSizeToGoal[toPoint] = pathSize(cameFrom, toPoint)
                    if (pointClosestToGoal.isZero ||
                        costToGoal < (distanceToGoal[pointClosestToGoal] ?: Int.MAX_VALUE)
                    ) {
//                        val pathSize = pathSize(cameFrom, toPoint)
                        // if distance to goal is same, select based on path
//                        pointClosestToGoalPathSize = pathSize
                        pointClosestToGoal = toPoint
                    }
                    distanceToGoal[toPoint] = costToGoal
                    costFromStart[toPoint] = pathCost
                    totalCosts[toPoint] = totalCost
                    cameFrom[toPoint] = point
                    // needs to test equality of directions
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
        // if there is no goal, then use the closest point to the goal
        return generatePath(target, cameFrom, pointClosestToGoal).also {
            if (it.isEmpty() || it.size == 1) {
                if (DEBUG) {
                    d { " ****** EMPTY ****** " }
                }
//                writePassable(param.start)
            }
        }
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

    private fun pathSize(cameFrom: Map<FramePoint, FramePoint>, from: FramePoint): Int {
        var current = from

        val path = mutableListOf(current)
//        d { " came froms "}
//        for (entry in cameFrom) {
//            d { " ${entry.key} -> ${entry.value}"}
//        }
        var size = 0
        while (cameFrom.containsKey(current)) {
            current = cameFrom.getValue(current)
            size += 1
        }

        return size
    }

    private fun generatePath(
        targets: List<FramePoint>,
        cameFrom: Map<FramePoint, FramePoint>,
        lastExplored: FramePoint
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

    fun clearAvoid() {
        avoid.clear()
    }

    private fun writePassableFile(forcePassable: List<FramePoint>) {
        if (forcePassable.isNotEmpty()) {
            d { " force passable " }
            passable.write("forcePassable.csv") { v, x, y ->
                when {
                    v -> "."
                    forcePassable[0].isInGrid(FramePoint(x, y)) -> "L"
                    else -> "X"
                }
            }
        }
    }

    private fun writePassable(start: FramePoint) {
        passable.write("passable.csv") { v, x, y ->
            when {
                x == start.x && y == start.y -> "L"
                v -> "."
                else -> "X"
            }
        }
    }

    inner class GridCustomizer {
        fun customize(param: ZRouteParam) {
            val startSum = sum()
            d { "Plan: iter = enemies ${param.enemies.size} near $startSum" }
            resetPassable()
            // only if inside a radius
//            setEnemyCosts(param.start, param.enemies)
            // fails, why?
            setEnemyCostsByIntersect(param.enemies)
            setForceHighCost(param.rParam.forceHighCost)
            setForcePassable(param.rParam.forcePassable)
            setZeroCost(param.rParam.attackTarget)
        }

        fun reset() {
            costsF = initialMap.copy()
        }

        private fun resetPassable() {
            passable = initialPassable.copy()
            neighborFinder.passable = passable
        }

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

        private fun setNearEnemy(from: FramePoint, point: FramePoint) {
            costsF.modify(from, point, MapConstants.oneGrid) { dist, current ->
                current + nearEnemyCost
            }
        }

        fun setEnemyBig(from: FramePoint, point: FramePoint) {
//            costsF.modify(from, point.upLeftOneGrid, MapConstants.twoGrid) { _, current ->
//                current + nearEnemyCost
//            }
            d { " enemy cost modify from ${point.upHalfLeftOneGrid}" }
            // try it
            costsF.modify(
                from,
                point.upLeftHalfOneGrid,
                sizeWide = MapConstants.twoGrid,
                sizeTall = MapConstants.oneGridPoint5
//                sizeWide = MapConstants.threeGrid,
//                sizeTall = MapConstants.twoGridPoint5
            ) { _, current ->
                current + nearEnemyCost
            }

            // actual enemy higher cost then around the enemy
            costsF.modify(from, point, MapConstants.oneGrid) { _, current ->
                current + nearEnemyCost //onEnemyCost
            }
        }

        fun setEnemyCosts(from: FramePoint, enemies: List<FramePoint> = emptyList()) {
            reset()
            for (enemy in enemies) {
                d { " set enemy cost for $enemy" }
                setEnemyBig(from, enemy)
            }
            ///???
//        setForcePassable(enemies, setTo = false)
        }

        private fun setEnemyCostsByIntersect(enemies: List<FramePoint> = emptyList()) {
            reset()
//            val enemyRect = enemies.map { it.toRectPlus(MapConstants.halfGrid) }
            val enemyRect = enemies.map { it.toRect() }

            d { " set enemy cost for intersecting" }
            costsF.mapXyCurrent { x, y, current ->
//                val pt = FramePoint(x,y).toRectPlus(MapConstants.halfGrid)
                val pt = FramePoint(x,y).toRect() //Plus(MapConstants.oneGrid)

//                val cost = enemyRect.sumOf { 1 / it.distTo(pt) } * nearEnemyCost
//                (current + cost).toInt()

                if (enemyRect.any { pt.intersect(it) }) {
                    current + nearEnemyCost
                } else {
                    current
                }
            }
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

        private fun setForceHighCost(grids: List<FramePoint> = emptyList()) {
            for (grid in grids) {
                d { " set highcost $grid" }
                costsF.modifyTo(grid, MapConstants.oneGrid, ENEMY_COST)
            }
        }

        private fun setZeroCost(target: FramePoint?) {
            target?.let {
                d { "set zero cost $target" }
                // actual enemy higher cost then around the enemy
                costsF.modifyTo(target, MapConstants.oneGrid, 0)
            }
        }
    }
}
