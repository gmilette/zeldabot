package bot.plan.action

import bot.state.FramePoint
import bot.state.isInLevelMap
import bot.state.justRightEnd
import bot.state.map.MapConstants
import org.junit.Test
import io.kotest.matchers.shouldBe

class FramePointExtKtTest {
    @Test
    fun a() {
        val link = FramePoint(172, 24)
        val other = FramePoint(112, 64)
//        val enemy = FramePoint(176, 24)
        val enemy = FramePoint(128, 80)
// 10
        val a = enemy.x
        val b = enemy.justRightEnd.x
        other.isInGrid(enemy) shouldBe true
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