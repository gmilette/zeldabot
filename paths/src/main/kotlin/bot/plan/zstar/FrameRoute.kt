package bot.plan.zstar

import bot.plan.action.PrevBuffer
import bot.state.*
import bot.state.map.*
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

    fun cornering(linkPt: FramePoint, linkDir: Direction?): FrameRoute {
//        d { "VERTEX: cornering $linkPt $linkDir" }
        if (pathStack.size < 4) return this
        val linkDir = linkDir ?: return this

        // if there is more than one direction
        // pick the second direction
        val dirs = PrevBuffer<Direction>(size = 4)
        dirs.add(linkDir)
        pathStack.take(4).zipWithNext().forEach { (first, second) ->
            val dir = first.dirTo(second)
            dirs.add(dir)
        }
//        pathStack.take(4).forEachIndexed { index, pt ->
//            if (index == 0) {
//                dirs.add(linkDir)
//            } else {
//                dirs.add(prev.dirTo(pt))
//            }
//            prev = pt
//        }

        if (dirs.allSame()) {
            d { "all same no change " }
        } else {
            dirs.buffer.firstOrNull { linkDir.perpendicularTo(it) }?.let { dir ->
                val modifier = linkDir.pointModifier(1)
                var secondPoint = modifier(linkPt)
                d { "corner go: $dir to $secondPoint" }
                val ptList = mutableListOf<FramePoint>()
                repeat(3) {
                    ptList.add(secondPoint)
                    secondPoint = modifier(secondPoint)
                }
                pathStack.addAll(1, ptList)
            }
        }
        return this
    }

    fun decideDirection(linkPt: FramePoint, linkDir: Direction?): Direction? {
        d { "VERTEX: cornering $linkPt $linkDir" }
        if (pathStack.size < 4) return null
        val linkDir = linkDir ?: return null

        // if there is more than one direction
        // pick the second direction
        val dirs = PrevBuffer<Direction>(size = 4)
        dirs.add(linkDir)
        pathStack.take(4).zipWithNext().forEach { (first, second) ->
            println("Pair: $first, $second")
            dirs.add(first.dirTo(second))
        }

        return if (dirs.allSame()) {
            d { "all same no change " }
            null
        } else {
            dirs.buffer.firstOrNull { linkDir.perpendicularTo(it) }.also {
                d { "moving corner in direction $it" }
            }
        }
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