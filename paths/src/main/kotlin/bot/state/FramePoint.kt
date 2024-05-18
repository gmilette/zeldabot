package bot.state

import bot.plan.action.NavUtil
import bot.state.map.Direction
import bot.state.map.MapConstants
import util.Geom
import kotlin.math.abs
import kotlin.math.sqrt

data class FramePoint(val x: Int = 0, val y: Int = 0, val direction: Direction? = null) {
    constructor(x: Int = 0, y: Int = 0) : this(x, y, null)

    val isZero: Boolean
        get() = x == 0 && y == 0

    override fun equals(other: Any?): Boolean {
        return if (other is FramePoint) {
            other.x == x && other.y == y
        } else false
    }

    val oneStr: String
        get() = "${x}_$y"

    override fun toString(): String {
        return "($x, $y)"
    }
}

data class FloatPoint(val x: Float, val y: Float) {
    fun nearest(): FramePoint =
        FramePoint(x.toInt(), y.toInt())
}

object FramePointBuilder {
    fun has(xys: Map<Int, Int>): List<FramePoint> =
        xys.map { FramePoint(it.key, it.value) }

    fun hasL(xys: List<Pair<Int, Int>>): List<FramePoint> =
        xys.map { FramePoint(it.first, it.second) }
}

fun FramePoint.directionToDir(to: FramePoint): Direction {
    return when {
        this.x == to.x -> {
            if (this.y < to.y) Direction.Down else Direction.Up
        }

        this.y == to.y -> {
            if (this.x < to.x) Direction.Right else Direction.Left
        }

        else -> Direction.Left
    }
}

fun FramePoint.toRect(): Geom.Rectangle =
    Geom.Rectangle(this, this.justRightEndBottom)

fun FramePoint.toRectPlus(adjustment: Int): Geom.Rectangle =
    Geom.Rectangle(this.adjustBy((-1 * adjustment)), this.justRightEndBottom.adjustBy(adjustment))

fun FramePoint.adjustBy(adjustment: Int): FramePoint =
    FramePoint(x + adjustment, y + adjustment)

// maybe have to actually do the top right, not top left
val FramePoint.isTopRightCorner: Boolean
    get() =
        onHighwayX && onHighwayYJust

// ?? not sure
val FramePoint.isCorner: Boolean
    get() =
        onHighwayX && onHighwayY
val FramePoint.onHighwayYJust
    get() = this.down.onHighwayY

// one higher than the highway
val FramePoint.onHighwayYAlmost
    get() = y.notOnEdgeY && (y + 1) % 8 == 0

val FramePoint.onHighwayYAlmostBeyond
    get() = y.notOnEdgeY && (y - 1) % 8 == 0

val FramePoint.onHighwayXAlmost
    get() = x.notOnEdge && (x + 1) % 8 == 0

val FramePoint.onHighwayXAlmostBeyond
    get() = x.notOnEdge && (x - 1) % 8 == 0

val Int.notOnEdgeY: Boolean
    get() = this in (MapConstants.oneGrid + 4) ..(MapConstants.MAX_Y - MapConstants.oneGrid)

val Int.notOnEdge: Boolean
    get() = this in (MapConstants.oneGrid + 4)..(MapConstants.MAX_X - MapConstants.oneGrid)
val FramePoint.onHighway: Boolean
    get() = onHighwayX || onHighwayY

val FramePoint.onHighwayX: Boolean
    get() = x % 8 == 0

// subtracting by 61 to get the original coordinates so that this modular division works
val FramePoint.onHighwayY: Boolean
    get() = y % 8 == 0
//    get() = y.yAdjust % 8 == 0

private val Int.yAdjust: Int
    get() = this + MapConstants.yAdjust

val FramePoint.toG: FramePoint
    get() = FramePoint(x / 16, y / 16)

fun FramePoint.addDirection(dir: Direction) =
    this.copy(direction = dir)

fun FramePoint.directionTo(to: FramePoint): GamePad {
    return when {
        x == to.x -> {
            if (y < to.y) GamePad.MoveDown else GamePad.MoveUp
        }

        y == to.y -> {
            if (x < to.x) GamePad.MoveRight else GamePad.MoveLeft
        }

        else -> NavUtil.directionToDist(this, to)
    }
}

fun FramePoint.dirTo(to: FramePoint): Direction {
    return when {
        x == to.x -> {
            if (y < to.y) Direction.Down else Direction.Up
        }

        y == to.y -> {
            if (x < to.x) Direction.Right else Direction.Left
        }

        else -> Direction.Down
    }
}

fun FramePoint.adjustBy(pad: GamePad) =
    when (pad) {
        GamePad.MoveUp -> this.up
        GamePad.MoveDown -> this.down
        GamePad.MoveRight -> this.right
        GamePad.MoveLeft -> this.left
        else -> this
    }

val FramePoint.toScreenY
    get() = FramePoint(x, y + 61)

// three grid ensures that it is routing to where link can go i think
val FramePoint.isInLevelMap
    get() = y in MapConstants.twoGrid.. (MapConstants.MAX_Y - MapConstants.threeGrid) &&
            x in MapConstants.twoGrid..(MapConstants.MAX_X - MapConstants.threeGrid)

val FramePoint.isOnMap
    get() = y in 1.. MapConstants.MAX_Y &&
            x in 1..MapConstants.MAX_X

fun FramePoint.cornersIn(): List<FramePoint> {
    return listOf(this, this.justRightEnd, justLeftDown, this.justRightEndBottom)
}

fun FramePoint.corners(): List<FramePoint> {
    return listOf(this.justOutside, this.justRightEndBottomOutside, justRightEndOutside, this.justLeftDownOutside)
}

val FramePoint.corners: List<FramePoint>
    get() = cornersIn()

fun FramePoint.cornersInLevel(): List<FramePoint> {
    return corners().filter { it.isInLevelMap }
}

val FramePoint.isTop
    get() = y == 0
val FramePoint.isBottom
    get() = y == MapConstants.MAX_Y
val FramePoint.isRight
    get() = x == MapConstants.MAX_X
val FramePoint.isLeft
    get() = x == 0

val FramePoint.justMid
    get() = FramePoint(x, y + 8)
val FramePoint.justMidEnd
    get() = FramePoint(x + 15, y + 8)

fun FramePoint.relativeTo(slope: FramePoint): FramePoint {
    return this.plus(slope.x, slope.y)
//    // normalize it first somehow
//    val unitVector = slope.unitVector()
//    val deltaX = MapConstants.oneGridF * unitVector.x
//    val deltaY = MapConstants.oneGridF * unitVector.y
//    return this.plus(deltaX.toInt(), deltaY.toInt())
}

fun FramePoint.pointAtDistance(slope: FramePoint, distance: Double): FramePoint {
    val slope = slope.x.toFloat() / slope.y.toFloat()
    val dx = distance / sqrt(1 + slope * slope)
    val dy = slope * dx
    return FramePoint(x + dx.toInt(), y + dy.toInt())
}

fun FramePoint.unitVector(): FloatPoint {
    val magnitude = sqrt(x.toFloat() * x.toFloat() + y.toFloat() * y.toFloat())
    return if (magnitude != 0.0f) {
        FloatPoint(x / magnitude, y / magnitude)
    } else {
        FloatPoint(0.0f, 0.0f)
    }
}

fun FramePoint.plus(xAdd: Int, yAdd: Int) =
    FramePoint(x + xAdd, y = y + yAdd)

val FramePoint.upEnd
    get() = FramePoint(x, y + 8 - 1)
val FramePoint.upEndRight
    get() = FramePoint(x + 16, y + 8 - 1)
val FramePoint.upOneGrid
    get() = FramePoint(x, y - 16)
val FramePoint.downOneGrid
    get() = FramePoint(x, y + 16)
val FramePoint.downHalf
    get() = FramePoint(x, y + 8)
val FramePoint.rightOneGrid
    get() = FramePoint(x + 16, y )
val FramePoint.leftOneGrid
    get() = FramePoint(x - 16, y)

val FramePoint.upHalfLeftOneGrid
    get() = FramePoint(x - 16, y - 8)
val FramePoint.upLeftHalfOneGrid
    get() = FramePoint(x - 8, y - 8)
val FramePoint.upHalfGrid
    get() = FramePoint(x, y - 8)
val FramePoint.leftHalfGrid
    get() = FramePoint(x - 8, y)

val FramePoint.upTwoGrid
    get() = FramePoint(x, y - 32)
val FramePoint.downTwoGrid
    get() = FramePoint(x, y + 32)
val FramePoint.rightTwoGrid
    get() = FramePoint(x + 32, y )
val FramePoint.leftTwoGrid
    get() = FramePoint(x - 32, y)

val FramePoint.upLeftOneGrid
    get() = FramePoint(x - 16, y - 16)
val FramePoint.upLeftOneGridALittleLess
    get() = FramePoint(x - 15, y - 16) // get within sword range, hopefully don't get hit
val FramePoint.up
    get() = FramePoint(x, y - 1)
val FramePoint.up2
    get() = FramePoint(x, y - 2)
val FramePoint.down
    get() = FramePoint(x, y + 1)
val FramePoint.down7
    get() = FramePoint(x, y + 7)
val FramePoint.down2
    get() = FramePoint(x, y + 2)
val FramePoint.downEnd
    get() = FramePoint(x, y + 16 + 1)
val FramePoint.downEndRight
    get() = FramePoint(x + 16, y + 16 + 1) // extra 1 just like downEnd
val FramePoint.right
    get() = FramePoint(x + 1, y)
val FramePoint.right2
    get() = FramePoint(x + 2, y)

val FramePoint.rightEnd
    get() = FramePoint(x + 16 + 1, y)
val FramePoint.rightEndDown
    get() = FramePoint(x + 16 + 1, y + 15) // why is this 15????
val FramePoint.rightEndDownHalf
    get() = FramePoint(x + 8, y + 8)

/**
 * loot can appear inside a grid, but link can pick it up from half a grid away, so allow those targets
 */
val FramePoint.lootTargets: List<FramePoint>
    get() = listOf(this, this.leftHalf, this.upHalf, this.leftUpHalf, this.nearestGrid) //, this.nearestBigGrid) // round to nearest grid? // big grid too far

val FramePoint.nearestGrid: FramePoint
    get() {
        val mod = this.x % 8
        val mody = this.y.yAdjust % 8
        return FramePoint(this.x - mod, this.y - mody)
    }

val FramePoint.nearestBigGrid: FramePoint
    get() {
        val mod = this.x % 16
        val mody = this.y.yAdjust % 16
        return FramePoint(this.x - mod, this.y - mody)
    }

val FramePoint.leftHalf
    get() = FramePoint(x - MapConstants.halfGrid, y)
val FramePoint.upHalf
    get() = FramePoint(x, y - MapConstants.halfGrid)
val FramePoint.leftUpHalf
    get() = FramePoint(x - MapConstants.halfGrid, y - MapConstants.halfGrid)


val FramePoint.left
    get() = FramePoint(x - 1, y)
val FramePoint.left2
    get() = FramePoint(x - 2, y)
val FramePoint.leftDown
    get() = FramePoint(x - 1, y + 15)

val FramePoint.justLeftDown
    get() = FramePoint(x, y + 15)
val FramePoint.justRightEnd
    get() = FramePoint(x + 15, y)
val FramePoint.justRightHalf
    get() = FramePoint(x + 8, y)
val FramePoint.justRightEndBottom
    get() = FramePoint(x + 15, y + 15)
val FramePoint.justLeftBottom
    get() = FramePoint(x, y + 15)
val FramePoint.justLeftBottomHalf
    get() = FramePoint(x, y + 8)
val FramePoint.justLeftHalf
    get() = FramePoint(x, y + 8)

val FramePoint.justDownFourth
    get() = FramePoint(x, y + 4)
val FramePoint.justDownThreeFourth
    get() = FramePoint(x, y + 12)
val FramePoint.justRightFourth
    get() = FramePoint(x + 4, y)
val FramePoint.justRightThreeFourth
    get() = FramePoint(x + 12, y)

val sizeOfSword = 4
val FramePoint.justDown6
    get() = FramePoint(x, y + 6)
val FramePoint.justDownLast6
    get() = FramePoint(x, y + 10)
val FramePoint.justRight6
    get() = FramePoint(x + 6, y)
val FramePoint.justRightLast6
    get() = FramePoint(x + 10, y)


fun FramePoint.withX(changeX: Int): FramePoint = FramePoint(changeX, y)
fun FramePoint.withY(changeY: Int): FramePoint = FramePoint(x, changeY)

val FramePoint.justLeftDownOutside
    get() = FramePoint(x + 2, y + 18)
val FramePoint.justRightEndOutside
    get() = FramePoint(x + 18, y + 2)
val FramePoint.justRightEndBottomOutside
    get() = FramePoint(x + 18, y + 18)
val FramePoint.justOutside
    get() = FramePoint(x - 2, y - 2)

fun FramePoint.distTo(other: FramePoint) =
    abs(x - other.x) + abs(y - other.y)

fun FramePoint.minDistToAny(other: List<FramePoint>) =
    other.minOf { this.distTo(it) }

val Undefined = FramePoint(0, 0)