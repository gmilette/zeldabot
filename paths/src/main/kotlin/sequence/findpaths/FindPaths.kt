package sequence.findpaths

import bot.state.map.mapPassable
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import org.jgrapht.Graph
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.io.File


class FindPaths {
    fun mapToX(y: Int): String {
        val letters = (y / 26) - 1
        val extra = y % 26
        return "${letter(letters)}${letter(extra)}"
    }

    fun letter(which: Int): String {
        return when (which) {
            0 -> "a"
            1 -> "b"
            2 -> "c"
            3 -> "d"
            4 -> "e"
            5 -> "f"
            6 -> "g"
            7 -> "h"
            8 -> "i"
            9 -> "j"
            10 -> "k"
            11 -> "l"
            12 -> "m"
            13 -> "n"
            14 -> "o"
            15 -> "p"
            16 -> "q"
            17 -> "r"
            18 -> "s"
            19 -> "t"
            20 -> "u"
            21 -> "v"
            22 -> "w"
            23 -> "x"
            24 -> "y"
            25 -> "z"
            else -> "?"
        }
    }

    // used?
    fun buildMapLocFiles() {
        val fileContent =
            this::class.java.classLoader.getResource("nes_zelda_overworld_tile_map_holes.txt")
                .readText()

        val replaced = fileContent.replace(" ", ",")
        val rows: List<List<String>> = CsvReader().readAll(replaced)

        // mapTile2.csv: original
        // mapTile3.csv: contains bomb holes
        // this works, just need to make sure the bottom is cut off
        val csvWriter2 = CsvWriter()
        csvWriter2.open("mapTile4.csv", false) {
            // how many rows to read for the first set
            rows.forEachIndexed { x, row ->
                val rowData = mutableListOf<String>()
                row.forEachIndexed { y, got ->
                    repeat(16) {
                        rowData.add(got)
                    }
                }
                // need to verify this
                val isBottomRow = (x+1) % 11 == 0
                repeat(if (isBottomRow) 8 else 16) {
                    writeRow(rowData)
                }
            }
            this.close()
        }

        // then I can read this into chunks of 256*?

        // vertical 8 rows
        // last row is 16x8
//        for (tilex in 0..1) { // 256
//            for (tiley in 0..16) { // 88 // probably not 16
//                val row = allRows[tiley]
//                val tileLetter = rows[tiley][tilex]
//                val passable = !passable.contains(tileLetter)
//                val letter = if (passable) "." else "X"
//                val isBottomRow = (tiley+1) % 11 == 0
//                repeat(if (isBottomRow) 8 else 16) {
//                    row.add(letter)
//                }
//            }
//        }
    }

    fun start() {
        val fileContent =
            this::class.java.classLoader.getResource("nes_zelda_overworld_tile_map.txt")
                .readText()
        val replaced = fileContent.replace(" ", ",")

        // load map
        val file = File("nes_zelda_overworld_tile_map.txt")
//        val rows: List<List<String>> = CsvReader().readAll(file)
        val rows: List<List<String>> = CsvReader().readAll(replaced)

        val g: Graph<Point, DefaultEdge> =
            DefaultDirectedGraph(DefaultEdge::class.java)

//        val passable = setOf(
//            sequence.findpaths.ZeldaMapCode.Cave.code, sequence.findpaths.ZeldaMapCode.Ground
//                .code, sequence.findpaths.ZeldaMapCode.Bridge.code, sequence.findpaths.ZeldaMapCode.Dock.code,
//            sequence.findpaths.ZeldaMapCode.PathBridge.code,
//            sequence.findpaths.ZeldaMapCode.PathGraveyard.code,
//            sequence.findpaths.ZeldaMapCode.PathBridgeGraveyard.code,
//            sequence.findpaths.ZeldaMapCode.PathBridgeGraveyardGrave.code,
//            sequence.findpaths.ZeldaMapCode.GraveYardBridge.code, "7f", "40"
//        )
        // should be all of these
        // note I added 7f manually
                val grid = mutableListOf<MutableList<Point>>()
        val csvWriter = CsvWriter()
        csvWriter.open("passableSpot.csv", false) {

            rows.forEachIndexed { x, row ->
                val rowData = mutableListOf<Point>()
                grid.add(rowData)
                val rowPass = mutableListOf<String>()
                row.forEachIndexed { y, got ->
                    val p = Point(x, y, got)
                    rowData.add(p)
                    g.addVertex(p)
                    if (got == "12") {
                        // in spreadsheet need to add 1 to x to see the column
                        println("SPOT $x $y ${mapToX(y)} SPOT")
                    }
                    if (got == "97") {
                        println("RIV $x $y ${mapToX(y)} RIV dock")
                    }
                    if (got == "79") {
                        println("W $x $y ${mapToX(y)}")
                    }
                    if (got == "7f") {
                        println("F $x $y ${mapToX(y)}")
                    }
                    if (got == "02") {
                        println("2 $x $y ${mapToX(y)}")
                    }
                    if (mapPassable.contains(got)) {
                        if (got == "12") {
                            rowPass.add("C")
                        } else {
                            rowPass.add("_")
                        }
                    } else {
                        rowPass.add("X")
                    }
                }
                this.writeRow(rowPass)
            }
            this.close()
        }
        grid.forEachIndexed { x, row ->
            val rowPass = mutableListOf<String>()
            row.forEachIndexed { y, current ->
//                println("test $x, $y, $current")
                var addedEdge = false
                if (x > 0) {
                    val left =
                        grid[x - 1][y]
                    if (mapPassable.contains(left.data)) {
                        g.addEdge(current, left)
                        addedEdge = true
                    }
                }
                if (x < grid.size - 1) {
                    val right = grid[x + 1][y]
                    if (mapPassable.contains(right.data)) {
                        g.addEdge(current, right)
                        addedEdge = true
                    }
                }
                if (y > 0) {
                    val up = grid[x][y - 1]
                    if (mapPassable.contains(up.data)) {
                        g.addEdge(current, up)
                        addedEdge = true
                    }
                }
                if (y < row.size - 1) {
                    val down = grid[x][y + 1]
                    if (mapPassable.contains(down.data)) {
                        g.addEdge(current, down)
                        addedEdge = true
                    }
                }

                if (addedEdge) {
                    if (current.data == "12") {
                        rowPass.add("C")
                    } else {
                        rowPass.add("_")
                    }
                } else {
                    rowPass.add("X")
                }
            }
        }


//        FloydWarshallShortestPaths
        // iterate over the destinations to find their paths to other
        // destinations
        val dijkstraAlg = DijkstraShortestPath(g)
//        var iPaths = dijkstraAlg.getPaths(sequence.findpaths.ZeldaDestinations.start.point)
        var iPaths = dijkstraAlg.getPaths(ZeldaDestinations.start.point)
        println("PATHS")
        println(
            """
                to lev 1: ${iPaths.getPath(ZeldaDestinations.level(1).point)?.length}

                to lev 2: ${iPaths.getPath(ZeldaDestinations.level(2).point)?.length}

                to lev 3: ${iPaths.getPath(ZeldaDestinations.level(3).point)?.length}
                to lev 4: ${iPaths.getPath(ZeldaDestinations.level(4).point)?.length}
                to lev 5: ${iPaths.getPath(ZeldaDestinations.level(5).point)?.length}
                to lev 6: ${iPaths.getPath(ZeldaDestinations.level(6).point)?.length}
                to lev 7: ${iPaths.getPath(ZeldaDestinations.level(7).point)?.length}
                to lev 8: ${iPaths.getPath(ZeldaDestinations.level(8).point)?.length}
                to lev 9: ${iPaths.getPath(ZeldaDestinations.level9.point)?.length}

                """.trimIndent()
        )

        if (true) {
            return
        }

//        val tour = listOf(
//            sequence.findpaths.ZeldaDestinations.start.point,
//            sequence.findpaths.ZeldaDestinations.level(1).point,
//            sequence.findpaths.ZeldaDestinations.level(2).point,
//            sequence.findpaths.ZeldaDestinations.level(3).point
//        )
//
        // TODO: add vertex and locations for all levels
//        val tours = getSearchList()
//        tours.forEach { aTour ->
//            evaluateTour(aTour.map { it.point }, dijkstraAlg)
//        }

//        https://graphviz.org/Gallery/undirected/grid.html

        val distTable = DistanceTable()

        // destination coordinates

        val levelSequence = Plan(
            ZeldaDestinations.levels
        )

        val score = evaluate(levelSequence, distTable)

        println("Score: $score")

    }

    private fun evaluateTour(
        tour: List<Point>,
        dijkstraAlg: DijkstraShortestPath<Point, DefaultEdge>
    ) {
        val dTable = mutableMapOf<Point, SingleSourcePaths<Point,
                DefaultEdge>>()
        tour.forEach {
            dTable[it] = dijkstraAlg.getPaths(it)
        }

        var current = ZeldaDestinations.start.point
        var total: Int = 0
        tour.forEach { pt ->
            val length = dTable[current]?.getPath(pt)?.length ?: 0
            println("from ${current} to $pt length $length")
            total += length
            current = pt
        }
        println("total: $total")
    }

    private fun testGrid(g: Graph<Point, DefaultEdge>): MutableList<MutableList<Point>> {
        val grid = mutableListOf<MutableList<Point>>(
            mutableListOf(
                Point(0, 0, "02"),
                Point(1, 0, "02"),
                Point(2, 0, "02"),
            ),
            mutableListOf(
                Point(0, 1, "02"),
                Point(1, 1, "02"),
                Point(2, 1, "02"),
            )
        )
        grid.forEach {
            it.forEach { p ->
                g.addVertex(p)
            }
        }
        return grid
    }

    private fun evaluate(plan: Plan, distanceTable: DistanceTable): Int {
        var total = 0
        var prev: Destination? = null
        plan.destinations.forEach { next ->
            val add = prev?.let {
                distanceTable.dist(it, next)
            } ?: 0
            total += add
            prev = next
        }
        return total
    }

    fun greedySolve() {
        // given there are X destinations
        // run all the combinations of start destinations

        // branching factor:
        // for each destination, try the nearest D destinations
    }

    fun eval(dest: List<Destination>): Boolean {
        val tour = dest.map { it.point }
        val indexOf4 = indexOf(tour, ZeldaDestinations.level(4).point)
        val indexOf3 = indexOf(tour, ZeldaDestinations.level(3).point)
        val indexOf5 = indexOf(tour, ZeldaDestinations.level(5).point)
        val indexOf7 = indexOf(tour, ZeldaDestinations.level(7).point)
        val indexOf8 = indexOf(tour, ZeldaDestinations.level(8).point)
        // 4 before 3
        return when {
            indexOf4 < indexOf3 -> false
            indexOf7 < indexOf5 -> false
            else -> true
        }
    }

    private fun indexOf(points: List<Point>, p: Point): Int {
        points.forEachIndexed { index, point ->
            if (point == p) return index
        }
        return -1
    }





}

