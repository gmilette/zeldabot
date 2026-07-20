package bot.plan.zstar

import bot.ZeldaBot
import bot.plan.zstar.route.BreadthFirstSearch
import bot.plan.zstar.route.BreadthFirstSearch.Companion.framePointComparator
import bot.state.FramePoint
import bot.state.map.Direction
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import util.LoggerOverride
import util.Map2d

class BreadthFirstSearchTest {

    private fun allPassableMap(): Map2d<Boolean> =
        Map2d.Builder<Boolean>().add(168, 256, true).build()

    private fun makeNeighborFinder(): NeighborFinder =
        NeighborFinder(allPassableMap(), halfPassable = false).also {
            it.costF = Map2d.Builder<Int>().add(168, 256, 0).build()
        }

    @Before
    fun setup() {
        ZeldaBot.log = true
        LoggerOverride.init()
    }

    /**
     * Regression test for direction-aware visited set (TreeSet with framePointComparator).
     *
     * From start (8, 8):
     *   - (16, 8, Right) is reached at depth 1 and added to visited
     *   - (16, 8, Down)  is later enqueued via (8,8) -> up -> (8,0) -> right -> (16,0) -> down -> (16,8)
     *
     * If visited used x,y equality only (HashSet with FramePoint.equals),
     * (16, 8, Down) would be blocked as already visited and the goal would never be found.
     */
    @Test
    fun `BFS finds goal reachable only via specific direction through a previously visited position`() {
        val goalX = 16
        val goalY = 8

        val isGoal: (FramePoint) -> Boolean = { point ->
            point.x == goalX && point.y == goalY && point.direction == Direction.Down
        }

        val bfs = BreadthFirstSearch(isGoal, makeNeighborFinder())
        val paths = bfs.breadthFirstSearch(FramePoint(8, 8))

        paths.shouldNotBeEmpty()
        val goal = paths.first().last()
        goal.x shouldBe goalX
        goal.y shouldBe goalY
        goal.direction shouldBe Direction.Down
    }

    @Test
    fun `BFS finds straight line route of 3 points`() {
        val start = FramePoint(8, 8)
        val goal = FramePoint(10, 8)

        val bfs = BreadthFirstSearch({ it.x == goal.x && it.y == goal.y }, makeNeighborFinder())
        val paths = bfs.breadthFirstSearch(start)

        paths.shouldNotBeEmpty()
        val path = paths.first()
        path.last().x shouldBe goal.x
        path.last().y shouldBe goal.y
        path.size shouldBe 3
    }

    @Test
    fun `framePointComparator treats same position with different directions as distinct`() {
        val visited = sortedSetOf(framePointComparator)

        val pointRight = FramePoint(16, 8, Direction.Right)
        val pointDown = FramePoint(16, 8, Direction.Down)
        val pointNull = FramePoint(16, 8, null)
        framePointComparator.compare(pointRight, pointNull) shouldNotBe 0

        visited.add(pointRight)
        visited.add(pointNull)
        visited.add(pointDown)

        visited.size shouldBe 3
        (pointRight in visited) shouldBe true
        (pointDown in visited) shouldBe true
        (pointNull in visited) shouldBe true
    }
}
