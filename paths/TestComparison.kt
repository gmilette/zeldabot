import bot.plan.zstar.NeighborFinder
import bot.plan.zstar.ZStar
import bot.plan.zstar.route.BreadthFirstSearchWrapper
import bot.plan.zstar.route.ZStarWrapper
import bot.plan.zstar.route.PathfindingComparator
import bot.state.FramePoint
import util.Map2d

fun main() {
    println("Testing BFS vs Z* pathfinding algorithms...")

    try {
        // Create a simple test map
        val size = 50
        val passableMap = Map2d((0 until size).map {
            (0 until size).map { true }.toMutableList()
        }.toMutableList())

        // Add obstacles
        for (x in 20..30) {
            passableMap.set(FramePoint(x, 25), false)
        }

        println("Created ${size}x${size} test map with obstacles")

        val neighborFinder = NeighborFinder(passableMap, halfPassable = true, isLevel = false)
        val zstar = ZStar(passableMap, halfPassable = true, isLevel = false)

        val bfsWrapper = BreadthFirstSearchWrapper(
            ableToLongAttack = false,
            ableToAttack = true,
            neighborFinder = neighborFinder
        )

        val zstarWrapper = ZStarWrapper(zstar)

        val comparator = PathfindingComparator()

        // Simple test scenario
        val scenario = PathfindingComparator.TestScenario(
            name = "Basic Test",
            start = FramePoint(10, 25),
            targets = listOf(FramePoint(40, 25)),
            description = "Navigate around obstacle"
        )

        println("Running comparison...")
        val result = comparator.compareAlgorithms(scenario, listOf(bfsWrapper, zstarWrapper))

        println("\n=== RESULTS ===")
        println(result.summary)

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}