package bot.state

import nintaco.api.GamepadButtons
import kotlin.random.Random

enum class GamePad {
    None, MoveRight, MoveLeft, MoveDown, MoveUp,
    A, B, Select, Start, ReleaseA, ReleaseB;

    companion object {
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