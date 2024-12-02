package bot.plan.action

import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import org.junit.Test

class MinDistTotalFramesCountTest {
    @Test
    fun `test small`() {
        val t = MinDistTotalFramesCount()
        t.record(FramePoint(199, 80))
        t.record(FramePoint(200, 80))
        t.distance() shouldBe 1
    }

    @Test
    fun `test big`() {
        val t = MinDistTotalFramesCount()
        t.record(FramePoint(199, 80))
        t.record(FramePoint(200, 80))
        t.record(FramePoint(200, 81))
        t.record(FramePoint(200, 82))
        t.distance() shouldBe 3
    }

    @Test
    fun `test none`() {
        val t = MinDistTotalFramesCount()
        t.record(FramePoint(199, 80))
        t.record(FramePoint(199, 80))
        t.distance() shouldBe 0
    }

    @Test
    fun `test escape`() {
        val t = MinDistTotalFramesCount()
        t.record(FramePoint(199, 80))
        t.record(FramePoint(200, 80))
        t.record(FramePoint(199, 80))
        t.record(FramePoint(197, 80))
        t.record(FramePoint(196, 80))
        t.record(FramePoint(194, 80))
        t.record(FramePoint(193, 80))
        t.record(FramePoint(192, 80))
        t.record(FramePoint(192, 80))
        t.record(FramePoint(191, 80))
        t.record(FramePoint(189, 80))
        t.record(FramePoint(188, 80))
        t.record(FramePoint(186, 80))
        t.record(FramePoint(185, 80))
        t.record(FramePoint(184, 80))
        t.record(FramePoint(183, 80))
        t.record(FramePoint(181, 80))
        t.record(FramePoint(180, 80))
        t.record(FramePoint(178, 80))
        t.record(FramePoint(177, 80))
        t.record(FramePoint(176, 80))
        t.record(FramePoint(176, 80))
        t.record(FramePoint(176, 82))
        t.record(FramePoint(176, 83))
        t.record(FramePoint(176, 85))
        t.record(FramePoint(176, 86))
        t.record(FramePoint(176, 88))
        t.record(FramePoint(174, 88))
        t.record(FramePoint(173, 88))
        t.record(FramePoint(171, 88))
        t.record(FramePoint(170, 88))
        t.record(FramePoint(168, 88))

        println(t.distance())
    }
}