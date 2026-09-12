import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window

/**
 * The routing test UI, opened on the breadth first search algorithm.
 * Everything lives in [RoutingTestUI], the algorithm can be switched in the window.
 */
@Composable
fun BreadthFirstSearchTestUI() {
    RoutingTestUI(initialAlgorithm = RoutingAlgorithm.BreadthFirst)
}

@Composable
fun BreadthFirstSearchTestUIWindow() {
    Window(
        onCloseRequest = { kotlin.system.exitProcess(0) },
        title = "BreadthFirst Search Test UI",
        resizable = true
    ) {
        BreadthFirstSearchTestUI()
    }
}

@Preview
@Composable
fun BreadthFirstSearchTestUIPreview() {
    BreadthFirstSearchTestUI()
}
