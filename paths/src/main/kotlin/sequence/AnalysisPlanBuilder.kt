package sequence

import bot.state.*
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.map.MapCell
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import util.d

class AnalysisPlanBuilder() {
    companion object {
        const val DEBUG = false
    }
    fun build(may: Hyrule) {
        // levels 1-9
    }

    fun buildHalfPlan(map: Hyrule) {
        // start plan (119)
//        * level 1 // 55
//        * bomb heart // 44
//        * bomb secret // 45
//        * letter // 14
//        * 100 secret // 15(-1?) (skip it)
//        * 30 secret (61)
//        * white sword (10)
//        * candle (102)
//        * 100 secret (98)
//        * white ring 52  (optional, could go to level 3 first)
//        * lev 3 116
//        * lev 4 69
//        * go down around for other bomb heart, 123
//        * buy arrow 111
//        * collect ladder heart 95
//        * raft heart 47
//        * level 2 60
//        * DONE! 27 min
        // it would be nice if these were destinations
        // like named map loc or something
        optimize(map)
//        val optimizer = MasterPlanOptimizer(map)
    }

    private fun generateZeldaPlanSearchSpace() {
        // the algorithm that expands nodes and terminates infeasible ones
        // reasons about how much gold we have
        // if next objective is, get arrow, then find nearest show and get it, if
        // don't have enough $$, just fail it
        // instead of putting money locations, maybe just insert "get nearest money"

        // expand all next objectives
    }

    private fun optimize(map: Hyrule) {
        val tour = Tour(listOf(119, 55, 44, 45, 14, 15, 61, 10, 102, 98, 52, 116, 69, 123, 111, 95, 47, 60))

        val levels = mutableListOf<MapLoc>(55, 60, 46, 69, 11, 34, 66, 109)
        val start = listOf<MapLoc>(119)
        val end = listOf<MapLoc>(5)

        val optimizer = MasterPlanOptimizer(map)

        var best: TourEvaluation = optimizer.evaluate(Tour(start + levels + end))
        var iter = 100000
        var explore: List<MapLoc> = levels
        repeat(iter) { iter ->
            var ind = listOf<MapLoc>()
            var nextExplore = Tour(start + explore + end)
            while (
                ind.isEmpty() ||
                ind[3] > ind[4] ||
                ind[4] > ind[6] ||
                ind[4] > ind[5] ||
                ind[4] > ind[7] || //need ladder for 7
                ind[5] > ind[7] ||
                ind[1] > ind[8] || // need to get magic key past a spider
                ind[1] > ind[6] || //you need a arrow to get past 6
    //                ind[4] > ind[8] || //just a pref
    //                ind[2] > ind[8] || //just a pref
    //                ind[1] > ind[3] || //just a pref
//                ind[4] > ind[2] || //just a pref
//                ind[4] > ind[1] || //just a pref
//                ind[8] != 8 || // force 8 at end
                ind[1] > ind[6]
            ) {
                // filter out more
                explore = explore.shuffled()
                nextExplore = Tour(start + explore + end)
                ind = sequenceOf(119, 55, 60, 46, 69, 11, 34, 66, 109, 5).map { nextExplore.destinations.indexOf(it) }.toList()
    //                d {"try $nextExplore $ind"}
            }

    //            d {"iter $iter"}
            val result = optimizer.evaluate(nextExplore)
            if (result.total < best.total) {
                d { " better result ${result} ${result.total}" }
                result.log()
                best = result
            }
        }

        d { " best " }
        best.log()
    }

    data class Tour(val destinations: List<MapLoc>)


    fun Tour.shuffle(): Tour =
        Tour(destinations.shuffled())

    data class TourEvaluation(val distances: List<Int>, val tour: Tour) {
        val total = distances.sum()

        fun log() {
            d { " total ${total}" }
            d { " tour $tour"}
            var ist = ""
            var i = 1
            for (destination in tour.destinations) {
//                val levels = Tour(listOf(55, 60, 46, 69, 11, 34, 66, 109, 5))
                val name = when (destination) {
                    119 -> 0
                    55 -> 1
                    60 -> 2
                    46 -> 3
                    69 -> 4
                    11 -> 5
                    34 -> 6
                    66 -> 7
                    109 -> 8
                    5 -> 9
                    else -> 0
                }
                ist = ist + "$name, "
                d {"$i: $name"}
                i++
            }
            d { ist }
        }
    }

    class RoutePattern(
        val from: MapLoc,
        val to: MapLoc,
        val transform: List<MapCell>) {

        fun match(fromT: MapCell, toT: MapCell): Boolean =
            (from == fromT.mapLoc && to == toT.mapLoc)
    }

    class MasterPlanOptimizer(private val hyrule: Hyrule) {
        private val graph: Graph<MapCell, DefaultEdge> =
            DefaultDirectedGraph(DefaultEdge::class.java)

        //        private lateinit var shortestPath: DijkstraShortestPath<MapCell, DefaultEdge>
        private lateinit var shortestPath: FloydWarshallShortestPaths<MapCell, DefaultEdge>

        private val patterns: MutableList<RoutePattern> = mutableListOf()

        val Int.asLoc: MapLoc
            get() = this

        fun correct(path: List<MapCell>): List<MapCell> {
            val corrected = mutableListOf<MapCell>()
            var prev: MapCell? = null
            for (mapCell in path) {
                if (prev != null) {
                    for (pattern in patterns) {
                        if (pattern.match(prev, mapCell)) {
                            corrected.addAll(pattern.transform)
                        }
                    }
                }
                corrected.add(mapCell)
                prev = mapCell
            }
            return corrected
        }

        fun makePatterns() {
            val start = 84.asLoc
            val p = RoutePattern(
                84, 85, hyrule.mapCellsFor(start, start.down, start.right, start.up)
            )
            patterns.add(p)
        }

        // find the path between all the MapCell objects
        init {
            hyrule.mapCells.forEach {
                graph.addVertex(it)
            }

            makePatterns()

            if (DEBUG) {
                d { " Num vertext: ${graph.vertexSet().size} " }
            }

            hyrule.mapCells.forEach {
                if (it.hasExit(Direction.Down)) {
                    val near = hyrule.near(it, Direction.Down)
                    if (DEBUG) {
                        d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                    }
                    if (near != null) {
                        graph.addEdge(it, near)
                    }
                }
                if (it.hasExit(Direction.Up)) {
                    val near = hyrule.near(it, Direction.Up)
                    if (DEBUG) {
                        d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                    }
                    if (near != null) {
                        graph.addEdge(it, near)
                    }
                }
                if (it.hasExit(Direction.Left)) {
                    val near = hyrule.near(it, Direction.Left)
                    if (DEBUG) {
                        d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                    }
                    if (near != null) {
                        graph.addEdge(it, near)
                    }
                }
                if (it.hasExit(Direction.Right)) {
                    val near = hyrule.near(it, Direction.Right)
                    if (DEBUG) {
                        d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                    }
                    if (near != null) {
                        graph.addEdge(it, near)
                    }
                }
            }
            // manually add
            add(85, 69, graph) // level 4
            add(63, 47, graph) // raft heart
            add(31, 15, graph) // 100 secret

            // ahh he wont go through the water, need to make it accessible
//            add(23, 24, graph)

            // dont go up or down in lost woods
            // only across it
            val lostWoods: MapLoc = 97
            remove(lostWoods, 97.up, graph)
            remove(lostWoods, 97.down, graph)
            // this goes forever
            graph.removeEdge(hyrule.getMapCell(lostWoods), hyrule.getMapCell(lostWoods.left))
            remove(85, 86, graph) // can't go through to level 4 from here
//            remove(98, 99, graph)
            // maybe need?
//            remove(98, 97, graph) // through 100 secret
            remove(85, 86, graph) // can't go through to level 4 from here
            remove(85, 84, graph) // can't go through to water

            // yes can do
//            remove(85, 69, graph) // can't go up through this water

            remove(29, 28, graph) // can't go through mountain
            val lostDesert: MapLoc = 27
            remove(lostDesert, lostDesert + 16, graph) // do not get lost in the part before the level 5

            remove(116, 117, graph) // dont go through this weird place
            remove(101, 117, graph)

            remove(39, 38, graph) // crossing a river, lazy, I could just add to map too

            remove(109, 110, graph) // dont get stuck trying to pass through level 8 tree

//            shortestPath = DijkstraShortestPath(graph)
            shortestPath = FloydWarshallShortestPaths(graph)

            // fix path

            // wtf 71
//            d { " 71 ${hyrule.getMapCell(71).exits}" }
//            val path = shortestPath.getPath(hyrule.getMapCell(3.levToMap), hyrule.getMapCell(2.levToMap))
            //Debug: (Kermit) 116
            //Debug: (Kermit) 115
            //Debug: (Kermit) 99
            //Debug: (Kermit) 100
            //Debug: (Kermit) 101
            //Debug: (Kermit) 102
            //Debug: (Kermit) 103
            //Debug: (Kermit) 104
            //Debug: (Kermit) 105
            //Debug: (Kermit) 106
            //Debug: (Kermit) 90
            //Debug: (Kermit) 91
            //Debug: (Kermit) 92
            //Debug: (Kermit) 93
            //Debug: (Kermit) 77
            //Debug: (Kermit) 76
            //Debug: (Kermit) 60


//            val path = shortestPath.getPath(hyrule.getMapCell(3.levToMap), hyrule.getMapCell(4.levToMap))
            //Debug: (Kermit)  path 6
            //Debug: (Kermit) 116
            //Debug: (Kermit) 115
            //Debug: (Kermit) 99
            //Debug: (Kermit) 100
            //Debug: (Kermit) 84
            //Debug: (Kermit) 85
            //Debug: (Kermit) 69

            //11
            val path = shortestPath.getPath(hyrule.getMapCell(3.levToMap), hyrule.getMapCell(1.levToMap))

            //11
//            val path = shortestPath.getPath(hyrule.getMapCell(119), hyrule.getMapCell(2.levToMap))
            // 6
//            val path = shortestPath.getPath(hyrule.getMapCell(119), hyrule.getMapCell(1.levToMap))

            d { " path ${path?.length}" }
            for (mapCell in path.vertexList) {
                d { "${mapCell.mapLoc}" }
            }
            val a = 1
        }


        val Int.levToMap
            get() = when (this) {
                0 -> 119
                1 -> 55
                2 -> 60
                3 -> 116
                4 -> 69
                5 -> 11
                6 -> 34
                7 -> 66
                8 -> 109
                9 -> 5
                else -> 0
            }

        private fun add(from: Int, to: Int, graph: Graph<MapCell, DefaultEdge>) {
            graph.addEdge(hyrule.getMapCell(from), hyrule.getMapCell(to))
            graph.addEdge(hyrule.getMapCell(to), hyrule.getMapCell(from))
        }

        private fun remove(from: Int, to: Int, graph: Graph<MapCell, DefaultEdge>) {
            graph.removeEdge(hyrule.getMapCell(from), hyrule.getMapCell(to))
            graph.removeEdge(hyrule.getMapCell(to), hyrule.getMapCell(from))
        }

        fun evaluate(plan: Tour): TourEvaluation {
            val cells = plan.destinations.map { hyrule.getMapCell(it) }
            val distances = mutableListOf<Int>()
            var total = 0
            var prev: MapCell? = null
            for (next in cells) {
                val add = prev?.let {
                    val path = findPath(it, next)
                    path
                }?.length ?: 0
//                d { " from: ${prev?.mapData?.name ?: ""} ${prev?.mapLoc} to ${next.mapLoc} dist: ${add}" }
                distances.add(add)
                total += add
                prev = next
            }
            return TourEvaluation(distances, plan)
        }

        fun findPath(fromLoc: MapLoc, toLoc: MapLoc): GraphPath<MapCell, DefaultEdge>? {
            // return a set of mapCel
            val from = hyrule.getMapCell(fromLoc)
            val to = hyrule.getMapCell(toLoc)
            val paths = shortestPath.getPaths(from)
            return paths.getPath(to)
        }

        fun findPath(from: MapCell, to: MapCell): GraphPath<MapCell, DefaultEdge>? {
            // return a set of mapCel
            val paths = shortestPath.getPaths(from)
            return paths.getPath(to)
        }
    }

//        fun destinationLookup(mapData: Map<MapLoc, MapCell>) {
//            val lookup = mutableMapOf<DestType, MapCell>()
//            mapData.forEach { loc, data ->
//                data.mapData.objective.forEach {
//                    lookup.put(it.type, data)
//                }
//            }
//
//            // all required objectives
//
//            // make list of destinations
////            val sequence: List<MapCell> = listOf(
////                lookup[Dest.level(1)],
////                Dest.itemLookup[ZeldaItem.BombHeart],
////                lookup[Dest.level(2)],
////                lookup[Dest.level(3)],
////            )
//
////            val sequence: List<MapCell> = listOf(
////                mapData[119] ?: throw RuntimeException("err"),
////                mapData[120] ?: throw RuntimeException("err"),
////                mapData[104] ?: throw RuntimeException("err"),
////                mapData[88] ?: throw RuntimeException("err"),
////                mapData[72] ?: throw RuntimeException("err"),
////                mapData[56] ?: throw RuntimeException("err"),
////                mapData[55] ?: throw RuntimeException("err")
////            )
//
//            val sequence: List<MapCell> = listOf(
//                mapData[119] ?: throw RuntimeException("err"),
//                mapData[55] ?: throw RuntimeException("err")
//            )
//
//            // this creates a plan step
//            // for each objective
//            var prev = sequence.first()
//            sequence.forEach {
//                if (prev != sequence.first()) {
//                    val path = findPath(prev, it)
//                    path?.vertexList?.forEach {
//                        d { " from: ${it.mapData.name} ${it.mapLoc}" }
//                    }
//                }
//                prev = it
//            }
//        }
//    }

}
