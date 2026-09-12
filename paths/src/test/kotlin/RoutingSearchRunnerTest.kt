import bot.state.FramePoint
import bot.state.map.Direction
import bot.state.map.MapConstants
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

/**
 * The routing test UI runs real routing code, these keep the glue honest.
 */
class RoutingSearchRunnerTest {
    private val enemy = FramePoint(112, 80, direction = Direction.Left)
    private val start = FramePoint(48, 80)

    private fun request(
        algorithm: RoutingAlgorithm,
        targetMode: TargetMode = TargetMode.AttackPoints
    ) = RoutingRequest(
        algorithm = algorithm,
        targetMode = targetMode,
        start = start,
        enemies = listOf(enemy),
        gridWidth = MapConstants.MAX_X + 1,
        gridHeight = MapConstants.MAX_Y + 1,
        halfPassable = false
    )

    @Test
    fun `attack points leave out the direction the enemy is facing`() {
        val targets = RoutingSearchRunner.targetsFor(listOf(enemy), TargetMode.AttackPoints)
        // the spot to the right of the enemy, where link stands to strike left into its face
        val inFrontOfIt = FramePoint(enemy.x + MapConstants.swordGridPlusOne, enemy.y)

        targets.shouldNotBeEmpty()
        // link can still come from below and strike up
        targets.shouldContain(FramePoint(enemy.x, enemy.y + MapConstants.swordGridPlusOne))
        // but not walk into what the enemy is facing
        targets.contains(inFrontOfIt) shouldBe false
        RoutingSearchRunner.targetsFor(listOf(enemy), TargetMode.AttackPointsNoCorner)
            .shouldContain(inFrontOfIt)
    }

    @Test
    fun `every algorithm routes to an attack point`() {
        for (algorithm in RoutingAlgorithm.entries) {
            val result = RoutingSearchRunner.run(request(algorithm))
            withClue(algorithm) {
                result.paths.shouldNotBeEmpty()
                val path = result.paths.first()
                path.first() shouldBe start
                result.targets shouldContain path.last()
            }
        }
    }

    @Test
    fun `breadth first stops when the start is already at the goal`() {
        val onTarget = RoutingSearchRunner.targetsFor(listOf(enemy), TargetMode.AttackPoints).first()
        val result = RoutingSearchRunner.run(
            request(RoutingAlgorithm.BreadthFirst).copy(start = onTarget)
        )

        result.paths shouldBe emptyList()
    }

    private fun withClue(clue: Any, block: () -> Unit) {
        try {
            block()
        } catch (e: AssertionError) {
            throw AssertionError("$clue: ${e.message}", e)
        }
    }
}
