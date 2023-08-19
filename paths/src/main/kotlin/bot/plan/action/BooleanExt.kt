package bot.plan.action

val Boolean.weapon: String
    get() = if (this) "Bomb" else "Sword"