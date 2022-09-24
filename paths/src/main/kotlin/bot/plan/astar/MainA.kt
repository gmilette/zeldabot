import bot.state.FramePoint
import bot.state.distTo

data class Coordinates(
    val lat: Double,
    val lon: Double
) : Graph.Vertex

data class Route(
    override val a: FramePoint,
    override val b: FramePoint
) : Graph.Edge<FramePoint> {
    val distance: Double
        get() {
            return a.distTo(b).toDouble()
        }
}

class AlgorithmAStarImpl(edges: List<Route>) : AlgorithmAStar<FramePoint, Route>(edges) {
    override fun costToMoveThrough(edge: Route): Double {
        return edge.distance
    }

    override fun createEdge(from: FramePoint, to: FramePoint): Route {
        return Route(from, to)
    }
}

object TestStar {
    fun testStar() {
        val routes = listOf(
            Route(FramePoint(1, 1), FramePoint(1, 3)),
            Route(FramePoint(1, 1), FramePoint(5, 1)),
            Route(FramePoint(5, 1), FramePoint(2, 2)),
            Route(FramePoint(1, 3), FramePoint(2, 2))
        )

        val result = AlgorithmAStarImpl(routes)
            .findPath(
                begin = FramePoint(1, 1),
                end = FramePoint(2, 2)
            )

        val pathString =
            result.first.joinToString(separator = ", ") { "[${it.x},${it.y}]" }

        println("Result:")
        println("  Path: $pathString")
        println("  Cost: ${result.second}")
    }
}