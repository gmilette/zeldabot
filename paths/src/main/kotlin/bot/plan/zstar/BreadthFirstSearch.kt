package bot.plan.zstar

import bot.plan.action.Action
import bot.plan.action.AttackActionBlockDecider
import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.plan.action.RouteTo.WhatToAvoid
import bot.plan.zstar.ZStar.Companion.DEBUG_B
import bot.plan.zstar.ZStar.Companion.MAX_ITER
import bot.plan.zstar.ZStar.ZRouteParam
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.dirTo
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.minDistToRect
import util.d
import java.util.*

typealias Paths = List<Path>
typealias Path = List<FramePoint>

data class SearchNode(
    val point: FramePoint,
    val path: List<FramePoint>,
    val depth: Int,
    val foundSafe: Boolean = false
)

class BreadthFirstSearch(
    private var ableToLongAttack: Boolean = false,
    private var ableToAttack: Boolean = true,
    private val neighborFinder: NeighborFinder,
    private val longDecider: AttackLongActionDecider = AttackLongActionDecider,
    private val shortDecider: AttackActionDecider = AttackActionDecider
) {
    companion object {
        val MAX_PATHS = 3
    }

    init {
        initialCalculations()
    }

    private fun sortPathsByBestFirst(foundPaths: List<List<FramePoint>>): List<List<FramePoint>> {
        return foundPaths.sortedWith(
            compareBy<List<FramePoint>> { countUnsafe(it) }
                .thenBy { it.size }
        )
    }

    private fun countSafe(path: List<FramePoint>): Double {
        val sum: Double = path.sumOf { if (neighborFinder.isSafe(it)) 1.0 else 0.0 }
        return sum
    }

    private fun countUnsafe(path: List<FramePoint>): Double {
        val sum: Double = path.sumOf { if (neighborFinder.isSafe(it)) 0.0 else 1.0 }
        return sum
    }

    private fun initialCalculations() {
//        ableToLongAttack = longDecider.ableToShoot(state)
    }
    /**
     * search out from current location
     * and find all goals
     * then we have
     */

    fun isGoal(point: FramePoint, targets: List<FramePoint>): Boolean {
        d { " goal from $point}"}
        val longAttack = ableToLongAttack && longDecider.targetInLongRange(neighborFinder.passable, point, targets)
        val attackAction by lazy { shortDecider.inRangeOf(point.direction ?: Direction.None, point, targets, false) }
        // could add overlapping attack.
//        d { " attackAction $attackAction"}
        val attack = ableToAttack && attackAction.isAttack
        val safe = isSafe(point)

        // when you are long attacking, just try to stay in a safe area the safer the better

        // if you dont find a safe route



        // need two levels of safety
        // intersection!
        // close (1/2 more grid)

        // if you are trying to shoot, stay 1.5 grid away
        // if you need to attack, you need to get within 1.5 grid
        // when travelling, stay 1/2 grid away if possible

        // TODO: need to make this more exact when comparing to the grid of hte targets
//        val tooClose = targets.any { it.distTo(point) <= MapConstants.twoGrid }

        // this seems ok
//        val tooClose by lazy { targets.any { it.minDistToRect(point) <= MapConstants.oneGridPoint5 } }
        val tooClose by lazy { false }
//        val tooCloseForAttack by lazy { targets.any { it.minDistToRect(point) <= MapConstants.halfGrid } }
        val tooCloseForAttack by lazy { false }

        // pick the route that minimizes time in unsafe territory

        // should return a goal type of LONG_ATTACK, vs SHORT_ATTACK
        // and the algorithm should prefer LONG_ATTACK range goals
        return when {
            longAttack && !tooClose && safe -> true // could be B or A
            attack && !tooCloseForAttack && safe -> true
            // need some better logic for getting loot than exact target
            // we don't want to use the other routing for loot getting because
            // that will put link in danger, could use it for now I guess
//            point in lootTargets -> true // this is for getting items not enemies

            // otherwise, link is probably unsafe, and link should therefor get to a safe spot
            // and attack again
            // then again maybe it link is on top of an enemy he should attack anyway
            else -> false
        }.also {
            if (it) {
                d { "FOUND GOAL!"}
            }
        }
    }

    enum class GoalType(val rank: Int) {
        UNSAFE_NO_ACTION(0),
        UNSAFE_BUT_CAN_BLOCK(1),
        UNSAFE_BUT_CAN_ATTACK(2),
        ATTACK(3),
        ATTACK_FROM_SAFE_DISTANCE(10)
    }

    private fun processGoals(targets: List<FramePoint>, point: FramePoint) {
        // which are safe locations?
        // which are more than 1/2 grid away or more
        // closest
        // is there an acceptable path to reach them that is also safe
//        val longAttack = ableToLongAttack && AttackLongActionDecider.targetInLongRange(targets)
    }

    private fun isSafe(point: FramePoint): Boolean {
        return neighborFinder.isSafe(point)
    }

    // this should return an action not a point

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

    sealed class ActionRoute {
        data class Attack(val useB: Boolean) : ActionRoute()
        data class Route(val route: List<FramePoint>) : ActionRoute()
    }

    fun bestRoute(
        start: FramePoint,
        targets: List<FramePoint>,
        maxDepth: Int = 255
    ): ActionRoute {
        return if (isGoal(start, targets)) {
            d { " BFS: Started at goal: $start"}
            ActionRoute.Attack(false)
        } else {
            val routes = breadthFirstSearch(start, targets, maxDepth)
            ActionRoute.Route(routes.first())
        }
    }

    fun breadthFirstSearch(
        start: FramePoint,
        targets: List<FramePoint>,
        maxDepth: Int = 5000
    ): List<List<FramePoint>> {
        val queue = LinkedList<SearchNode>()
        val visited = mutableSetOf<FramePoint>()
        val foundPaths = mutableListOf<List<FramePoint>>()

        queue.offer(SearchNode(start, listOf(start), 0))
        
        var iterations = 0
        while (queue.isNotEmpty() && iterations < MAX_ITER && foundPaths.size < MAX_PATHS) {
            iterations++
            val current = queue.poll()
            
            if (DEBUG_B) {
                d { "$iterations: exploring ${current.point} at depth ${current.depth}" }
            }
            
            if (current.depth > maxDepth) continue
            
            if (isGoal(current.point, targets)) {
                // Ensure the final path includes the current point (last visited point)
                val completePath = if (current.path.last() == current.point) {
                    current.path
                } else {
                    current.path + current.point
                }
                
                foundPaths.add(completePath)
                if (DEBUG_B) {
                    d { "Found goal path: $completePath" }
                }
                continue
            }
            
            if (current.point in visited) continue
            visited.add(current.point)

            val from = current.path.getOrNull(current.path.size - 2)
            
            val neighbors = neighborFinder.neighbors(
                current.point, 
                from = from
            )
            if (DEBUG_B) {
                d { "Neighbors of ${current.point} from ${from}: $neighbors" }
            }

            for (neighbor in neighbors - visited) {
                val safe = isSafe(neighbor)
                val shouldExplore = if (current.foundSafe) safe else true
                val newFoundSafe = current.foundSafe || safe

                if (shouldExplore) {
                    val newPath = current.path + neighbor.copy(
                        direction = current.point.dirTo(neighbor)
                    )

                    queue.offer(
                        SearchNode(
                            point = neighbor,
                            path = newPath,
                            depth = current.depth + 1,
                            foundSafe = newFoundSafe
                        )
                    )
                } else {
                    d { " do not explore "}
                }
            }
        }
        
        if (DEBUG_B) {
            d { "BFS completed: found ${foundPaths.size} paths in $iterations iterations" }
            for (path in foundPaths) {
                d { "BFS path: ${path.size} $path"}
            }
        }
        
//        return foundPaths.sortedBy { it.size }
        return sortPathsByBestFirst(foundPaths)
    }

//    private fun breadthSearch(
//        param: ZRouteParam,
//        current: FramePoint,
//        targets: List<FramePoint>,
//        pointBeforeStart: FramePoint?,
//        visited: MutableSet<FramePoint> = mutableSetOf()
//    ): List<FramePoint> {
//        val toExplore = neighborFinder.neighbors(current, from = pointBeforeStart).toMutableList()
//        val cameFrom = mutableMapOf<FramePoint, FramePoint>()
//        var finalPoint = FramePoint()
//
//        // doesn't work. more testing needed
//        // d { "start dodge targets $targets start $current toExplore: ${toExplore.size}" }
//        var i = 0
//        while (toExplore.isNotEmpty() && i < MAX_ITER) {
//            if (DEBUG_B) {
//                d { "$i: open nodes: ${toExplore.size} : $toExplore" }
//            }
//            // needs way more testing
////            customizer.setAllEnemyCosts(param, i)
//            for (nearPoint in toExplore.toMutableList()) {
//                if (DEBUG_B) {
//                    d { "$i: -->explore $nearPoint cost=${costsF.get(nearPoint)} safe=${costsF.safe(nearPoint)} safeIntersect=${costsF.safeFromIntersect(nearPoint)}" }
//                }
//                // check if it intersects any enemies
/////                if (costsF.safeFromIntersect(nearPoint)) {
//                if (costsF.safe(nearPoint)) {
////                if (nearPoint in targets) {
//                    finalPoint = nearPoint
//                    toExplore.clear()
//                    break;
//                } else {
//                    visited.add(nearPoint)
//                    val neighbors = neighborFinder.neighbors(nearPoint, from = current) - visited
//                    for (neighbor in neighbors) {
//                        cameFrom[neighbor] = nearPoint
//                    }
//                    // evaluate all the neighbors, and randomly pick minimal cost
//                    toExplore.addAll(neighbors)
//                }
//                i++
//            }
//        }
//
//        val path = mutableListOf(finalPoint)
//        if (DEBUG_B) {
//            d { " came froms " }
//            for (entry in cameFrom) {
//                d { " ${entry.key} -> ${entry.value}" }
//            }
//            d { " came from $finalPoint" }
//        }
//
//        var lastPoint = finalPoint
//        while (cameFrom.containsKey(lastPoint)) {
//            lastPoint = cameFrom.getValue(lastPoint)
//            path.add(0, lastPoint)
//        }
//
//        // add the current point because the route code assumes the current
//        // point to be the first one in the path
//        path.add(0, current)
//        d { " the final path is $path"}
//        return path
//    }

}

