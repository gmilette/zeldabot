package bot.plan.zstar.route

import bot.plan.zstar.ZStar
import bot.plan.action.RouteTo
import bot.state.FramePoint
import kotlin.system.measureTimeMillis

/**
 * Wrapper for ZStar to implement PathfindingAlgorithm interface
 */
class ZStarWrapper(
    private val zstar: ZStar
) : PathfindingAlgorithm {

    override fun findRoute(
        start: FramePoint,
        targets: List<FramePoint>,
        maxDepth: Int
    ): PathfindingResult {
        val runtime = Runtime.getRuntime()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        var resultPath: List<FramePoint> = emptyList()

        val executionTime = measureTimeMillis {
            val param = ZStar.ZRouteParam(
                start = start,
                targets = targets,
                rParam = RouteTo.RoutingParamCommon()
            )
            resultPath = zstar.route(param)
        }

        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = maxOf(0, memoryAfter - memoryBefore)

        val safetyScore = calculateSafetyScore(resultPath)

        val metrics = PathfindingMetrics(
            executionTimeMs = executionTime,
            nodesExplored = zstar.lastNodesExplored,
            iterationsUsed = zstar.lastIterationsUsed,
            pathLength = resultPath.size,
            safetyScore = safetyScore,
            algorithmName = getName(),
            memoryUsageBytes = memoryUsed,
            maxQueueSize = zstar.lastMaxOpenListSize,
            nodesConsidered = zstar.lastClosedListSize
        )

        return PathfindingResult(resultPath, metrics)
    }

    override fun getName(): String = "ZStar"

    private fun calculateSafetyScore(path: List<FramePoint>): Double {
        if (path.isEmpty()) return 0.0
        val safePoints = path.count { zstar.neighborFinder.isSafe(it) }
        return safePoints.toDouble() / path.size
    }

}