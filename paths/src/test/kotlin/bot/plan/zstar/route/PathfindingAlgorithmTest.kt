package bot.plan.zstar.route

import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class PathfindingAlgorithmTest {

    @Test
    fun `PathfindingResult should indicate success when path is not empty`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 10L,
            nodesExplored = 5,
            iterationsUsed = 3,
            pathLength = 4,
            safetyScore = 1.0,
            algorithmName = "TestAlgorithm"
        )
        val path = listOf(FramePoint(0, 0), FramePoint(1, 1))
        val result = PathfindingResult(path, metrics)
        
        result.success shouldBe true
        result.path.size shouldBe 2
    }

    @Test
    fun `PathfindingResult should indicate failure when path is empty`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 5L,
            nodesExplored = 0,
            iterationsUsed = 0,
            pathLength = 0,
            safetyScore = 0.0,
            algorithmName = "TestAlgorithm"
        )
        val result = PathfindingResult(emptyList(), metrics)
        
        result.success shouldBe false
        result.path.size shouldBe 0
    }

    @Test
    fun `PathfindingResult can be explicitly set to failed even with path`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 10L,
            nodesExplored = 5,
            iterationsUsed = 3,
            pathLength = 2,
            safetyScore = 0.5,
            algorithmName = "TestAlgorithm"
        )
        val path = listOf(FramePoint(0, 0), FramePoint(1, 1))
        val result = PathfindingResult(path, metrics, success = false)
        
        result.success shouldBe false
        result.path.size shouldBe 2
    }

    @Test
    fun `PathfindingMetrics searchEfficiency calculates correctly with nodes`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 100L,
            nodesExplored = 50,
            iterationsUsed = 10,
            pathLength = 20,
            safetyScore = 0.8,
            algorithmName = "TestAlgorithm",
            nodesConsidered = 100
        )
        
        metrics.searchEfficiency shouldBe 0.5
    }

    @Test
    fun `PathfindingMetrics searchEfficiency returns zero when no nodes considered`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 100L,
            nodesExplored = 50,
            iterationsUsed = 10,
            pathLength = 20,
            safetyScore = 0.8,
            algorithmName = "TestAlgorithm",
            nodesConsidered = 0
        )
        
        metrics.searchEfficiency shouldBe 0.0
    }

    @Test
    fun `PathfindingMetrics searchEfficiency is 1 when all nodes explored`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 50L,
            nodesExplored = 100,
            iterationsUsed = 50,
            pathLength = 10,
            safetyScore = 0.9,
            algorithmName = "EfficientAlgorithm",
            nodesConsidered = 100
        )
        
        metrics.searchEfficiency shouldBe 1.0
    }

    @Test
    fun `PathfindingMetrics stores all provided values correctly`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 123L,
            nodesExplored = 456,
            iterationsUsed = 789,
            pathLength = 321,
            safetyScore = 0.75,
            algorithmName = "CustomAlgorithm",
            memoryUsageBytes = 1024L,
            maxQueueSize = 250,
            nodesConsidered = 500
        )
        
        metrics.executionTimeMs shouldBe 123L
        metrics.nodesExplored shouldBe 456
        metrics.iterationsUsed shouldBe 789
        metrics.pathLength shouldBe 321
        metrics.safetyScore shouldBe 0.75
        metrics.algorithmName shouldBe "CustomAlgorithm"
        metrics.memoryUsageBytes shouldBe 1024L
        metrics.maxQueueSize shouldBe 250
        metrics.nodesConsidered shouldBe 500
    }

    @Test
    fun `PathfindingMetrics handles default memory and queue values`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 10L,
            nodesExplored = 5,
            iterationsUsed = 3,
            pathLength = 4,
            safetyScore = 1.0,
            algorithmName = "TestAlgorithm"
        )
        
        metrics.memoryUsageBytes shouldBe 0L
        metrics.maxQueueSize shouldBe 0
        metrics.nodesConsidered shouldBe 0
    }

    @Test
    fun `PathfindingMetrics searchEfficiency handles edge case of more explored than considered`() {
        // This shouldn't normally happen, but test defensive behavior
        val metrics = PathfindingMetrics(
            executionTimeMs = 10L,
            nodesExplored = 150,
            iterationsUsed = 10,
            pathLength = 20,
            safetyScore = 0.8,
            algorithmName = "TestAlgorithm",
            nodesConsidered = 100
        )
        
        // Should still calculate ratio even if greater than 1
        metrics.searchEfficiency shouldBe 1.5
    }

    @Test
    fun `PathfindingResult with complex path maintains order`() {
        val path = listOf(
            FramePoint(0, 0),
            FramePoint(1, 1),
            FramePoint(2, 2),
            FramePoint(3, 3),
            FramePoint(4, 4)
        )
        val metrics = PathfindingMetrics(
            executionTimeMs = 50L,
            nodesExplored = 25,
            iterationsUsed = 10,
            pathLength = path.size,
            safetyScore = 0.9,
            algorithmName = "TestAlgorithm"
        )
        val result = PathfindingResult(path, metrics)
        
        result.path shouldBe path
        result.path[0] shouldBe FramePoint(0, 0)
        result.path[4] shouldBe FramePoint(4, 4)
        result.success shouldBe true
    }

    @Test
    fun `PathfindingMetrics with zero safety score`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 100L,
            nodesExplored = 50,
            iterationsUsed = 20,
            pathLength = 15,
            safetyScore = 0.0,
            algorithmName = "UnsafeAlgorithm"
        )
        
        metrics.safetyScore shouldBe 0.0
    }

    @Test
    fun `PathfindingMetrics with perfect safety score`() {
        val metrics = PathfindingMetrics(
            executionTimeMs = 100L,
            nodesExplored = 50,
            iterationsUsed = 20,
            pathLength = 15,
            safetyScore = 1.0,
            algorithmName = "SafeAlgorithm"
        )
        
        metrics.safetyScore shouldBe 1.0
    }

    @Test
    fun `PathfindingMetrics compares execution times`() {
        val fastMetrics = PathfindingMetrics(
            executionTimeMs = 10L,
            nodesExplored = 5,
            iterationsUsed = 3,
            pathLength = 4,
            safetyScore = 0.8,
            algorithmName = "FastAlgorithm"
        )
        
        val slowMetrics = PathfindingMetrics(
            executionTimeMs = 100L,
            nodesExplored = 50,
            iterationsUsed = 30,
            pathLength = 4,
            safetyScore = 0.8,
            algorithmName = "SlowAlgorithm"
        )
        
        fastMetrics.executionTimeMs shouldBeLessThan slowMetrics.executionTimeMs.toDouble()
    }

    @Test
    fun `PathfindingResult handles single point path`() {
        val path = listOf(FramePoint(5, 5))
        val metrics = PathfindingMetrics(
            executionTimeMs = 1L,
            nodesExplored = 1,
            iterationsUsed = 1,
            pathLength = 1,
            safetyScore = 1.0,
            algorithmName = "TestAlgorithm"
        )
        val result = PathfindingResult(path, metrics)
        
        result.success shouldBe true
        result.path.size shouldBe 1
        result.path[0] shouldBe FramePoint(5, 5)
    }

    @Test
    fun `Multiple PathfindingResults can be created and compared`() {
        val metrics1 = PathfindingMetrics(
            executionTimeMs = 10L,
            nodesExplored = 5,
            iterationsUsed = 3,
            pathLength = 4,
            safetyScore = 0.9,
            algorithmName = "Algorithm1"
        )
        val result1 = PathfindingResult(
            listOf(FramePoint(0, 0), FramePoint(1, 1)),
            metrics1
        )
        
        val metrics2 = PathfindingMetrics(
            executionTimeMs = 20L,
            nodesExplored = 10,
            iterationsUsed = 6,
            pathLength = 8,
            safetyScore = 0.8,
            algorithmName = "Algorithm2"
        )
        val result2 = PathfindingResult(
            listOf(FramePoint(0, 0), FramePoint(2, 2)),
            metrics2
        )
        
        result1.success shouldBe true
        result2.success shouldBe true
        result1.path shouldNotBe result2.path
        result1.metrics.executionTimeMs shouldBeLessThan result2.metrics.executionTimeMs.toDouble()
    }
}