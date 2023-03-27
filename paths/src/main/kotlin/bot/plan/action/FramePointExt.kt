package bot.plan.action

import bot.state.FramePoint
import bot.state.justLeftBottom
import bot.state.justRightEnd

fun FramePoint.isInGrid(other: FramePoint) =
    other.x in this.x..this.justRightEnd.x &&
            other.y in this.y..this.justLeftBottom.y
