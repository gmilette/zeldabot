package bot.plan.zstar.route

import bot.plan.zstar.NeighborFinder
import bot.plan.zstar.ZStar
import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import util.Map2d

class PathfindingComparisonIntegrationTest {

    private fun createTestMap(size: Int = 100): Map2d<Boolean> {
        return Map2d((0 until size).map {
            (0 until size).map { true }.toMutableList()
        }.toMutableList())
    }

    private fun addObstacleWall(map: Map2d<Boolean>, xRange: IntRange, yRange: IntRange) {
        for (x in xRange) {
            for (y in yRange) {
                map.set(FramePoint(x, y), false)
            }
        }
    }

    @Test
    fun `end to end comparison between BFS and ZStar on simple map`() {
        val passableMap = createTestMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Simple Path Test",
            start = FramePoint(10, 10),
            targets = listOf(FramePoint(20, 20))
        )

        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        result.results.size shouldBe 2
        result.results["BreadthFirstSearch"] shouldNotBe null
        result.results["ZStar"] shouldNotBe null
        result.winner shouldNotBe ""
    }

    @Test
    fun `end to end comparison with obstacle navigation`() {
        val passableMap = createTestMap()
        addObstacleWall(passableMap, 40..60, 40..60)

        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Obstacle Navigation Test",
            start = FramePoint(30, 50),
            targets = listOf(FramePoint(70, 50))
        )

        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        result.results.size shouldBe 2
        result.scenario.name shouldBe "Obstacle Navigation Test"
    }

    @Test
    fun `comprehensive comparison with multiple scenarios`() {
        val passableMap = createTestMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenarios = listOf(
            PathfindingComparator.TestScenario(
                name = "Short Distance",
                start = FramePoint(10, 10),
                targets = listOf(FramePoint(15, 15))
            ),
            PathfindingComparator.TestScenario(
                name = "Medium Distance",
                start = FramePoint(20, 20),
                targets = listOf(FramePoint(40, 40))
            ),
            PathfindingComparator.TestScenario(
                name = "Long Distance",
                start = FramePoint(10, 10),
                targets = listOf(FramePoint(80, 80))
            )
        )

        val results = comparator.runComprehensiveComparison(scenarios, listOf(bfsWrapper, zstarWrapper))

        results shouldHaveSize 3
        results.forEach { result ->
            result.results.size shouldBe 2
        }
    }

    @Test
    fun `comparison with unreachable target scenario`() {
        val passableMap = createTestMap(50)
        // Create walls that completely surround the target
        for (x in 23..27) {
            passableMap.set(FramePoint(x, 23), false)
            passableMap.set(FramePoint(x, 27), false)
        }
        for (y in 23..27) {
            passableMap.set(FramePoint(23, y), false)
            passableMap.set(FramePoint(27, y), false)
        }

        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Unreachable Target",
            start = FramePoint(10, 10),
            targets = listOf(FramePoint(25, 25)),
            maxDepth = 100
        )

        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        result.results.size shouldBe 2
        // Both algorithms should handle unreachable targets gracefully
        result.winner shouldNotBe ""
    }

    @Test
    fun `comparison with multiple target points`() {
        val passableMap = createTestMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Multiple Targets Test",
            start = FramePoint(50, 50),
            targets = listOf(
                FramePoint(60, 50),
                FramePoint(55, 55),
                FramePoint(50, 60),
                FramePoint(70, 70)
            )
        )

        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        result.results.size shouldBe 2
        result.scenario.targets shouldHaveSize 4
    }

    @Test
    fun `comparison summary contains performance metrics`() {
        val passableMap = createTestMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Metrics Test",
            start = FramePoint(10, 10),
            targets = listOf(FramePoint(30, 30))
        )

        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        result.summary shouldContain "Metrics Test"
        result.summary shouldContain "Time:"
        result.summary shouldContain "Safety:"
        result.summary shouldContain "Nodes explored:"
    }

    @Test
    fun `standard test scenarios are comprehensive`() {
        val comparator = PathfindingComparator()
        val scenarios = comparator.createStandardTestScenarios()

        scenarios.shouldNotBeEmpty()
        scenarios.size shouldBeGreaterThan 3

        // Verify variety of test cases
        val hasShortPath = scenarios.any { it.name.contains("Direct", ignoreCase = true) }
        val hasObstacle = scenarios.any { it.name.contains("Obstacle", ignoreCase = true) }
        val hasMultipleTargets = scenarios.any { it.name.contains("Multiple", ignoreCase = true) }

        hasShortPath shouldBe true
        hasObstacle shouldBe true
        hasMultipleTargets shouldBe true
    }

    @Test
    fun `comparison tracks winner correctly across multiple runs`() {
        val passableMap = createTestMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Consistency Test",
            start = FramePoint(10, 10),
            targets = listOf(FramePoint(20, 20))
        )

        // Run comparison multiple times
        val result1 = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))
        val result2 = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))
        val result3 = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        // All runs should produce valid winners
        result1.winner shouldNotBe ""
        result2.winner shouldNotBe ""
        result3.winner shouldNotBe ""
    }

    @Test
    fun `both algorithms handle edge case of start equals target`() {
        val passableMap = createTestMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Start Equals Target",
            start = FramePoint(25, 25),
            targets = listOf(FramePoint(25, 25))
        )

        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        result.results.size shouldBe 2
    }

    @Test
    fun `comparison with diagonal path scenario`() {
        val passableMap = createTestMap()
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )
        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Diagonal Path",
            start = FramePoint(10, 10),
            targets = listOf(FramePoint(50, 50))
        )

        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        result.results["BreadthFirstSearch"]?.metrics?.algorithmName shouldBe "BreadthFirstSearch"
        result.results["ZStar"]?.metrics?.algorithmName shouldBe "ZStar"
    }

    @Test
    fun `algorithms with different configurations can be compared`() {
        val passableMap = createTestMap()
        val neighborFinder1 = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val neighborFinder2 = NeighborFinder(passableMap, halfPassable = false, isLevel = false)

        val bfsWithHalfPassable = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder1
        )
        val bfsWithoutHalfPassable = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder2
        )

        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Configuration Test",
            start = FramePoint(10, 10),
            targets = listOf(FramePoint(30, 30))
        )

        val result = comparator.compareAlgorithms(
            scenario,
            listOf(bfsWithHalfPassable, bfsWithoutHalfPassable)
        )

        result.results.size shouldBe 2
    }
}