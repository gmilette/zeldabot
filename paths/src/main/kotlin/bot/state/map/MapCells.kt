package bot.state.map

import bot.state.MapLoc
import bot.state.map.destination.DestType
import bot.state.map.destination.ZeldaItem

class MapCells(
    private val mapCells: Map<MapLoc, MapCell> = emptyMap()
) {
    operator fun invoke(mapLoc: MapLoc): MapCell {
        return cell(mapLoc)
    }

    fun findObjective(item: ZeldaItem): MapCell {
       return mapCells.values.firstOrNull {
            (it.mapData.objective.type as? DestType.Item)?.let {
                it.item == item
            } ?: false
        } ?: throw IllegalArgumentException("there is no $item")
    }

    fun findObjective(type: DestType): MapCell {
        return mapCells.values.firstOrNull {
            it.mapData.objective.type == type
        } ?: throw IllegalArgumentException("there is no $type")
    }

    private inline fun <reified T>findObjectiveWithType(having: (Objective) -> Boolean): MapCell? =
        mapCells.values.firstOrNull {
            it.mapData.objective.type is T && it.mapData.objective.let(having)
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