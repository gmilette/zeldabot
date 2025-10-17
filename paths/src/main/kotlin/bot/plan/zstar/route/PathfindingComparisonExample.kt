package bot.plan.zstar.route

import bot.plan.zstar.ZStar
import bot.plan.zstar.NeighborFinder
import bot.state.FramePoint
import util.Map2d
import util.d

/**
 * Example usage of the pathfinding comparison framework
 */
object PathfindingComparisonExample {

    @JvmStatic
    fun main(args: Array<String>) {
        runComparison()
    }

    /**
     * Run a simple comparison between BFS and Z* algorithms
     */
    fun runComparison() {
        d { "Starting pathfinding algorithm comparison..." }

        // Create a simple passable map for testing
        val passableMap = Map2d((0 until 100).map {
            (0 until 100).map { true }.toMutableList()
        }.toMutableList())

        // Add some obstacles
        for (x in 30..50) {
            for (y in 30..50) {
                passableMap.set(FramePoint(x, y), false)
            }
        }

        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        // Set up cost grid for neighbor finder (BFS needs this)
        val costMap = Map2d((0 until 100).map {
            (0 until 100).map { 1 }.toMutableList()
        }.toMutableList())

        // Set higher costs for obstacles
        for (x in 30..50) {
            for (y in 30..50) {
                costMap.set(FramePoint(x, y), 1000)
            }
        }

        neighborFinder.costF = costMap

        // Create algorithm wrappers
        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val zstarWrapper = ZStarWrapper(zstar)

        val algorithms = listOf(bfsWrapper, zstarWrapper)
        val comparator = PathfindingComparator()

        // Create test scenarios
        val scenarios = listOf(
            PathfindingComparator.TestScenario(
                name = "Simple Path",
                start = FramePoint(10, 10),
                targets = listOf(FramePoint(90, 90)),
                description = "Basic pathfinding test"
            ),
            PathfindingComparator.TestScenario(
                name = "Around Obstacle",
                start = FramePoint(20, 20),
                targets = listOf(FramePoint(80, 80)),
                description = "Navigate around large obstacle"
            ),
            PathfindingComparator.TestScenario(
                name = "Close Combat",
                start = FramePoint(70, 70),
                targets = listOf(FramePoint(80, 80)),
                description = "Short range positioning"
            )
        )

        // Run the comparison
        val results = comparator.runComprehensiveComparison(scenarios, algorithms)

        // Print detailed results
        d { "" }
        d { "===== DETAILED RESULTS =====" }
        results.forEach { result ->
            d { result.summary }
        }
    }

    /**
     * Create a more comprehensive comparison with custom scenarios
     */
    fun runAdvancedComparison(
        passableMap: Map2d<Boolean>,
        enemyPositions: List<FramePoint> = emptyList()
    ) {
        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        // Set up enemy costs in the grid if enemies are provided
        if (enemyPositions.isNotEmpty()) {
            // This would typically be done by the grid customizer
            // but for testing we can simulate it
        }

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = true,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val zstarWrapper = ZStarWrapper(zstar)

        val algorithms = listOf(bfsWrapper, zstarWrapper)
        val comparator = PathfindingComparator()

        // Use standard test scenarios plus custom ones
        val standardScenarios = comparator.createStandardTestScenarios()
        val customScenarios = listOf(
            PathfindingComparator.TestScenario(
                name = "Enemy Avoidance",
                start = FramePoint(enemyPositions.firstOrNull()?.x?.minus(50) ?: 50,
                                 enemyPositions.firstOrNull()?.y?.minus(50) ?: 50),
                targets = listOf(FramePoint(enemyPositions.firstOrNull()?.x?.plus(50) ?: 150,
                                          enemyPositions.firstOrNull()?.y?.plus(50) ?: 150)),
                description = "Navigate around enemies"
            )
        )

        val allScenarios = standardScenarios + customScenarios

        d { "Running advanced comparison with ${allScenarios.size} scenarios..." }
        val results = comparator.runComprehensiveComparison(allScenarios, algorithms)

        // You can process results further here
        val bfsWins = results.count { it.winner == "BreadthFirstSearch" }
        val zstarWins = results.count { it.winner == "ZStar" }

        d { "" }
        d { "===== FINAL SUMMARY =====" }
        d { "BFS wins: $bfsWins" }
        d { "Z* wins: $zstarWins" }
        d { "Overall winner: ${if (bfsWins > zstarWins) "BreadthFirstSearch" else "ZStar"}" }
    }
}