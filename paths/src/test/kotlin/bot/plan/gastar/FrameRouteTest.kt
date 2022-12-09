package bot.plan.gastar

import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import org.junit.Test


internal class FrameRouteTest {
    @Test
    fun `test it`() {
        val route = FrameRoute(listOf(
            FramePoint(1,1),
            FramePoint(2,2),
            FramePoint(3,3),
            FramePoint(4,4),
        ))

        route.isOn(FramePoint(2, 2)) shouldBe 1
        route.isOn(FramePoint(2, 99)) shouldBe null

        route.popUntil(FramePoint(3,3)) shouldBe FramePoint(3, 3)
        route.numPoints shouldBe 1
    }
}