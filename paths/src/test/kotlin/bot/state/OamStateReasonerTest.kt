package bot.state

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.kotlin.mock


class OamStateReasonerTest {
    @Test
    fun `test some`() {
        val reasoner = OamStateReasoner(mock())
        val sprites = listOf(
            SpriteData(176, FramePoint(128, 55), 176, 3),
            SpriteData(178, FramePoint(136, 55), 178, 3)
        )
        reasoner.combine(sprites).apply {
            size shouldBe 1
            get(0).point.x shouldBe 128
        }
    }

}