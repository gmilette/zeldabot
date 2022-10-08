package bot.state

import bot.GamePad
import bot.plan.NavUtil
import bot.plan.gastar.GStar
import sequence.DestType
import util.Map2d
import util.d
import java.util.function.Supplier
import kotlin.random.Random


/**
 * it's the map
 */
class Hyrule {
    val map: Map<MapLoc, MapCell>

    init {
        val mapData = MapBuilder().build()
        map = MapMaker.createMapCells(mapData)
    }

    val mapCellsObject: MapCells
        get() = MapCells(map)

    val mapCells: Collection<MapCell>
        get() = map.values

    fun near(from: MapCell, direction: Direction): MapCell? {
        try {
            return when (direction) {
                Direction.Up -> map[from.mapLoc.up]
                Direction.Down -> map[from.mapLoc.down]
                Direction.Left -> map[from.mapLoc.left]
                Direction.Right -> map[from.mapLoc.right]
            }
        } catch (a: ArrayIndexOutOfBoundsException) {
            return null
        }
    }

    fun getMapCell(loc: MapLoc): MapCell {
        return map[loc] ?: MapCell.unknown
    }
}

class MapCellData(
    val name: String,
    val objectives: List<Objective> = emptyList(),
    var exits: ExitSet = ExitSet(),
    // path parameters: if its a room with u shaped traps,
    // path planning better do full plans, if there are
    // no traps, short plan depth. maybe level of danger
    // if the map is cut off top from bottom,
) {
    constructor(
        name: String,
        objective: Objective,
        exits: ExitSet = ExitSet(),
    ) : this(name, listOf(objective), exits)

    constructor(
        name: String,
        objective: Objective,
        objective2: Objective,
        exits: ExitSet = ExitSet(),
    ) : this(name, listOf(objective, objective2), exits)

    companion object {
        val empty = MapCellData("empty")
    }
}

class ExitSet(vararg direction: Direction) {
    private val directions: List<Direction>

    init {
        directions = direction.toList()
    }

    override fun toString(): String {
        return directions.fold("", { R, t: Direction -> "$R ${t.name} " })
    }
}

val ExitSetAll =
    ExitSet(Direction.Right, Direction.Down, Direction.Up, Direction.Left)



class Plan(stepsIn: List<MapCell>) {
    val steps = stepsIn.toMutableList()

    var step: Int = 0

    val current: MapCell
        get() = steps.getOrElse(step) { MapCell.unknown }

    val next: MapCell
        get() = steps.getOrElse(step + 1) { MapCell.end }

    val nextDirection: Direction
        get() = NavUtil.directionToDir(
            current.point.toFrame(), next.point
                .toFrame()
        )

    fun advanceIfComplete(whereAmI: MapCell) {
        val now = current
        if (now == whereAmI) {
//            d { " I am here: $whereAmI" }
        } else {
            d { " switch to here = $whereAmI" }
            // pop until
//            while (steps.removeLast() != whereAmI) {
//                d { " removed one"}
//            }
            step = steps.indexOf(whereAmI)
//            if (next == whereAmI) {
//                d { " got to the next place where=$whereAmI next=$next " +
//                        "step=$step"}
//                step++
//            }
//            val nextOne = current
//            if (nextOne == whereAmI) {
//                d { " went to the correct place where=$whereAmI next=$nextOne"}
//                step++
//            } else {
//                d { " Oh no you should be here!! where=$whereAmI " +
//                        "next=$nextOne step $step"}
//            }
        }
    }
}

class MapRoute() {

}

data class Objective(
    val point: FramePoint,
    val type: DestType,
//    val requires: DestType.ITEM
) {
    constructor(x: Int, y: Int, type: DestType) : this(FramePoint(x, y), type)
}

enum class Direction {
    Left, Right, Up, Down
}

fun Direction.pointModifier(): (FramePoint) -> FramePoint {
    return when (this) {
        Direction.Up -> { p -> FramePoint(p.x, p.y - 1)}
        Direction.Down -> { p -> FramePoint(p.x, p.y + 1)}
        Direction.Left -> { p -> FramePoint(p.x - 1, p.y)}
        Direction.Right -> { p -> FramePoint(p.x + 1, p.y)}
    }
}

val Direction.vertical: Boolean
    get() = this == Direction.Up || this == Direction.Down


class MapCell(
    val point: MapCellPoint,
    val mapLoc: MapLoc,
    val mapData: MapCellData,
    val passable: Map2d<Boolean> = Map2d(emptyList())
) {
    override fun toString(): String {
        return "Map $mapLoc $point"
    }

    companion object {
        val unknown = MapCell(MapCellPoint(0, 0), 0, MapCellData.empty)
        val end = MapCell(MapCellPoint(-1, -1), 0, MapCellData.empty)
    }

    val exits = mutableMapOf<Direction, MutableList<FramePoint>>()
    val gstar: GStar = GStar(passable)

    val exitNames: String
        get() {
            return exits.keys.fold("") { R, t: Direction ->
                "$R ${
                    t
                        .name
                }:${exits[t]?.size ?: -1} "
            }
        }

    fun anyExit(): FramePoint? {
        var exit: FramePoint? = null
        exits.forEach { t, u ->
            val index = u.size / 2
            exit = u[index]
        }
        return exit
    }

    fun exitFor(dir: Direction): FramePoint? {
        val exitPoints = exits[dir] ?: return null
        // has to be the top of the available points
        // can be anything between top
        // as long as there is 16 beneath it. oy, i'll just pick
        // first one for now
//        val index = exitPoints.size / 2
//        return exitPoints[index]
        return exitPoints.firstOrNull()
    }

    fun exitsFor(dir: Direction): List<FramePoint>? {
        return exits[dir]
    }

    val costImpassible = 99999.0
    val costPassable = 1.0
    // impassible = 99999
    // passible = 1 (non-zero cost)
    // near enemy = +

//    private val aStar: AStarShortestPath<FramePoint, DefaultEdge>

    init {
        findExits()
//        val start = System.currentTimeMillis()
////        val g: Graph<FramePoint, DefaultEdge> =
////            DefaultDirectedGraph(DefaultEdge::class.java)
//        val g =
//            GraphTypeBuilder.directed<FramePoint, DefaultEdge>()
//                .allowingMultipleEdges(false)
//                .allowingSelfLoops(false).weighted(true).edgeClass(
//                    DefaultEdge::class.java
//                ).vertexSupplier(FrameV())
//        .buildGraph()
//
//        val generator = GridGraphGenerator<FramePoint, DefaultEdge>(256, 88)
//        generator.generateGraph(g)
//        val cost =
//            AStarAdmissibleHeuristic<FramePoint> { sourceVertex, targetVertex ->
//                // no this is just the total distance to the goal from this
//                // source
//                // if target vertext is passable
//                val passable = passable.get(targetVertex)
//                if (passable) costPassable else costImpassible
//            }
//        aStar = AStarShortestPath(g, cost)
//        val total = System.currentTimeMillis() - start
//        d { " total time $total"}
    }

    private fun findExits() {
        if (passable.empty || this == unknown) {
            return
        }
        Direction.values().forEach {
            exits[it] = mutableListOf()
        }
        val debug = false
        if (debug) d { " exits for ${this.mapLoc}" }
        for (x in 0..MapConstants.MAX_X) {
            if (passable.get(x, 10)) {
                if (debug) d { " top exit: $x" }
                exits[Direction.Up]?.add(FramePoint(x, 0))
            }
            if (passable.get(x, MapConstants.MAX_Y - 10)) {
                if (debug) d { " bottom exit: $x" }
                exits[Direction.Down]?.add(FramePoint(x, MapConstants.MAX_Y))
            }
        }

        for (y in 0..MapConstants.MAX_Y) {
            if (passable.get(10, y)) {
                if (debug) d { " left exit: $y" }
                exits[Direction.Left]?.add(FramePoint(0, y))
            }
            if (passable.get(MapConstants.MAX_X - 10, y)) {
                if (debug) d { " right exit: $y" }
                exits[Direction.Right]?.add(FramePoint(MapConstants.MAX_X, y))
            }
        }
    }

//    fun path(from: FramePoint, to: FramePoint): List<FramePoint> {
//        return aStar.getPath(from, to).vertexList
//    }


}

class FrameV : Supplier<FramePoint> {
    override fun get(): FramePoint {
        return FramePoint(Random.nextInt(100000), Random.nextInt(100000))
    }
}

data class ZMapTile(val passable: Boolean) {
    fun up(map: MapCell): ZMapTile {
        return ZMapTile(true)
    }
}
