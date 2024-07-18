package bot.state.map

import io.kotest.matchers.shouldBe
import org.junit.Test


class DirectionKtTest {
    @Test
    fun `testo`() {
        Direction.Right.perpendicularTo(Direction.Up) shouldBe true
        Direction.Right.perpendicularTo(Direction.Down) shouldBe true
        Direction.Down.perpendicularTo(Direction.Right) shouldBe true
        Direction.Down.perpendicularTo(Direction.Down) shouldBe false
    }

    @Test
    fun `test none never perp`() {
        Direction.None.perpendicularTo(Direction.Up) shouldBe false
        Direction.None.perpendicularTo(Direction.Left) shouldBe false
        Direction.None.perpendicularTo(Direction.None) shouldBe false
        Direction.Left.perpendicularTo(Direction.None) shouldBe false
        Direction.Up.perpendicularTo(Direction.None) shouldBe false
    }

}