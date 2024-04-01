package bot.state

import bot.state.FramePoint
import bot.state.map.MovingDirection
import io.kotest.matchers.shouldBe
import org.junit.Test

class FramePointKtTest {

    @Test
    fun `test relative to`() {
        val point = FramePoint(1, 1)
        val pt = point.pointAtDistance(FramePoint(2, 2), 2.0)
        println(pt.oneStr)
    }

    @Test
    fun `test relative to horizontal`() {
        val point = FramePoint(1, 1)
        val pt = point.pointAtDistance(FramePoint(2, 1), 2.0)
        println(pt.oneStr)
    }

    @Test
    fun `test relative to horizontal f`() {
        val point = FramePoint(1, 2)
        val pt = point.pointAtDistance(FramePoint(3, 2), 10.0)
        println(pt.oneStr)
    }
}