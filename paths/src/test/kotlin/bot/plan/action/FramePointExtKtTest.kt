package bot.plan.action

import bot.state.*
import bot.state.map.MapConstants
import io.kotest.matchers.shouldBe
import org.junit.Test

class FramePointExtKtTest {

    @Test
    fun nearestGrid() {
        val key = FramePoint(84, 88)
        key.nearestGrid.apply {
            x shouldBe 80
            y shouldBe 83
        }
        key.nearestBigGrid.apply {
            x shouldBe 80
            y shouldBe 83
        }
    }
    @Test
    fun a() {
        val link = FramePoint(160, 51)
        val other = FramePoint(112, 64)
//        val enemy = FramePoint(176, 24)
        val enemy = FramePoint(160, 48)
// 10
        val a = enemy.x
        val b = enemy.justRightEnd.x
//        link.isInGrid(enemy, buffer = 1) shouldBe true
        enemy.isInGrid(link) shouldBe true
    }

    @Test
    fun inLev() {
//        (112, 155), (112, 91), (80, 123), (144, 123)
        val a = MapConstants.MAX_X - MapConstants.threeGrid
        val b = MapConstants.MAX_Y - MapConstants.twoGrid

        val pt = FramePoint(112, 155)
        pt.isInLevelMap shouldBe true
//        val yIn = pt.y in MapConstants.twoGrid.. (MapConstants.MAX_Y - MapConstants.threeGrid)
//        val xIn = pt.x in MapConstants.twoGrid..(MapConstants.MAX_X - MapConstants.threeGrid)
//        val ab = 10
    }
    @Test
    fun attackFromTopReal() {
        val link = FramePoint(160, 56)
        val attackGrid = FramePoint(160, 75)
        val enemy = FramePoint(160, 89)
        attackGrid.isInGrid(enemy) shouldBe true
    }

}