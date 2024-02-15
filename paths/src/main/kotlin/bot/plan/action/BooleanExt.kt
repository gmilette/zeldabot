package bot.plan.action

import bot.state.GamePad

val Boolean.weapon: String
    get() = if (this) "Bomb" else "Sword"

val Boolean.isB: GamePad
    get() = if (this) GamePad.B else GamePad.A