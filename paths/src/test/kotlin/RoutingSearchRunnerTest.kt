import bot.plan.action.AttackActionDecider
import bot.state.FramePoint
import bot.state.noDir
import bot.state.dirTo
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
        algorithm: RoutingAlgorithm
    ) = RoutingRequest(
        algorithm = algorithm,
        start = start,
        enemies = listOf(enemy),
        gridWidth = MapConstants.MAX_X + 1,
        gridHeight = MapConstants.MAX_Y + 1,
        halfPassable = false
    )

    @Test
    fun `every algorithm routes to where production would attack`() {
        // not "ends on a target": the goal is production's IsGoal, which stops on a square link can
        // attack FROM, and the targets are the attack points themselves
        for (algorithm in RoutingAlgorithm.entries) {
            val result = RoutingSearchRunner.run(request(algorithm))
            withClue(algorithm) {
                result.paths.shouldNotBeEmpty()
                val path = result.paths.first()
                path.first() shouldBe start
                result.goals.first().point shouldBe path.last()
                result.goals.first().reached shouldBe true
            }
        }
    }

    @Test
    fun `breadth first stops when the start is already at the goal`() {
        val onTarget = RoutingSearchRunner.targetsFor(listOf(enemy)).first()
        val result = RoutingSearchRunner.run(
            request(RoutingAlgorithm.BreadthFirst).copy(start = onTarget)
        )

        result.paths shouldBe emptyList()
        // no path, but the reason the start counted still gets reported
        result.goals.first().reached shouldBe true
    }

    @Test
    fun `every algorithm reports the goal each path reached`() {
        for (algorithm in RoutingAlgorithm.entries) {
            val result = RoutingSearchRunner.run(request(algorithm))
            withClue(algorithm) {
                result.goals.size shouldBe result.paths.size
                result.paths.forEachIndexed { index, path ->
                    val goal = result.goals[index]
                    goal.point shouldBe path.last()
                    goal.reached shouldBe true
                    goal.nearestEnemy shouldBe enemy.noDir()
                }
            }
        }
    }

    @Test
    fun `striking range reports a goal link never stands on a target to reach`() {
        for (algorithm in RoutingAlgorithm.entries) {
            val result = RoutingSearchRunner.run(
                request(algorithm)
            )
            withClue(algorithm) {
                result.paths.shouldNotBeEmpty()
                val goal = result.goals.first()
                result.targets.contains(goal.point) shouldBe false
                goal.reached shouldBe !result.summary.contains("stopped short")
            }
        }
    }

    @Test
    fun `both breadth first searches stop where production would attack`() {
        for (algorithm in listOf(RoutingAlgorithm.BreadthFirst, RoutingAlgorithm.ZStarBreadthFirst)) {
            val result = RoutingSearchRunner.run(
                request(algorithm)
            )
            withClue(algorithm) {
                result.goals.shouldNotBeEmpty()
                result.goals.forEach {
                    it.reached shouldBe true
                    it.rules shouldContain "ShortAttack"
                }
            }
        }
    }

    @Test
    fun `the last point of a path faces the way link walked into it`() {
        val result = RoutingSearchRunner.run(request(RoutingAlgorithm.ZStarAStar))
        val path = result.paths.first()

        path.last().direction shouldBe path[path.size - 2].dirTo(path.last())
    }

    @Test
    fun `a goal override replaces production's IsGoal`() {
        val stopHere = FramePoint(start.x + MapConstants.oneGrid, start.y)
        val result = RoutingSearchRunner.run(
            request(RoutingAlgorithm.BreadthFirst)
                .copy(goalOverride = { point, _ -> point.x == stopHere.x && point.y == stopHere.y })
        )

        result.paths.shouldNotBeEmpty()
        val goal = result.goals.first()
        goal.point.x shouldBe stopHere.x
        goal.point.y shouldBe stopHere.y
        goal.rules shouldBe listOf("injected isGoal")
    }

    @Test
    fun `an enemy with no direction does not crash the sword tile lookup`() {
        val noDirEnemy = FramePoint(112, 80, direction = Direction.None)
        val result = RoutingSearchRunner.run(
            request(RoutingAlgorithm.ZStarBreadthFirst)
                .copy(enemies = listOf(noDirEnemy), enemyKind = EnemyKind.Sword)
        )

        result.goals.shouldNotBeEmpty()
    }

    @Test
    fun `targets are always the no corner attack points`() {
        RoutingSearchRunner.targetsFor(listOf(enemy)) shouldBe
                AttackActionDecider.attackPointsNoCorner(enemy.noDir()).distinct()
    }

    private fun withClue(clue: Any, block: () -> Unit) {
        try {
            block()
        } catch (e: AssertionError) {
            throw AssertionError("$clue: ${e.message}", e)
        }
    }
}
