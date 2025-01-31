package util

import bot.state.FramePoint
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.test.assertTrue

class GeomTest {
    @Test
    fun equalsSame() {
        // test the equals operator for two rectangles
        assertTrue(Geom.Rectangle(FramePoint(1, 1), FramePoint(5, 5))
                == Geom.Rectangle(FramePoint(1, 1), FramePoint(5, 5))
        )
    }

    @Test
    fun ptInside() {
        Geom.Rectangle(FramePoint(1, 1), FramePoint(5, 5)).apply {
            pointInside(FramePoint(2, 2)) shouldBe true
            pointInside(FramePoint(10, 2)) shouldBe false
            pointInside(FramePoint(2, 10)) shouldBe false
        }
    }

    @Test
    fun `test inter`() {
        val link = Geom.Rectangle(FramePoint(182, 105), FramePoint(186, 168))
        val enemy = Geom.Rectangle(FramePoint(166, 128), FramePoint(182, 128))

        // intersect sword rect Rectangle(topLeft=(182, 105)  , bottomRight=(186, 168)  )

        //        / enemy: (166, 128) -> 182, 128 .. it does intersect along side
        //        // link (176, 105) DOWN
       val a = Geom.intersect(link, enemy)
        println(a)
        val b = a
    }

    @Test
    fun `test it`() {

        // Test cases
        val testCases = listOf(
            // Two rectangles completely separate from each other
//            Pair(
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(2, 2)),
//                Geom.Rectangle(FramePoint(3, 3), FramePoint(5, 5))
//            ),
//            // Two rectangles partially overlapping along one axis
//            Pair(
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(4, 4)),
//                Geom.Rectangle(FramePoint(3, 3), FramePoint(6, 6))
//            ),
//            // Two rectangles partially overlapping along both axes
//            Pair(
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(3, 3)),
//                Geom.Rectangle(FramePoint(2, 2), FramePoint(5, 5))
//            ),
//            // Two rectangles sharing one corner
//            Pair(
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(2, 2)),
//                Geom.Rectangle(FramePoint(2, 2), FramePoint(4, 4))
//            ),
//            // Two rectangles sharing one side
//            Pair(
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(4, 4)),
//                Geom.Rectangle(FramePoint(2, 2), FramePoint(6, 6))
//            ),
//            // Two rectangles completely overlapping (identical)
//            Pair(
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(4, 4)),
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(4, 4))
//            ),
//            // Two rectangles with one contained within the other
//            Pair(
//                Geom.Rectangle(FramePoint(0, 0), FramePoint(6, 6)),
//                Geom.Rectangle(FramePoint(2, 2), FramePoint(4, 4))
//            ),
            // mine
            Pair(
                Geom.Rectangle(FramePoint(1, 0), FramePoint(5, 4)),
                Geom.Rectangle(FramePoint(0, 1), FramePoint(6, 2))
            )
        )

        // Perform intersection check for each test case
        testCases.forEachIndexed { index, (rect1, rect2) ->
            if (Geom.intersect(rect1, rect2)) {
                println("Test case ${index + 1}: Rectangles intersect. ")
            } else {
                println("Test case ${index + 1}: Rectangles do not intersect.")
            }
            if (Geom.intersect(rect2, rect1)) {
                println("Test case ${index + 1}: 2Rectangles intersect. ")
            } else {
                println("Test case ${index + 1}: 2Rectangles do not intersect.")
            }
        }

    }
}