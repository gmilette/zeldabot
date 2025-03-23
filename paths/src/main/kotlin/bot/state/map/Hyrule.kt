package bot.state.map

import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.destination.DestType
import bot.state.map.level.LevelMapCellsLookup
import bot.state.map.level.LevelStartMapLoc
import util.Map2d
import util.d
import java.io.File


/**
 * it's the map
 */
class Hyrule {
    val map: Map<MapLoc, MapCell>

    val levelMap = LevelMapCellsLookup()

    val level1EntranceCell: MapCell
        get() = levelMap.cell(1, LevelStartMapLoc.lev(1))

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
    val attributes: Set<MapCellAttribute> = emptySet(),
    // path parameters: if its a room with u shaped traps,,
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

data class Objective(
    // how to enter the location
    val point: FramePoint = FramePoint(),
    val type: DestType,
    // like which part of the shop to go to
    val itemLoc: ItemLoc = ItemLoc.Center
//    val requires: DestType.ITEM
) {
    constructor(x: Int, y: Int, type: DestType) : this(FramePoint(x, y), type)

    companion object {
        val empty = Objective(FramePoint(), DestType.None)
    }

    // if in a level
    enum class ItemLoc(val point: FramePoint) {
        None(FramePoint(0, 0)),
        Enter(FramePoint(120, 100)),
        Left(FramePoint(88, 88-2)),
        Center(FramePoint(120, 88-2)),
        Right(FramePoint(152, 96-2))
    }
}

enum class MapCellAttribute {
    None,
    NoAttack,
    HasBombEnemies,
    SlowForEnemiesToAppear
}

class MapCell(
    val point: MapCellPoint,
    val mapLoc: MapLoc,
    val mapData: MapCellData,
    val passable: Map2d<Boolean> = Map2d(emptyList()),
    halfPassable: Boolean = true,
    isLevel: Boolean = false,
) {
    override fun toString(): String {
        return "Map $mapLoc $point"
    }

    companion object {
        val unknown = MapCell(MapCellPoint(0, 0), 0, MapCellData.empty)
        val end = MapCell(MapCellPoint(-1, -1), 0, MapCellData.empty)
    }

    val exits = mutableMapOf<Direction, MutableList<FramePoint>>()
    val zstar: ZStar = ZStar(passable, halfPassable, isLevel)

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
        exits.forEach { (t, u) ->
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

    init {
        findExits()
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
