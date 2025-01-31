package util

import bot.state.FramePoint
import bot.state.withX
import bot.state.withY
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object Geom {
//    fun doOverlap(l1: FramePoint, r1: FramePoint, l2: FramePoint, r2: FramePoint): Boolean {
//        // if rectangle has area 0, no overlap
//        if (l1.x == r1.x || l1.y == r1.y || r2.x == l2.x || l2.y == r2.y) return false
//
//        // If one rectangle is on left side of other
//        if (l1.x > r2.x || l2.x > r1.x) {
//            return false
//        }
//
//        // If one rectangle is above other
//        return !(r1.y > l2.y || r2.y > l1.y)
//    }

    data class Rect(val x: Int, val y: Int, val width: Int, val height: Int)

    fun valueInRange(value: Int, min: Int, max: Int): Boolean {
        return value in min..max
    }

    fun rectOverlap(A: Geom.Rect, B: Geom.Rect): Boolean {
        val xOverlap = valueInRange(A.x, B.x, B.x + B.width) ||
                valueInRange(B.x, A.x, A.x + A.width)
        val yOverlap = valueInRange(A.y, B.y, B.y + B.height) ||
                valueInRange(B.y, A.y, A.y + A.height)
        return xOverlap && yOverlap
    }

    data class Rectangle(val topLeft: FramePoint = FramePoint(), val bottomRight: FramePoint = FramePoint()) {
        fun intersect(other: Rectangle): Boolean {
            return intersect(this, other)
        }

        val height = abs(bottomRight.y - topLeft.y)
        val width = abs(bottomRight.x - topLeft.x)
        val topRight = topLeft.withX(topLeft.x + width)
        val bottomLeft = topLeft.withY(topLeft.y + height)

        fun distTo(rect2: Rectangle): Double {
            val dx = Math.max(0, abs(this.topLeft.x - rect2.bottomRight.x) - this.width / 2 - rect2.width / 2).toDouble()
            val dy = Math.max(0, abs(this.topLeft.y - rect2.bottomRight.y) - this.height / 2 - rect2.height / 2).toDouble()
            return abs(sqrt(dx * dx + dy * dy))
        }

        fun pointInside(other: FramePoint): Boolean {
            return valueInRange(other.x, topLeft.x, topLeft.x + width) && valueInRange(other.y, topLeft.y, topLeft.y + width)
        }

        fun dist2(rectangle2: Rectangle): Double {
            val dx = max(0, max(this.topLeft.x - rectangle2.bottomRight.x, rectangle2.topLeft.x - this.bottomRight.x)).toDouble()
            val dy = max(0, max(this.topLeft.y - rectangle2.bottomRight.y, rectangle2.topLeft.y - this.bottomRight.y)).toDouble()

            return if (dx == 0.0 || dy == 0.0) {
                // Rectangles overlap
                0.0
            } else {
                // Distance between rectangles
                sqrt(dx * dx + dy * dy)
            }
        }

//        fun closestPointOnSegment(p: FramePoint, a: FramePoint, b: FramePoint): FramePoint {
//            val ap = p - a
//            val ab = b - a
//            val distance = ap.dot(ab) / ab.dot(ab)
//            val closest = a + ab * distance
//            return if (distance < 0) a else if (distance > 1) b else closest
//        }
//
//        fun distanceTo(rect2: Rectangle): Double {
//            val distances = mutableListOf<Double>()
//
//            // Consider distances from each corner of rect1 to all edges of rect2
//            for (corner in listOf(this.topLeft, this.bottomRight, this.topRight, this.bottomLeft)) {
//                val left = closestPointOnSegment(corner, rect2.topLeft, rect2.bottomLeft)
//                val right = closestPointOnSegment(corner, rect2.topRight, rect2.bottomRight)
//                val top = closestPointOnSegment(corner, rect2.topLeft, rect2.topRight)
//                val bottom = closestPointOnSegment(corner, rect2.bottomLeft, rect2.bottomRight)
//                distances.addAll(listOf(corner.distanceTo(left), corner.distanceTo(right), corner.distanceTo(top), corner.distanceTo(bottom)))
//            }
//
//            return distances.minOrNull() ?: 0.0 // Return 0 if no distances are found (empty list)
//        }

        fun intersectsOtherRectangles() {}
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