package bot.plan

import bot.state.FramePoint
import org.junit.Assert.*
import org.junit.Test

class MoveTowardsUtilTest {
    @Test
    fun `move p`() {
        val move = MoveTowardsUtil()
        val pad = move.moveTowards(link = FramePoint(10, 10),
            target = FramePoint(15, 15),
            previousMove = PreviousMove(
                FramePoint(9, 10),
                FramePoint(10, 10),
                FramePoint(10, 10),
                true
            )
        )
    }

    @Test
    fun `move p2`() {
        val move = MoveTowardsUtil()
        val pad = move.moveTowards(link = FramePoint(10, 10),
            target = FramePoint(4, 14),
            previousMove = PreviousMove(
                FramePoint(9, 10),
                FramePoint(10, 10),
                FramePoint(10, 10),
                true
            )
        )
    }

}