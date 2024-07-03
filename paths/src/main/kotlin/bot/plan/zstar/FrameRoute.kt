package bot.plan.zstar

import bot.state.FramePoint
import bot.state.isTopRightCorner
import bot.state.map.Direction
import bot.state.map.opposite
import bot.state.map.pointModifier
import bot.state.onHighway
import bot.state.onHighwayVertex
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
            d { " : $framePoint ${framePoint.isTopRightCorner}"}
        }
    }

    fun next15() {
        if (pathStack.isEmpty() || pathStack.size < 3) {
            return
        }
        for (framePoint in pathStack.subList(0, min(15, pathStack.size-1))) {
            d { " : $framePoint ${framePoint.isTopRightCorner}"}
        }
        d { " :: ${pathStack.last()}"}
    }

    fun duplicateCornerFirst(linkPt: String, linkDir: Direction?): FrameRoute {
        d { "VERTEX: duplicateCornerFirst $linkPt $linkDir" }
        if (pathStack.size <= 1) return this
        val linkDir = linkDir ?: return this
        if (true) return this

        // too much replanning, if there is any corner within next 4 moves just go in that direction
        // 4 times until there is no corner


        val firstPoint = pathStack.first()
        val secondPoint = pathStack[1]
        val nextPointGoingSameWay = linkDir.pointModifier(1)(firstPoint)
        val goingSameWay = secondPoint == nextPointGoingSameWay
        d { "VERTEX: ZZZ firstPoint: $firstPoint, secondPoint: $secondPoint, Next: $nextPointGoingSameWay going same: $goingSameWay" }
        if (firstPoint.onHighwayVertex) {
            d { "VERTEX: firstPoint: $firstPoint going same: $goingSameWay" }
        }
        // TURNING
        if (firstPoint.onHighwayVertex && !goingSameWay) {
            d { "VERTEX: turning firstPoint: $firstPoint, secondPoint: $secondPoint, Next: $nextPointGoingSameWay" }
            // repeat the first point 4 times, so link can complete the turn
            // it's going to take 4 moves, add 3, the first point will still be there
            repeat(3) {
                pathStack.add(1, secondPoint)
            }
        }
        return this
    }

    fun cornerSoon(linkPt: String, linkDir: Direction?): FrameRoute {
        d { "VERTEX: duplicateCornerFirst $linkPt $linkDir" }
        if (pathStack.size <= 4) return this
        val linkDir = linkDir ?: return this

        // too much replanning, if there is any corner within next 4 moves just go in that direction
        // 4 times until there is no corner


        val firstPoint = pathStack.first()
        val secondPoint = pathStack[1]
        val thirdPoint = pathStack[2]
        val fourthPoint = pathStack[3]

        // first to second
        if (firstPoint.onHighwayVertex) {
            val nextPointGoingSameWay = linkDir.pointModifier(1)(firstPoint)
            val nextPointGoingSameWayOpp = linkDir.opposite().pointModifier(1)(firstPoint)
            val goingSameWay = secondPoint == nextPointGoingSameWay || secondPoint == nextPointGoingSameWayOpp
            if (!goingSameWay) {
                d { "CORNER at $firstPoint then $secondPoint"}
                // go in that
                // add the next point
                // it does a NO alive enemies, no need to replan just go plan count max
                pathStack.add(2, secondPoint)
            }
        }

        if (secondPoint.onHighwayVertex) {
            val nextPointGoingSameWay = linkDir.pointModifier(1)(secondPoint)
            val nextPointGoingSameWayOpp = linkDir.opposite().pointModifier(1)(secondPoint)
            val goingSameWay = thirdPoint == nextPointGoingSameWay || thirdPoint == nextPointGoingSameWayOpp
            if (!goingSameWay) {
                d { "CORNER at $secondPoint then $thirdPoint"}
            }
        }

        if (thirdPoint.onHighwayVertex) {
            val nextPointGoingSameWay = linkDir.pointModifier(1)(thirdPoint)
            val nextPointGoingSameWayOpp = linkDir.opposite().pointModifier(1)(thirdPoint)
            val goingSameWay = fourthPoint == nextPointGoingSameWay || fourthPoint == nextPointGoingSameWayOpp
            if (!goingSameWay) {
                d { "CORNER at $thirdPoint then $fourthPoint"}
            }
        }
        return this
    }

    fun duplicateCorner() {
        val first10 = pathStack.subList(0, min(10, pathStack.size-1))
        var prev: FramePoint? = null
        for (i in first10.indices) {
            val previous = if (i > 0) first10[i - 1] else null
            val current = first10[i]
            val next = if (i < first10.size - 1) first10[i + 1] else null

            if (previous != null && next != null && current.onHighwayVertex) {
                d { "VERTEX: Previous: $previous, Current: $current, Next: $next" }
            }
        }
    }

    fun adjustCorner() {
        val first5 = pathStack.subList(0, min(5, pathStack.size-1))
        if (first5.size < 2) return
        val corner = first5[1]
        if (!corner.isTopRightCorner) return
        d { "is CORNER $corner" }

//        first5.set(1)
//        : (208, 82) false
//        Debug: (Kermit)  : (208, 81) true
//        Debug: (Kermit)  : (208, 80) false
//        Debug: (Kermit)  : (209, 80) false
//        Debug: (Kermit)  : (210, 80) false
//        Debug: (Kermit) CORNER (208, 81)
//        Debug: (Kermit)  next is (208, 81)
    }
}