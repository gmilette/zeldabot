package util

import bot.state.FramePoint
import bot.state.map.MovingDirection
import bot.state.relativeTo
import io.kotest.matchers.shouldBe
import org.junit.Test

class CalculateDirectionTest {
//    @Test
//    fun testUpRightDirection() {
//        val p1 = FramePoint(0, 0)
//        val p2 = FramePoint(3, 4)
//        MovingDirection.UP_RIGHT shouldBe  CalculateDirection.calculateDirection(p1, p2)
//    }
//
//    @Test
//    fun testUpLeftDirection() {
//        val p1 = FramePoint(3, 0)
//        val p2 = FramePoint(0, 4)
//        MovingDirection.UP_LEFT shouldBe CalculateDirection.calculateDirection(p1, p2)
//    }
//
//    @Test
//    fun testDownRightDirection() {
//        val p1 = FramePoint(0, 4)
//        val p2 = FramePoint(3, 0)
//        CalculateDirection.MovingDirection.DOWN_RIGHT shouldBe CalculateDirection.calculateDirection(p1, p2)
//    }
//
//    @Test
//    fun testDownLeftDirection() {
//        val p1 = FramePoint(3, 4)
//        val p2 = FramePoint(0, 0)
//        CalculateDirection.MovingDirection.DOWN_LEFT shouldBe CalculateDirection.calculateDirection(p1, p2)
//    }

    @Test
    fun testNoDirection() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(0, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe MovingDirection.UNKNOWN_OR_STATIONARY
    }

    @Test
    fun `test up direction`() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(0, 4)
        CalculateDirection.calculateDirection(p1, p2) shouldBe MovingDirection.UP
    }

    @Test
    fun `test down direction`() {
        val p1 = FramePoint(0, 4)
        val p2 = FramePoint(0, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe MovingDirection.DOWN
    }

    @Test
    fun `test real direction`() {
        val p1 = FramePoint(128, 108)
        val p2 = FramePoint(128, 124)
        val dir = CalculateDirection.calculateDirection(p1, p2)
        val a = 1
    }

    @Test
    fun `test left direction`() {
        val p1 = FramePoint(3, 0)
        val p2 = FramePoint(0, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe MovingDirection.LEFT
    }

    @Test
    fun `test right direction`() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(3, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe MovingDirection.RIGHT
    }

    @Test
    fun `test no direction`() {
        val p1 = FramePoint(0, 0)
        val p2 = FramePoint(0, 0)
        CalculateDirection.calculateDirection(p1, p2) shouldBe MovingDirection.UNKNOWN_OR_STATIONARY
    }

}