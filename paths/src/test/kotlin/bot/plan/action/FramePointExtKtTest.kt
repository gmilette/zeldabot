package bot.plan.action

import bot.state.FramePoint
import bot.state.justRightEnd
import org.junit.Test
import io.kotest.matchers.shouldBe

class FramePointExtKtTest {
    @Test
    fun a() {
        val link = FramePoint(172, 24)
        val other = FramePoint(156, 24)
//        val enemy = FramePoint(176, 24)
        val enemy = FramePoint(165, 30)
// 10
        val a = enemy.x
        val b = enemy.justRightEnd.x
        other.isInGrid(enemy) shouldBe true
    }
}