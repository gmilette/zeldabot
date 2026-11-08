package bot.plan.zstar.route

import bot.plan.action.distTo
import bot.plan.zstar.NeighborFinder
import bot.plan.zstar.ZStar.Companion.DEBUG_B
import bot.plan.zstar.ZStar.Companion.MAX_ITER
import bot.state.FramePoint
import bot.state.dirTo
import util.d
import java.util.*

typealias Paths = List<Path>
typealias Path = List<FramePoint>

private var idCt = 0

data class SearchNode(
    val point: FramePoint,
    val path: List<FramePoint>,
    val depth: Int,
    val foundSafe: Boolean = false,
    val cost: Int,
    val id: Int = idCt++,
)

class CachingGoalChecker(
    private val isGoalCheck: (FramePoint, Boolean) -> Boolean = { _, _ -> false },
) {
    private val cache = TreeMap<FramePoint, Boolean>(BreadthFirstSearch.framePointComparator)

    fun isGoal(framePoint: FramePoint, isSafe: Boolean): Boolean =
        cache.getOrPut(framePoint) { isGoalCheck(framePoint, isSafe) }
}


class BreadthFirstSearch(
    isGoal: (FramePoint, Boolean) -> Boolean = { _, _ -> false },
    private val neighborFinder: NeighborFinder,
) {
    private val goalChecker = CachingGoalChecker(isGoal)

    companion object {
        val MAX_PATHS = 3
        // problems
        // link goes to a corner
        // link walks off map
        // let's enemies get close, can't plan out routes that eventually
        // get link farther away
        // todo: need to have a constraint to not allow the route to leave an area ever
        // maybe prioritize longer routes?
        val SAFE_GOAL = false
        val framePointComparator: Comparator<FramePoint> = compareBy({ it.x }, { it.y }, { it.direction?.ordinal ?: -1 })
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

    private fun sortPathsByBestFirstDist(foundPaths: List<List<FramePoint>>, enemies: List<FramePoint>): List<List<FramePoint>> {
        return foundPaths.sortedWith(
            compareBy<List<FramePoint>> { countUnsafe(it) }
                .thenByDescending { it.countDistance(enemies) }
        )
    }

    private fun List<FramePoint>.countDistance(enemies: List<FramePoint>): Int {
        val sum: Int = sumOf { it.distTo(enemies) }
        return sum
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
    fun isTheGoal(point: FramePoint): Boolean {
        d { " goal from $point}"}
        return goalChecker.isGoal(point, true)
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

    fun breadthFirstSearch(
        start: FramePoint,
        targets: List<FramePoint> = emptyList(),
        maxDepth: Int = 5000
    ): List<Path> {
        val queue = PriorityQueue<SearchNode>( compareBy { it.cost })
//        val queue = LinkedList<SearchNode>()
        val visited = TreeSet(framePointComparator)
        val foundPaths = mutableListOf<List<FramePoint>>()

        queue.offer(SearchNode(start, listOf(start), 0, cost = 0))
        
        var iterations = 0
        while (queue.isNotEmpty() && iterations < MAX_ITER && foundPaths.size < MAX_PATHS) {
            val current = queue.poll()
            
            if (DEBUG_B) {
                d { "$iterations: exploring ${current.point} at depth ${current.depth} queue: ${queue.size}" }
                for (node in queue) {
                    d { "   $iterations: $node" }
                }
                if (DEBUG_B) {
                    d { "currentNode: $current" }
                }
            }
            
            if (current.depth > maxDepth) continue

            // find a safe route first!!
            // could say that it MUST find a route to safety first, then start looking for goals
            // so run this twice, once to find routes to safe starting points, then route to goals

            // maybe just do limited path length
            // then pick safest
            // generally going towards safety is going to help keep link alive
            // an opportunity to attack will appear

            // how would this work if it was an 8x8 grid?
            // start at nearest point on grid. then at end of grid.
            //  before routing on the grid. explore the routes within 8 or 16 depth, if there is nothing there, then change
            //  to the 8 grid route, and explore it all. Maybe if it find no action it can find the safest route that leads
            // to a grid location instead. Rough 8 grid is all i need to create a go in X direction.

            // current.depth == 0
            if (current.foundSafe && goalChecker.isGoal(current.point, true)) {
                // Ensure the final path includes the current point (last visited point)
                //                val completePath = if (current.path.last().equalsAndDirection(current.point)) {
                val completePath = if (current.path.last() == current.point) {
                    current.path
                } else {
                    current.path + current.point
                }
                
                foundPaths.add(completePath)
                if (DEBUG_B) {
                    d { "Found goal path: $completePath" }
                }
                visited.add(current.point)
                continue
            }
            
            if (current.point in visited) continue
            iterations++
            visited.add(current.point)

            val from = current.path.getOrNull(current.path.size - 2)

//            val ONLY_SAFE = !isSafe(current.point)
//            d(DEBUG_B) { "ONLY_SAFE $ONLY_SAFE" }
            val neighbors = neighborFinder.neighbors(
                current.point, 
                from = from
            )//.filter { !ONLY_SAFE || isSafe(it) }
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

                    // add turning cost?, it's better to keep going straight
//                    val arriveDir = neighbor.direction ?: current.point.dirTo(neighbor)
//                    val turning = current.point.direction != null && current.point.direction != arriveDir
//                    val cost = neighborFinder.costF.get(neighbor) + if (turning) TURN_COST else 0
                    val cost = neighborFinder.costF.get(neighbor)
                    d(DEBUG_B) { "  $neighbor: $cost to $newPath" }

                    queue.offer(
                        SearchNode(
                            point = neighbor,
                            path = newPath,
                            depth = current.depth + 1,
                            foundSafe = newFoundSafe,
                            cost = current.cost + cost,
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
                val dist = path.countDistance(targets)
                d { "BFS path: ${path.size} $path $dist"}
            }
        }

        return if (SAFE_GOAL) {
            d { "BFS sort by dist" }
            sortPathsByBestFirstDist(foundPaths, enemies = targets) // TODO: Add back in the enemies
        } else {
            d { "BFS sort by size" }
            sortPathsByBestFirst(foundPaths)
        }.also {
            if (DEBUG_B) {
                d { "BFS sorted" }
                for (path in it) {
                    val dist = path.countDistance(targets)
                    d { "BFS sorted path: ${path.size} $path $dist"}
                }
            }
        }
    }
}

