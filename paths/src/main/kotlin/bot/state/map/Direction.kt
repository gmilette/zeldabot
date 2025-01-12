package bot.state.map

import bot.state.*
import bot.state.GamePad.MoveDown
import bot.state.GamePad.MoveLeft
import bot.state.GamePad.MoveRight
import bot.state.GamePad.MoveUp
import util.Geom
import kotlin.random.Random

enum class Direction {
    Left, Right, Up, Down, None;

    companion object {
        val horizontal: List<Direction> = listOf(Left, Right)
        val vertical: List<Direction> = listOf(Up, Down)
        val all: List<Direction>
            get() = listOf(Up, Right, Down, Left)
        fun randomDirection(): Direction =
            when (Random.nextInt(4)) {
                0 -> Direction.Up
                1 -> Direction.Down
                2 -> Direction.Left
                3 -> Right
                else -> Direction.Down
            }
    }

    fun vertical() = this in Companion.vertical

    fun horizontal() = this in Companion.horizontal

    fun toArrow(): String =
        when (this) {
            Left -> "<--"
            Right -> "-->"
            Up -> "^"
            Down -> "_"
            else -> "x"
        }
}

fun Direction.ifHave(message: String): String = if (this != Direction.None) message else ""

//fun FramePoint.facing(rect: Geom.Rectangle): Boolean = when (this) {
//    Direction.Left -> x
//    Direction.Right -> x < rect.topLeft.x
//    Direction.Up -> Direction.Down
//    Direction.Down -> Direction.Up
//    Direction.None -> Direction.None
//}

private fun FramePoint.isLeftOf(rect: Geom.Rectangle): Boolean =
    x < rect.topLeft.x

val Direction.upOrLeft: Boolean
    get() = this == Direction.Up || this == Direction.Left

fun Direction.opposite(): Direction = when (this) {
    Direction.Left -> Direction.Right
    Direction.Right -> Direction.Left
    Direction.Up -> Direction.Down
    Direction.Down -> Direction.Up
    Direction.None -> Direction.None
}

fun Direction.toGamePad(): GamePad = when (this) {
    Direction.Left -> GamePad.MoveLeft
    Direction.Right -> GamePad.MoveRight
    Direction.Up -> GamePad.MoveUp
    Direction.Down -> GamePad.MoveDown
    Direction.None -> GamePad.None
}

fun Direction.pointModifier(adjustment: Int = 1): (FramePoint) -> FramePoint {
    return when (this) {
        Direction.Up -> { p -> FramePoint(p.x, p.y - adjustment) }
        Direction.Down -> { p -> FramePoint(p.x, p.y + adjustment) }
        Direction.Left -> { p -> FramePoint(p.x - adjustment, p.y) }
        Direction.Right -> { p -> FramePoint(p.x + adjustment, p.y) }
        Direction.None -> { p -> FramePoint(p.x, p.y) }
    }
}

fun Direction.mapLocModifier(): (MapLoc) -> MapLoc {
    return when (this) {
        Direction.Up -> { p: MapLoc -> p.up }
        Direction.Down -> { p: MapLoc -> p.down }
        Direction.Left -> { p: MapLoc -> p.left }
        Direction.Right -> { p: MapLoc -> p.right }
        Direction.None -> { p: MapLoc -> p }
    }
}


val Direction.isLeftUp: Boolean
    get() = when (this) {
        Direction.Left,
        Direction.Up -> true

        Direction.Right,
        Direction.Down -> false

        else -> false
    }

fun Direction.perpendicularTo(other: Direction) =
    this != Direction.None && other != Direction.None &&
            this.vertical == other.horizontal

val Direction.vertical: Boolean
    get() = this == Direction.Up || this == Direction.Down

val Direction.horizontal: Boolean
    get() = this == Direction.Left || this == Direction.Right
