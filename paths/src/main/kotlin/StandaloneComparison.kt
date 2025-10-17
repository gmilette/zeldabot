import bot.plan.zstar.NeighborFinder
import bot.plan.zstar.ZStar
import bot.plan.zstar.route.BreadthFirstSearchWrapper
import bot.plan.zstar.route.ZStarWrapper
import bot.plan.zstar.route.PathfindingComparator
import bot.state.FramePoint
import util.Map2d

fun main() {
    println("=== BFS vs Z* Pathfinding Comparison ===")

    // Create a simple 100x100 passable map for testing
    val passableMap = Map2d((0 until 100).map {
        (0 until 100).map { true }.toMutableList()
    }.toMutableList())

    // Add some obstacles (a wall)
    for (x in 30..70) {
        for (y in 45..55) {
            passableMap.set(FramePoint(x, y), false)
        }
    }

    println("Created 100x100 grid with obstacle wall from (30,45) to (70,55)")

    val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
    val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

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
            targets = listOf(FramePoint(20, 20)),
            description = "Basic pathfinding test without obstacles"
        ),
        PathfindingComparator.TestScenario(
            name = "Around Wall",
            start = FramePoint(20, 50),
            targets = listOf(FramePoint(80, 50)),
            description = "Navigate around wall obstacle"
        ),
        PathfindingComparator.TestScenario(
            name = "Diagonal Path",
            start = FramePoint(10, 20),
            targets = listOf(FramePoint(90, 80)),
            description = "Long diagonal path avoiding obstacles"
        ),
        PathfindingComparator.TestScenario(
            name = "Close Combat",
            start = FramePoint(40, 40),
            targets = listOf(FramePoint(45, 45)),
            description = "Short range positioning near obstacle"
        )
    )

    // Run the comparison
    println("\nRunning ${scenarios.size} test scenarios...")
    val results = comparator.runComprehensiveComparison(scenarios, algorithms)

    // Print detailed results
    println("\n" + "=".repeat(50))
    println("DETAILED RESULTS")
    println("=".repeat(50))
    results.forEach { result ->
        println("\n${result.summary}")
    }

    // Summary
    val bfsWins = results.count { it.winner == "BreadthFirstSearch" }
    val zstarWins = results.count { it.winner == "ZStar" }

    println("=".repeat(50))
    println("FINAL SUMMARY")
    println("=".repeat(50))
    println("BFS wins: $bfsWins/${results.size}")
    println("Z* wins: $zstarWins/${results.size}")
    println("Overall winner: ${if (bfsWins > zstarWins) "BreadthFirstSearch" else if (zstarWins > bfsWins) "ZStar" else "TIE"}")
}