package bot.state.map.level

import bot.state.MapLoc
import bot.state.map.MapCell
import bot.state.map.MapCells
import java.io.File

data class LevelMapCellsLookup(val mapCells: MutableMap<Int, MapCells> = mutableMapOf()) {
    val getItemCell: MapCell? = null
    init {
        val reader = LevelCellBuilder()
        val lev1Cells = reader.level1()
        mapCells[1] = lev1Cells
        mapCells[2] = reader.level2()
        mapCells[3] = reader.level3()
        mapCells[4] = reader.level4()
        mapCells[5] = reader.level5()
        mapCells[6] = reader.level6()
        mapCells[7] = reader.level7()
        mapCells[8] = reader.level8()
        mapCells[9] = reader.level9()

        val write = false
        if (write) {
            mapCells[1]?.write("map/1")
            mapCells[2]?.write("map/2")
            mapCells[3]?.write("map/3")
            mapCells[4]?.write("map/4")
            mapCells[5]?.write("map/5")
            mapCells[6]?.write("map/6")
            mapCells[7]?.write("map/7")
            mapCells[8]?.write("map/8")
            mapCells[9]?.write("map/9")
        }
    }

    fun cell(level: Int, mapLoc: MapLoc): MapCell {
        val levelMap = mapCells[level] ?: throw IllegalArgumentException("not exist " +
                "$mapLoc")
        return levelMap.cell(mapLoc)
    }
}