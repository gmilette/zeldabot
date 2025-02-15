package bot.state.map.level

import bot.plan.zstar.ZStar
import bot.state.FramePoint
import bot.state.MapCellPoint
import bot.state.MapLoc
import bot.state.isTopRightCorner
import bot.state.map.*
import util.Map2d

class LevelCellBuilder {
    companion object {
        val passable = "."
        val notPassable = "X"
    }

    private val templates = LevelMapTemplateReader()

    fun level1(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel1())

    fun level2(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel2())

    fun level3(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel3())

    fun level4(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel4())

    fun level5(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel5())

    fun level6(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel6())

    fun level7(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel7())

    fun level8(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel8())

    fun level9(): MapCells =
        buildLevels(LevelSpecBuilder().buildLevel9())

    private fun buildLevels(spec: List<LevelSpec>): MapCells {
        val mapCells = mutableMapOf<MapLoc, MapCell>()
        for (lev in spec) {
            val mapCell = buildLevel(lev)
            mapCells[lev.loc] = mapCell
        }
        return MapCells(mapCells)
    }

    private fun buildLevel(spec: LevelSpec): MapCell {
        // rows of X's
        // add the outside part
        // to the inside part
        val template = templates.templates[spec.template.name] ?: return MapCell.unknown

        val level = mutableListOf<String>()

        val top1 = if (spec.isGetItem) rowGetItem() else row(spec.exits.has(Direction.Up))
        val top2 = if (spec.isGetItem) rowGetItem() else row(spec.exits.has(Direction.Up))
        level.add(top1)
        level.add(top2)

        level.addAll(template.map.mapIndexed { i, v ->
            fill(i, v, spec.exits)
        })

        level.add(row(spec.exits.has(Direction.Down)))
        level.add(row(spec.exits.has(Direction.Down)))

        // have to add the left right!

        // todo, need to test this function
        val passable = toPass(level)
//        val typed = toTyped(level)

        // getItem true,
        return MapCell(MapCellPoint(0, 0), spec.loc, MapCellData(spec.name, Objective.empty, exits = ExitSet()), passable,
            halfPassable = !spec.isGetItem && spec.isHalfPassable,
            isLevel = true)
    }

    private fun fill(index: Int, fill: String, exits: ExitSet): String =
        if (index.isTemplateExit && exits.hasAtLeastOneHorizontal) {
            if (exits.hasHorizontal) {
                "..${fill}.."
            } else if (exits.has(Direction.Right)) {
                "XX${fill}.."
            } else {
                "..${fill}XX"
            }
        } else {
            "XX${fill}XX"
        }

    private val Int.isExit: Boolean
        get() = (this == 7 || this == 8) // it's only half of 7, 8 though

    private val Int.isTemplateExit: Boolean
        get() = (this == 3)

    private fun row(withExit: Boolean): String {
        var row = ""
        repeat(16) {
            row += when {
//                (withExit && it.isExit && it == 7) -> "R"
//                (withExit && it.isExit && it == 8) -> "L"
                (withExit && it == 7) -> "R"
                (withExit && it == 8) -> "L"
                else -> "X"
            }
        }
        return row
    }

    private fun rowGetItem(): String {
        var row = ""
        repeat(16) {
            row += when {
                it == 3 -> "."
                it == 12 -> "."
                else -> "X"
            }
        }
        return row
    }

    fun toPass(rows: List<String>): Map2d<Boolean> {
        //map { it.asSequence().map { it != 'X' }
        // 16 trues
        val passableCell = mutableListOf<Boolean>()
        val notPassableCell = mutableListOf<Boolean>()
        val halfPassableCellLeft = mutableListOf<Boolean>()
        val halfPassableCellRight = mutableListOf<Boolean>()
        repeat(16) {
            passableCell.add(true)
            notPassableCell.add(false)
        }
        repeat(8) {
            halfPassableCellLeft.add(true)
            halfPassableCellRight.add(false)
        }
        repeat(8) {
            halfPassableCellLeft.add(false)
            halfPassableCellRight.add(true)
        }

//        val passableCellC = passableCell.map { 'P' }
//        val passableCellW = passableCell.map { 'W' }
//        val notPassableCellC = notPassableCell.map { 'X' }
//        // no
//        val halfPassableCellLeftC = notPassableCell.map { 'X' }
//        val halfPassableCellRightC = notPassableCell.map { 'X' }

        val longRows = mutableListOf<MutableList<Boolean>>()
//        val longRowsType = mutableListOf<MutableList<Char>>()
        var rowCt = 0
        for (row in rows) {
            val longRow = mutableListOf<Boolean>()
            val longPassableRow = mutableListOf<Boolean>()
//            val longRowC = mutableListOf<Char>()
            for (c in row) {
                longPassableRow.addAll(passableCell)
                longRow.addAll(
                    when (c) {
                        'W' -> notPassableCell
                        '.' -> passableCell
                        'R' -> halfPassableCellRight
                        'L' -> halfPassableCellLeft
                        else -> notPassableCell
                    }
                )
//                longRowC.addAll(
//                    when (c) {
//                        'W' -> passableCellW
//                        '.' -> passableCellC
//                        'R' -> halfPassableCellRightC
//                        'L' -> halfPassableCellLeftC
//                        else -> notPassableCellC
//                    }
//                )
            }
            // if it is the last row then half right?
            repeat(if (rowCt == 10) 8 else 16) {
                longRows.add(longRow)
            }
//                repeat(if (rowCt == 10) 4 else 8) {
//                    longRows.add(longRow)
//                }
//                repeat(if (rowCt == 10) 4 else 8) {
//                    longRows.add(longPassableRow)
//                }
            rowCt++
        }
        val passMap = Map2d(longRows.toList())
//> 128,119*C  -> 128,120*  -> 129,120*  -> 1
        return if (ZStar.DO_MAKE_CORNERS) {
            passMap.mapXy { y, x ->
                val pt = FramePoint(x, y)
                when {
                    passMap.get(pt) -> true
                    else -> pt.isTopRightCorner
                }
            }
        } else {
            passMap
        }
    }
}