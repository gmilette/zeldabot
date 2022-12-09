package bot.state.map

import bot.state.MapLoc

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

    fun write(dir: String) {
        for (cell in mapCells.values) {
            cell.write(dir)
        }
    }
}