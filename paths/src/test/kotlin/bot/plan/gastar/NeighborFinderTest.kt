package bot.plan.gastar

import bot.state.Direction
import bot.state.FramePoint
import bot.state.Hyrule
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import org.junit.Assert.*
import org.junit.Test

class NeighborFinderTest {
    val hyrule = Hyrule()

    val cell = hyrule.getMapCell(120)

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