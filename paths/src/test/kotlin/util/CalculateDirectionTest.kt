package util

import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import org.junit.Test

class CalculateDirectionTest {
    @Test
    fun testUpRightDirection() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(3, 4)
        CalculateDirection.MovingDirection.UP_RIGHT shouldBe  CalculateDirection.calculateDirection(p1, p2)
    }

    @Test
    fun testUpLeftDirection() {
        val p1 = FramePoint(3, 0)
        val p2 = FramePoint(0, 4)
        CalculateDirection.MovingDirection.UP_LEFT shouldBe CalculateDirection.calculateDirection(p1, p2)
    }

    @Test
    fun testDownRightDirection() {
        val p1 = FramePoint(0, 4)
        val p2 = FramePoint(3, 0)
        CalculateDirection.MovingDirection.DOWN_RIGHT shouldBe CalculateDirection.calculateDirection(p1, p2)
    }

    @Test
    fun testDownLeftDirection() {
        val p1 = FramePoint(3, 4)
        val p2 = FramePoint(0, 0)
        CalculateDirection.MovingDirection.DOWN_LEFT shouldBe CalculateDirection.calculateDirection(p1, p2)
    }

    @Test
    fun testNoDirection() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(0, 0)
        CalculateDirection.MovingDirection.UNKNOWN shouldBe CalculateDirection.calculateDirection(p1, p2)
    }

    @Test
    fun `test up direction`() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(0, 4)
        CalculateDirection.calculateDirection(p1, p2) shouldBe CalculateDirection.MovingDirection.UP
    }

    @Test
    fun `test down direction`() {
        val p1 = FramePoint(0, 4)
        val p2 = FramePoint(0, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe CalculateDirection.MovingDirection.DOWN
    }

    @Test
    fun `test left direction`() {
        val p1 = FramePoint(3, 0)
        val p2 = FramePoint(0, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe CalculateDirection.MovingDirection.LEFT
    }

    @Test
    fun `test right direction`() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(3, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe CalculateDirection.MovingDirection.RIGHT
    }

    @Test
    fun `test no direction`() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(0, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe CalculateDirection.MovingDirection.UNKNOWN
    }

}