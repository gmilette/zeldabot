package util

import io.kotest.matchers.shouldBe
import org.junit.Test

class Map2dTest {
    @Test
    fun `xy ok`() {
        // 1, 2, 3
        // 4, 5, 6
        val mapInt = Map2d(mutableListOf(mutableListOf(1,2, 3), mutableListOf(4, 5, 6)))
        mapInt.get(0, 0) shouldBe 1
        mapInt.get(1, 0) shouldBe 2
        mapInt.get(1, 1) shouldBe 5
        mapInt.get(2, 1) shouldBe 6
    }

    @Test
    fun `xy mapXyCurrent`() {
        // 1, 2 (1,0), 3(2,0)
        // 4, 5(1,1), 6
        val mapInt = Map2d(mutableListOf(mutableListOf(1,2,3), mutableListOf(4, 5, 6)))
        mapInt.mapXyCurrent { x, y, c ->
            System.out.println("$x, $y, $c")
            c
        }
    }

    @Test
    fun `xy mapXyCurrent boolean`() {
        val mapInt = Map2d(mutableListOf(mutableListOf(false,false,false), mutableListOf(true, true, true)))
        val mapInt2 = Map2d(mutableListOf(mutableListOf(true, true, true), mutableListOf(true, false, true)))

        mapInt.mapXyCurrent { x, y, t ->
            t || mapInt2.get(x, y)
        }
        mapInt.map { x -> println(x) }
        mapInt.map { x -> x shouldBe true }
    }

    @Test
    fun `copy creates independent copy for Boolean map`() {
        val original = Map2d(mutableListOf(
            mutableListOf(true, false, true),
            mutableListOf(false, true, false)
        ))

        val copy = original.copy()

        // Verify copy has same values as original
        copy.get(0, 0) shouldBe true
        copy.get(1, 0) shouldBe false
        copy.get(2, 0) shouldBe true
        copy.get(0, 1) shouldBe false
        copy.get(1, 1) shouldBe true
        copy.get(2, 1) shouldBe false

        // Modify every element in the copy
        copy.set(bot.state.FramePoint(0, 0), false)
        copy.set(bot.state.FramePoint(1, 0), true)
        copy.set(bot.state.FramePoint(2, 0), false)
        copy.set(bot.state.FramePoint(0, 1), true)
        copy.set(bot.state.FramePoint(1, 1), false)
        copy.set(bot.state.FramePoint(2, 1), true)

        // Verify ALL original values are unchanged
        original.get(0, 0) shouldBe true
        original.get(1, 0) shouldBe false
        original.get(2, 0) shouldBe true
        original.get(0, 1) shouldBe false
        original.get(1, 1) shouldBe true
        original.get(2, 1) shouldBe false

        // Verify copy has the new values
        copy.get(0, 0) shouldBe false
        copy.get(1, 0) shouldBe true
        copy.get(2, 0) shouldBe false
        copy.get(0, 1) shouldBe true
        copy.get(1, 1) shouldBe false
        copy.get(2, 1) shouldBe true

    }

    @Test
    fun `modifying original does not affect copy`() {
        val original = Map2d(mutableListOf(
            mutableListOf(true, false),
            mutableListOf(false, true)
        ))

        val copy = original.copy()

        // Modify every element in the original (flip all values)
        original.set(bot.state.FramePoint(0, 0), false)
        original.set(bot.state.FramePoint(1, 0), true)
        original.set(bot.state.FramePoint(0, 1), true)
        original.set(bot.state.FramePoint(1, 1), false)

        // Verify copy still has the original values
        copy.get(0, 0) shouldBe true
        copy.get(1, 0) shouldBe false
        copy.get(0, 1) shouldBe false
        copy.get(1, 1) shouldBe true
    }
}