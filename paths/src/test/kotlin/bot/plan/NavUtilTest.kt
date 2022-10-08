package bot.plan

import bot.GamePad
import bot.plan.gastar.GStar
import bot.plan.gastar.SkipLocations
import bot.state.*
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
                      dirResult: GamePad) {
        val hyrule = Hyrule()

        val cell = hyrule.getMapCell(cell)

        val pass = cell.passable.get(107 ,10)
        d { " pass $pass"}

        val gstar = GStar(cell.passable)

        cell.passable.write("check_1192") { v, x, y ->
            when {
                (x == from.x && y == from.y) -> "L"
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

        //
        cell.passable.write("check_1192_r") { v, x, y ->
            val pt = FramePoint(x, y)
            when {
                route.any { it.x == x && it.y == y } -> "Z"
                v -> "."
                else -> "X"
            }
        }

        route shouldNotBe null
        route.size shouldBeGreaterThan 1
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
        check(55, FramePoint(224 ,72), FramePoint(255,72), GamePad.MoveRight)
    }

    @Test
    fun `test move 40`() {
        // should go up or at least right
//        check(56, FramePoint(0, 72), FramePoint(112,0), GamePad.MoveRight)

        //40
//        check(40, FramePoint(112, 108), FramePoint(112,0), GamePad.MoveRight)

        GStar.DEBUG = true
        //check(55, FramePoint(112, 88), FramePoint(112,64), GamePad.MoveUp)
//        42 target (152, 100) link (104, 98)
        check(42, FramePoint(152, 100), FramePoint(104,98), GamePad.MoveUp)
    }

    @Test
    fun `test move 44`() {
        GStar.DEBUG = true
        //check(55, FramePoint(112, 88), FramePoint(112,64), GamePad.MoveUp)
//        42 target (152, 100) link (104, 98)
        check(44, FramePoint(144, 120), FramePoint(144, 104), GamePad.MoveUp)
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