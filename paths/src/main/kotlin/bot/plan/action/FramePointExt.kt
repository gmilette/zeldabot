package bot.plan.action

import bot.state.*

fun FramePoint.distToSquare(other: FramePoint) = squarePts().minOf { it.distTo(other) }

fun FramePoint.squarePts() = listOf(this, this.justRightEnd, this.justLeftBottom, this.justLeftDown)

fun FramePoint.isInGrid(other: FramePoint, buffer: Int = 0): Boolean {
    val xIn = other.x in (this.x - buffer)..(this.justRightEnd.x + buffer)
    val yIn = other.y in (this.y - buffer)..(this.justLeftBottom.y + buffer)
    return xIn && yIn
}

fun FramePoint.isInHalfGrid(other: FramePoint, buffer: Int = 0): Boolean {
    val xIn = other.x in (this.x - buffer)..(this.justRightHalf.x + buffer)
    val yIn = other.y in (this.y - buffer)..(this.justLeftBottomHalf.y + buffer)
    return xIn && yIn
}

fun FramePoint.isInHalfFatGrid(other: FramePoint, wide: Boolean = false): Boolean {
    val xIn = other.x in (this.x)..(if (wide) this.justRightEnd.x else this.justRightHalf.x)
    val yIn = other.y in (this.y)..(if (wide) this.justLeftBottomHalf.y else this.justLeftBottom.y )
    return xIn && yIn
}

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
