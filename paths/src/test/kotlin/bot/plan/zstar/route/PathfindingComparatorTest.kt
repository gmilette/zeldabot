package bot.plan.zstar.route

import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.Test

class PathfindingComparatorTest {

    private class TestAlgorithm(
        private val name: String,
        private val pathToReturn: List<FramePoint>,
        private val executionTime: Long = 10L,
        private val nodesExplored: Int = 5
    ) : PathfindingAlgorithm {
        override fun findRoute(
            start: FramePoint,
            targets: List<FramePoint>,
            maxDepth: Int
        ): PathfindingResult {
            val metrics = PathfindingMetrics(
                executionTimeMs = executionTime,
                nodesExplored = nodesExplored,
                iterationsUsed = 3,
                pathLength = pathToReturn.size,
                safetyScore = if (pathToReturn.isNotEmpty()) 0.8 else 0.0,
                algorithmName = name,
                maxQueueSize = 10,
                nodesConsidered = nodesExplored * 2
            )
            return PathfindingResult(pathToReturn, metrics)
        }

        override fun getName(): String = name
    }

    private class FailingAlgorithm(private val name: String) : PathfindingAlgorithm {
        override fun findRoute(
            start: FramePoint,
            targets: List<FramePoint>,
            maxDepth: Int
        ): PathfindingResult {
            throw RuntimeException("Algorithm failed")
        }

        override fun getName(): String = name
    }

    @Test
    fun `compareAlgorithms with single successful algorithm`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Simple Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        val algorithm = TestAlgorithm(
            "TestAlgo",
            listOf(FramePoint(0, 0), FramePoint(5, 5), FramePoint(10, 10))
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(algorithm))
        
        result.scenario shouldBe scenario
        result.results.size shouldBe 1
        result.results["TestAlgo"]?.success shouldBe true
        result.winner shouldBe "TestAlgo"
    }

    @Test
    fun `compareAlgorithms with multiple algorithms selects winner`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Multi Algorithm Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        
        val fastAlgo = TestAlgorithm(
            "FastAlgo",
            listOf(FramePoint(0, 0), FramePoint(10, 10)),
            executionTime = 5L
        )
        val slowAlgo = TestAlgorithm(
            "SlowAlgo",
            listOf(FramePoint(0, 0), FramePoint(5, 5), FramePoint(10, 10)),
            executionTime = 50L
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(fastAlgo, slowAlgo))
        
        result.results.size shouldBe 2
        result.results["FastAlgo"]?.success shouldBe true
        result.results["SlowAlgo"]?.success shouldBe true
        result.winner shouldNotBe "None (all failed)"
    }

    @Test
    fun `compareAlgorithms handles all algorithms failing`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Failure Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        
        val failedAlgo1 = TestAlgorithm("FailedAlgo1", emptyList())
        val failedAlgo2 = TestAlgorithm("FailedAlgo2", emptyList())
        
        val result = comparator.compareAlgorithms(scenario, listOf(failedAlgo1, failedAlgo2))
        
        result.results.size shouldBe 2
        result.results["FailedAlgo1"]?.success shouldBe false
        result.results["FailedAlgo2"]?.success shouldBe false
        result.winner shouldBe "None (all failed)"
    }

    @Test
    fun `compareAlgorithms handles algorithm exceptions gracefully`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Exception Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        
        val failingAlgo = FailingAlgorithm("CrashingAlgo")
        val workingAlgo = TestAlgorithm(
            "WorkingAlgo",
            listOf(FramePoint(0, 0), FramePoint(10, 10))
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(failingAlgo, workingAlgo))
        
        result.results.size shouldBe 2
        result.results["CrashingAlgo"]?.success shouldBe false
        result.results["CrashingAlgo"]?.metrics?.executionTimeMs shouldBe -1
        result.results["WorkingAlgo"]?.success shouldBe true
        result.winner shouldBe "WorkingAlgo"
    }

    @Test
    fun `compareAlgorithms summary contains scenario name`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Summary Test Scenario",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        val algorithm = TestAlgorithm(
            "SummaryAlgo",
            listOf(FramePoint(0, 0), FramePoint(10, 10))
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(algorithm))
        
        result.summary shouldContain "Summary Test Scenario"
        result.summary shouldContain "SummaryAlgo"
    }

    @Test
    fun `runComprehensiveComparison processes multiple scenarios`() {
        val comparator = PathfindingComparator()
        val scenarios = listOf(
            PathfindingComparator.TestScenario(
                name = "Scenario1",
                start = FramePoint(0, 0),
                targets = listOf(FramePoint(10, 10))
            ),
            PathfindingComparator.TestScenario(
                name = "Scenario2",
                start = FramePoint(5, 5),
                targets = listOf(FramePoint(15, 15))
            ),
            PathfindingComparator.TestScenario(
                name = "Scenario3",
                start = FramePoint(10, 10),
                targets = listOf(FramePoint(20, 20))
            )
        )
        
        val algo1 = TestAlgorithm("Algo1", listOf(FramePoint(0, 0), FramePoint(10, 10)))
        val algo2 = TestAlgorithm("Algo2", listOf(FramePoint(0, 0), FramePoint(10, 10)))
        
        val results = comparator.runComprehensiveComparison(scenarios, listOf(algo1, algo2))
        
        results shouldHaveSize 3
        results[0].scenario.name shouldBe "Scenario1"
        results[1].scenario.name shouldBe "Scenario2"
        results[2].scenario.name shouldBe "Scenario3"
    }

    @Test
    fun `runComprehensiveComparison with empty scenarios list`() {
        val comparator = PathfindingComparator()
        val algo = TestAlgorithm("TestAlgo", listOf(FramePoint(0, 0)))
        
        val results = comparator.runComprehensiveComparison(emptyList(), listOf(algo))
        
        results shouldHaveSize 0
    }

    @Test
    fun `createStandardTestScenarios returns expected scenarios`() {
        val comparator = PathfindingComparator()
        val scenarios = comparator.createStandardTestScenarios()
        
        scenarios.size shouldBeGreaterThan 0
        val scenarioNames = scenarios.map { it.name }
        scenarioNames shouldContainAll listOf(
            "Simple Direct Path",
            "Around Obstacle",
            "Multiple Targets",
            "Long Distance",
            "Combat Scenario"
        )
    }

    @Test
    fun `createStandardTestScenarios has valid start and target points`() {
        val comparator = PathfindingComparator()
        val scenarios = comparator.createStandardTestScenarios()
        
        scenarios.forEach { scenario ->
            scenario.start shouldNotBe null
            scenario.targets.isNotEmpty() shouldBe true
            scenario.maxDepth shouldBeGreaterThan 0
        }
    }

    @Test
    fun `TestScenario stores all properties correctly`() {
        val scenario = PathfindingComparator.TestScenario(
            name = "Custom Scenario",
            start = FramePoint(100, 200),
            targets = listOf(FramePoint(300, 400), FramePoint(500, 600)),
            maxDepth = 500,
            description = "Test description"
        )
        
        scenario.name shouldBe "Custom Scenario"
        scenario.start shouldBe FramePoint(100, 200)
        scenario.targets shouldHaveSize 2
        scenario.maxDepth shouldBe 500
        scenario.description shouldBe "Test description"
    }

    @Test
    fun `TestScenario uses default maxDepth when not specified`() {
        val scenario = PathfindingComparator.TestScenario(
            name = "Default Depth",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        
        scenario.maxDepth shouldBe 255
        scenario.description shouldBe ""
    }

    @Test
    fun `ComparisonResult stores all components correctly`() {
        val scenario = PathfindingComparator.TestScenario(
            name = "Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        val metrics = PathfindingMetrics(
            executionTimeMs = 10L,
            nodesExplored = 5,
            iterationsUsed = 3,
            pathLength = 2,
            safetyScore = 0.8,
            algorithmName = "TestAlgo"
        )
        val result = PathfindingResult(listOf(FramePoint(0, 0), FramePoint(10, 10)), metrics)
        val resultsMap = mapOf("TestAlgo" to result)
        
        val comparisonResult = PathfindingComparator.ComparisonResult(
            scenario = scenario,
            results = resultsMap,
            winner = "TestAlgo",
            summary = "Test summary"
        )
        
        comparisonResult.scenario shouldBe scenario
        comparisonResult.results shouldBe resultsMap
        comparisonResult.winner shouldBe "TestAlgo"
        comparisonResult.summary shouldBe "Test summary"
    }

    @Test
    fun `compareAlgorithms with identical performance should pick one as winner`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Identical Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        
        // Create two algorithms with identical metrics
        val algo1 = TestAlgorithm(
            "Algo1",
            listOf(FramePoint(0, 0), FramePoint(10, 10)),
            executionTime = 10L,
            nodesExplored = 5
        )
        val algo2 = TestAlgorithm(
            "Algo2",
            listOf(FramePoint(0, 0), FramePoint(10, 10)),
            executionTime = 10L,
            nodesExplored = 5
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(algo1, algo2))
        
        result.winner shouldNotBe "None (all failed)"
        result.winner shouldNotBe ""
    }

    @Test
    fun `compareAlgorithms prefers shorter paths when safety is equal`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Path Length Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        
        val shortPathAlgo = TestAlgorithm(
            "ShortPath",
            listOf(FramePoint(0, 0), FramePoint(10, 10)),
            executionTime = 10L
        )
        val longPathAlgo = TestAlgorithm(
            "LongPath",
            listOf(FramePoint(0, 0), FramePoint(5, 5), FramePoint(7, 7), FramePoint(10, 10)),
            executionTime = 10L
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(shortPathAlgo, longPathAlgo))
        
        result.results.size shouldBe 2
        // Winner should favor shorter path (path efficiency component)
        result.winner shouldNotBe "None (all failed)"
    }

    @Test
    fun `compareAlgorithms handles multiple targets scenario`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Multiple Targets",
            start = FramePoint(0, 0),
            targets = listOf(
                FramePoint(10, 10),
                FramePoint(20, 20),
                FramePoint(30, 30)
            )
        )
        
        val algorithm = TestAlgorithm(
            "MultiTarget",
            listOf(FramePoint(0, 0), FramePoint(10, 10))
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(algorithm))
        
        result.scenario.targets shouldHaveSize 3
        result.results["MultiTarget"]?.success shouldBe true
    }

    @Test
    fun `summary includes success symbol for successful algorithms`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Symbol Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        val algorithm = TestAlgorithm(
            "SuccessAlgo",
            listOf(FramePoint(0, 0), FramePoint(10, 10))
        )
        
        val result = comparator.compareAlgorithms(scenario, listOf(algorithm))
        
        result.summary shouldContain "✓"
        result.summary shouldContain "SuccessAlgo"
    }

    @Test
    fun `summary includes failure symbol for failed algorithms`() {
        val comparator = PathfindingComparator()
        val scenario = PathfindingComparator.TestScenario(
            name = "Failure Symbol Test",
            start = FramePoint(0, 0),
            targets = listOf(FramePoint(10, 10))
        )
        val algorithm = TestAlgorithm("FailedAlgo", emptyList())
        
        val result = comparator.compareAlgorithms(scenario, listOf(algorithm))
        
        result.summary shouldContain "✗"
        result.summary shouldContain "Failed to find path"
    }

    @Test
    fun `runComprehensiveComparison tracks wins across scenarios`() {
        val comparator = PathfindingComparator()
        val scenarios = listOf(
            PathfindingComparator.TestScenario(
                name = "Test1",
                start = FramePoint(0, 0),
                targets = listOf(FramePoint(10, 10))
            ),
            PathfindingComparator.TestScenario(
                name = "Test2",
                start = FramePoint(5, 5),
                targets = listOf(FramePoint(15, 15))
            )
        )
        
        val algo1 = TestAlgorithm("Algo1", listOf(FramePoint(0, 0), FramePoint(10, 10)))
        val algo2 = TestAlgorithm("Algo2", emptyList())
        
        val results = comparator.runComprehensiveComparison(scenarios, listOf(algo1, algo2))
        
        results shouldHaveSize 2
        // Algo1 should win both scenarios since Algo2 returns empty paths
        results.forEach { result ->
            result.winner shouldBe "Algo1"
        }
    }
}