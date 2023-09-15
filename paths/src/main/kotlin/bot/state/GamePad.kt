package bot.state

import bot.state.map.MapConstants
import nintaco.api.GamepadButtons
import kotlin.random.Random

enum class GamePad {
    None, MoveRight, MoveLeft, MoveDown, MoveUp,
    A, B, Select, Start, ReleaseA, ReleaseB;

    val isDirection: Boolean
        get() = this == MoveUp || this == MoveDown || this == MoveLeft || this == MoveRight

    val isHorizontal: Boolean
        get() = this == MoveLeft || this == MoveRight

    companion object {
        fun randomDirection(from: FramePoint): GamePad {
            val possible = mutableListOf<GamePad>()
            if (from.x > MapConstants.oneGrid + 2) {
                possible.add(MoveLeft)
            }
            if (from.y > MapConstants.oneGrid + 2) {
                possible.add(MoveUp)
            }
            if (from.y < MapConstants.MAX_Y - MapConstants.oneGrid - 2) {
                possible.add(MoveDown)
            }
            if (from.x < MapConstants.MAX_X - MapConstants.oneGrid - 2) {
                possible.add(MoveRight)
            }
            possible.shuffle()
            return possible.firstOrNull() ?: None
        }

        fun randomDirection() =
            when (Random.nextInt(4)) {
                1 -> MoveUp
                2 -> MoveDown
                3 -> MoveLeft
                4 -> MoveRight
                else -> MoveDown
            }
    }
}

val GamePad.toGamepadButton
    get() = when (this) {
        GamePad.MoveRight -> GamepadButtons.Right
        GamePad.MoveLeft -> GamepadButtons.Left
        GamePad.MoveUp -> GamepadButtons.Up
        GamePad.MoveDown -> GamepadButtons.Down
        GamePad.A -> GamepadButtons.A
        GamePad.B -> GamepadButtons.B
        GamePad.Select -> GamepadButtons.Select
        GamePad.Start -> GamepadButtons.Start
        else -> 0
    }

val GamePad.moveActions: MutableSet<GamePad>
    get() = mutableSetOf(GamePad.MoveRight, GamePad.MoveUp, GamePad.MoveLeft, GamePad.MoveDown)

fun GamePad.isMoveAction(): Boolean =
    this == GamePad.MoveRight || this == GamePad.MoveLeft || this == GamePad.MoveUp || this == GamePad.MoveDown