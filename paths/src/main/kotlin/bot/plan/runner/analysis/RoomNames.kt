package bot.plan.runner.analysis

object RoomNames {
    private val names = mapOf(
        "2_79" to "blue_boom",
        "8_62" to "master_battle",
        "5_101" to "mummies",
        "8_94" to "proj_sword"
    )

    fun label(room: String): String = names[room]?.let { "$room($it)" } ?: room
}
