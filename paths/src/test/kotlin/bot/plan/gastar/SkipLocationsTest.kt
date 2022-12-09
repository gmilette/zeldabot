package bot.plan.gastar

import bot.state.map.Direction
import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import org.junit.Test

class SkipLocationsTest {
    @Test
    fun `check skip`() {
        SkipLocations.getNext(FramePoint(33, 100), Direction.Right).x shouldBe 35
        SkipLocations.getNext(FramePoint(33, 100), Direction.Left).x shouldBe 32
        SkipLocations.getNext(FramePoint(33, 100), Direction.Up).y shouldBe 98
        SkipLocations.getNext(FramePoint(33, 100), Direction.Down).y shouldBe 101
    }
}