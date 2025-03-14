package bot.state

import bot.state.map.Direction
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

    fun opposite(): GamePad = when (this) {
        MoveRight -> MoveLeft
        MoveLeft -> MoveRight
        MoveUp -> MoveDown
        MoveDown -> MoveUp
        None -> None
        else -> this
    }

    val isAttack: Boolean
        get() = this == A || this == B

    fun toDirection(): Direction = when (this) {
        MoveLeft -> Direction.Left
        MoveRight -> Direction.Right
        MoveUp -> Direction.Up
        MoveDown -> Direction.Down
        None -> Direction.None
        else -> Direction.None // a , b
    }

    companion object {
        fun aOrB(useB: Boolean) = if (useB) B else A

        fun cycleDirection(from: GamePad): GamePad {
            return when (from) {
                MoveUp -> MoveRight
                MoveRight -> MoveDown
                MoveDown -> MoveLeft
                MoveLeft -> MoveUp
                else -> MoveDown
            }
        }

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
                0 -> MoveUp
                1 -> MoveDown
                2 -> MoveLeft
                3 -> MoveRight
                else -> MoveDown
            }

        fun randomDirection(besides: GamePad): GamePad {
            var dir = randomDirection()
            while (dir != besides) {
                dir = randomDirection()
            }
            return dir
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