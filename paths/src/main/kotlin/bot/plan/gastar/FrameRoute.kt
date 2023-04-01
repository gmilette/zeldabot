package bot.plan.gastar

import bot.state.FramePoint
import util.d
import kotlin.math.min

class FrameRoute(val path: List<FramePoint>) {
    private val pathStack = path.toMutableList()

    private var lastPopped: FramePoint? = null

    private val defaultLookAhead = 5

    val numPoints: Int
        get() = pathStack.size

    fun isOn(link: FramePoint, lookAhead: Int = defaultLookAhead): Int? {
        val max = min(lookAhead, numPoints) - 1
        for (i in 0..max) {
            if (pathStack[i] == link) {
                return i
            }
        }

        return null
    }

    fun popUntil(target: FramePoint): FramePoint {
        var point = pop()
        while (point != target) {
            point = pop()
        }
        if (point != target) {
            throw IllegalArgumentException("$point not in the route")
        }
        return point
    }

    fun pop(): FramePoint? = pathStack.removeFirstOrNull().also {
        lastPopped = it
    }

    fun popOrEmpty(): FramePoint = pop() ?: FramePoint()

    fun next5() {
        if (pathStack.isEmpty() || pathStack.size < 3) {
            return
        }
        for (framePoint in pathStack.subList(0, min(5, pathStack.size-1))) {
            d { " : $framePoint"}
        }
    }
}