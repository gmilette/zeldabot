package bot.plan.action

import bot.state.FramePoint
import bot.state.map.Direction
import io.kotest.matchers.shouldBe
import org.junit.Test

class AttackActionDeciderTest() {

//    Debug: (Kermit)  rhino location (128, 96) target -> (128, 112) dir: Down
//    Debug: (Kermit)  move to spot (128, 112) with Bomb force new
//    Debug: (Kermit)  route To
//    Debug: (Kermit) should attack dir = Down link = (128, 107) dirGrid = (128, 122) numEnemies 1

    @Test
    fun `go`() {
        AttackActionDecider.shouldAttack(Direction.Down, FramePoint(128, 107), listOf(FramePoint(128, 112))) shouldBe true
    }
}