package bot.plan

import bot.plan.action.PushDirection
import bot.state.*
import bot.state.map.MapConstants
import bot.state.map.andAHalf
import bot.state.map.grid


object InLocations {
    val topMiddleBombSpot = FramePoint(120, 32)
    val bombRight = FramePoint(13.grid - 5, 5.grid)
    val bombRightExactly = FramePoint(13.grid, 5.grid)
    val bombLeft = FramePoint(2.grid + 5, 5.grid)
    val diamondLeftTopPush = FramePoint(96, 64 )
    val diamondLeftBottomPush = FramePoint(96, 96)
    val diamondLeft = FramePoint(96, 96).upOneGrid
    val getItem = FramePoint(135, 80) // right side
    val getOutRight = FramePoint(12.grid, 2.grid) // right side
    val getOutLeft = FramePoint(3.grid, 1.grid) // right side
    val rightStair = FramePoint(209, 80)
    val middleStair = FramePoint(128, 80) //8x5 -- cant nav here?
    val rightStairGrid = FramePoint(13.grid, 5.grid)
    val cornerStairs = FramePoint(13.grid, 2.grid)

    enum class StairsLocation(val point: FramePoint) {
        corner(FramePoint(13.grid, 2.grid)),
        center(FramePoint(128, 80)),
        right(FramePoint(209, 80)),
        rightStairGrid(FramePoint(13.grid, 5.grid))
    }

    enum class OutLocation(val point: FramePoint) {
        item(FramePoint(135, 80)), // right side
        outRight(FramePoint(12.grid, 2.grid)), // right side
        outLeft(FramePoint(3.grid, 1.grid)) // right side
    }

    enum class Push(val point: FramePoint,
                    val dir: PushDirection,
                    val position: FramePoint,
                    val needAway: Boolean = false,
                    val ignoreProjectiles: Boolean = false,
                    val highCost: List<FramePoint> = emptyList()) {
        statue(FramePoint(7.grid, 5.grid), PushDirection(true, true), FramePoint(3.grid, 8.grid), needAway = true),
        singleLeft(FramePoint(7.grid, 5.grid), PushDirection(true, true), FramePoint(3.grid, 8.grid), needAway = true),
        moveLeftOfTwo(
            FramePoint(6.grid, 5.grid),
            PushDirection(true, true),
            FramePoint(3.grid, 8.grid),
            needAway = true,
            // each could be where link pushed
            highCost = listOf()),
        //                FramePoint(6.grid, 5.grid).upOneGrid,
//                FramePoint(6.grid, 5.grid).downOneGrid,
//                FramePoint(6.grid, 5.grid).leftOneGrid,
//                FramePoint(6.grid, 5.grid).rightOneGrid
//            )),
        diamondLeft(
            FramePoint(6.grid, 5.grid),
            PushDirection(false, true),
            position = FramePoint(5.grid, 7.grid),
            ignoreProjectiles = true,
            highCost = listOf(FramePoint(5.grid, 5.grid))),
        right(
            FramePoint(12.grid, 5.grid),
            // maybe push only from right
            PushDirection(horizontal = true, vertical = false),
            position = FramePoint(3.grid, 5.grid),
            highCost = listOf(FramePoint(12.grid, 5.grid).rightOneGrid,
                FramePoint(12.grid, 5.grid).rightOneGrid.upOneGrid,
                FramePoint(12.grid, 5.grid).rightOneGrid.upOneGrid.upOneGrid),
            ignoreProjectiles = true
        ),
        none(
            FramePoint(),
            PushDirection(horizontal = false, vertical = false),
            position = FramePoint()
        )
    }

    object Overworld {
        val shopRightItem = FramePoint(152, 96) // 97 failed to get heart
        //        val selectHeart = FramePoint(152, 90) // still at location 44
        val centerItem = FramePoint(118, 88) // not 96
        val centerItemLetter = FramePoint(120, 88)
        val shopHeartItem = FramePoint(152, 96)
        val shopLeftItem = FramePoint(88, 88)

        val start: MapLoc = 119
    }

    object BombDirection {
        val right = FramePoint(200, 92) //?
        val left = FramePoint(2.grid + 2, 5.grid) //?
    }
}

object Phases {
    val level2Harvest = "level 2 harvest"
    val reenterLevel2 = "Reenter level 2"
    val grabHearts = "grab hearts"
    val forest30 = "forest 30"
    val level3 = "level 3"
    val level3After = "level 3"
    val afterLevel6 = "after level 6"
    val level4 = "level 4"
    val level5 = "level 5"
    val level6 = "level 6"
    val level7 = "level 7"
    val level8 = "level 8"
    val level9 = "level 9"
    val level9before = "level 9before"
    val ladderHeart = "ladder heart"
    fun lev(level: Int) = "Destroy level $level"

    object Segment {
        val lev2Boss = "kill boss"
        val lev6End = "level6 end"
    }
}