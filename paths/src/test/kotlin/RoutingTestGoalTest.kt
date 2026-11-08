import bot.state.FramePoint
import bot.state.noDir
import bot.state.oam.swordDir
import bot.state.map.Direction
import bot.state.map.MapConstants
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.Test

class RoutingTestGoalTest {
    private val enemy = FramePoint(112, 80, direction = Direction.Left)
    private val start = FramePoint(48, 80)

    private fun goal(fullHealth: Boolean) = RoutingTestGoal.productionGoal(
        start, Direction.Right, listOf(enemy), isLevel = false, mapLoc = 0, fullHealth = fullHealth
    )::isGoal

    private fun search(
        kind: EnemyKind = EnemyKind.Generic,
        avoidFront: Boolean = false,
        algorithm: RoutingAlgorithm = RoutingAlgorithm.ZStarBreadthFirst
    ) = RoutingSearchRunner.run(
        RoutingRequest(
            algorithm = algorithm,
            start = start,
            enemies = listOf(enemy),
            gridWidth = MapConstants.MAX_X + 1,
            gridHeight = MapConstants.MAX_Y + 1,
            halfPassable = false,
            enemyKind = kind,
            avoidFrontOfEnemies = avoidFront
        )
    )

    @Test
    fun `the synthetic state leaves link able to swing and not mid attack`() {
        val state = RoutingTestGoal.syntheticState(
            start, Direction.Right, listOf(enemy), isLevel = false, mapLoc = 0
        )

        state.frameState.canUseSword shouldBe true
        state.frameState.linkDoingAnAttack() shouldBe false
        state.frameState.enemies.size shouldBe 1
        state.frameState.link.point shouldBe start.noDir()
    }

    @Test
    fun `without ranged attacks the goal is sword range`() {
        val isGoal = goal(fullHealth = false)

        isGoal(FramePoint(87, 80, direction = Direction.Right), true) shouldBe true
        isGoal(start.copy(direction = Direction.Right), true) shouldBe false
    }

    @Test
    fun `at full health the beam makes the enemy's line a goal`() {
        goal(fullHealth = true)(start.copy(direction = Direction.Right), true) shouldBe true
    }

    @Test
    fun `the search routes to sword range with production's own IsGoal`() {
        val result = search(algorithm = RoutingAlgorithm.BreadthFirst)

        result.paths.shouldNotBeEmpty()
        val reached = result.goals.first()
        reached.reached shouldBe true
        reached.rules shouldBe listOf("ShortAttack")
        reached.distToNearestEnemy shouldBe MapConstants.swordGridPlusOne + 1
    }

    @Test
    fun `sword enemies get a level with swords and a tile swordDir agrees with`() {
        val state = RoutingTestGoal.syntheticState(
            start, Direction.Right, listOf(enemy), isLevel = false, mapLoc = 0, swordEnemies = true
        )
        val agent = state.frameState.enemies.first()

        state.frameState.level shouldBe RoutingTestGoal.swordLevel
        swordDir.dirFront(agent.tile) shouldBe Direction.Left
        agent.dir shouldBe Direction.Left
        agent.canAttackFront(state.frameState.level) shouldBe false
    }

    @Test
    fun `a generic enemy can be attacked from the front`() {
        val state = RoutingTestGoal.syntheticState(
            start, Direction.Right, listOf(enemy), isLevel = false, mapLoc = 0
        )

        state.frameState.enemies.first()
            .canAttackFront(state.frameState.level) shouldBe true
    }

    @Test
    fun `a sword enemy's front is not a goal but its back and sides are`() {
        val g = RoutingTestGoal.productionGoal(
            start, Direction.Right, listOf(enemy), isLevel = false, mapLoc = 0, swordEnemies = true
        )

        // the enemy is at x=112 facing Left, so standing left of it and striking Right is its front
        g.isGoal(FramePoint(87, 80, direction = Direction.Right), true) shouldBe false
        g.isGoal(FramePoint(137, 80, direction = Direction.Left), true) shouldBe true
        g.isGoal(FramePoint(112, 55, direction = Direction.Down), true) shouldBe true
        g.isGoal(FramePoint(112, 105, direction = Direction.Up), true) shouldBe true
    }

    @Test
    fun `a generic enemy's front is a goal`() {
        val g = RoutingTestGoal.productionGoal(
            start, Direction.Right, listOf(enemy), isLevel = false, mapLoc = 0
        )

        g.isGoal(FramePoint(87, 80, direction = Direction.Right), true) shouldBe true
    }

    @Test
    fun `don't attack front prices the squares beside a sword enemy out of the route`() {
        val adjacent = MapConstants.swordGridPlusOne + 1

        // without it the route happily stops right beside the enemy, on its front side
        search(EnemyKind.Sword, avoidFront = false)
            .goals.minOf { it.distToNearestEnemy } shouldBe adjacent
        // with it, every goal is further out
        (search(EnemyKind.Sword, avoidFront = true)
            .goals.minOf { it.distToNearestEnemy } > adjacent) shouldBe true
    }

    @Test
    fun `don't attack front says so when there are no sword enemies`() {
        search(EnemyKind.Generic, avoidFront = true)
            .summary.contains("priced out nothing") shouldBe true
    }
}
