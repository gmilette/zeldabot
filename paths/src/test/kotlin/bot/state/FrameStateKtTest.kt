package bot.state

import io.kotest.matchers.shouldBe
import org.junit.Test


class FrameStateKtTest{
    @Test
    fun `test in map`() {
        val pt = FramePoint(104, 64)
        pt.isInLevelMap shouldBe true
    }

    @Test
    fun `test in map all`() {
        FramePoint(31, 64).isInLevelMap shouldBe false
        FramePoint(32, 64).isInLevelMap shouldBe true
        FramePoint(16, 64).isInLevelMap shouldBe false
        FramePoint(0, 64).isInLevelMap shouldBe false
        FramePoint(34, 64).isInLevelMap shouldBe true

        FramePoint(64, 31).isInLevelMap shouldBe false
        FramePoint(64, 32).isInLevelMap shouldBe true
        FramePoint(64, 16).isInLevelMap shouldBe false
        FramePoint(64, 0).isInLevelMap shouldBe false
        FramePoint(64, 34).isInLevelMap shouldBe true
    }

}