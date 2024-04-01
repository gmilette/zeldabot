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

    fun toArrow(): String =
        when (this) {
            is DIAGONAL -> "/"
            is LEFT -> "<--"
            is RIGHT -> "-->"
            is UP -> "^"
            is DOWN -> "_"
            else -> "x"
        }
}
