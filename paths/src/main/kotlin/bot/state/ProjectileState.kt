package bot.state

enum class ProjectileState(val code: Int) {
    UnknownProjectile(-99),
    NotProjectile(-1),
    Gone(0),
    Moving(16),
    Start(32),
    End(46),
    Deflect(48)
}

object ProjectileMapper {
    fun map(index: Int, code: Int): ProjectileState =
        if (index < 5) {
            ProjectileState.NotProjectile
        } else {
            ProjectileState.values().firstOrNull { it.code == code } ?: ProjectileState.UnknownProjectile
        }
}
