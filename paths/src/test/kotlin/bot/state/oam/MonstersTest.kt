package bot.state.oam

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.test.assertTrue

class MonstersTest {
    @Test
    fun `test mummy`() {
//        Monsters.damaged(0xa4 to 2) shouldBe true
    }

    @Test
    fun `test moblin`() {
        Monsters.damaged(level = 0, 0xf2 to 43) shouldBe false
    }

    @Test
    // these this
    fun `test mummy in level 1`() {
        val a = Monsters.lookup(9)[circleMonster]?.affectedByBoomerang
        println("AAA $a")
    }
}
