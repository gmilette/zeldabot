package bot.plan.action

import bot.state.FramePoint
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Test

class MoveBufferTest {
    val pt1 = FramePoint(1,1)
    val pt2 = FramePoint(2,2)
    val pt3 = FramePoint(3,3)
    val pt4 = FramePoint(4,4)
    @Test
    fun `test buff`() {
        val b1 = MoveBuffer(2)
        val b2 = MoveBuffer(2)

        b1.add(pt1)
        b1.add(pt2)

        b1.buffer shouldContain pt1
        b1.buffer shouldContain pt2

        b1.add(pt3)
        b1.buffer shouldContain pt2
        b1.buffer shouldContain pt3
        b1.buffer[0] shouldBe pt2
        b1.buffer[1] shouldBe pt3

        b2.add(pt2)
        b2.compare(b1) shouldBe false
        b1.compare(b2) shouldBe false
        b2.add(pt3)
        b2.compare(b1) shouldBe true
        b1.compare(b2) shouldBe true
    }
}