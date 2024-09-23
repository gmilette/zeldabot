package bot.state.map

import bot.state.FramePoint

//enum class MovingDirection {
//    UP_RIGHT,
//    UP_LEFT,
//    DOWN_RIGHT,
//    DOWN_LEFT,
//    LEFT,
//    RIGHT,
//    UP,
//    DOWN,
//    UNKNOWN_OR_STATIONARY,
//}

sealed class MovingDirection {
    data class DIAGONAL(val slope: FramePoint): MovingDirection()
    object LEFT: MovingDirection()
    object RIGHT: MovingDirection()
    object UP: MovingDirection()
    object DOWN: MovingDirection()
    object UNKNOWN_OR_STATIONARY: MovingDirection()

    companion object {
        fun from(dir: Direction): MovingDirection =
            when (dir) {
                Direction.Left -> LEFT
                Direction.Right -> RIGHT
                Direction.Down -> DOWN
                Direction.Up -> UP
                else -> UNKNOWN_OR_STATIONARY
            }
    }

    fun toArrow(): String =
        when (this) {
            is DIAGONAL -> "/"
            is LEFT -> "<--"
            is RIGHT -> "-->"
            is UP -> "^"
            is DOWN -> "_"
            else -> "x"
        }

    fun toDirection(): Direction =
        when (this) {
            is LEFT -> Direction.Left
            is RIGHT ->Direction.Right
            is UP -> Direction.Up
            is DOWN -> Direction.Down
            else -> Direction.None
        }

    override fun toString(): String {
        return toArrow()
    }
}
