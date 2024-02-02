package bot.plan.action

import bot.state.FramePoint
import bot.state.justRightEndBottom
import bot.state.map.Direction
import bot.state.map.MapConstants
import io.kotest.matchers.shouldBe
import org.junit.Test
import util.Geom


class AttackLongActionDeciderTest {
    @Test
    fun `long attack`() {
        val rect = AttackLongActionDecider.swordRectangle(FramePoint(0, 0), FramePoint(200, 0), Direction.Right)
        rect.intersect(FramePoint(100, 0).toRect()) shouldBe true
    }
    private fun FramePoint.toRect(size: Int = MapConstants.oneGrid): Geom.Rectangle =
        Geom.Rectangle(this, this.justRightEndBottom)
}