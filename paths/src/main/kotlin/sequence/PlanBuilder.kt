package sequence

import bot.state.*
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import sequence.findpaths.ZeldaDestinations
import util.d

class PlanBuilder() {
    fun build(may: Hyrule) {
//        mapData[119].loc.right.up.up.up.up.left,
//        PlanStep(
//            "get to level 1",
//            mapData[119],
//            mapData[120],
//            mapData[104],
//            mapData[88],
//            mapData[72],
//            mapData[56],
//            mapData[55],
//        )
    }

    class MasterPlanOptimizer() {
        private val graph: Graph<MapCell, DefaultEdge> =
            DefaultDirectedGraph(DefaultEdge::class.java)
        private lateinit var shortestPath: DijkstraShortestPath<MapCell, DefaultEdge>
        // find the path between all the MapCell objects
        fun buildNetwork(hyrule: Hyrule) {
            hyrule.mapCells.forEach {
                graph.addVertex(it)
            }

            hyrule.mapCells.forEach {
                when {
                    it.exits.contains(Direction.Down) -> graph.addEdge(it, hyrule
                        .near(it, Direction.Down))
                    it.exits.contains(Direction.Up) -> graph.addEdge(it, hyrule
                        .near(it, Direction.Up))
                    it.exits.contains(Direction.Left) -> graph.addEdge(it, hyrule
                        .near(it, Direction.Left))
                    it.exits.contains(Direction.Right) -> graph.addEdge(it, hyrule
                        .near(it, Direction.Right))
                }
            }
            shortestPath = DijkstraShortestPath(graph)
//        var iPaths = dijkstraAlg.getPaths(sequence.findpaths.ZeldaDestinations.start.point)
        }

        fun findPath(from: MapCell, to: MapCell): GraphPath<MapCell, DefaultEdge> {
            // return a set of mapCel
            var paths = shortestPath.getPaths(from)
            return paths.getPath(to)
        }

        fun destinationLookup(mapData: Map<MapLoc, MapCell>) {
            val lookup = mutableMapOf<DestType, MapCell>()
            mapData.forEach { loc, data ->
                data.mapData.objectives.forEach {
                    lookup.put(it.type, data)
                }
            }

            // all required objectives

            // make list of destinations
//            val sequence: List<MapCell> = listOf(
//                lookup[Dest.level(1)],
//                Dest.itemLookup[ZeldaItem.BombHeart],
//                lookup[Dest.level(2)],
//                lookup[Dest.level(3)],
//            )

//            val sequence: List<MapCell> = listOf(
//                mapData[119] ?: throw RuntimeException("err"),
//                mapData[120] ?: throw RuntimeException("err"),
//                mapData[104] ?: throw RuntimeException("err"),
//                mapData[88] ?: throw RuntimeException("err"),
//                mapData[72] ?: throw RuntimeException("err"),
//                mapData[56] ?: throw RuntimeException("err"),
//                mapData[55] ?: throw RuntimeException("err")
//            )

            val sequence: List<MapCell> = listOf(
                mapData[119] ?: throw RuntimeException("err"),
                mapData[55] ?: throw RuntimeException("err")
            )

            // this creates a plan step
            // for each objective
            var prev = sequence.first()
            sequence.forEach {
                if (prev != sequence.first()) {
                    val path = findPath(prev, it)
                    path.vertexList.forEach {
                        d { " from: ${it.mapData.name} ${it.mapLoc}"}
                    }
                }
                prev = it
            }
        }
    }

}
