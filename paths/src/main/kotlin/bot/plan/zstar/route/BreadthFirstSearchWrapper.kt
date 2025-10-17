package bot.plan.zstar.route

import bot.plan.zstar.NeighborFinder
import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.state.FramePoint
import kotlin.system.measureTimeMillis

/**
 * Wrapper for BreadthFirstSearch to implement PathfindingAlgorithm interface
 */
class BreadthFirstSearchWrapper(
    private val ableToLongAttack: Boolean = false,
    private val ableToAttack: Boolean = true,
    private val neighborFinder: NeighborFinder,
    private val longDecider: AttackLongActionDecider = AttackLongActionDecider,
    private val shortDecider: AttackActionDecider = AttackActionDecider
) : PathfindingAlgorithm {

    private val bfs = BreadthFirstSearch(
        ableToLongAttack, ableToAttack, neighborFinder, longDecider, shortDecider
    )


    override fun findRoute(
        start: FramePoint,
        targets: List<FramePoint>,
        maxDepth: Int
    ): PathfindingResult {
        val runtime = Runtime.getRuntime()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        var resultPath: List<FramePoint> = emptyList()

        val executionTime = measureTimeMillis {
            resultPath = when (val result = bfs.bestRoute(start, targets, maxDepth)) {
                is BreadthFirstSearch.ActionRoute.Attack -> emptyList()
                is BreadthFirstSearch.ActionRoute.Route -> result.route
            }
        }

        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = maxOf(0, memoryAfter - memoryBefore)

        val safetyScore = calculateSafetyScore(resultPath)

        val metrics = PathfindingMetrics(
            executionTimeMs = executionTime,
            nodesExplored = bfs.lastNodesExplored,
            iterationsUsed = bfs.lastIterationsUsed,
            pathLength = resultPath.size,
            safetyScore = safetyScore,
            algorithmName = getName(),
            memoryUsageBytes = memoryUsed,
            maxQueueSize = bfs.lastMaxQueueSize,
            nodesConsidered = bfs.lastNodesConsidered
        )

        return PathfindingResult(resultPath, metrics)
    }

    override fun getName(): String = "BreadthFirstSearch"

    private fun calculateSafetyScore(path: List<FramePoint>): Double {
        if (path.isEmpty()) return 0.0
        val safePoints = path.count { neighborFinder.isSafe(it) }
        return safePoints.toDouble() / path.size
    }
}