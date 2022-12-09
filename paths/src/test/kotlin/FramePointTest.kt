import bot.state.FramePoint
import bot.state.minDistToAny
import io.kotest.matchers.shouldBe
import org.junit.Test

class FramePointTest {
    @Test
    fun `get closest`() {
        val pts = listOf(
            FramePoint(0,10),
            FramePoint(0,20),
            FramePoint(0,30),
            )
        FramePoint(0,0).minDistToAny(pts) shouldBe 10
        FramePoint(0,20).minDistToAny(pts) shouldBe 0
        FramePoint(0,27).minDistToAny(pts) shouldBe 3
    }

}