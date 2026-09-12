import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * Same test UI as [BreadthFirstSearchTestUIMain], opened on the ZStar A* algorithm.
 */
fun main() {
    application {
        Window(
            onCloseRequest = { kotlin.system.exitProcess(0) },
            title = "ZStar Routing Test UI",
            resizable = true
        ) {
            RoutingTestUI(initialAlgorithm = RoutingAlgorithm.ZStarAStar)
        }
    }
}
