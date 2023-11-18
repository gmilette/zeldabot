package bot.plan

import bot.plan.action.NavUtil
import bot.plan.zstar.NearestSafestPoint
import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.Hyrule
import bot.state.map.MapCell
import bot.state.map.MapCellData
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import util.Map2d
import util.d

/**
 * unit test to investiate routes
 */
class NavUtilTest {
    //@Ignore
    @Test
    fun `test g`() {
        ZStar.DEBUG = true
        val map = Map2d.Builder<Boolean>().add(
            100, 100, true
        ).build()

        val cell = MapCell(MapCellPoint(0,0), 0, MapCellData.empty)

        val from = FramePoint(10, 24)
        val zstar = ZStar(map)
        zstar.setEnemy(from, FramePoint(20, 24), 10)
//        zstar.setEnemy(from, FramePoint(30, 15), 10)
//        zstar.setEnemy(FramePoint(40, 25), 1)

        val route = zstar.route(from, listOf(FramePoint(45, 24)), null)
    }

    @Test
    fun testN() {
        val hyrule = Hyrule()
        val level = 5
        val loc = 102
        val cell = if (level == 0) hyrule.getMapCell(loc) else hyrule.levelMap.cell(level, loc)

        val start = FramePoint(88, 91)

        val enemies = listOf(
            FramePoint(94, 112),
            FramePoint(110, 32),
//            FramePoint(97, 32)
        )
        cell.zstar.setEnemyCosts(start, enemies)
//        for (enemy in enemies) {
//            cell.zstar.setEnemy(start, enemy)
//        }

        val near = NearestSafestPoint.nearestSafePoints(FramePoint(101, 32), cell.zstar.costsF, cell.zstar.passable)
        val a = 1
    }

    private fun check(cell: MapLoc, from: FramePoint, target: FramePoint,
                      dirResult: GamePad, level: Int = 0, before: FramePoint? = null, makePassable: FramePoint? = null,
                      enemies: List<FramePoint> = emptyList(),
                      ladderSpec: ZStar.LadderSpec? = null
    ) {
        checkA(cell, from, listOf(target), before, dirResult, level, makePassable = makePassable, enemies = enemies, ladderSpec)
    }
    private fun checkA(cell: MapLoc, from: FramePoint, targets: List<FramePoint>,
                       before: FramePoint? = null, dirResult: GamePad, level: Int = 0, makePassable: FramePoint? = null,
                       enemies: List<FramePoint> = emptyList(), ladderSpec: ZStar.LadderSpec? = null) {
        val hyrule = Hyrule()
        val cell = if (level == 0) hyrule.getMapCell(cell) else hyrule.levelMap.cell(level, cell)
        cell.write()

        val passPt = FramePoint(134, 35)
        val inLev = passPt.isInLevelMap
        val pass = cell.passable.get(passPt)
        d { " pass check $inLev $pass level $level lb ${cell.passable.get(passPt.justLeftBottom)}"}

        val zstar = ZStar(cell.passable, halfPassable = false, isLevel = level != 0)

        val target = targets.get(0)

//        cell.zstar.initialMap.write("TESTORIGINAMAP.csv") {  v, x, y ->
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
                v -> {
                    if (FramePoint(x, y).onHighway) {
                        ":"
                    } else {
                        "."
                    }
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

        val passable = mutableListOf<FramePoint>()
        if (makePassable != null) {
            passable.add(makePassable)
        }

        val route = zstar.route(start = from,
            listOf(target),
            pointBeforeStart = before,
            enemies = enemies,
            forcePassable = passable,
            ladderSpec = ladderSpec)

        val firstPt = route.get(0)
        cell.passable.write("check_1192_r") { v, x, y ->
            val pt = FramePoint(x, y)
            when {
                (x == target.x && y == target.y) -> "T"
                route.any { it.x == x && it.y == y && it.x == firstPt.x && it.y == firstPt.y } -> "S"
                route.any { it.x == x && it.y == y } -> "Z"
                FramePoint(x, y).isTopRightCorner -> "C"
                v -> if (FramePoint(x, y).onHighway) {
                    ":"
                } else {
                    "."
                }
                else -> "X"
            }
        }

        zstar.passable.write("check_1192_rzstar") { v, x, y ->
            val pt = FramePoint(x, y)
            when {
                (x == target.x && y == target.y) -> "T"
                route.any { it.x == x && it.y == y && it.x == firstPt.x && it.y == firstPt.y } -> "S"
                route.any { it.x == x && it.y == y } -> "Z"
                FramePoint(x, y).isTopRightCorner -> "C"
                v -> if (FramePoint(x, y).onHighway) {
                    ":"
                } else {
                    "."
                }
                else -> "X"
            }
        }

        zstar.costsF.write("check_1192_rzstar_costs") { v, x, y ->
            val pt = FramePoint(x, y)
            when {
                (x == target.x && y == target.y) -> "T"
                route.any { it.x == x && it.y == y && it.x == firstPt.x && it.y == firstPt.y } -> "S"
                route.any { it.x == x && it.y == y } -> "Z"
                FramePoint(x, y).isTopRightCorner -> "C"
                !zstar.passable.get(x, y) -> "X"
                v == 0 -> ":"
                v > 100000 -> "!"
                v > 9000 -> "@"
                v > 900 -> "9"
                v > 90 -> "8"
                v < 10 -> "$v"
                v <= 900 -> "."
                else -> "X"
            }
        }

        route shouldNotBe null
        route.size shouldBeGreaterThan 1
//        d { " Route: $firstPt then ${route[1]} c ${cell.passable.get(FramePoint(122, 79).justRightEnd)}"}
        NavUtil.directionTo(from, route[1]) shouldBe dirResult
    }

    fun adjustCorner(first5: MutableList<FramePoint>) {
        if (first5.size < 2) return
        val corner = first5[0]
        if (!corner.isTopRightCorner) return
        d { "THE CORNER $corner" }
        first5.set(1, first5[0].right)
        first5.set(2, first5[0].right.right)

//        first5.set(1)
//        : (208, 82) false
//        Debug: (Kermit)  : (208, 81) true
//        Debug: (Kermit)  : (208, 80) false
//        Debug: (Kermit)  : (209, 80) false
//        Debug: (Kermit)  : (210, 80) false
//        Debug: (Kermit) CORNER (208, 81)
//        Debug: (Kermit)  next is (208, 81)
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
        ZStar.DEBUG = true
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

        ZStar.DEBUG = true
        //check(55, FramePoint(112, 88), FramePoint(112,64), GamePad.MoveUp)
//        42 target (152, 100) link (104, 98)
        // not on highway
        val start = FramePoint(61, 121)
        check(13, start, FramePoint(32, 96), GamePad.MoveUp, level = 7,
            before = start.left)
    }

    @Test
    fun `test move lev5`() {
        ZStar.DEBUG = false
        val start = FramePoint(61, 121)
        check(13, start, FramePoint(32, 96), GamePad.MoveUp, level = 7,
            before = start.left)
    }

    @Test
    fun `test move 3`() {
        // route to nearest safe corner


        ZStar.DEBUG = false
        ZStar.MAX_ITER = 1000000
        val start = FramePoint(88, 91)

        val enemies = listOf(
            FramePoint(94, 112),
            FramePoint(110, 32),
//            FramePoint(97, 32)
        )
//        check(102, start, FramePoint(110, 32), GamePad.MoveUp, level = 5,
//            before = start.left)
//        check(102, start, FramePoint(101, 32), GamePad.MoveUp, level = 5,
//            before = start.left, enemies = enemies)
        // nearest safe corner
        // go left, up, down, right until reach safe point
        // on one highway (modify highway? this point of 32 is
        // correct
        check(102, start, FramePoint(93, 32), GamePad.MoveUp, level = 5,
            before = start.left, enemies = enemies)
    }

    @Test
    fun `test move 1 corner`() {
        ZStar.DEBUG = true
//        val start = FramePoint(160 - MapConstants.twoGrid, 119)
        val start = FramePoint(208, 120)
        check(115, start, FramePoint(255, 120), GamePad.MoveUp, level = 1,
            before = start.left)
    }

    @Test
    fun `test move 2 corner`() {
        ZStar.DEBUG = true
//        val start = FramePoint(160 - MapConstants.twoGrid, 119)
        val start = FramePoint(208, 75) // higher up
        check(114, start, FramePoint(250, 80), GamePad.MoveUp, level = 1,
            before = start.left)
    }

//target (166, 67) link (72, 54)
@Test
fun `test dragon`() {
    ZStar.DEBUG = true
//    zstar.SHORT_ITER = 150
    ZStar.MAX_ITER = 10000
    val start = FramePoint(72, 54) // higher up
    // wtf is wrong
    // it has two coordinates that are off grid
    val target = FramePoint(170, 67)
//        val target = FramePoint(166, 128)
    // it should be 209
//        val target = FramePoint(215, 128)
    check(53, start, target, GamePad.MoveUp, level = 1,
        before = start.left)
}

    @Test
    fun `test lev 1 bat`() {
        ZStar.DEBUG = true
//    zstar.SHORT_ITER = 150
        // works if I give it more iterations, to find a route
        // that isn't on the highway
        ZStar.MAX_ITER = 1000
        //177, 35
//        val start = FramePoint(112, 69) // higher up
        val start = FramePoint(112, 69) // higher up
//        val target = FramePoint(177, 35) // find it because it is near highway
        val target = FramePoint(178, 35) // find it
        check(114, start, target, GamePad.MoveUp, level = 1,
            before = start.left)
    }

    @Test
    fun `test lev 1 ladder`() {
        ZStar.DEBUG = true
//    zstar.SHORT_ITER = 150
        // works if I give it more iterations, to find a route
        // that isn't on the highway
        ZStar.MAX_ITER = 1000
        val ladder = FramePoint(120, 99)
        val link = FramePoint(120, 104)
//        val target = FramePoint(48, 70)
        val target = FramePoint(56, 90)
        check(51, link, target, GamePad.MoveUp, level = 1,
            before = link.left, ladderSpec = ZStar.LadderSpec(false, ladder))
    }

    @Test
    fun `test lev 1 bats`() {
        ZStar.DEBUG = false
//    zstar.SHORT_ITER = 150
        // works if I give it more iterations, to find a route
        // that isn't on the highway
        ZStar.MAX_ITER = 10000
        val link = FramePoint(96, 59)
        val target = FramePoint(172, 52)
        check(114, link, target, GamePad.MoveUp, level = 1,
            before = link.left)
    }

    @Test
    fun `test rhino`() {
        ZStar.DEBUG = true
        ZStar.SHORT_ITER = 150
        ZStar.MAX_ITER = 150
//        val start = FramePoint(54, 128) // higher up
//        val rhinoLocation = FramePoint(90, 128)
//        val target = FramePoint(126, 128)


        val start = FramePoint(107, 112) // higher up
        val rhinoLocation = FramePoint(112, 87)
        // rule, dont' route on top of the monster
//        val target = FramePoint(216, 128)
        val target = FramePoint(112, 119)
//        val target = FramePoint(166, 128)
        // it should be 209
//        val target = FramePoint(215, 128)
        check(14, start, target, GamePad.MoveUp, level = 2,
            before = start.left, enemies = listOf(rhinoLocation))
    }

    @Test
    fun `test rhinoTwo`() {
        ZStar.DEBUG = false
//        val start = FramePoint(54, 128) // higher up
//        val rhinoLocation = FramePoint(90, 128)
//        val target = FramePoint(126, 128)

        val start = FramePoint(48, 64) // higher up
        val rhinoLocation = FramePoint(48, 87)
        // rule, dont' route on top of the monster
//        val target = FramePoint(216, 128)
        val target = FramePoint(48, 119)
//        val target = FramePoint(166, 128)
        // it should be 209
//        val target = FramePoint(215, 128)
        check(14, start, target, GamePad.MoveUp, level = 2,
            before = start.left, enemies = listOf(rhinoLocation))
    }


    @Test
    fun `test move 3 corner`() {
        ZStar.DEBUG = true
//        val start = FramePoint(160 - MapConstants.twoGrid, 119)
        val start = FramePoint(136, 61) // higher up
        check(68, start, FramePoint(128, 48), GamePad.MoveUp, level = 1,
            before = start.left)
    }

    @Test
    fun `test make passable)`() {
        ZStar.DEBUG = true
        //passable 128, 32)
        val passable = FramePoint(128, 32)

//        val start = FramePoint(160 - MapConstants.twoGrid, 119)
        val start = FramePoint(104, 46)
        check(98, start, FramePoint(128, 32), GamePad.MoveUp,
            before = start.left, makePassable = passable)
    }

    @Test
    fun `test move 45`() {
        //(64, 24) to next (0, 0) [(80, 16)
        ZStar.DEBUG = true
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
        ZStar.DEBUG = true
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
        checkA(109, FramePoint(147, 56), targets, null, GamePad.MoveUp, level = 2)
    }

    @Test
    fun `test move level 4`() {
        ZStar.DEBUG = true
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
        ZStar.DEBUG = true
        check(115, FramePoint(208, 81), FramePoint(255, 80), GamePad.MoveRight, level = 1,
            before = FramePoint(208, 79))
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
        ZStar.DEBUG = true
        check(58, FramePoint(144, 88), FramePoint(112, 167), GamePad.MoveDown)
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