package bot.plan.action

import bot.state.FramePoint
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.vertical
import io.kotest.matchers.shouldBe
import org.junit.Test

class AttackActionDeciderTest() {
    @Test
    fun att() {
//        fun inRangeOf(
//            from: Direction,
//            link: FramePoint,
//            enemies: List<FramePoint>,
//            useB: Boolean

        AttackActionDecider.inRangeOf(Direction.Right, FramePoint(55, 24),
            listOf(FramePoint(45, 32)), false) shouldBe null
    }

//    @Test
//    fun att() {
//        val attackDirectionGrid = FramePoint(88 + MapConstants.oneGridPoint5, 48)
//        val enemy = FramePoint(105, 48)
//        val link = FramePoint(88, 48)
//        val high = (enemy.isInGrid(attackDirectionGrid))
//        high shouldBe true
//    }
//    @Test
//    fun att2() {
////        Debug: (Kermit)    Enemy (128, 89) middle: (132, 89) Up up [(128, 113), (136, 113)]
////        Debug: (Kermit)    linkg: false or false (23) (128, 112)
////        Debug: (Kermit)    gridH: false false false (128, 105) mid=(132, 105)
//        val attackDirectionGrid = FramePoint(175, 128)
//        val enemy = FramePoint(180, 128)
//        val link = FramePoint(159, 128)
//        val high = (enemy.isInGrid(attackDirectionGrid))
//        high shouldBe true
//    }
//
//    @Test
//    fun `go`() {
//        AttackActionDecider.shouldAttack(Direction.Down, FramePoint(128, 107), listOf(FramePoint(128, 112))) shouldBe true
//    }
//    //83,144_32,144_53,21,1,MoveUp
//    @Test
//    fun `go kil skelli`() {
//        AttackActionDecider.shouldAttack(
//            Direction.Up, FramePoint(144, 53), listOf(FramePoint(144, 32))) shouldBe true
//    }
//
//    @Test
//    fun `go off center`() {
//        AttackActionDecider.shouldAttack(
//            Direction.Right, FramePoint(150, 88), listOf(FramePoint(159, 96))) shouldBe true
//    }
//
//    @Test
//    fun `too far, do not attack`() {
//        AttackActionDecider.DEBUG = true
//        // 36 distance
////        val target = FramePoint(114, 80)
//        // 80 + 16 = 96
//        val target = FramePoint(122, 80)
//        AttackActionDecider.shouldAttack(
//            Direction.Right, FramePoint(88, 80), listOf(target)) shouldBe false
//    }
//
//    @Test
//    fun `too far, dont attack missed target was in square left down`() {
//        AttackActionDecider.DEBUG = true
//        val target = FramePoint(40, 126)
//        val target2 = FramePoint(32, 126)
//        AttackActionDecider.shouldAttack(
//            Direction.Right, FramePoint(60, 112), listOf(target2)) shouldBe false
//    }
//    @Test
//    fun `bat below sword still hits`() {
//        AttackActionDecider.DEBUG = true
//        val target = FramePoint(80, 98)
//        // hits
//        AttackActionDecider.shouldAttack(
//            Direction.Left, FramePoint(101, 88), listOf(target)) shouldBe true
//    }
//    //link = (88, 80) dirGrid = (103, 80)
//
//    @Test
//    fun `test in grid`() {
//        val enemy = FramePoint(50, 48)
//        val link = FramePoint(64, 48)
//        val from = Direction.Left
//
//        val attackDirectionGrid = AttackActionDecider.attackGrid(from, link)
//        enemy.isInHalfFatGrid(attackDirectionGrid, wide = from.vertical) shouldBe true
//    }
//
//    @Test
//    fun `out of range from below sword too far away`() {
//        //Route Action -> ATTACK
//        // maybe its because of this list of enemies
////        Debug: (Kermit) enemy: (136, 43) useThird true in grid false (16) in link false (31)
////        Debug: (Kermit) enemy: (128, 43) useThird true in grid false (8) in link false (23)
////        Debug: (Kermit) enemy: (128, 37) useThird true in grid false (14) in link false (29)
////        Debug: (Kermit) enemy: (172, 32) useThird true in grid false (63) in link false (78)
////        Debug: (Kermit) enemy: (106, 64) useThird true in grid false (35) in link false (24)
////        check(Direction.Left, FramePoint(128, 66), FramePoint(128, 43), true)
//        //check(Direction.Up, FramePoint(128, 66), FramePoint(106, 64), true)
//        AttackActionDecider.DEBUG = true
//        // hits
//        AttackActionDecider.shouldAttack(
//            Direction.Up, FramePoint(128, 66), listOf(
//                FramePoint(136,43),
////                FramePoint(106,64),
////            FramePoint(128,43),
////                FramePoint(128,37),
////                FramePoint(172,32),
//                )
//        ) shouldBe true
//    }
//
//    @Test
//    fun `attack skeli`() {
//        check(Direction.Right, FramePoint(68, 112), FramePoint(98, 112), false)
//    }
//
//    @Test
//    fun `attack below`() {
//        check(Direction.Right, link = FramePoint(169, 88),
//            FramePoint(154, 96), true)
//    }
//        //(104, 64)
//    fun check(dir: Direction, link: FramePoint, target: FramePoint, should: Boolean) {
//        AttackActionDecider.DEBUG = true
//        // hits
//        AttackActionDecider.shouldAttack(
//            dir, link, listOf(target)) shouldBe should
//    }
}
// target (80, 98) link (101, 88)