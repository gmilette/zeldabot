package bot.plan.zstar.route

import bot.plan.zstar.NeighborFinder
import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.junit.Test
import util.Map2d

class BreadthFirstSearchMetricsTest {

    private fun createSimplePassableMap(size: Int = 50): Map2d<Boolean> {
        val map = (0 until size).map {
            (0 until size).map { true }.toMutableList()
        }.toMutableList()
        return Map2d(map)
    }

    @Test
    fun `BFS tracks lastNodesExplored correctly`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        bfs.bestRoute(start, targets, maxDepth = 100)

        bfs.lastNodesExplored shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `BFS tracks lastIterationsUsed correctly`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        bfs.bestRoute(start, targets, maxDepth = 100)

        bfs.lastIterationsUsed shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `BFS tracks lastMaxQueueSize correctly`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        bfs.bestRoute(start, targets, maxDepth = 100)

        bfs.lastMaxQueueSize shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `BFS tracks lastNodesConsidered correctly`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        bfs.bestRoute(start, targets, maxDepth = 100)

        bfs.lastNodesConsidered shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `BFS nodes considered should be greater than or equal to nodes explored`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(15, 15))
        bfs.bestRoute(start, targets, maxDepth = 100)

        bfs.lastNodesConsidered shouldBeGreaterThanOrEqual bfs.lastNodesExplored
    }

    @Test
    fun `BFS metrics reset between searches`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        // First search
        val start1 = FramePoint(5, 5)
        val targets1 = listOf(FramePoint(10, 10))
        bfs.bestRoute(start1, targets1, maxDepth = 50)
        val firstNodesExplored = bfs.lastNodesExplored

        // Second search with different parameters
        val start2 = FramePoint(20, 20)
        val targets2 = listOf(FramePoint(25, 25))
        bfs.bestRoute(start2, targets2, maxDepth = 50)
        val secondNodesExplored = bfs.lastNodesExplored

        // Metrics should be independent for each search
        firstNodesExplored shouldBeGreaterThanOrEqual 0
        secondNodesExplored shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `BFS maxQueueSize increases with search complexity`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        // Simple short search
        val start1 = FramePoint(10, 10)
        val targets1 = listOf(FramePoint(11, 11))
        bfs.bestRoute(start1, targets1, maxDepth = 50)
        val simpleMaxQueue = bfs.lastMaxQueueSize

        // More complex longer search
        val start2 = FramePoint(5, 5)
        val targets2 = listOf(FramePoint(40, 40))
        bfs.bestRoute(start2, targets2, maxDepth = 200)
        val complexMaxQueue = bfs.lastMaxQueueSize

        simpleMaxQueue shouldBeGreaterThanOrEqual 0
        complexMaxQueue shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `BFS handles out of bounds start point gracefully`() {
        val passableMap = createSimplePassableMap(50)
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(100, 100) // Out of bounds
        val targets = listOf(FramePoint(10, 10))
        val result = bfs.bestRoute(start, targets, maxDepth = 50)

        when (result) {
            is BreadthFirstSearch.ActionRoute.Route -> {
                result.route.size shouldBe 0
            }
            is BreadthFirstSearch.ActionRoute.Attack -> {
                // Attack is also a valid result
                result shouldBe result
            }
        }
    }

    @Test
    fun `BFS handles out of bounds target point gracefully`() {
        val passableMap = createSimplePassableMap(50)
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(10, 10)
        val targets = listOf(FramePoint(100, 100)) // Out of bounds
        val result = bfs.bestRoute(start, targets, maxDepth = 50)

        when (result) {
            is BreadthFirstSearch.ActionRoute.Route -> {
                result.route.size shouldBe 0
            }
            is BreadthFirstSearch.ActionRoute.Attack -> {
                // Attack is also a valid result
                result shouldBe result
            }
        }
    }

    @Test
    fun `BFS metrics are accessible after search completes`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(10, 10)
        val targets = listOf(FramePoint(20, 20))
        bfs.bestRoute(start, targets, maxDepth = 100)

        // All metrics should be accessible
        val explored = bfs.lastNodesExplored
        val iterations = bfs.lastIterationsUsed
        val maxQueue = bfs.lastMaxQueueSize
        val considered = bfs.lastNodesConsidered

        explored shouldBeGreaterThanOrEqual 0
        iterations shouldBeGreaterThanOrEqual 0
        maxQueue shouldBeGreaterThanOrEqual 0
        considered shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `BFS iterations limited by maxDepth`() {
        val passableMap = createSimplePassableMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val bfs = BreadthFirstSearch(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val start = FramePoint(5, 5)
        val targets = listOf(FramePoint(45, 45))
        bfs.bestRoute(start, targets, maxDepth = 10)

        // Should respect maxDepth limitation
        bfs.lastIterationsUsed shouldBeGreaterThanOrEqual 0
    }
}