package bot.state

import bot.state.map.Direction
import bot.state.map.MapConstants
import kotlin.math.abs

data class FramePoint(val x: Int = 0, val y: Int = 0, val direction: Direction? = null) {
    constructor(x: Int = 0, y: Int = 0) : this(x, y, null)

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
    get() = y.yAdjust % 8 == 0

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

        else -> GamePad.MoveLeft
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

fun FramePoint.corners(): List<FramePoint> {
    return listOf(this.justOutside, this.justRightEndBottomOutside, justRightEndOutside, this.justLeftDownOutside)
}

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

val FramePoint.upEnd
    get() = FramePoint(x, y + 8 - 1)
val FramePoint.upEndRight
    get() = FramePoint(x + 16, y + 8 - 1)
val FramePoint.upOneGrid
    get() = FramePoint(x, y - 16)
val FramePoint.downOneGrid
    get() = FramePoint(x, y + 16)
val FramePoint.rightOneGrid
    get() = FramePoint(x + 16, y )
val FramePoint.leftOneGrid
    get() = FramePoint(x - 16, y)

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
val FramePoint.justRightEndBottom
    get() = FramePoint(x + 15, y + 15)
val FramePoint.justLeftBottom
    get() = FramePoint(x, y + 15)

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