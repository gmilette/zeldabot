package bot.state

data class MapCoordinates(val level: Int, val loc: MapLoc) {
    val id get() = "${level}_${loc}"
}
