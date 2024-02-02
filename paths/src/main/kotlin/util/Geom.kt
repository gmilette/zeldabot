package util

import bot.state.FramePoint

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

    data class Rectangle(val topLeft: FramePoint, val bottomRight: FramePoint) {
        fun intersect(other: Rectangle): Boolean {
            return Geom.intersect(this, other)
        }
    }

    fun intersect(rect1: Rectangle, rect2: Rectangle): Boolean {
        // Check if rectangles do not overlap along x-axis
        if (rect1.topLeft.x >= rect2.bottomRight.x || rect2.topLeft.x >= rect1.bottomRight.x) {
            return false
        }

        // Check if rectangles do not overlap along y-axis
        if (rect1.topLeft.y >= rect2.bottomRight.y || rect2.topLeft.y >= rect1.bottomRight.y) {
            return false
        }

        return true
    }
}