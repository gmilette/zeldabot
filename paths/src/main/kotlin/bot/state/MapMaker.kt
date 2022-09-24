package bot.state

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import util.Map2d
import util.d

object MapMaker {
    fun createMapCells(data: Map<MapLoc, MapCellData>): Map<MapLoc, MapCell> {
        d { " read map cells " }

        val emptyMapData = MapCellData("unknown")

        // now read in mapTile2
        val fileContent =
            this::class.java.classLoader.getResource("mapTile2.csv")
                .readText()
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
                rows.subList(yPx, yPx + heightOfCell).forEach { row ->
                    val part = row.subList(xPx, xPx + widthOfCell)
                        .map { mapPassable.contains(it) } //passable.contains(it
//                    d { " row ${passable.contains(row.subList(xPx, xPx +
//                            widthOfCell)[1])}"}
//                    d { " roww $part"}
                    mapData.add(part.toMutableList())
                }
                val mapCell = MapCell(
                    point, mapLoc, data.get(mapLoc) ?: emptyMapData,
                    Map2d
                        (mapData)
                )
                mapCells[mapLoc] = mapCell
            }
        }
//
//        passableMap.forEach {
//            it.forEach {
//                print(if (it) "t" else "f")
//            }
//            println()
//        }

        return mapCells
    }

}

val mapPassable = setOf(
    "7f",
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
    "9d"
)
