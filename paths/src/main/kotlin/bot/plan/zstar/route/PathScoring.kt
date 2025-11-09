package bot.plan.zstar.route

import bot.plan.action.distTo
import bot.plan.zstar.NeighborFinder
import bot.state.FramePoint
import bot.state.dirTo
import bot.state.distTo
import bot.state.map.Direction

/**
 * Enhanced path scoring metrics for better path comparison
 */
data class PathScore(
    val unsafeCount: Double,
    val maxConsecutiveUnsafe: Int,
    val pathLength: Int,
    val averageEnemyDistance: Double,
    val stepsToFirstSafe: Int,
    val directionChanges: Int
) : Comparable<PathScore> {
    override fun compareTo(other: PathScore): Int {
        // Priority order:
        // 1. Minimize unsafe tiles
        unsafeCount.compareTo(other.unsafeCount).takeIf { it != 0 }?.let { return it }

        // 2. Minimize consecutive unsafe tiles (clustered danger is worse)
        maxConsecutiveUnsafe.compareTo(other.maxConsecutiveUnsafe).takeIf { it != 0 }?.let { return it }

        // 3. Prefer paths that reach safety quickly
        stepsToFirstSafe.compareTo(other.stepsToFirstSafe).takeIf { it != 0 }?.let { return it }

        // 4. Maximize distance from enemies (negative because we want larger distances first)
        (-averageEnemyDistance).compareTo(-other.averageEnemyDistance).takeIf { it != 0 }?.let { return it }

        // 5. Prefer shorter paths
        pathLength.compareTo(other.pathLength).takeIf { it != 0 }?.let { return it }

        // 6. Prefer smoother paths (fewer direction changes)
        return directionChanges.compareTo(other.directionChanges)
    }
}

/**
 * Utility class for calculating path scores and metrics.
 * Provides comprehensive path evaluation based on safety, efficiency, and smoothness.
 */
class PathScoring(private val neighborFinder: NeighborFinder) {

    /**
     * Calculate comprehensive path score for enhanced comparison
     */
    fun calculatePathScore(path: List<FramePoint>, enemies: List<FramePoint>): PathScore {
        return PathScore(
            unsafeCount = countUnsafe(path),
            maxConsecutiveUnsafe = maxConsecutiveUnsafe(path),
            pathLength = path.size,
            averageEnemyDistance = averageEnemyDistance(path, enemies),
            stepsToFirstSafe = stepsToFirstSafe(path),
            directionChanges = countDirectionChanges(path)
        )
    }

    /**
     * Sort paths using enhanced scoring that considers multiple factors:
     * - Unsafe tile count and distribution
     * - Distance from enemies
     * - Speed to reach safety
     * - Path smoothness
     */
    fun sortPathsByScore(paths: List<List<FramePoint>>, enemies: List<FramePoint> = emptyList()): List<List<FramePoint>> {
        return paths.sortedBy { path -> calculatePathScore(path, enemies) }
    }

    /**
     * Count total unsafe tiles in the path
     */
    fun countUnsafe(path: List<FramePoint>): Double {
        return path.sumOf { if (neighborFinder.isSafe(it)) 0.0 else 1.0 }
    }

    /**
     * Count total safe tiles in the path
     */
    fun countSafe(path: List<FramePoint>): Double {
        return path.sumOf { if (neighborFinder.isSafe(it)) 1.0 else 0.0 }
    }

    /**
     * Calculate maximum consecutive unsafe tiles in a path.
     * Consecutive unsafe sections are more dangerous than scattered ones.
     */
    fun maxConsecutiveUnsafe(path: List<FramePoint>): Int {
        var maxConsecutive = 0
        var currentConsecutive = 0

        for (point in path) {
            if (!neighborFinder.isSafe(point)) {
                currentConsecutive++
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 0
            }
        }

        return maxConsecutive
    }

    /**
     * Calculate average distance from enemies throughout the path.
     * Paths that keep Link farther from enemies are safer.
     */
    fun averageEnemyDistance(path: List<FramePoint>, enemies: List<FramePoint>): Double {
        if (path.isEmpty() || enemies.isEmpty()) return 0.0
        val totalDistance = path.sumOf { it.distTo(enemies) }
        return totalDistance.toDouble() / path.size
    }

    /**
     * Calculate total distance from enemies throughout the path.
     */
    fun totalEnemyDistance(path: List<FramePoint>, enemies: List<FramePoint>): Int {
        return path.sumOf { it.distTo(enemies) }
    }

    /**
     * Calculate number of steps until reaching first safe tile.
     * Paths that reach safety quickly are preferred.
     */
    fun stepsToFirstSafe(path: List<FramePoint>): Int {
        val firstSafeIndex = path.indexOfFirst { neighborFinder.isSafe(it) }
        return if (firstSafeIndex == -1) path.size else firstSafeIndex
    }

    /**
     * Count direction changes in the path.
     * Fewer direction changes means smoother, more efficient movement.
     */
    fun countDirectionChanges(path: List<FramePoint>): Int {
        if (path.size < 2) return 0

        var changes = 0
        var lastDirection: Direction? = null

        for (i in 1 until path.size) {
            val currentDirection = path[i - 1].dirTo(path[i])
            if (lastDirection != null && currentDirection != lastDirection && currentDirection != Direction.None) {
                changes++
            }
            if (currentDirection != Direction.None) {
                lastDirection = currentDirection
            }
        }

        return changes
    }
}
