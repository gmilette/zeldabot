package util

import bot.state.FramePoint
import bot.state.map.MovingDirection
import kotlin.math.abs


object CalculateDirection {
    fun calculateDirection(p1: FramePoint, p2: FramePoint): MovingDirection {
        val deltaX = p2.x - p1.x
        val deltaY = p2.y - p1.y

        val sameLine = sameLine(p1, p2)

        return when {
            sameLine && deltaX > 0 && deltaY > 0 -> MovingDirection.UP_RIGHT
            sameLine && deltaX < 0 && deltaY > 0 -> MovingDirection.UP_LEFT
            sameLine && deltaX > 0 && deltaY < 0 -> MovingDirection.DOWN_RIGHT
            sameLine && deltaX < 0 && deltaY < 0 -> MovingDirection.DOWN_LEFT
            deltaX == 0 && deltaY == 0 -> MovingDirection.UNKNOWN_OR_STATIONARY
            deltaX == 0 -> if (deltaY > 0) MovingDirection.UP else MovingDirection.DOWN
            deltaY == 0 -> if (deltaX > 0) MovingDirection.RIGHT else MovingDirection.LEFT
            else -> MovingDirection.UNKNOWN_OR_STATIONARY
        }
    }

    fun isOnDiagonal(point1: FramePoint, point2: FramePoint): Boolean {
        val deltaX = abs(point2.x - point1.x)
        val deltaY = abs(point2.y - point1.y)
        return deltaX == deltaY
    }

    fun sameLine(point1: FramePoint, point2: FramePoint): Boolean {
        val slope1 = (point1.y - point2.y).toDouble() / (point1.x - point2.x)
        val slope2 = (point2.y - point1.y).toDouble() / (point2.x - point1.x)

        // If the slopes are equal, the points are on the same line
        return slope1 == slope2
    }
}