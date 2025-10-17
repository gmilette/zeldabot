package bot.plan.zstar.route

import bot.state.FramePoint
import util.d

/**
 * Framework for comparing different pathfinding algorithms
 */
class PathfindingComparator {

    /**
     * Test scenario for algorithm comparison
     */
    data class TestScenario(
        val name: String,
        val start: FramePoint,
        val targets: List<FramePoint>,
        val maxDepth: Int = 255,
        val description: String = ""
    )

    /**
     * Comparison result between algorithms
     */
    data class ComparisonResult(
        val scenario: TestScenario,
        val results: Map<String, PathfindingResult>,
        val winner: String,
        val summary: String
    )

    /**
     * Compare multiple algorithms on a single scenario
     */
    fun compareAlgorithms(
        scenario: TestScenario,
        algorithms: List<PathfindingAlgorithm>
    ): ComparisonResult {
        d { "=== Running scenario: ${scenario.name} ===" }
        d { "Start: ${scenario.start}, Targets: ${scenario.targets}" }

        val results = mutableMapOf<String, PathfindingResult>()

        for (algorithm in algorithms) {
            d { "Testing ${algorithm.getName()}..." }
            try {
                val result = algorithm.findRoute(scenario.start, scenario.targets, scenario.maxDepth)
                results[algorithm.getName()] = result
                d { "  Result: ${if (result.success) "SUCCESS" else "FAILED"}, " +
                   "Path length: ${result.path.size}, " +
                   "Time: ${result.metrics.executionTimeMs}ms, " +
                   "Safety: ${String.format("%.2f", result.metrics.safetyScore)}, " +
                   "Nodes: ${result.metrics.nodesExplored}/${result.metrics.nodesConsidered} " +
                   "(${String.format("%.1f%%", result.metrics.searchEfficiency * 100)})" }
            } catch (e: Exception) {
                d { "  ERROR: ${e.message}" }
                results[algorithm.getName()] = PathfindingResult(
                    emptyList(),
                    PathfindingMetrics(
                        executionTimeMs = -1,
                        nodesExplored = 0,
                        iterationsUsed = 0,
                        pathLength = 0,
                        safetyScore = 0.0,
                        algorithmName = algorithm.getName()
                    ),
                    false
                )
            }
        }

        val winner = determineWinner(results)
        val summary = generateSummary(scenario, results, winner)

        return ComparisonResult(scenario, results, winner, summary)
    }

    /**
     * Run comprehensive comparison across multiple scenarios
     */
    fun runComprehensiveComparison(
        scenarios: List<TestScenario>,
        algorithms: List<PathfindingAlgorithm>
    ): List<ComparisonResult> {
        d { "===== COMPREHENSIVE PATHFINDING COMPARISON =====" }
        d { "Algorithms: ${algorithms.map { it.getName() }.joinToString(", ")}" }
        d { "Scenarios: ${scenarios.size}" }
        d { "" }

        val results = scenarios.map { scenario ->
            compareAlgorithms(scenario, algorithms)
        }

        generateOverallReport(results, algorithms)

        return results
    }

    private fun determineWinner(results: Map<String, PathfindingResult>): String {
        val successfulResults = results.filter { it.value.success }

        if (successfulResults.isEmpty()) {
            return "None (all failed)"
        }

        // Scoring system: Safety (40%) + Speed (30%) + Path efficiency (30%)
        val scores = successfulResults.mapValues { (_, result) ->
            val safetyScore = result.metrics.safetyScore * 0.4
            val speedScore = (1.0 / (result.metrics.executionTimeMs + 1)) * 0.3
            val efficiencyScore = if (result.metrics.pathLength > 0) {
                (1.0 / result.metrics.pathLength) * 0.3
            } else 0.0

            safetyScore + speedScore + efficiencyScore
        }

        return scores.maxByOrNull { it.value }?.key ?: "Unknown"
    }

    private fun generateSummary(
        scenario: TestScenario,
        results: Map<String, PathfindingResult>,
        winner: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Scenario: ${scenario.name}")
        sb.appendLine("Winner: $winner")
        sb.appendLine()

        results.forEach { (algorithmName, result) ->
            sb.appendLine("$algorithmName:")
            if (result.success) {
                sb.appendLine("  ✓ Path found (${result.path.size} steps)")
                sb.appendLine("  ⏱ Time: ${result.metrics.executionTimeMs}ms")
                sb.appendLine("  🛡 Safety: ${String.format("%.2f%%", result.metrics.safetyScore * 100)}")
                sb.appendLine("  🔍 Nodes explored: ${result.metrics.nodesExplored}/${result.metrics.nodesConsidered}")
                sb.appendLine("  📊 Search efficiency: ${String.format("%.1f%%", result.metrics.searchEfficiency * 100)}")
                sb.appendLine("  📈 Max queue size: ${result.metrics.maxQueueSize}")
                sb.appendLine("  🔄 Iterations: ${result.metrics.iterationsUsed}")
            } else {
                sb.appendLine("  ✗ Failed to find path")
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun generateOverallReport(
        results: List<ComparisonResult>,
        algorithms: List<PathfindingAlgorithm>
    ) {
        d { "" }
        d { "===== OVERALL COMPARISON REPORT =====" }

        val algorithmWins = algorithms.associate { it.getName() to 0 }.toMutableMap()
        val algorithmStats = algorithms.associate {
            it.getName() to mutableListOf<PathfindingMetrics>()
        }

        results.forEach { comparison ->
            algorithmWins[comparison.winner] = algorithmWins[comparison.winner]!! + 1

            comparison.results.forEach { (name, result) ->
                if (result.success) {
                    algorithmStats[name]?.add(result.metrics)
                }
            }
        }

        d { "Win Count:" }
        algorithmWins.forEach { (name, wins) ->
            d { "  $name: $wins/${results.size} scenarios" }
        }

        d { "" }
        d { "Average Performance (successful runs only):" }
        algorithmStats.forEach { (name, metrics) ->
            if (metrics.isNotEmpty()) {
                val avgTime = metrics.map { it.executionTimeMs }.average()
                val avgSafety = metrics.map { it.safetyScore }.average()
                val avgPath = metrics.map { it.pathLength }.average()

                d { "$name:" }
                d { "  Avg Time: ${String.format("%.1f", avgTime)}ms" }
                d { "  Avg Safety: ${String.format("%.2f%%", avgSafety * 100)}" }
                d { "  Avg Path Length: ${String.format("%.1f", avgPath)} steps" }
            } else {
                d { "$name: No successful runs" }
            }
        }
        d { "" }
    }

    /**
     * Create common test scenarios for comparison
     */
    fun createStandardTestScenarios(): List<TestScenario> {
        return listOf(
            TestScenario(
                name = "Simple Direct Path",
                start = FramePoint(100, 100),
                targets = listOf(FramePoint(200, 100)),
                description = "Straight line movement test"
            ),
            TestScenario(
                name = "Around Obstacle",
                start = FramePoint(100, 100),
                targets = listOf(FramePoint(200, 200)),
                description = "Pathfinding around obstacles"
            ),
            TestScenario(
                name = "Multiple Targets",
                start = FramePoint(50, 50),
                targets = listOf(
                    FramePoint(150, 50),
                    FramePoint(100, 150),
                    FramePoint(200, 100)
                ),
                description = "Finding path to any of multiple targets"
            ),
            TestScenario(
                name = "Long Distance",
                start = FramePoint(0, 0),
                targets = listOf(FramePoint(500, 500)),
                maxDepth = 1000,
                description = "Long distance pathfinding test"
            ),
            TestScenario(
                name = "Combat Scenario",
                start = FramePoint(120, 120),
                targets = listOf(FramePoint(140, 140)),
                description = "Short range combat positioning"
            )
        )
    }
}