package bot.plan.action

import bot.plan.action.AttackActionDecider.downPoint
import bot.plan.action.AttackActionDecider.rightPoints
import bot.state.FramePoint
import bot.state.map.Direction
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class AttackActionDeciderTest() {

    // downPoint() = spots to attack an enemy while facing DOWN, so Link stands
    // ABOVE the enemy. longExtra = swordGridPlusOne = 24, shortExtra = swordGrid = 8.
    @Test
    fun `downPoint returns the eight attack-from-above positions`() {
        // enemy at (64, 64): two columns (x and x+8), rows from y-27 to y-24
        FramePoint(64, 64).downPoint() shouldContainExactly listOf(
            FramePoint(64, 37),
            FramePoint(64, 38),
            FramePoint(64, 39),
            FramePoint(64, 40),
            FramePoint(72, 40),
            FramePoint(72, 39),
            FramePoint(72, 38),
            FramePoint(72, 37),
        )
    }

    @Test
    fun `downPoint places link above the enemy within sword range`() {
        val enemy = FramePoint(100, 100)
        val points = enemy.downPoint()

        points.size shouldBe 8
        // every attack spot is above the enemy (smaller y)...
        points.all { it.y < enemy.y } shouldBe true
        // ...at exactly one sword-grid-plus-one back, give or take the 3px chase slack
        points.map { enemy.y - it.y }.toSet() shouldBe setOf(24, 25, 26, 27)
        // ...and only in the two columns x and x + swordGrid
        points.map { it.x }.toSet() shouldBe setOf(100, 108)
    }

    @Test
    fun att2() {
        val pt1 = AttackActionDecider.attackPointsNoCorner(FramePoint(120, 99))
        val pt2 = AttackActionDecider.attackPointsNoCorner(FramePoint(112, 99))
        val ptl = FramePoint(112, 99).rightPoints()
        for (framePoint in ptl) {
            println("Pt $framePoint")
        }

        val move = AttackActionDecider.inRangeOf(Direction.Left, FramePoint(88, 109),
            listOf(FramePoint(112, 99), FramePoint(120, 99)), false)
        val a = 1
    }

    @Test
    fun att() {
        AttackActionDecider.inRangeOf(Direction.Right, FramePoint(55, 24),
            listOf(FramePoint(45, 32)), false) shouldBe null
    }

    @Test
    fun testDown() {
        val pt = FramePoint(192, 66, direction = Direction.Down)
        val pts = AttackActionDecider.attackPoints(pt, not = Direction.Down)
        val a = 1


    }

    @Test
    fun testUp() {
        val pt = FramePoint(192, 101, direction = Direction.Up)
        val pts = AttackActionDecider.attackPoints(pt, not = Direction.Up)
        val a = 1

    }
}
