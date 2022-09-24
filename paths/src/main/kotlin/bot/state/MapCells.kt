package bot.state

class MapCells(
    private val mapCells: Map<MapLoc, MapCell>
) {
    operator fun invoke(mapLoc: MapLoc): MapCell {
        return cell(mapLoc)
    }
    fun cell(mapLoc: MapLoc): MapCell {
        return mapCells[mapLoc] ?: throw IllegalArgumentException("not exist " +
                "$mapLoc")
    }
}