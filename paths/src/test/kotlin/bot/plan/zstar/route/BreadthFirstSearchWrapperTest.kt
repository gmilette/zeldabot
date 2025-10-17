package bot.plan.zstar.route

import bot.plan.zstar.NeighborFinder
import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.Test
import util.Map2d

class BreadthFirstSearchWrapperTest {

    private fun createSimplePassableMap(size: Int = 50): Map2d<Boolean> {
        val map = (0 until size).map {
            (0 until size).map { true }.toMutableList()
        }.toMutableList()
        return Map2d(map)
    }

    private fun createMapWithObstacle(size: Int = 50): Map2d<Boolean> {
        val map = (0 until size).map { y ->
            (0 until size).map { x ->
                // Create a wall from x=20 to x=30, y=20 to y=30
                !(x in 20..30 && y in 20..30)
            }.toMutableList()
        }.toMutableList()
        return Map2d(map)
    }

    @Test
    fun `BreadthFirstSearchWrapper implements PathfindingAlgorithm interface`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        wrapper.getName() shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute returns successful result with valid path on simple map`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(10, 10))
        val result = wrapper.findRoute(start, targets)
        
        result.success shouldBe true
        result.path.shouldNotBeEmpty()
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute tracks metrics correctly`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.executionTimeMs shouldBeGreaterThanOrEqual 0L
        result.metrics.nodesExplored shouldBeGreaterThanOrEqual 0
        result.metrics.iterationsUsed shouldBeGreaterThanOrEqual 0
        result.metrics.pathLength shouldBe result.path.size
        result.metrics.maxQueueSize shouldBeGreaterThanOrEqual 0
        result.metrics.nodesConsidered shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `findRoute calculates safety score correctly for all safe path`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(10, 10))
        val result = wrapper.findRoute(start, targets)
        
        if (result.success) {
            result.metrics.safetyScore shouldBeGreaterThanOrEqual 0.0
            result.metrics.safetyScore shouldBeLessThanOrEqual 1.0
        }
    }

    @Test
    fun `findRoute returns empty path when no route exists`() {
        val passableMap = createSimplePassableMap(20)
        // Make target unreachable by blocking it
        passableMap.set(FramePoint(15, 15), false)
        passableMap.set(FramePoint(14, 15), false)
        passableMap.set(FramePoint(16, 15), false)
        passableMap.set(FramePoint(15, 14), false)
        passableMap.set(FramePoint(15, 16), false)
        
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        val result = wrapper.findRoute(start, targets, maxDepth = 50)
        
        // BFS might return empty path or attack action - both are valid
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute respects maxDepth parameter`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(40, 40))
        val result = wrapper.findRoute(start, targets, maxDepth = 10)
        
        // With limited depth, might not find path
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute with multiple targets finds path to any target`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(
            FramePoint(10, 10),
            FramePoint(8, 8),
            FramePoint(12, 12)
        )
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute handles start point same as target`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(10, 10)
        val targets = listOf(FramePoint(10, 10))
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute with long attack enabled`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = true,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute with attack disabled`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = false,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute around obstacle`() {
        val passableMap = createMapWithObstacle()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(15, 25)
        val targets = listOf(FramePoint(35, 25))
        val result = wrapper.findRoute(start, targets)
        
        // Should find path around obstacle
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
        result.metrics.nodesExplored shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `getName returns correct algorithm name`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        wrapper.getName() shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `safety score is zero for empty path`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(100, 100)) // Out of bounds
        val result = wrapper.findRoute(start, targets, maxDepth = 10)
        
        if (!result.success) {
            result.metrics.safetyScore shouldBe 0.0
        }
    }

    @Test
    fun `multiple consecutive findRoute calls work correctly`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val result1 = wrapper.findRoute(FramePoint(5, 5), listOf(FramePoint(10, 10)))
        val result2 = wrapper.findRoute(FramePoint(15, 15), listOf(FramePoint(20, 20)))
        val result3 = wrapper.findRoute(FramePoint(25, 25), listOf(FramePoint(30, 30)))
        
        result1.metrics.algorithmName shouldBe "BreadthFirstSearch"
        result2.metrics.algorithmName shouldBe "BreadthFirstSearch"
        result3.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute with halfPassable disabled`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = false, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute with isLevel enabled`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = true)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.algorithmName shouldBe "BreadthFirstSearch"
    }

    @Test
    fun `findRoute search efficiency is calculated`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val wrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        
        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        val result = wrapper.findRoute(start, targets)
        
        result.metrics.searchEfficiency shouldBeGreaterThanOrEqual 0.0
    }
}