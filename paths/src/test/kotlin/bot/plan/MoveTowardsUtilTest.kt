package bot.plan

import bot.GamePad
import bot.plan.action.MoveTowardsUtil
import bot.plan.action.PreviousMove
import bot.state.FramePoint
import org.junit.Assert.*
import org.junit.Test

class MoveTowardsUtilTest {

    @Test
    fun `move p2`() {
        val move = MoveTowardsUtil()
        val pad = move.moveTowards(link = FramePoint(10, 10),
            target = FramePoint(4, 14),
            previousMove = PreviousMove(
                null,
                FramePoint(9, 10),
                FramePoint(10, 10),
                FramePoint(10, 10),
                GamePad.MoveUp,
                true
            )
        )
    }

}