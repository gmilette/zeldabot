package bot.state.oam

import io.kotest.matchers.shouldBe
import org.junit.Test

class EnemyGroupTest {
    @Test
    fun `test sword`() {
        EnemyGroup.projectilePairs.contains(0x82 to 0x02) shouldBe true
        EnemyGroup.ignorePairs.contains(0x82 to 0x02) shouldBe false
    }
}