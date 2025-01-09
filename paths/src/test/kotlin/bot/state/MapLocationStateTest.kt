package bot.state

import bot.plan.action.MoveBuffer
import bot.state.map.Direction
import bot.state.map.Hyrule
import io.kotest.matchers.be
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.kotlin.mock
import util.d

class MapLocationStateTest {
    @Test
    fun `test best direction`() {
        //val mapLocationState = MapLocationState(Hyrule())
        //lastPoints
        val p32 = FramePoint(32, 88)
        val p33 = FramePoint(33, 88)
        //mapLocationState.lastPoints.addAll(listOf(p32, p33, p32, p32, p32, p33, p32, p32, p32, p33))
        val lastPoints = MoveBuffer(10)
        //32_88, 33_88, 32_88, 32_88, 32_88, 33_88, 32_88, 32_88, 32_88, 33_88
        lastPoints.addAll(listOf(p32, p33, p32, p32, p32, p33, p32, p32, p32, p33))
        val bestDirection = bestDirection(lastPoints)
        bestDirection shouldBe Direction.Right
    }

    @Test
    fun `test dirto`() {
        val p32 = FramePoint(32, 88)
        val p33 = FramePoint(33, 88)
        p32.dirTo(p32) shouldBe Direction.None
    }

}