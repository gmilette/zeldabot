package util

import bot.state.FramePoint
import org.junit.Test

class GeomTest {

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