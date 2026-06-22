package bot.plan.zstar.route

import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
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

data class SearchNode(
    val point: FramePoint,
    val path: List<FramePoint>,
    val depth: Int,
    val foundSafe: Boolean = false
)

class BreadthFirstSearch(
    private val isGoal: (FramePoint) -> Boolean = { false },
    private val neighborFinder: NeighborFinder,
) {
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
        return isGoal(point)
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
        return if (isGoal(start)) {
            d { " BFS: Started at goal: $start"}
            ActionRoute.Attack(false)
        } else {
            val routes = breadthFirstSearch(start, targets, maxDepth)
            ActionRoute.Route(routes.first())
        }
    }

    fun breadthFirstSearch(
        start: FramePoint,
        targets: List<FramePoint> = emptyList(),
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

            // current.depth == 0
            if (isGoal(current.point)) {
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
        
//        if (DEBUG_B) {
//            d { "BFS completed: found ${foundPaths.size} paths in $iterations iterations" }
//            for (path in foundPaths) {
//                val dist = path.countDistance(targets)
//                d { "BFS path: ${path.size} $path $dist"}
//            }
//        }

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
//        return foundPaths.sortedBy { it.size }
//        return sortPathsByBestFirst(foundPaths)
    }
}

