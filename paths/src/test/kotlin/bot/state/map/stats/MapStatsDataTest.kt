package bot.state.map.stats

import bot.state.MapCoordinates
import io.kotest.matchers.shouldBe
import org.junit.Test

class MapStatsDataTest {
    @Test
    fun testIt() {
        val co = MapCoordinates(2, 2)
        val data1 = MapStatsData(co)
        val aCount = AttributeCount()
        aCount.count(10)
        aCount.count(10)
        aCount.count(10)
        data1.tileAttributeCount[1] = aCount

        val data2 = MapStatsData(co)
        val ount = AttributeCount()
        ount.count(10)
        ount.count(10)
        ount.count(10)
        data2.tileAttributeCount[1] = ount

        data1.add(data2).apply {
            (data1.tileAttributeCount[1]?.attribCount?.get(10) ?: 0) shouldBe 6
        }
    }

    @Test
    fun testMissing() {
        val co = MapCoordinates(2, 2)
        val data1 = MapStatsData(co)
        val aCount = AttributeCount()
        aCount.count(10)
        aCount.count(10)
        aCount.count(2)
        data1.tileAttributeCount[1] = aCount

        val data2 = MapStatsData(co)
        val ount = AttributeCount()
        ount.count(10)
        ount.count(1)
        ount.count(1)
        data2.tileAttributeCount[1] = ount

        data1.add(data2).apply {
            (data1.tileAttributeCount[1]?.attribCount?.get(10) ?: 0) shouldBe 3
            (data1.tileAttributeCount[1]?.attribCount?.get(1) ?: 0) shouldBe 2
            (data1.tileAttributeCount[1]?.attribCount?.get(2) ?: 0) shouldBe 1
        }
    }

    @Test
    fun testHEx() {
        val co = MapCoordinates(2, 2)
        val data1 = MapStatsData(co)
        val aCount = AttributeCount()
        aCount.count(10)
        aCount.count(10)
        aCount.count(10)
        val ba = (0xba).toInt()
        data1.tileAttributeCount[ba] = aCount

        val ount = AttributeCount()
        ount.count(10)
        ount.count(10)
        ount.count(10)
        data1.tileAttributeCount[186] = ount

        print("data2 $data1")
    }


}