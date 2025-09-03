import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import bot.plan.zstar.NeighborFinder
import bot.plan.zstar.route.BreadthFirstSearch
import bot.state.*
import bot.state.map.Direction
import util.Map2d
import java.io.File

data class GridCell(
    val x: Int,
    val y: Int,
    val isStart: Boolean = false,
    val hasEnemy: Boolean = false,
    val isPath: Boolean = false,
    val cost: Int = 0,
    val direction: Direction? = null
)

class BreadthFirstSearchTestUIState {
    var gridWidth = 300
    var gridHeight = 300
    var cellSize = 16 // 16x16 pixel cells
    
    var startPoint: FramePoint? by mutableStateOf(null)
    var startDirection by mutableStateOf(Direction.None)
    var enemies = mutableStateListOf<FramePoint>()
    var foundPaths = mutableStateListOf<List<FramePoint>>()
    var ableToLongAttack by mutableStateOf(false)
    var showInstructions by mutableStateOf(false)
    var selectedPathIndex by mutableStateOf(0)  // -1 means show all paths, 0+ means show specific path
    
    private val settingsFile = File("breadthfirstsearch_settings.json")
    
    private val passableGrid = Map2d((0 until gridHeight).map { 
        (0 until gridWidth).map { true }.toMutableList() 
    }.toMutableList())
    
    private val costGrid = Map2d((0 until gridHeight).map { 
        (0 until gridWidth).map { 0 }.toMutableList() 
    }.toMutableList())
    
    fun getCellAt(gridX: Int, gridY: Int): GridCell {
        val framePoint = FramePoint(gridX, gridY)
        
        // Find the direction for this point if it's in a path
        var direction: Direction? = null
        val isPath = if (foundPaths.isEmpty()) {
            false
        } else if (selectedPathIndex == -1) {
            // Show all paths
            foundPaths.any { path -> 
                val matchingPoint = path.find { it.x == gridX && it.y == gridY }
                if (matchingPoint != null) {
                    direction = matchingPoint.direction
                    true
                } else {
                    false
                }
            }
        } else {
            // Show only the selected path
            val selectedPath = foundPaths.getOrNull(selectedPathIndex)
            if (selectedPath != null) {
                val matchingPoint = selectedPath.find { it.x == gridX && it.y == gridY }
                if (matchingPoint != null) {
                    direction = matchingPoint.direction
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
        
        return GridCell(
            x = gridX,
            y = gridY,
            isStart = framePoint == startPoint,
            hasEnemy = enemies.any { it.x == gridX && it.y == gridY },
            isPath = isPath,
            cost = if (costGrid.map.isNotEmpty()) costGrid.get(framePoint) else 0,
            direction = direction
        )
    }
    
    fun setStartPoint(gridX: Int, gridY: Int) {
        startPoint = FramePoint(gridX, gridY, direction = startDirection)
        foundPaths.clear()
        selectedPathIndex = 0
        saveSettings()
    }
    
    fun toggleEnemy(gridX: Int, gridY: Int) {
        // Use the grid cell as the enemy position (1:1 mapping)
        val framePoint = FramePoint(gridX, gridY)
        
        // Check if there's already an enemy here
        val existingEnemy = enemies.find { it.x == gridX && it.y == gridY }
        
        if (existingEnemy != null) {
            // Remove existing enemy
            enemies.remove(existingEnemy)
            
            // Clear cost for 16x16 area starting from enemy point
            for (dx in 0 until 16) {
                for (dy in 0 until 16) {
                    val enemyX = framePoint.x + dx
                    val enemyY = framePoint.y + dy
                    val costPoint = FramePoint(enemyX, enemyY)
                    if (enemyX < gridWidth && enemyY < gridHeight) {
                        costGrid.set(costPoint, 0)
                    }
                }
            }
        } else {
            // Add new enemy at the grid cell
            enemies.add(framePoint)
            
            // Set cost to 1000 for 16x16 area starting from enemy point
            for (dx in 0 until 16) {
                for (dy in 0 until 16) {
                    val enemyX = framePoint.x + dx
                    val enemyY = framePoint.y + dy
                    val costPoint = FramePoint(enemyX, enemyY)
                    if (enemyX < gridWidth && enemyY < gridHeight) {
                        costGrid.set(costPoint, 1000)
                    }
                }
            }
        }
        foundPaths.clear()
        selectedPathIndex = 0
        saveSettings()
    }
    
    fun runBreadthFirstSearch() {
        val start = startPoint ?: return
        if (enemies.isEmpty()) return
        
        foundPaths.clear()
        
        try {
            val neighborFinder = NeighborFinder(passableGrid).apply {
                costF = costGrid
            }
            
            // Create a real BreadthFirstSearch instance - now much simpler!
            val breadthFirstSearch = BreadthFirstSearch(
                ableToLongAttack = ableToLongAttack,
                neighborFinder = neighborFinder
            )
            
            // Use the real algorithm with actual isGoal logic
            val paths = breadthFirstSearch.breadthFirstSearch(
                start = start,
                targets = enemies.toList(),
                maxDepth = 300
            )
            foundPaths.addAll(paths)
            selectedPathIndex = if (paths.isNotEmpty()) 0 else -1
            
        } catch (e: Exception) {
            println("Error running BreadthFirstSearch: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun saveSettings() {
        try {
            val lines = mutableListOf<String>()
            
            // Save start point
            if (startPoint != null) {
                lines.add("start=${startPoint!!.x},${startPoint!!.y}")
            }
            
            // Save enemies
            if (enemies.isNotEmpty()) {
                val enemyString = enemies.joinToString(";") { "${it.x},${it.y}" }
                lines.add("enemies=$enemyString")
            }
            
            // Save long attack setting
            lines.add("longAttack=$ableToLongAttack")
            
            // Save start direction
            lines.add("startDirection=${startDirection.name}")
            
            settingsFile.writeText(lines.joinToString("\n"))
            println("Settings saved to ${settingsFile.absolutePath}")
        } catch (e: Exception) {
            println("Error saving settings: ${e.message}")
        }
    }
    
    fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                val lines = settingsFile.readLines()
                
                // First pass: load simple settings like direction and long attack
                for (line in lines) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size != 2) continue
                    
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    when (key) {
                        "longAttack" -> {
                            ableToLongAttack = value.toBooleanStrictOrNull() ?: false
                        }
                        "startDirection" -> {
                            startDirection = when (value) {
                                "Up" -> Direction.Up
                                "Down" -> Direction.Down
                                "Left" -> Direction.Left
                                "Right" -> Direction.Right
                                else -> Direction.None
                            }
                        }
                    }
                }
                
                // Second pass: load complex settings that depend on the first pass
                for (line in lines) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size != 2) continue
                    
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    when (key) {
                        "start" -> {
                            val coords = value.split(",")
                            if (coords.size == 2) {
                                val x = coords[0].toIntOrNull()
                                val y = coords[1].toIntOrNull()
                                if (x != null && y != null) {
                                    startPoint = FramePoint(x, y, direction = startDirection)
                                }
                            }
                        }
                        "enemies" -> {
                            enemies.clear()
                            if (value.isNotEmpty()) {
                                val enemyPairs = value.split(";")
                                for (enemyPair in enemyPairs) {
                                    val coords = enemyPair.split(",")
                                    if (coords.size == 2) {
                                        val x = coords[0].toIntOrNull()
                                        val y = coords[1].toIntOrNull()
                                        if (x != null && y != null) {
                                            enemies.add(FramePoint(x, y))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Update cost grid for loaded enemies
                updateCostGridForEnemies()
                
                println("Settings loaded from ${settingsFile.absolutePath}")
            }
        } catch (e: Exception) {
            println("Error loading settings: ${e.message}")
        }
    }
    
    private fun updateCostGridForEnemies() {
        // Clear existing costs
        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                costGrid.set(FramePoint(x, y), 0)
            }
        }
        
        // Set costs for each enemy
        for (enemy in enemies) {
            for (dx in 0 until 16) {
                for (dy in 0 until 16) {
                    val enemyX = enemy.x + dx
                    val enemyY = enemy.y + dy
                    val costPoint = FramePoint(enemyX, enemyY)
                    if (enemyX < gridWidth && enemyY < gridHeight) {
                        costGrid.set(costPoint, 1000)
                    }
                }
            }
        }
    }
    
    fun deleteSavedSettings() {
        try {
            if (settingsFile.exists()) {
                settingsFile.delete()
                println("Saved settings deleted from ${settingsFile.absolutePath}")
            }
        } catch (e: Exception) {
            println("Error deleting saved settings: ${e.message}")
        }
    }
    
    
}

@Composable
fun BreadthFirstSearchTestUI() {
    val uiState = remember { 
        BreadthFirstSearchTestUIState().apply {
            loadSettings()
        }
    }
    
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { uiState.runBreadthFirstSearch() },
                    enabled = uiState.startPoint != null && uiState.enemies.isNotEmpty()
                ) {
                    Text("Run BreadthFirst Search")
                }
                
                Button(
                    onClick = { 
                        uiState.foundPaths.clear()
                        uiState.enemies.clear()
                        uiState.startPoint = null
                        uiState.selectedPathIndex = 0
                        uiState.ableToLongAttack = false
                        uiState.startDirection = Direction.None
                        uiState.deleteSavedSettings()
                    }
                ) {
                    Text("Clear All")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.ableToLongAttack,
                        onCheckedChange = { 
                            uiState.ableToLongAttack = it
                            uiState.saveSettings()
                        }
                    )
                    Text("Long Attack")
                }
                
                // Start direction dropdown
                var directionExpanded by remember { mutableStateOf(false) }
                
                Box {
                    Button(
                        onClick = { directionExpanded = !directionExpanded }
                    ) {
                        Text("Dir: ${uiState.startDirection.name}")
                    }
                    
                    DropdownMenu(
                        expanded = directionExpanded,
                        onDismissRequest = { directionExpanded = false }
                    ) {
                        listOf(Direction.None, Direction.Up, Direction.Down, Direction.Left, Direction.Right).forEach { direction ->
                            DropdownMenuItem(
                                onClick = { 
                                    uiState.startDirection = direction
                                    uiState.saveSettings()
                                    directionExpanded = false
                                }
                            ) {
                                Text(direction.name)
                            }
                        }
                    }
                }
                
                // Path selection dropdown
                if (uiState.foundPaths.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    
                    Box {
                        Button(
                            onClick = { expanded = !expanded }
                        ) {
                            Text(
                                if (uiState.selectedPathIndex == -1) "All Paths" 
                                else "Path ${uiState.selectedPathIndex + 1}"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                onClick = { 
                                    uiState.selectedPathIndex = -1
                                    expanded = false
                                }
                            ) {
                                Text("All Paths")
                            }
                            
                            uiState.foundPaths.forEachIndexed { index, path ->
                                DropdownMenuItem(
                                    onClick = { 
                                        uiState.selectedPathIndex = index
                                        expanded = false
                                    }
                                ) {
                                    Text("Path ${index + 1} (${path.size} steps)")
                                }
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { uiState.showInstructions = !uiState.showInstructions }
                ) {
                    Text("Info")
                }
                
                Spacer(Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Coordinates Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text("Coordinates:", fontWeight = FontWeight.Bold)
                    if (uiState.startPoint != null) {
                        Text("Start Point: (${uiState.startPoint!!.x}, ${uiState.startPoint!!.y})")
                    }
                    uiState.enemies.forEachIndexed { index, enemy ->
                        Text("Enemy ${index + 1}: (${enemy.x}, ${enemy.y})")
                    }
                    if (uiState.foundPaths.isNotEmpty()) {
                        Text("Found ${uiState.foundPaths.size} path(s)")
                        uiState.foundPaths.forEachIndexed { index, path ->
                            Text("Path ${index + 1}: ${path.size} steps")
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Instructions (conditionally visible)
            if (uiState.showInstructions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Instructions:", fontWeight = FontWeight.Bold)
                        Text("• Left click: Set start point (green)")
                        Text("• Double click: Toggle enemy (red)")
                        Text("• Blue: Found paths with directional lines")
                        Text("• Magenta lines: Movement direction (top=up, bottom=down, left=left, right=right)")
                        Text("• Yellow: High-cost enemy areas (16x16)")
                        Text("• Grid: 300x300 units")
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.Gray)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    repeat(uiState.gridHeight) { gridY ->
                        Row {
                            repeat(uiState.gridWidth) { gridX ->
                                val cell = uiState.getCellAt(gridX, gridY)
                                GridCellView(
                                    cell = cell,
                                    onLeftClick = { uiState.setStartPoint(gridX, gridY) },
                                    onRightClick = { uiState.toggleEnemy(gridX, gridY) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridCellView(
    cell: GridCell,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    val backgroundColor = when {
        cell.isStart -> Color.Green
        cell.hasEnemy -> Color.Red
        cell.isPath -> Color.Blue
        cell.cost > 0 -> Color.Yellow.copy(alpha = 0.3f)
        cell.x % 8 == 0 || cell.y % 8 == 0 -> Color.LightGray.copy(alpha = 0.2f)
        else -> Color.White
    }
    
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(backgroundColor)
            .border(0.2.dp, Color.Gray)
            .combinedClickable(
                onClick = { onLeftClick() },
                onDoubleClick = { onRightClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Draw directional lines for path points
        if (cell.isPath && cell.direction != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineThickness = size.width / 3f  // 1/3 of the square
                val lineColor = Color.Magenta  // Bright magenta color
                
                when (cell.direction) {
                    Direction.Up -> {
                        // Draw thick line on top 1/3 of square
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = lineThickness
                        )
                    }
                    Direction.Down -> {
                        // Draw thick line on bottom 1/3 of square
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = lineThickness
                        )
                    }
                    Direction.Left -> {
                        // Draw thick line on left 1/3 of square
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = lineThickness
                        )
                    }
                    Direction.Right -> {
                        // Draw thick line on right 1/3 of square
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = lineThickness
                        )
                    }
                    Direction.None -> {
                        // No direction line
                    }
                }
            }
        }
        
        when {
            cell.isStart -> Text("S", color = Color.White, fontSize = 4.sp)
            cell.hasEnemy -> Text("E", color = Color.White, fontSize = 4.sp)
            cell.isPath -> Text("•", color = Color.White, fontSize = 6.sp)
            else -> {
                // Show grid coordinates for empty cells on a smaller font
                if (cell.x % 10 == 0 && cell.y % 10 == 0) {
                    Text("${cell.x},${cell.y}", color = Color.Gray, fontSize = 2.sp)
                }
            }
        }
    }
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