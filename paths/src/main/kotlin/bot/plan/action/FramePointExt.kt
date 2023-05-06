package bot.plan.action

import bot.state.FramePoint
import bot.state.justLeftBottom
import bot.state.justRightEnd

fun FramePoint.isInGrid(other: FramePoint) =
    other.x in this.x..this.justRightEnd.x &&
            other.y in this.y..this.justLeftBottom.y

fun FramePoint.toLineOf(length: Int = 8): List<FramePoint> {
    val pts = mutableListOf<FramePoint>()
    for (i in 0..length) {
        pts.add(FramePoint(this.x + i, this.y))
    }

    return pts
}

fun FramePoint.about(horizontal: Int = 4, vertical: Int = 2): List<FramePoint> {
    val targets = mutableListOf<FramePoint>()
    repeat(vertical) {
        val line = this.copy(y = this.y + it).toLineOf(horizontal)
        targets.addAll(line)
    }
    return targets
}
