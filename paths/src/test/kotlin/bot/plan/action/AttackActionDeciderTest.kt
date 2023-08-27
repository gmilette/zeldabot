package bot.plan.action

import bot.state.FramePoint
import bot.state.map.Direction
import io.kotest.matchers.shouldBe
import org.junit.Test

class AttackActionDeciderTest() {

    @Test
    fun `go`() {
        AttackActionDecider.shouldAttack(Direction.Down, FramePoint(128, 107), listOf(FramePoint(128, 112))) shouldBe true
    }
}