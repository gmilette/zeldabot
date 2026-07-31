package bot.plan.zstar

import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.plan.action.NavUtil
import bot.plan.action.RouteTo
import bot.plan.action.isInGrid
import bot.plan.zstar.route.BreadthFirstSearch
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import util.JsonFile
import util.Map2d
import util.d
import java.util.*

/**
 * a star implementations modified for zelda routing
 */
class ZStar(
    private var passable: Map2d<Boolean>,
    private val halfPassable: Boolean = true,
    isLevel: Boolean = false
) {
    fun passable(): Map2d<Boolean> {
        return passable
    }
    private val allPassable = passable.copy().mapXy { i, i2 -> true }
    companion object {
        var DEBUG = false
        var DEBUG_B = false
        var DEBUG_V = false
        private val DEBUG_DIR = false
        val DEBUG_ONE = false

        // at 1000, link cant go into the squares
        // at 10000, it is slow, cant find way out of level3 angle room
        var MAX_ITER = 10000 // not sure if need more than this
        var SHORT_ITER = MAX_ITER // 5000 // not sure if i should use
        val LIMIT_ITERATIONS = false
        val DO_CORNERS = true
        val DO_MAKE_CORNERS = false

        val nearEnemyCost = 10000
        val onEnemyCost = 100000
        // this would make it safe
//        val HIGH_COST = nearEnemyCost - 1000 // less than near enemy so app can tell
        // a little worse than just plain near enemy
        val HIGH_COST = nearEnemyCost + 1000
//        val HIGH_COST = onEnemyCost - 1000// this makes link avoid the swords even more, but also attack less
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

    val neighborFinder = NeighborFinder(passable, halfPassable, isLevel)

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
        val projectiles: List<FramePoint> = emptyList(),
        val pointBeforeStart: FramePoint? = null,
        val enemies: List<FramePoint> = emptyList(),
        val isGoal: (FramePoint) -> Boolean = { false },
        val rParam: RouteTo.RoutingParamCommon = RouteTo.RoutingParamCommon(),
    )

    private fun sum(): Int {
        var sum = 0
        for (costRow in costsF.map) {
            sum += costRow.sum()
        }
        return sum
    }

    private fun Map2d<Int>.safe(point: FramePoint): Boolean =
        get(point) < 1000

    private fun Map2d<Int>.safeFromIntersect(point: FramePoint): Boolean =
        get(point) < nearEnemyCost // note: if a cell is "high cost" it will be less than this too

    private val file = JsonFile("routeInput", "safe")

    fun routeNearestSafe(
        param: ZRouteParam
    ): List<FramePoint> {
        if (DEBUG_B) {
            // keeps overwriting it
            file.write(param)
        }
        customizer.customize(param)
        return breadthSearch(param, param.start, targets = param.targets,
            pointBeforeStart = param.pointBeforeStart)
    }

    /**
     * find the path that gets closest to the goal
     */
    private fun greedySearch() {

    }

    // for each step in the breadth first search, move the enemies closer to link
    // regardless if the enemy is moving in the direction as link, just assume it's going
    // to do the worst thing
    // if I know the direction the enemy is facing, then be smart.
    // some enemies only travel on the highways
    /**
     * @param time how long to progress enemy
     */
    private fun progressEnemies(speed: Int = 1, time: Int) {
        // for now, only progress projectiles, how fast do they go? let's say 1 pixel per frame?
    }

    private fun breadthSearch(
        param: ZRouteParam,
        current: FramePoint,
        targets: List<FramePoint>,
        pointBeforeStart: FramePoint?,
        visited: MutableSet<FramePoint> = mutableSetOf()
    ): List<FramePoint> {
        d { " find neighbors bf "}
        val toExplore = neighborFinder.neighbors(current, from = pointBeforeStart).toMutableList()
        val cameFrom = mutableMapOf<FramePoint, FramePoint>()
        var finalPoint = FramePoint()

        // doesn't work. more testing needed
        // d { "start dodge targets $targets start $current toExplore: ${toExplore.size}" }
        var i = 0
        while (toExplore.isNotEmpty() && i < MAX_ITER) {
            if (DEBUG_B) {
                d { "$i: open nodes: ${toExplore.size} : $toExplore" }
            }
            // needs way more testing
//            customizer.setAllEnemyCosts(param, i)
            for (nearPoint in toExplore.toMutableList()) {
                if (DEBUG_B) {
                    d { "$i: -->explore $nearPoint cost=${costsF.get(nearPoint)} safe=${costsF.safe(nearPoint)} safeIntersect=${costsF.safeFromIntersect(nearPoint)}" }
                }
                // check if it intersects any enemies
///                if (costsF.safeFromIntersect(nearPoint)) {
                if (costsF.safe(nearPoint)) {
//                if (nearPoint in targets) {
                    finalPoint = nearPoint
                    toExplore.clear()
                    break;
                } else {
                    visited.add(nearPoint)
                    val neighbors = neighborFinder.neighbors(nearPoint, from = current) - visited
                    for (neighbor in neighbors) {
                        cameFrom[neighbor] = nearPoint
                    }
                    // evaluate all the neighbors, and randomly pick minimal cost
                    toExplore.addAll(neighbors)
                }
                i++
            }
        }

        val path = mutableListOf(finalPoint)
        if (DEBUG_B) {
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

        // add the current point because the route code assumes the current
        // point to be the first one in the path
        path.add(0, current)
        if (DEBUG_B) {
            d { " the final path is $path" }
        }
        return path
    }

    private fun targetAnalysis() {
        // determine
    }

    fun setNeighborFinder(
        param: ZRouteParam
    ) {
        customizer.customize(param)
        neighborFinder.costF = costsF
        neighborFinder.passable = passable
    }

    fun routeWithBfs(
        param: ZRouteParam,
        isGoal: (FramePoint) -> Boolean
    ): List<FramePoint> {
        // what kind of route? Safety? Route to shoot? Route to stab?
        setNeighborFinder(param)
        val search = BreadthFirstSearch(isGoal, neighborFinder)
        if (search.isTheGoal(param.start)) {
            return emptyList()
        }
        val route = search.breadthFirstSearch(param.start).firstOrNull() ?: emptyList()
        d { " Route with bfs is ${route}"}
        return route
    }

    fun route(
        param: ZRouteParam
    ): List<FramePoint> {
        // used for debugging
        val totalCosts = mutableMapOf<FramePoint, Int>()
        // used for debugging
        val distanceToGoal = mutableMapOf<FramePoint, Int>()

        setNeighborFinder(param)

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
        } else {
            d { " make the route " }
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

        d { " target before ${targets.size} now ${target.size}"}

        // stil getting NO ROUTE
//        d { "From safe: $startIsSafe cost = ${costsF.get(param.start)} targets size = ${target.size} routeToSafe=${routeToSafe}"}
        d { " targets zstar: "}
        for (framePoint in target) {
            d { "$framePoint"}
        }

        val openList = PriorityQueue<OpenNode>(compareBy { it.priority })

        val costFromStart = mutableMapOf(param.start to 0)
        var pointClosestToGoal = FramePoint()
        var pointClosestToGoalPathSize = Int.MAX_VALUE

        openList.add(OpenNode(param.start, 0))

        iterCount = 0
//        while (true && iterCount < MAX_ITER) {
        while (iterCount < maxIter) {
            iterCount++
            if (DEBUG) {
                d { " ****** ITERATION $iterCount open ${openList.size} ****** " }
            }
            if (DEBUG) {
                openList.forEach { node ->
                    val pt = node.point
                    d {
                        " open: ${pt.x}, ${pt.y} cost ${totalCosts[pt]} to " +
                                "goal" +
                                " " +
                                "${distanceToGoal[pt]}"
                    }
                }
            }
            // 6.5%
            val node = openList.poll() ?: break
            val point = node.point
            if (point in closedList) continue

            if (DEBUG) {
                d { " explore $point" }
            }

            // enemy target is always null currently, this is going to route to nearest
            // which is what we want anyway I think
            var doneBecause = "unknown"
            // iterCount > 0 check is here so that this doesn't return an empty route
            // if the current point really was in range of striking it wouldn't have entered
            // this routine
            val done = when {
                param.rParam.finishWithinStrikingRange && param.isGoal(point) -> {
                    doneBecause = "goal"
                    true
                }
                (false && iterCount > 0 && param.rParam.finishWithinStrikingRange) -> {
                    doneBecause = "strike"
                    val inLongRange = param.rParam.finishWithinLongStrikingRange &&
                            // hard to do without direction..
                            AttackLongActionDecider.inStrikingRange(point, enemies = param.enemies)
                    // this does match how inRangeOf determines attacking
                    // TODO: Ignores sword in front of!!
                    AttackActionDecider.inStrikingRange(point, enemies = param.enemies)
                }
                (routeToSafe && costsF.safe(point)) -> {
                    doneBecause = "safe"
                    true
                }
                else -> {
                    doneBecause = "at target"
                    target.contains(point) // && costsF.safe(point)
                }
            }

            if (done) {
                if (DEBUG) {
                    d { " explore found: $point done because $doneBecause" }
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
            val fromPoint = cameFrom[point]
            d { " find neighbors "}
            val neighbors =
                (neighborFinder.neighbors(point, dir, dist ?: 0, from = fromPoint) - closedList - avoid).shuffled()
            if (neighbors.isEmpty()) {
                d(DEBUG) { " no neighbors for $point closed: $closedList avoid: $avoid" }
            } else {
                d(DEBUG) { "found neighbors for $point: ${neighbors.size}" }
            }
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
                //  cost < maximumCost failed attempt to discourage
                if (pathCost < costS) {
                    // todo: prefer short path, so weight path length vs. distance to
                    if (pointClosestToGoal.isZero ||
                        costToGoal < (distanceToGoal[pointClosestToGoal] ?: Int.MAX_VALUE)
                    ) {
                        pointClosestToGoal = toPoint
                    }
                    distanceToGoal[toPoint] = costToGoal
                    costFromStart[toPoint] = pathCost
                    totalCosts[toPoint] = totalCost
                    cameFrom[toPoint] = point
                    // needs to test equality of directions
                    openList.add(OpenNode(toPoint, totalCost))
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

        d { " route iterations $iterCount"}

        if (DEBUG) {
            d { " ****** DONE $iterCount ****** " }
        }
        // todo: actually should pick the best path so far..
        // if there is no goal, then use the closest point to the goal
        return generatePath(target, cameFrom, pointClosestToGoal, totalCosts).also {
            if (it.isEmpty() || it.size == 1) {
                if (DEBUG) {
                    d { " ****** EMPTY ****** " }
                }
//                writePassable(param.start)
            }
        }
    }

    private fun directionToDir(from: FramePoint, to: FramePoint): Direction {
        return NavUtil.directionToDir(from, to)
    }

    private fun pathSize(cameFrom: Map<FramePoint, FramePoint>, from: FramePoint): Int {
        var current = from
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
        lastExplored: FramePoint,
        totalCosts: Map<FramePoint, Int> = emptyMap()
    ): List<FramePoint> {
        val target = targets.firstOrNull { cameFrom.containsKey(it) }

        // last explored is a problem
        var current = target ?: lastExplored

        val path = mutableListOf(current)
        while (cameFrom.containsKey(current)) {
            current = cameFrom.getValue(current)
            path.add(0, current)
        }
        if (DEBUG) {
            if (!cameFrom.containsKey(target)) {
                d { "no target use $lastExplored looked for $target" }

                cameFrom.forEach { (t, u) ->
                    d { "came from $t -> $u" }
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
        // !!! need to remove the DIR or it just doesnt work
        val pathAdjusted = path.toMutableList().map { it.noDir() }

        return pathAdjusted.also {
            if (DEBUG) {
                d {
                    it.fold("") { sum, e -> "$sum -> ${e.x},${e.y}${if (e.onHighway) "*" else ""}${if (e.isTopRightCorner) "C" else ""} ${e.direction ?: ""}" }
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
            d { "Plan: iter = enemies ${param.enemies.size}" }
            resetPassable(param.start)
            setAllEnemyCosts(param)
            setForcePassable(param.rParam.forcePassable)
        }

        fun reset() {
            costsF = initialMap.copy()
        }

        private fun resetPassable(start: FramePoint) {
            // and no neighbors
            d { " reset passable " }
            val initialNeighbors = neighborFinder.neighbors(start, Direction.None)
            passable = if (initialNeighbors.isNotEmpty() && neighborFinder.passableAndWithin(start)) {
                initialPassable.copy()
            } else {
                d { " !!! ALL PASSABLE !!! ESCAPE!"}
                // make current block passable
                initialPassable.copy().also {
                    it.modifyTo(start, 16, true)
                }
            }
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
            d { " enemy cost modify from ${point.upHalfLeftOneGrid}" }
            // try it
            costsF.modify(
                from,
                point.upLeftHalfOneGrid,
                sizeWide = MapConstants.twoGrid,
                sizeTall = MapConstants.oneGridPoint5
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
        }

        private fun setAllEnemyCosts(param: ZRouteParam, progress: Int = 0) {
            setEnemyCostsByIntersect(param.enemies, param.projectiles, progress)
            setForceHighCost(param.rParam.forceHighCost)
        }

        private fun setEnemyCostsByIntersect(enemies: List<FramePoint> = emptyList(),
                                             projectiles: List<FramePoint>,
                                             progress: Int = 0) {
            reset()
            // it takes about 4 to turn around
            val turnAroundTolerance = 0 // eh, maybe helps a little but no
            val enemyRect = if (progress == 0) {
                enemies.map { it.toRectPlus(turnAroundTolerance) }
            } else {
                projectiles.map { pt ->
                    pt.direction?.pointModifier(progress)?.let { mod ->
                        val adj = mod(pt)
                        d { "$progress: moveit to $pt to $adj"}
                        adj
                    } ?: pt
                }.map { it.toRect() } + (enemies - projectiles.toSet()).map { it.toRect() }
            }

            // note: more efficient to cycle over enemies than over points, but this more logical
            costsF.mapXyCurrent { x, y, current ->
                val pt = FramePoint(x,y).toRect()
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
            // was some repeats
            for (grid in grids.toSet()) {
                d { " set highcost $grid" }
                costsF.modifyTo(grid, MapConstants.oneGrid, HIGH_COST)
            }
        }
    }
}

private data class OpenNode(val point: FramePoint, val priority: Int)