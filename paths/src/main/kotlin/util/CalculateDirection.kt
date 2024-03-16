package util

import bot.state.FramePoint
import kotlin.math.abs

object CalculateDirection {
    enum class MovingDirection {
        UP_RIGHT,
        UP_LEFT,
        DOWN_RIGHT,
        DOWN_LEFT,
        LEFT,
        RIGHT,
        UP,
        DOWN,
        STATIONARY,
        UNKNOWN
    }

    fun calculateDirection(p1: FramePoint, p2: FramePoint): MovingDirection {
        val deltaX = p2.x - p1.x
        val deltaY = p2.y - p1.y

        val onDiagonal = isOnDiagonal(p1, p2)

        return when {
            onDiagonal && deltaX > 0 && deltaY > 0 -> MovingDirection.UP_RIGHT
            onDiagonal && deltaX < 0 && deltaY > 0 -> MovingDirection.UP_LEFT
            onDiagonal && deltaX > 0 && deltaY < 0 -> MovingDirection.DOWN_RIGHT
            onDiagonal && deltaX < 0 && deltaY < 0 -> MovingDirection.DOWN_LEFT
            deltaX == 0 && deltaY == 0 -> MovingDirection.STATIONARY
            deltaX == 0 -> if (deltaY > 0) MovingDirection.UP else MovingDirection.DOWN
            deltaY == 0 -> if (deltaX > 0) MovingDirection.RIGHT else MovingDirection.LEFT
            else -> MovingDirection.UNKNOWN
        }
    }

    fun isOnDiagonal(point1: FramePoint, point2: FramePoint): Boolean {
        val deltaX = abs(point2.x - point1.x)
        val deltaY = abs(point2.y - point1.y)
        return deltaX == deltaY
    }
}