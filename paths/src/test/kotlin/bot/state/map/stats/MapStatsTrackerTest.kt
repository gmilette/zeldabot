package bot.state.map.stats

import io.kotest.matchers.shouldBe
import org.junit.Test

class MapStatsTrackerTest {
    @Test
    fun testSort() {
        val attribCount = mutableMapOf<Int, Int>()
        attribCount[3] = 40
        attribCount[1] = 20
        attribCount[2] = 30

        val sorted = attribCount.entries.toList()
            .sortedBy { it.value }

        sorted[0].value shouldBe 20
        sorted.last().value shouldBe 40

//        attribCount.firstKey() shouldBe 1
//        attribCount.lastKey() shouldBe 3
//        ((attribCount[attribCount.lastKey()] ?: 0) - (attribCount[attribCount.firstKey()] ?: 0)) shouldBe
    }
}