package bot.state.map

import bot.state.GamePad
import bot.plan.action.NavUtil
import bot.plan.gastar.GStar
import bot.state.*
import bot.state.map.level.LevelMapCellsLookup
import sequence.DestType
import util.Map2d
import util.d
import java.io.File


/**
 * it's the map
 */
class Hyrule {
    val map: Map<MapLoc, MapCell>

    val levelMap = LevelMapCellsLookup()

    val shopMapCell: MapCell
        get() = getMapCell(58)

    init {
        d { " Create Hyrule "}
        val mapData = MapBuilder().build()
        map = MapMaker.createMapCells(mapData)
    }

    val mapCellsObject: MapCells
        get() = MapCells(map)

    val mapCells: Collection<MapCell>
        get() = map.values

    fun mapCellsFor(vararg locs: MapLoc) =
        locs.map { getMapCell(it) }

    fun near(from: MapCell, direction: Direction): MapCell? {
        try {
            val next = when (direction) {
                Direction.Up -> map[from.mapLoc.up]
                Direction.Down -> map[from.mapLoc.down]
                Direction.Left -> map[from.mapLoc.left]
                Direction.Right -> map[from.mapLoc.right]
                Direction.None -> map[from.mapLoc]
            }
            return if (next == null) {
                null
            } else {
                if (next.mapLoc < 0 || next.mapLoc > 127) null else next
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
    val name: String = "",
    val objective: Objective = Objective.empty,
    // path parameters: if its a room with u shaped traps,
    // path planning better do full plans, if there are
    // no traps, short plan depth. maybe level of danger
    // if the map is cut off top from bottom,
    val exits: ExitSet = ExitSet() // unused
) {
    companion object {
        val empty = MapCellData("empty")
    }
}

class ExitSet(vararg direction: Direction) {
    private val directions: List<Direction>

    init {
        directions = direction.toList()
    }

    val hasHorizontal: Boolean
        get() = has(Direction.Right) && has(Direction.Left)

    val hasAtLeastOneHorizontal: Boolean
        get() = has(Direction.Right) || has(Direction.Left)
    fun has(dir: Direction) = directions.contains(dir)

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
    // how to enter the location
    val point: FramePoint,
    val type: DestType,
    // like which part of the shop to go to
    val itemLoc: ItemLoc = ItemLoc.Center
//    val requires: DestType.ITEM
) {
    constructor(x: Int, y: Int, type: DestType) : this(FramePoint(x, y), type)

    companion object {
        val empty = Objective(FramePoint(), DestType.None)
    }

//    val shopRightItem = FramePoint(152, 96) // 97 failed to get heart
//    //        val selectHeart = FramePoint(152, 90) // still at location 44
//    val centerItem = FramePoint(118, 88) // not 96
//    val centerItemLetter = FramePoint(120, 88)
//    val shopHeartItem = FramePoint(152, 96)
//    val shopLeftItem = FramePoint(88, 88)

    // if in a level
    enum class ItemLoc(val point: FramePoint) {
        None(FramePoint(0, 0)),
        Enter(FramePoint(120, 100)),
        Left(FramePoint(88, 88-2)),
        Center(FramePoint(120, 88-2)),
        Right(FramePoint(152, 96-2))
    }
}


enum class Direction {
    Left, Right, Up, Down, None;

    companion object {
        val horizontal: List<Direction> = listOf(Direction.Left, Direction.Right)
        val vertical: List<Direction> = listOf(Direction.Up, Direction.Down)
        val all: List<Direction>
            get() = listOf(Up, Right, Down, Left)
    }
}

val Direction.upOrLeft: Boolean
    get() = this == Direction.Up || this == Direction.Left

fun Direction.opposite(): Direction = when (this) {
    Direction.Left -> Direction.Right
    Direction.Right -> Direction.Left
    Direction.Up -> Direction.Down
    Direction.Down -> Direction.Up
    Direction.None -> Direction.None
}

fun Direction.toGamePad(): GamePad = when (this) {
    Direction.Left -> GamePad.MoveLeft
    Direction.Right -> GamePad.MoveRight
    Direction.Up -> GamePad.MoveUp
    Direction.Down -> GamePad.MoveDown
    Direction.None -> GamePad.None
}
fun Direction.pointModifier(adjustment: Int = 1): (FramePoint) -> FramePoint {
    return when (this) {
        Direction.Up -> { p -> FramePoint(p.x, p.y - adjustment) }
        Direction.Down -> { p -> FramePoint(p.x, p.y + adjustment) }
        Direction.Left -> { p -> FramePoint(p.x - adjustment, p.y) }
        Direction.Right -> { p -> FramePoint(p.x + adjustment, p.y) }
        Direction.None -> { p -> FramePoint(p.x, p.y) }
    }
}

val Direction.vertical: Boolean
    get() = this == Direction.Up || this == Direction.Down

val Direction.horizontal: Boolean
    get() = this == Direction.Left || this == Direction.Right

class MapCell(
    val point: MapCellPoint,
    val mapLoc: MapLoc,
    val mapData: MapCellData,
    val passable: Map2d<Boolean> = Map2d(emptyList()),
    halfPassable: Boolean = true,
    isLevel: Boolean = false
) {
    override fun toString(): String {
        return "Map $mapLoc $point"
    }

    companion object {
        val unknown = MapCell(MapCellPoint(0, 0), 0, MapCellData.empty)
        val end = MapCell(MapCellPoint(-1, -1), 0, MapCellData.empty)
    }

    val exits = mutableMapOf<Direction, MutableList<FramePoint>>()
    val gstar: GStar = GStar(passable, halfPassable, isLevel)

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

    fun hasExit(dir: Direction): Boolean =
        exitsFor(dir)?.isNotEmpty() ?: false

    fun exitsFor(dir: Direction): List<FramePoint>? {
        return exits[dir]
    }

    fun firstExit(): List<FramePoint>? = exits.values.firstOrNull()

    fun allExits(): List<FramePoint> = exits.values.flatten()

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
            try {
            if (passable.get(MapConstants.MAX_X - 10, y)) {
                if (debug) d { " right exit: $y" }
                exits[Direction.Right]?.add(FramePoint(MapConstants.MAX_X, y))
            }
            } catch (e: Exception) {
//                d { " IGNORE "}
            }

        }
    }

//    fun path(from: FramePoint, to: FramePoint): List<FramePoint> {
//        return aStar.getPath(from, to).vertexList
//    }

    fun write(dir: String = "") {
        val dirWithPath = if (dir.isNotEmpty()) "$dir${File.separator}" else ""
        passable.write("${dirWithPath}cell_$mapLoc.csv") { v, x, y ->
            when {
                v -> { "." }
                else -> "X"
            }
        }
    }
}
