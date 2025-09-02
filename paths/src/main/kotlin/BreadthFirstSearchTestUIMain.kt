import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window

fun main() {
    application {
        Window(
            onCloseRequest = { kotlin.system.exitProcess(0) },
            title = "BreadthFirst Search Test UI",
            resizable = true
        ) {
            BreadthFirstSearchTestUI()
        }
    }
}