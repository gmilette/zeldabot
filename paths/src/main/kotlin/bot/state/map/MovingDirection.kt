package bot.state.map

import bot.state.FramePoint

sealed class MovingDirection {
    sealed class Diagonal(val slope: FramePoint): MovingDirection() {
        object UpRight: Diagonal(FramePoint(1, 1))
        object UpLeft: Diagonal(FramePoint(-1, 1))
        object DownRight: Diagonal(FramePoint(1, -1))
        object DownLeft: Diagonal(FramePoint(-1, -1))
    }
    object Left: MovingDirection()
    object Right: MovingDirection()
    object Up: MovingDirection()
    object Down: MovingDirection()
    object UnknownOrStationary: MovingDirection()

    companion object {
        fun from(dir: Direction): MovingDirection =
            when (dir) {
                Direction.Left -> Left
                Direction.Right -> Right
                Direction.Down -> Down
                Direction.Up -> Up
                else -> UnknownOrStationary
            }
    }

    fun toArrow(): String =
        when (this) {
            is Diagonal.UpRight   -> "↗"
            is Diagonal.UpLeft    -> "↖"
            is Diagonal.DownRight -> "↘"
            is Diagonal.DownLeft  -> "↙"
            Left -> "←"
            Right -> "→"
            Up -> "↑"
            Down -> "↓"
            else -> "x"
        }

    fun toDirection(): Direction =
        when (this) {
            is Left -> Direction.Left
            is Right ->Direction.Right
            is Up -> Direction.Up
            is Down -> Direction.Down
            else -> Direction.None
        }

    override fun toString(): String {
        return toArrow()
    }
}
