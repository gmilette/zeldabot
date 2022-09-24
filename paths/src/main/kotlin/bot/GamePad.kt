package bot

enum class GamePad {
    None, MoveRight, MoveLeft, MoveDown, MoveUp,
    A, B, Select, Start, ReleaseA
}

fun GamePad.isMoveAction(): Boolean =
    this == GamePad.MoveRight || this == GamePad.MoveLeft || this == GamePad
        .MoveUp || this == GamePad.MoveDown