package util

import io.kotest.matchers.shouldBe
import org.junit.Assert.*
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
}