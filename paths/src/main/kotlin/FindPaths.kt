import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import org.jgrapht.Graph
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths
import org.jgrapht.alg.shortestpath.AStarShortestPath
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths
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
//            ZeldaMapCode.Cave.code, ZeldaMapCode.Ground
//                .code, ZeldaMapCode.Bridge.code, ZeldaMapCode.Dock.code,
//            ZeldaMapCode.PathBridge.code,
//            ZeldaMapCode.PathGraveyard.code,
//            ZeldaMapCode.PathBridgeGraveyard.code,
//            ZeldaMapCode.PathBridgeGraveyardGrave.code,
//            ZeldaMapCode.GraveYardBridge.code, "7f", "40"
//        )
        // should be all of these
        // note I added 7f manually
        val passable = setOf(
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
                    if (passable.contains(got)) {
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

//        val grid = testGrid(g)

        // bridge
//        test 38, 127, Point(x=38, y=127, data=91)
//        test 38, 128, Point(x=38, y=128, data=91)
//        test 38, 129, Point(x=38, y=129, data=91)
//        test 38, 130, Point(x=38, y=130, data=91)
//        test 38, 131, Point(x=38, y=131, data=91)
//        test 38, 132, Point(x=38, y=132, data=91)
//        test 38, 133, Point(x=38, y=133, data=91)
//        test 38, 134, Point(x=38, y=134, data=91)

        grid.forEachIndexed { x, row ->
            val rowPass = mutableListOf<String>()
            row.forEachIndexed { y, current ->
//                println("test $x, $y, $current")
                var addedEdge = false
                if (x > 0) {
                    val left =
                        grid[x - 1][y]
                    if (passable.contains(left.data)) {
                        g.addEdge(current, left)
                        addedEdge = true
                    }
                }
                if (x < grid.size - 1) {
                    val right = grid[x + 1][y]
                    if (passable.contains(right.data)) {
                        g.addEdge(current, right)
                        addedEdge = true
                    }
                }
                if (y > 0) {
                    val up = grid[x][y - 1]
                    if (passable.contains(up.data)) {
                        g.addEdge(current, up)
                        addedEdge = true
                    }
                }
                if (y < row.size - 1) {
                    val down = grid[x][y + 1]
                    if (passable.contains(down.data)) {
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
//        var iPaths = dijkstraAlg.getPaths(ZeldaDestinations.start.point)
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
//            ZeldaDestinations.start.point,
//            ZeldaDestinations.level(1).point,
//            ZeldaDestinations.level(2).point,
//            ZeldaDestinations.level(3).point
//        )
//
        // TODO: add vertex and locations for all levels
        val tours = getSearchList()
        tours.forEach { aTour ->
            evaluateTour(aTour.map { it.point }, dijkstraAlg)
        }

        // can I show a path in the matrix?
        ///val g = LabeledGraph { a - b - c - a }
        //        .toJGraphT().toKaliningraph()
        //        .toTinkerpop().toKaliningraph()
        //        .toGraphviz().toKaliningraph()

//        https://graphviz.org/Gallery/undirected/grid.html

        val distTable = DistanceTable()

        // destination coordinates

        val levelSequence = Plan(
            ZeldaDestinations.levels
        )

        val score = evaluate(levelSequence, distTable)

        println("Score: $score")

        getSearchList()
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

    fun getSearchList(): List<List<Destination>> {
        var message: String = "a b c"

//       var items = listOf(ZeldaDestinations.level(1), ZeldaDestinations.level
//           (2), ZeldaDestinations.level(3))
        var items = ZeldaDestinations.levels
///        var items = message.split(" ")
        println("items ${items.size}")

        val result = items
            .flatMap { i1 ->
                items.flatMap { i2 ->
                    items.flatMap { i3 ->
                        items.flatMap { i4 ->
                            items.flatMap { i5 ->
                                items.flatMap { i6 ->
                                    items.flatMap { i7 ->
                                        items.mapNotNull { i8 ->
                                            val combination = listOf(
                                                i1,
                                                i2,
                                                i3,
                                                i4,
                                                i5,
                                                i6,
                                                i7,
                                                i8
                                            ).distinct()
                                            if (combination.count() == 8 &&
                                                eval(combination)
                                            )
                                                combination else null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
//            .mapIndexed { index, it -> "" + (index + 1) + ". " + it }
//40320.
        println("result")
        for (item in result) {
            println(item + "\n")
        }

        val tours = result.map {
            mutableListOf<Destination>().apply {
//                add(ZeldaDestinations.start)
                addAll(it)
                add(ZeldaDestinations.level9)
            }
        }

        return tours
    }
}

