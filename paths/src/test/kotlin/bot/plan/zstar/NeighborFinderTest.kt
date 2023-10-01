package bot.plan.zstar

import bot.state.map.Direction
import bot.state.FramePoint
import bot.state.map.Hyrule
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test

class NeighborFinderTest {
    val hyrule = Hyrule()

    val cell = hyrule.getMapCell(120)

    @Before
    fun setup() {
        ZStar.DEBUG = true
    }

    @Test
    fun `test up hole`() {
        val cell4 = hyrule.getMapCell(44)
        val finder = NeighborFinder(cell4.passable)

//        cell4.write()

        cell4.passable.get(143,111) shouldBe true
//        finder.blockPassable(FramePoint(143, 111)) shouldBe true

        finder.neighbors(FramePoint(143, 112)).apply {
            this shouldContain FramePoint(143, 111)
        }
    }


    @Test
    fun `test right corner`() {
        val finder = NeighborFinder(cell.passable)

        finder.corner(FramePoint(46, 80), Direction.Up) shouldBe FramePoint(48, 80)

        finder.neighbors(FramePoint(46, 80)).apply {
            this shouldContain FramePoint(48, 80)
        }
    }

    @Test
    fun `test left corner`() {
        val finder = NeighborFinder(cell.passable)

        finder.corner(FramePoint(50, 80), Direction.Up) shouldBe FramePoint(48, 80)

        finder.neighbors(FramePoint(50, 80)).apply {
            this shouldContain FramePoint(48, 80)
        }
    }
}