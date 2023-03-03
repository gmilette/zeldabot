package bot.plan

import bot.GamePad
import bot.plan.action.NavUtil
import bot.plan.astar.AStar
import bot.plan.gastar.GStar
import bot.state.*
import bot.state.map.Hyrule
import bot.state.map.MapCell
import bot.state.map.MapCellData
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Ignore
import org.junit.Test
import util.Map2d
import util.d

class NavUtilTest {
    @Ignore
    @Test
    fun `test g`() {
        val map = Map2d.Builder<Boolean>().add(
            100, 100, true
        ).build()

        val cell = MapCell(MapCellPoint(0,0), 0, MapCellData.empty)

        val gstar = GStar(map)
        gstar.setEnemy(FramePoint(20, 20), 10)
        gstar.setEnemy(FramePoint(30, 15), 10)
//        gstar.setEnemy(FramePoint(40, 25), 1)

        gstar.route(FramePoint(10, 20), FramePoint(45, 20))
    }

    @Test
    fun `test g in cell`() {
        val hyrule = Hyrule()

        val cell = hyrule.getMapCell(119)

        val gstar = GStar(cell.passable)

        val target = FramePoint(255, 80)
        val fromNearExit = FramePoint(225, 80)

        val route = gstar.route(fromNearExit, target)[1]
        route shouldNotBe null
        NavUtil.directionTo(fromNearExit, route) shouldBe GamePad.MoveRight
    }

    @Test
    fun `test g in wall`() {
        val hyrule = Hyrule()

        val cell = hyrule.getMapCell(119)

        val gstar = GStar(cell.passable)

        val target = FramePoint(255, 80)
//        val from = FramePoint(128-1, 49)
        var from = FramePoint(184, 80)

        val mark = FramePoint(207,80)
        from = mark

        cell.passable.write("check_1192") { v, x, y ->
            when {
                (x == mark.x && y == mark.y) -> "*"
                (x == mark.rightEnd.x && y == mark.rightEnd.y) -> "*"
                (x == mark.leftDown.x && y == mark.leftDown.y) -> "*"
                (x == mark.rightEndDown.x && y == mark.rightEndDown.y) -> "*"
                v -> "."
                else -> "X"
            }
//            when {
//                (x == mark.x && y == mark.y) -> "*"
//                (x == mark.rightEnd.x && y == mark.rightEnd.y) -> "*"
//                (x == mark.leftDown.x && y == mark.leftDown.y) -> "*"
//                (x == mark.downEndRight.x && y == mark.downEndRight.y) -> "*"
//                v -> "."
//                else -> "X"
//            }
        }

        val route = gstar.route(from, target)
        route shouldNotBe null
        route shouldHaveAtLeastSize 2
        NavUtil.directionTo(from, route[1]) shouldBe GamePad.MoveDown
    }

    private fun check(cell: MapLoc, from: FramePoint, target: FramePoint,
                      dirResult: GamePad, level: Int = 0) {
        checkA(cell, from, listOf(target), dirResult, level)
    }
    private fun checkA(cell: MapLoc, from: FramePoint, targets: List<FramePoint>,
                      dirResult: GamePad, level: Int = 0) {
        val hyrule = Hyrule()
        val cell = if (level == 0) hyrule.getMapCell(cell) else hyrule.levelMap.cell(level, cell)
        cell.write()

        val pass = cell.passable.get(107 ,10)
        d { " pass $pass"}

        val gstar = GStar(cell.passable)

        val target = targets.get(0)

//        cell.gstar.initialMap.write("TESTORIGINAMAP.csv") {  v, x, y ->
//            v.toString()
//        }

        cell.passable.write("check_1192") { v, x, y ->
            when {
                (x == 115 && y == 104) -> "M"
                (x == target.x && y == target.y) -> "T"
                (x == from.x && y == from.y) -> "L"
                (x == from.justMid.x && y == from.justMid.y) -> "M"
                (x == from.justMidEnd.x && y == from.justMidEnd.y) -> "P"
                (x == from.justRightEnd.x && y == from.justRightEnd.y) -> "R"
                (x == from.justLeftDown.x && y == from.justLeftDown.y) -> "D"
                (x == from.justRightEndBottom.x && y == from
                    .justRightEndBottom.y) -> "Z"
                v -> { "."
//                    if (SkipLocations.hasPt(FramePoint(x, y))) {
//                        "S"
//                    } else {
//                        "."
//                    }
                }
                else -> "X"
            }
        }

        d { " done writing "}

        val route = gstar.route(from, target)

        val firstPt = route.get(0)
        cell.passable.write("check_1192_r") { v, x, y ->
            val pt = FramePoint(x, y)
            when {
                (x == target.x && y == target.y) -> "T"
                route.any { it.x == x && it.y == y && it.x == firstPt.x && it.y == firstPt.y } -> "S"
                route.any { it.x == x && it.y == y } -> "Z"
                v -> "."
                else -> "X"
            }
        }

        route shouldNotBe null
        route.size shouldBeGreaterThan 1
//        d { " Route: $firstPt then ${route[1]} c ${cell.passable.get(FramePoint(122, 79).justRightEnd)}"}
        NavUtil.directionTo(from, route[1]) shouldBe dirResult
    }

    @Test
    fun `test g in up`() {
        val f = FramePoint(100, 100)
        f.down.y shouldBe 101
        f.downEnd.y shouldBe 100 + 16 + 1
        f.up.y shouldBe 99
    }

    @Test
    fun `test move`() {
        //FramePoint(48 ,5
        //106 ,128)
//        check(119, FramePoint(106 ,128), FramePoint(48,5), GamePad.MoveLeft)
//        check(120, FramePoint(106 ,128), FramePoint(48,1), GamePad.MoveUp)

        //(40 64) to (255 81)
//        check(119, FramePoint(40 ,64), FramePoint(255,80), GamePad.MoveRight)

        // leave level 1 but up too high
        GStar.DEBUG = true
//        32, 128
//        check(55, FramePoint(224 ,72), FramePoint(255,72), GamePad.MoveRight)
    }

    @Test
    fun `test move 40`() {
        //32, 96) link (56, 120
        // should go up or at least right
//        check(56, FramePoint(0, 72), FramePoint(112,0), GamePad.MoveRight)

        //40
//        check(40, FramePoint(112, 108), FramePoint(112,0), GamePad.MoveRight)

        GStar.DEBUG = true
        //check(55, FramePoint(112, 88), FramePoint(112,64), GamePad.MoveUp)
//        42 target (152, 100) link (104, 98)
        check(13, FramePoint(56, 120), FramePoint(32, 96), GamePad.MoveUp, level = 7)
    }

    @Test
    fun `test move 45`() {
        //(64, 24) to next (0, 0) [(80, 16)
        GStar.DEBUG = true
        //check(55, FramePoint(112, 88), FramePoint(112,64), GamePad.MoveUp)
//        42 target (152, 100) link (104, 98)
        // below the open hole, it just wont navigate
//        check(44, FramePoint(140, 112), FramePoint(143, 104), GamePad.MoveUp)
//        check(44, FramePoint(144, 112), FramePoint(144, 104), GamePad.MoveUp)
//        148, 128) to (147, 112) at 44

        // cant get in //144, 104
        check(45, FramePoint(64, 24), FramePoint(80, 16), GamePad.MoveRight)
    }

    @Test
    fun `test move level 2`() {
        GStar.DEBUG = true
//        check(126, FramePoint(128, 32), FramePoint(128, 0), GamePad.MoveUp, level = 2)
        check(78, FramePoint(120, 32), FramePoint(120, 0), GamePad.MoveUp, level = 2)
//        go to from (142, 56) to next (143, 56) [(120, 0), (121, 0), (122, 0), (123, 0), (124, 0), (125, 0), (126, 0), (127, 0), (128, 0)
//        , (129, 0), (130, 0), (131, 0), (132, 0), (133, 0), (134, 0), (135, 0)]

        val targets = listOf(FramePoint(120, 0), FramePoint(121, 0), FramePoint(122, 0),
            FramePoint(123, 0), FramePoint(124, 0), FramePoint(125, 0),
            FramePoint(126, 0), FramePoint(127, 0), FramePoint(128, 0),
            FramePoint(129, 0), FramePoint(130, 0),
            FramePoint(131, 0), FramePoint(132, 0), FramePoint(133, 0),
            FramePoint(134, 0), FramePoint(135, 0))
        checkA(109, FramePoint(147, 56), targets, GamePad.MoveUp, level = 2)
    }

    @Test
    fun `test move level 4`() {
        GStar.DEBUG = true
//        check(126, FramePoint(128, 32), FramePoint(128, 0), GamePad.MoveUp, level = 2)
//        check(49, FramePoint(208, 80), FramePoint(71, 97), GamePad.MoveUp, level = 4)
        //(71, 97) link (208, 80

        // stuck in middle
//(122, 80) to next (121, 80) [(255, 80), (255, 81), (255, 82), (255, 83), (255, 84), (255, 85), (255, 86), (255, 87), (255, 88), (255, 89), (255, 90), (255, 91), (255, 92), (255, 93), (255, 94), (255, 95)]
        check(49, FramePoint(122, 80), FramePoint(255, 80), GamePad.MoveLeft, level = 4)
        // can
    }
        @Test
    fun `test move level 1`() {
        GStar.DEBUG = true

        // not sure where going but half way up?
//        check(34, FramePoint(101, 104), FramePoint(96, 96), GamePad.MoveRight, level = 1)

//        val goals = listOf(FramePoint(120, 0), FramePoint(121, 0), FramePoint(122, 0),
//            FramePoint(123, 0), FramePoint(124, 0), FramePoint(125, 0), FramePoint(126, 0),
//            FramePoint(127, 0), FramePoint(128, 0), FramePoint(129, 0),
//            FramePoint(130, 0),
//            FramePoint(131, 0), FramePoint(132, 0), FramePoint(133, 0), FramePoint(134, 0)
//        )
        //131, 23
        //130, 23
        //129, 23
//        checkA(35, FramePoint(85, 88), listOf(FramePoint(136, 64), FramePoint(173, 64)), GamePad.MoveUp, level = 1)
//        checkA(99, FramePoint(136, 32), listOf(FramePoint(120, 0), FramePoint(173, 64)), GamePad.MoveUp, level = 1)

//        check(35, FramePoint(64, 87), FramePoint(0, 80), GamePad.MoveUp, level = 1)

        // stuck in the door34 (1, 80) to next (0, 0)
//        check(53, FramePoint(128, 128), FramePoint(127, 80), GamePad.MoveUp, level = 1)

        //128, 64
//        check(83, FramePoint(128, 64), FramePoint(130, 61), GamePad.MoveUp, level = 1)
        //64, 56
        check(99, FramePoint(121, 32), FramePoint(121, 0), GamePad.MoveUp, level = 1)
    }

    @Test
    fun `test hyrule`() {
        val hyrule = Hyrule()
        val level = 1
        val loc = 115
        val cell = if (level == 0) hyrule.getMapCell(loc) else hyrule.levelMap.cell(level, loc)
        val a = 1

    }

    @Test
    fun `test move shop`() {
        GStar.DEBUG = true
        // exit
//        check(58, FramePoint(144, 90), FramePoint(118, 167), GamePad.MoveDown)
        // move up to target
//        check(58, FramePoint(150, 120), FramePoint(152, 90), GamePad.MoveUp)
//
//
//        Debug: (Kermit)  exit -> (112, 167)
//        Debug: (Kermit)  exit -> (113, 167)
//        Debug: (Kermit)  exit -> (114, 167)
//        Debug: (Kermit)  exit -> (115, 167)
//        Debug: (Kermit)  exit -> (116, 167)
//        Debug: (Kermit)  exit -> (117, 167)
//        Debug: (Kermit)  exit -> (118, 167)
//        Debug: (Kermit)  exit -> (119, 167)
//        Debug: (Kermit)  exit -> (120, 167)
//        Debug: (Kermit)  exit -> (121, 167)
//        Debug: (Kermit)  exit -> (122, 167)
//        Debug: (Kermit)  exit -> (123, 167)
//        Debug: (Kermit)  exit -> (124, 167)
//        Debug: (Kermit)  exit -> (125, 167)
//        Debug: (Kermit)  exit -> (126, 167)
//        Debug: (Kermit)  exit -> (127, 167)
//        Debug: (Kermit)  exit -> (128, 167)
//        Debug: (Kermit)  exit -> (129, 167)
//        Debug: (Kermit)  exit -> (130, 167)
//        Debug: (Kermit)  exit -> (131, 167)
//        Debug: (Kermit)  exit -> (132, 167)
//        Debug: (Kermit)  exit -> (133, 167)
//        Debug: (Kermit)  exit -> (134, 167)
//        Debug: (Kermit)  exit -> (135, 167)
//        Debug: (Kermit)  exit -> (136, 167)
//        Debug: (Kermit)  exit -> (137, 167)
//        Debug: (Kermit)  exit -> (138, 167)
//        Debug: (Kermit)  exit -> (139, 167)
//        Debug: (Kermit)  exit -> (140, 167)
//        Debug: (Kermit)  exit -> (141, 167)
//        Debug: (Kermit)  exit -> (142, 167)
//        Debug: (Kermit)  exit -> (143, 167)
        // keeps switching
        //(143, 96)
        //144,98 to 144,96
        // 144,96 go Left
        // 143,96 go down
//        check(58, FramePoint(143, 96), FramePoint(143, 167), GamePad.MoveDown)

//        check(58, FramePoint(144, 96), FramePoint(143, 167), GamePad.MoveDown)
//        check(58, FramePoint(143, 96), FramePoint(143, 167), GamePad.MoveDown)

//        check(58, FramePoint(111, 96), FramePoint(112, 167), GamePad.MoveDown)
//        check(58, FramePoint(112, 96), FramePoint(112, 167), GamePad.MoveDown)

        check(58, FramePoint(144, 88), FramePoint(114, 167), GamePad.MoveDown)
        //        148, 128) to (147, 112) at 44
    }

     //40 from (0, 72) to (112, 0)
    //    56,72
    @Test
    fun `test directions`() {
        // generate simple 50x250 grid
        val map = Map2d.Builder<Boolean>().add(
            100, 250, true
        ).build()

        val cell = MapCell(MapCellPoint(0,0), 0, MapCellData.empty)

        NavUtil.directionToAvoidingObstacleZ(cell, FramePoint(200, 20),
            FramePoint(50, 20)) shouldBe GamePad.MoveLeft
        NavUtil.directionToAvoidingObstacleZ(cell, FramePoint(50, 20),
            FramePoint(200, 20)) shouldBe GamePad.MoveRight
        NavUtil.directionToAvoidingObstacleZ(cell, FramePoint(20, 20),
            FramePoint(20, 40)) shouldBe GamePad.MoveDown
        NavUtil.directionToAvoidingObstacleZ(cell, FramePoint(20, 40),
            FramePoint(20, 20)) shouldBe GamePad.MoveUp

        d { " do a star"}
        AStar().aStarFinder(cell, FramePoint(200, 20),
            FramePoint(50, 20))
    }

    @Test
    fun `test manhattan`() {
        // generate simple 50x250 grid
        val map = Map2d.Builder<Boolean>().add(
            100, 250, true
        ).build()

        val cell = MapCell(MapCellPoint(0,0), 0, MapCellData.empty)

        val path = NavUtil.manhattanPathFinder(cell, FramePoint(200, 20),
            FramePoint(50, 20))

//        path[0].x shouldBe 199
    }



    @Test
    fun `test do stuff`() {
        val hyrule = Hyrule()
        // 1, 2, 3
        // 4, 5, 6
        val mapInt = Map2d(
            mutableListOf(mutableListOf(true,true, true), mutableListOf(true, true,
            true))
        )
//        val mapB = Map2d(mapInt)
//        val cell = MapCell(MapCellPoint(0, 0), 0,
//            map
//        )
        val cell = hyrule.getMapCell(103)
        cell.passable.write("cell_${cell.mapLoc}") { v, _, _ ->
            if (v) "X" else "."
        }

        // map passable to 1

        // route from 1 point to another



    }
}