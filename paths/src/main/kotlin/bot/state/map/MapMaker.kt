package bot.state.map

import bot.state.MapCellPoint
import bot.state.MapLoc
import bot.state.MapLocFromPoint
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import util.Map2d
import util.d

object MapMaker {
    fun createMapCells(data: Map<MapLoc, MapCellData>): Map<MapLoc, MapCell> {
        val emptyMapData = MapCellData("", Objective.empty)

        // now read in mapTile2
        val fileContent =
            this::class.java.classLoader.getResource("mapTile4.csv").readText()
//        val replaced = fileContent.replace(" ", ",")
        val rows: List<List<String>> = CsvReader().readAll(fileContent)
        val passableMap = mutableListOf<List<Boolean>>()
        val mapCells = mutableMapOf<MapLoc, MapCell>()

        val widthOfCell = (256 / 16) * 16
        val heightOfCell = 11 * 16 - 8
        for (x in 0..15) { //15
            for (y in 0..7) { //8
//                d { " read x $x y $y" }
                val point = MapCellPoint(x, y)
                val mapLoc = MapLocFromPoint(x, y)
                val mapData = mutableListOf<MutableList<Boolean>>()
//                passableMap.add(mapData)
                val yPx = y * heightOfCell
                val xPx = x * widthOfCell
//                d { "y $yPx to ${yPx+heightOfCell} x $xPx to " +
//                        "${xPx + widthOfCell}"}
                val ALL_IMPASSIBLE = true

                if (ALL_IMPASSIBLE) {
                    rows.subList(yPx, yPx + heightOfCell).forEach { row ->
                        val part = row.subList(xPx, xPx + widthOfCell)
                            .map { mapPassable.contains(it) } //passable.contains(it
                        mapData.add(part.toMutableList())
                    }
                } else {
                    // doesnt work
                    // first set of rows actually check passible
                    rows.subList(yPx, yPx + (heightOfCell / 2)).forEach { row ->
                        val part = row.subList(xPx, xPx + widthOfCell)
                            .map { mapPassable.contains(it) } //passable.contains(it
                        mapData.add(part.toMutableList())
                    }
                    // nah the whole second half ends up empty
                    // second set of rows is only passible, ignore if it really is or not
                    rows.subList(yPx, yPx + (heightOfCell / 2)).forEach { row ->
                        val part = row.subList(xPx, xPx + widthOfCell)
                            .map { true }
                        mapData.add(part.toMutableList())
                    }
                }
                val mapCell = MapCell(
                    point, mapLoc, data.get(mapLoc) ?: emptyMapData, Map2d(mapData),
                    halfPassable = true,
                    isLevel = false
                )
                //168
//                d { "map size ${mapCell.passable.map.size}" }
                mapCells[mapLoc] = mapCell
            }
        }
        return mapCells
    }

}

val mapPassable = setOf(
    "XB", //added for bomb
    "XL", //added for ladder
    "XR", //added for easy raft, and rock near level 2
    "XP", //added to pass though like in the 100 secret
    "XC", //add for places than can be burned with candle // made some guesses about the heart burn
    "XS", //statues that can be pushed
    "XW", // revealed by whistle (level 7)
//    "XN", // not passible so I can block certain paths
//    "7f", // I do not think this is passable, it is the edge of the lakes, if I disable is it ok?
    //"3e", // gets link stuck /// that is the half mountain. not exactly passable, but link can walk on it, i added this
    "00",
    "02",
    "06",
    "0c",
    "0e",
    "14",
    "18",
    "1a",
    "20",
    "24",
    "40",
    "4c",
    "53",
    "54",
    "55",
    "59",
    "5a",
    "5b",
    "5f",
    "60",
    "61",
    "67",
    "68",
    "69",
    "6d",
    "6e",
    "6f",
    "73",
    "74",
    "75",
    "7b",
    "7c",
    "7d",
    "81",
    "82",
    "83",
    "87",
    "88",
    "89",
    "8c",
    "8d",
    "8e",
    "8f",
    "91",
    "92",
    "93",
    "94",
    "95",
    "97",
    "98",
    "99",
    "9a",
    "9b",
    "9d",
    "12"
)
