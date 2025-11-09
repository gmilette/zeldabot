package bot.plan.zstar.route

import bot.plan.zstar.NeighborFinder
import bot.state.FramePoint
import bot.state.map.Direction
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import util.Map2d

class PathScoringTest {

    private lateinit var pathScoring: PathScoring
    private lateinit var neighborFinder: NeighborFinder

    @Before
    fun setup() {
        // Create a simple passable map for testing
        val passableMap = Map2d.Builder<Boolean>().add(200, 200, true).build()

        // Create a cost map where certain areas are unsafe
        val costMap = Map2d.Builder<Int>().add(200, 200, 0).build()

        neighborFinder = NeighborFinder(passableMap)
        neighborFinder.costF = costMap

        pathScoring = PathScoring(neighborFinder)
    }

    // ========================================
    // countUnsafe / countSafe tests
    // ========================================

    @Test
    fun `countUnsafe should return 0 for all safe path`() {
        val safePath = listOf(
            FramePoint(0, 0),
            FramePoint(8, 0),
            FramePoint(16, 0)
        )

        // All points are safe (cost < 100)
        pathScoring.countUnsafe(safePath) shouldBeExactly 0.0
    }

    @Test
    fun `countUnsafe should count all unsafe tiles`() {
        val path = listOf(
            FramePoint(0, 0),
            FramePoint(1, 1),
            FramePoint(2, 2)
        )

        // Mark points as unsafe (cost >= 100)
        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 200)

        pathScoring.countUnsafe(path) shouldBeExactly 2.0
    }

    @Test
    fun `countSafe should return total safe tiles`() {
        val path = listOf(
            FramePoint(0, 0),
            FramePoint(1, 1),
            FramePoint(2, 2)
        )

        neighborFinder.costF.set(FramePoint(1, 1), 150) // unsafe

        pathScoring.countSafe(path) shouldBeExactly 2.0
    }

    @Test
    fun `countUnsafe should handle empty path`() {
        val emptyPath = emptyList<FramePoint>()
        pathScoring.countUnsafe(emptyPath) shouldBeExactly 0.0
    }

    // ========================================
    // maxConsecutiveUnsafe tests
    // ========================================

    @Test
    fun `maxConsecutiveUnsafe should return 0 for all safe path`() {
        val safePath = listOf(
            FramePoint(0, 0),
            FramePoint(8, 0),
            FramePoint(16, 0)
        )

        pathScoring.maxConsecutiveUnsafe(safePath) shouldBeExactly 0
    }

    @Test
    fun `maxConsecutiveUnsafe should find single consecutive unsafe section`() {
        val path = listOf(
            FramePoint(0, 0),  // safe
            FramePoint(1, 1),  // unsafe
            FramePoint(2, 2),  // unsafe
            FramePoint(3, 3),  // unsafe
            FramePoint(4, 4)   // safe
        )

        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 150)
        neighborFinder.costF.set(FramePoint(3, 3), 150)

        pathScoring.maxConsecutiveUnsafe(path) shouldBeExactly 3
    }

    @Test
    fun `maxConsecutiveUnsafe should find maximum among multiple sections`() {
        val path = listOf(
            FramePoint(0, 0),  // safe
            FramePoint(1, 1),  // unsafe
            FramePoint(2, 2),  // unsafe
            FramePoint(3, 3),  // safe
            FramePoint(4, 4),  // unsafe
            FramePoint(5, 5),  // unsafe
            FramePoint(6, 6),  // unsafe
            FramePoint(7, 7)   // safe
        )

        // First section: 2 consecutive
        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 150)

        // Second section: 3 consecutive (max)
        neighborFinder.costF.set(FramePoint(4, 4), 150)
        neighborFinder.costF.set(FramePoint(5, 5), 150)
        neighborFinder.costF.set(FramePoint(6, 6), 150)

        pathScoring.maxConsecutiveUnsafe(path) shouldBeExactly 3
    }

    @Test
    fun `maxConsecutiveUnsafe should handle all unsafe path`() {
        val path = listOf(
            FramePoint(1, 1),
            FramePoint(2, 2),
            FramePoint(3, 3)
        )

        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 150)
        neighborFinder.costF.set(FramePoint(3, 3), 150)

        pathScoring.maxConsecutiveUnsafe(path) shouldBeExactly 3
    }

    // ========================================
    // averageEnemyDistance tests
    // ========================================

    @Test
    fun `averageEnemyDistance should return 0 for empty path`() {
        val enemies = listOf(FramePoint(50, 50))
        pathScoring.averageEnemyDistance(emptyList(), enemies) shouldBeExactly 0.0
    }

    @Test
    fun `averageEnemyDistance should return 0 for no enemies`() {
        val path = listOf(FramePoint(0, 0), FramePoint(10, 10))
        pathScoring.averageEnemyDistance(path, emptyList()) shouldBeExactly 0.0
    }

    @Test
    fun `averageEnemyDistance should calculate correct average`() {
        val path = listOf(
            FramePoint(0, 0),   // distance to (10,0) = 10
            FramePoint(20, 0)   // distance to (10,0) = 10
        )
        val enemies = listOf(FramePoint(10, 0))

        // Average = (10 + 10) / 2 = 10.0
        pathScoring.averageEnemyDistance(path, enemies) shouldBeExactly 10.0
    }

    @Test
    fun `averageEnemyDistance should use closest enemy for each point`() {
        val path = listOf(
            FramePoint(0, 0),   // closest to (5,0) = 5
            FramePoint(50, 0)   // closest to (45,0) = 5
        )
        val enemies = listOf(FramePoint(5, 0), FramePoint(45, 0))

        // Average = (5 + 5) / 2 = 5.0
        pathScoring.averageEnemyDistance(path, enemies) shouldBeExactly 5.0
    }

    // ========================================
    // totalEnemyDistance tests
    // ========================================

    @Test
    fun `totalEnemyDistance should sum all distances`() {
        val path = listOf(
            FramePoint(0, 0),   // distance to (10,0) = 10
            FramePoint(20, 0)   // distance to (10,0) = 10
        )
        val enemies = listOf(FramePoint(10, 0))

        pathScoring.totalEnemyDistance(path, enemies) shouldBeExactly 20
    }

    // ========================================
    // stepsToFirstSafe tests
    // ========================================

    @Test
    fun `stepsToFirstSafe should return 0 when starting on safe tile`() {
        val path = listOf(
            FramePoint(0, 0),  // safe
            FramePoint(1, 1)
        )

        pathScoring.stepsToFirstSafe(path) shouldBeExactly 0
    }

    @Test
    fun `stepsToFirstSafe should count steps to first safe tile`() {
        val path = listOf(
            FramePoint(1, 1),  // unsafe - step 0
            FramePoint(2, 2),  // unsafe - step 1
            FramePoint(3, 3),  // safe - step 2
            FramePoint(4, 4)
        )

        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 150)

        pathScoring.stepsToFirstSafe(path) shouldBeExactly 2
    }

    @Test
    fun `stepsToFirstSafe should return path size if no safe tiles`() {
        val path = listOf(
            FramePoint(1, 1),
            FramePoint(2, 2),
            FramePoint(3, 3)
        )

        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 150)
        neighborFinder.costF.set(FramePoint(3, 3), 150)

        pathScoring.stepsToFirstSafe(path) shouldBeExactly 3
    }

    // ========================================
    // countDirectionChanges tests
    // ========================================

    @Test
    fun `countDirectionChanges should return 0 for straight path`() {
        val path = listOf(
            FramePoint(0, 0, Direction.Right),
            FramePoint(1, 0, Direction.Right),
            FramePoint(2, 0, Direction.Right)
        )

        pathScoring.countDirectionChanges(path) shouldBeExactly 0
    }

    @Test
    fun `countDirectionChanges should count single turn`() {
        val path = listOf(
            FramePoint(0, 0, Direction.Right),
            FramePoint(1, 0, Direction.Right),
            FramePoint(1, 1, Direction.Down)
        )

        pathScoring.countDirectionChanges(path) shouldBeExactly 1
    }

    @Test
    fun `countDirectionChanges should count multiple turns`() {
        val path = listOf(
            FramePoint(0, 0, Direction.Right),
            FramePoint(1, 0, Direction.Down),
            FramePoint(1, 1, Direction.Left),
            FramePoint(0, 1, Direction.Up)
        )

        pathScoring.countDirectionChanges(path) shouldBeExactly 3
    }

    @Test
    fun `countDirectionChanges should handle empty path`() {
        pathScoring.countDirectionChanges(emptyList()) shouldBeExactly 0
    }

    @Test
    fun `countDirectionChanges should handle single point path`() {
        val path = listOf(FramePoint(0, 0))
        pathScoring.countDirectionChanges(path) shouldBeExactly 0
    }

    // ========================================
    // PathScore comparison tests
    // ========================================

    @Test
    fun `PathScore should prioritize fewer unsafe tiles`() {
        val score1 = PathScore(
            unsafeCount = 1.0,
            maxConsecutiveUnsafe = 1,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        val score2 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 1,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        score1 shouldBeLessThan score2
    }

    @Test
    fun `PathScore should prioritize fewer consecutive unsafe when unsafe count equal`() {
        val score1 = PathScore(
            unsafeCount = 3.0,
            maxConsecutiveUnsafe = 1,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        val score2 = PathScore(
            unsafeCount = 3.0,
            maxConsecutiveUnsafe = 3,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        score1 shouldBeLessThan score2
    }

    @Test
    fun `PathScore should prioritize reaching safety faster`() {
        val score1 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        val score2 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 5,
            directionChanges = 2
        )

        score1 shouldBeLessThan score2
    }

    @Test
    fun `PathScore should prioritize greater enemy distance`() {
        val score1 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 10,
            averageEnemyDistance = 100.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        val score2 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        score1 shouldBeLessThan score2
    }

    @Test
    fun `PathScore should prioritize shorter paths`() {
        val score1 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 5,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        val score2 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 2
        )

        score1 shouldBeLessThan score2
    }

    @Test
    fun `PathScore should prioritize fewer direction changes`() {
        val score1 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 1
        )

        val score2 = PathScore(
            unsafeCount = 2.0,
            maxConsecutiveUnsafe = 2,
            pathLength = 10,
            averageEnemyDistance = 50.0,
            stepsToFirstSafe = 1,
            directionChanges = 5
        )

        score1 shouldBeLessThan score2
    }

    // ========================================
    // calculatePathScore integration tests
    // ========================================

    @Test
    fun `calculatePathScore should compute all metrics correctly`() {
        val path = listOf(
            FramePoint(0, 0),   // safe
            FramePoint(8, 0),   // unsafe
            FramePoint(16, 0),  // unsafe
            FramePoint(24, 0)   // safe
        )

        neighborFinder.costF.set(FramePoint(8, 0), 150)
        neighborFinder.costF.set(FramePoint(16, 0), 150)

        val enemies = listOf(FramePoint(50, 0))
        val score = pathScoring.calculatePathScore(path, enemies)

        score.unsafeCount shouldBeExactly 2.0
        score.maxConsecutiveUnsafe shouldBeExactly 2
        score.pathLength shouldBeExactly 4
        score.stepsToFirstSafe shouldBeExactly 0
        // All points are on a horizontal line, so 0 direction changes
        score.directionChanges shouldBeExactly 0
    }

    // ========================================
    // sortPathsByScore tests
    // ========================================

    @Test
    fun `sortPathsByScore should sort paths by quality`() {
        // Create three paths with different qualities
        val worstPath = listOf(
            FramePoint(1, 1),  // unsafe
            FramePoint(2, 2),  // unsafe
            FramePoint(3, 3),  // unsafe
            FramePoint(4, 4)
        )

        val mediumPath = listOf(
            FramePoint(0, 0),  // safe
            FramePoint(1, 1),  // unsafe
            FramePoint(8, 0)   // safe
        )

        val bestPath = listOf(
            FramePoint(0, 0),  // safe
            FramePoint(8, 0),  // safe
            FramePoint(16, 0)  // safe
        )

        // Mark points as unsafe
        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 150)
        neighborFinder.costF.set(FramePoint(3, 3), 150)

        val paths = listOf(worstPath, mediumPath, bestPath)
        val enemies = listOf(FramePoint(50, 50))

        val sorted = pathScoring.sortPathsByScore(paths, enemies)

        sorted[0] shouldBe bestPath
        sorted[1] shouldBe mediumPath
        sorted[2] shouldBe worstPath
    }

    @Test
    fun `sortPathsByScore should handle empty list`() {
        val sorted = pathScoring.sortPathsByScore(emptyList(), emptyList())
        sorted.shouldBeEmpty()
    }

    @Test
    fun `sortPathsByScore should prefer scattered unsafe over consecutive unsafe`() {
        // Path with scattered unsafe tiles
        val scatteredPath = listOf(
            FramePoint(0, 0),  // safe
            FramePoint(1, 1),  // unsafe
            FramePoint(8, 0),  // safe
            FramePoint(9, 1),  // unsafe
            FramePoint(16, 0)  // safe
        )

        // Path with consecutive unsafe tiles
        val consecutivePath = listOf(
            FramePoint(2, 2),  // unsafe
            FramePoint(3, 3),  // unsafe
            FramePoint(8, 0),  // safe
            FramePoint(16, 0), // safe
            FramePoint(24, 0)  // safe
        )

        // Mark points as unsafe
        neighborFinder.costF.set(FramePoint(1, 1), 150)
        neighborFinder.costF.set(FramePoint(9, 1), 150)
        neighborFinder.costF.set(FramePoint(2, 2), 150)
        neighborFinder.costF.set(FramePoint(3, 3), 150)

        val paths = listOf(consecutivePath, scatteredPath)
        val enemies = listOf(FramePoint(50, 50))

        val sorted = pathScoring.sortPathsByScore(paths, enemies)

        // Scattered should be better (both have 2 unsafe, but scattered has maxConsecutive=1 vs 2)
        sorted[0] shouldBe scatteredPath
        sorted[1] shouldBe consecutivePath
    }
}
