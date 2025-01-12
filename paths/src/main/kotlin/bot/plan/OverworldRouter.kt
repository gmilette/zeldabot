package bot.plan

import bot.state.*
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.map.MapCell
import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import sequence.AnalysisPlanBuilder
import util.d

/**
 * shortest path between locations in overworld, making it easier to author plans
 */
class OverworldRouter(private val hyrule: Hyrule) {
    private val graph: Graph<MapCell, DefaultEdge> =
        DefaultDirectedGraph(DefaultEdge::class.java)

    private var shortestPath: FloydWarshallShortestPaths<MapCell, DefaultEdge>

    private val patterns: MutableList<RoutePattern> = mutableListOf()

    private val Int.asLoc: MapLoc
        get() = this

    // find the path between all the MapCell objects
    init {
        hyrule.mapCells.forEach {
            graph.addVertex(it)
        }

        makePatterns()

        if (AnalysisPlanBuilder.DEBUG) {
            d { " Num vertext: ${graph.vertexSet().size} " }
        }

        hyrule.mapCells.forEach {
            if (it.hasExit(Direction.Down)) {
                val near = hyrule.near(it, Direction.Down)
                if (AnalysisPlanBuilder.DEBUG) {
                    d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                }
                if (near != null) {
                    graph.addEdge(it, near)
                }
            }
            if (it.hasExit(Direction.Up)) {
                val near = hyrule.near(it, Direction.Up)
                if (AnalysisPlanBuilder.DEBUG) {
                    d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                }
                if (near != null) {
                    graph.addEdge(it, near)
                }
            }
            if (it.hasExit(Direction.Left)) {
                val near = hyrule.near(it, Direction.Left)
                if (AnalysisPlanBuilder.DEBUG) {
                    d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                }
                if (near != null) {
                    graph.addEdge(it, near)
                }
            }
            if (it.hasExit(Direction.Right)) {
                val near = hyrule.near(it, Direction.Right)
                if (AnalysisPlanBuilder.DEBUG) {
                    d { " add ${it.mapLoc} -> ${near?.mapLoc}" }
                }
                if (near != null) {
                    graph.addEdge(it, near)
                }
            }
        }

        // these additions are required because depending on hour link enters a map cell
        // he might not be able to go in a certain direction
        // it's simpler to just avoid those than to modify shortest path

        // manually add some where link has to use raft
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
//        remove(85, 86, graph) // I think ok. can't go through to level 4 from here
        remove(85, 84, graph) // can't go through to water
        remove(98, 82, graph) // can't go up through 100 secret because you could be on wrong side
        remove(98, 82, graph) // can't go up through 100 secret because you could be on wrong side

        // looks like app is trying to do impossible route going right from fairy
//        remove(67, 68, graph)
        // trees near level 8
        removeJust(108, 107, graph)
        // going up into a place where you are in an elbow of trees
        removeJust(107, 92, graph)

        removeJust(27, 28, graph) // maze near level 5

        remove(98, 114, graph) // cant go down from secret place

        // go through green forest spot
        remove(108, 109, graph)
        // getting stuck in green forest
        remove(107, 108, graph)
        // yes can do
//            remove(85, 69, graph) // can't go up through this water

        remove(29, 28, graph) // can't go through mountain
        val lostDesert: MapLoc = 27
        remove(lostDesert, lostDesert + 16, graph) // do not get lost in the part before the level 5

        remove(116, 117, graph) // dont go through this weird place
        remove(101, 117, graph)

        remove(39, 38, graph) // crossing a river, lazy, I could just add to map too

        remove(109, 110, graph) // dont get stuck trying to pass through level 8 tree
        remove(75, 74, graph) // split area in blue forest where the potion is, dont try to go left

//            shortestPath = DijkstraShortestPath(graph)
        shortestPath = FloydWarshallShortestPaths(graph)

//        val path = shortestPath.getPath(hyrule.getMapCell(3.levToMap), hyrule.getMapCell(1.levToMap))
//        d { " path ${path?.length}" }
//        for (mapCell in path.vertexList) {
//            d { "${mapCell.mapLoc}" }
//        }
    }

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

    private fun makePatterns() {
        val start = 84.asLoc
        val p = RoutePattern(
            84, 85, hyrule.mapCellsFor(start, start.down, start.right, start.up)
        )
        patterns.add(p)
    }

    private val Int.levToMap
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

    /**
     * and remove reverse too
     */
    private fun remove(from: Int, to: Int, graph: Graph<MapCell, DefaultEdge>) {
        graph.removeEdge(hyrule.getMapCell(from), hyrule.getMapCell(to))
        graph.removeEdge(hyrule.getMapCell(to), hyrule.getMapCell(from))
    }

    private fun removeJust(from: Int, to: Int, graph: Graph<MapCell, DefaultEdge>) {
        graph.removeEdge(hyrule.getMapCell(from), hyrule.getMapCell(to))
    }

    fun findPath(fromLoc: MapLoc, toLoc: MapLoc): GraphPath<MapCell, DefaultEdge>? {
        val from = hyrule.getMapCell(fromLoc)
        val to = hyrule.getMapCell(toLoc)
        val paths = shortestPath.getPaths(from)
        return paths.getPath(to)
    }

    fun findPath(from: MapCell, to: MapCell): GraphPath<MapCell, DefaultEdge>? {
        val paths = shortestPath.getPaths(from)
        return paths.getPath(to)
    }

    class RoutePattern(
        val from: MapLoc,
        val to: MapLoc,
        val transform: List<MapCell>) {

        fun match(fromT: MapCell, toT: MapCell): Boolean =
            (from == fromT.mapLoc && to == toT.mapLoc)
    }
}