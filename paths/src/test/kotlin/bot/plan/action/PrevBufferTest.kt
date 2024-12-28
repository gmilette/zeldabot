import bot.plan.action.PrevBuffer
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class PrevBufferTest {

    @Test
    fun `test all same`() {
        val buffer = PrevBuffer<Int>(3)
        buffer.add(3)
        buffer.add(3)
        buffer.add(3)
        buffer.allSame() shouldBe true
        buffer.add(1)
        buffer.add(2)
        buffer.allSame() shouldBe false
    }

    @Test
    fun `buffer should add and maintain size`() {
        val buffer = PrevBuffer<Int>(3)
        buffer.isFull.shouldBeFalse()

        buffer.add(1)
        buffer.add(2)
        buffer.isFull.shouldBeFalse()

        buffer.add(3)
        buffer.isFull.shouldBeTrue()

        buffer.add(4)
        buffer.isFull.shouldBeTrue()
        buffer.buffer.size shouldBe 3
        buffer.buffer.toList() shouldBe listOf(2, 3, 4)
    }

    @Test
    fun `clear should empty the buffer`() {
        val buffer = PrevBuffer<String>(2)
        buffer.add("a")
        buffer.add("b")
        buffer.isFull.shouldBeTrue()

        buffer.clear()
        buffer.isFull.shouldBeFalse()
        buffer.buffer.shouldBeEmpty()
    }
}