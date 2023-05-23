package bot.state.map.level

object LevelStartMapLoc {
    fun lev(level: Int) = when (level) {
        1 -> 115
        2 -> 125
        3 -> 124
        4 -> 113
        5 -> 118
        6 -> 121
        7 -> 121
        8 -> 126
        9 -> 118
        else -> 0
//        else -> throw RuntimeException("not specified yet for level $level")
    }

    fun phase(level: Int) = "Beat Level $level"
}