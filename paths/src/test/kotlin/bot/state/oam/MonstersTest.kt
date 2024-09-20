package bot.state.oam

import io.kotest.matchers.shouldBe
import org.junit.Test

class MonstersTest {
    @Test
    fun `test mummy`() {
        Monsters.damaged(0xa4 to 2) shouldBe true
    }
}
