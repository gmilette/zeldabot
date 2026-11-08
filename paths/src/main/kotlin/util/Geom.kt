package util

import bot.state.FramePoint
import bot.state.withX
import bot.state.withY
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object Geom {
    fun valueInRange(value: Int, min: Int, max: Int): Boolean {
        return value in min..max
    }

    data class Rectangle(val topLeft: FramePoint = FramePoint(), val bottomRight: FramePoint = FramePoint()) {
        fun intersect(other: Rectangle): Boolean {
            return intersect(this, other)
        }

        val height = abs(bottomRight.y - topLeft.y)
        val width = abs(bottomRight.x - topLeft.x)

        fun distTo(rect2: Rectangle): Double {
            val dx = Math.max(0, abs(this.topLeft.x - rect2.bottomRight.x) - this.width / 2 - rect2.width / 2).toDouble()
            val dy = Math.max(0, abs(this.topLeft.y - rect2.bottomRight.y) - this.height / 2 - rect2.height / 2).toDouble()
            return abs(sqrt(dx * dx + dy * dy))
        }

        fun pointInside(other: FramePoint): Boolean {
            return valueInRange(other.x, topLeft.x, topLeft.x + width) && valueInRange(other.y, topLeft.y, topLeft.y + width)
        }
    }

    fun intersect(rect1: Rectangle, rect2: Rectangle): Boolean {
        // use >= for exclusive, but if both are same, then it is inclusive
        // i think we want inclusive overlapping

        if (rect1.topLeft.x > rect2.bottomRight.x || rect2.topLeft.x > rect1.bottomRight.x) {
            return false
        }

        // Check if rectangles do not overlap along y-axis
        if (rect1.topLeft.y > rect2.bottomRight.y || rect2.topLeft.y > rect1.bottomRight.y) {
            return false
        }

        return true
    }
}