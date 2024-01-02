package bot.state.map

import bot.state.FramePoint
import bot.state.GamePad

enum class Direction {
    Left, Right, Up, Down, None;

    companion object {
        val horizontal: List<Direction> = listOf(Direction.Left, Direction.Right)
        val vertical: List<Direction> = listOf(Direction.Up, Direction.Down)
        val all: List<Direction>
            get() = listOf(Up, Right, Down, Left)
    }
}

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

val Direction.isLeftUp: Boolean
    get() = when (this) {
        Direction.Left,
        Direction.Up -> true

        Direction.Right,
        Direction.Down -> false

        else -> false
    }

val Direction.vertical: Boolean
    get() = this == Direction.Up || this == Direction.Down

val Direction.horizontal: Boolean
    get() = this == Direction.Left || this == Direction.Right
