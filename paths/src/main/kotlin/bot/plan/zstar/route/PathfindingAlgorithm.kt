package bot.plan.zstar.route

import bot.state.FramePoint

/**
 * Common interface for pathfinding algorithms to enable comparison
 */
interface PathfindingAlgorithm {
    /**
     * Find the best route from start to targets
     */
    fun findRoute(
        start: FramePoint,
        targets: List<FramePoint>,
        maxDepth: Int = 255
    ): PathfindingResult

    /**
     * Get algorithm name for comparison reporting
     */
    fun getName(): String
}

/**
 * Result of a pathfinding operation with metrics
 */
data class PathfindingResult(
    val path: List<FramePoint>,
    val metrics: PathfindingMetrics,
    val success: Boolean = path.isNotEmpty()
)

/**
 * Performance metrics for algorithm comparison
 */
data class PathfindingMetrics(
    val executionTimeMs: Long,
    val nodesExplored: Int,
    val iterationsUsed: Int,
    val pathLength: Int,
    val safetyScore: Double,
    val algorithmName: String,
    val memoryUsageBytes: Long = 0,
    val maxQueueSize: Int = 0,
    val nodesConsidered: Int = 0
) {
    val searchEfficiency: Double
        get() = if (nodesConsidered > 0) nodesExplored.toDouble() / nodesConsidered else 0.0
}