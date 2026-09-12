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
import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.plan.action.RouteTo
import bot.plan.zstar.NeighborFinder
import bot.plan.zstar.ZStar
import bot.plan.zstar.route.BreadthFirstSearch
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.opposite
import bot.state.map.pointModifier
import util.Map2d
import java.io.File

/**
 * Which routing algorithm the test UI should run. All of them share the same scenario
 * (start point, enemies, target generation) so they can be compared side by side.
 */
enum class RoutingAlgorithm(val label: String) {
    BreadthFirst("BFS"),
    ZStarAStar("ZStar A*"),
    ZStarBreadthFirst("ZStar BFS")
}

/**
 * How the targets/goal are derived from the placed enemies.
 */
enum class TargetMode(val label: String) {
    /** the original BFS test goal: get within one grid of an enemy */
    EnemyProximity("Near enemy"),

    /**
     * goal is anywhere link can attack from, [AttackActionDecider.inRangeOf], the same call
     * RouteToDetermineAction makes. When link is safe a spot he only has to turn around in
     * counts too, when he is not safe he has to be able to swing right now
     */
    StrikingRange("Striking range"),

    /** what KillAll does: [AttackActionDecider.attackPoints] minus the enemy's facing direction */
    AttackPoints("Attack points (not dir)"),

    /** [AttackActionDecider.attackPointsNoCorner], used when the enemy can attack in front */
    AttackPointsNoCorner("Attack points no corner")
}

data class RoutingRequest(
    val algorithm: RoutingAlgorithm,
    val targetMode: TargetMode,
    val start: FramePoint,
    /** enemy points, the [FramePoint.direction] is the direction the enemy is facing */
    val enemies: List<FramePoint>,
    val gridWidth: Int,
    val gridHeight: Int,
    val startDirection: Direction = Direction.None,
    val ableToLongAttack: Boolean = false,
    val halfPassable: Boolean = true,
    val isLevel: Boolean = false,
    val mapNearest: Boolean = true,
    val finishWithinStrikingRange: Boolean = true,
    val findNearestSafeIfCurrentlyNotSafe: Boolean = true,
    val maxDepth: Int = 300
)

data class RoutingResult(
    val paths: List<List<FramePoint>>,
    val targets: List<FramePoint>,
    val costs: Map2d<Int>? = null,
    /** cost at or above this is "near an enemy" */
    val dangerCost: Int = ZStar.nearEnemyCost,
    /** cost below this is a cheap (highway) square */
    val normalCost: Int = 1,
    val summary: String = ""
)

/**
 * Runs any of the [RoutingAlgorithm]s against a scenario built in the UI. Everything here is
 * plain (non compose) code so it can be called from tests too.
 */
object RoutingSearchRunner {
    /** the cost the plain BFS grid uses around an enemy, matches what the BFS UI always did */
    private const val bfsEnemyCost = 1000

    fun targetsFor(enemies: List<FramePoint>, mode: TargetMode): List<FramePoint> =
        when (mode) {
            TargetMode.EnemyProximity,
            TargetMode.StrikingRange -> enemies.map { it.noDir() }

            TargetMode.AttackPoints -> enemies.flatMap {
                AttackActionDecider.attackPoints(it.noDir(), not = it.direction ?: Direction.None)
            }

            TargetMode.AttackPointsNoCorner -> enemies.flatMap {
                AttackActionDecider.attackPointsNoCorner(it.noDir())
            }
        }.distinct()

    /**
     * The goal the search stops at. Second parameter is whether link is safe at that point,
     * the searches pass it in now (ZStar uses its cost grid, the breadth first search always
     * says safe). Like RouteToDetermineAction does, it decides whether merely being able to
     * turn and face the enemy is good enough.
     */
    fun goalFor(
        mode: TargetMode,
        enemies: List<FramePoint>,
        targets: List<FramePoint>,
        ableToLongAttack: Boolean = false,
        passable: Map2d<Boolean>? = null
    ): (FramePoint, Boolean) -> Boolean {
        val enemyPoints = enemies.map { it.noDir() }
        val targetSet = targets.toHashSet()
        val shortGoal: (FramePoint, Boolean) -> Boolean = when (mode) {
            TargetMode.EnemyProximity -> { point, _ ->
                enemyPoints.any { point.distTo(it) <= MapConstants.oneGrid }
            }

            TargetMode.StrikingRange -> { point, isSafe ->
                val action = AttackActionDecider.inRangeOf(
                    point.direction ?: Direction.None, point, enemyPoints,
                    useB = false, faceEnemy = isSafe
                )
                // can swing from here, or is safe enough to spend a frame turning around first
                action.isAttack || (isSafe && action.isDirection)
            }

            TargetMode.AttackPoints,
            TargetMode.AttackPointsNoCorner -> { point, _ -> point in targetSet }
        }

        if (!ableToLongAttack) return shortGoal

        return { point, isSafe ->
            shortGoal(point, isSafe) || AttackLongActionDecider.inStrikingRange(point, enemyPoints) ||
                    (passable != null && AttackLongActionDecider.targetInLongRange(passable, point, enemyPoints))
        }
    }

    fun run(request: RoutingRequest): RoutingResult {
        // these are on by default and make the search crawl (they loop the open list every iteration)
        ZStar.DEBUG = false
        ZStar.DEBUG_B = false
        ZStar.DEBUG_V = false

        val targets = targetsFor(request.enemies, request.targetMode)
        return when (request.algorithm) {
            RoutingAlgorithm.BreadthFirst -> runBreadthFirst(request, targets)
            RoutingAlgorithm.ZStarAStar -> runZStar(request, targets)
            RoutingAlgorithm.ZStarBreadthFirst -> runZStarBreadthFirst(request, targets)
        }
    }

    private fun allPassable(request: RoutingRequest): Map2d<Boolean> =
        Map2d((0 until request.gridHeight).map {
            (0 until request.gridWidth).map { true }.toMutableList()
        })

    private fun bfsCosts(request: RoutingRequest): Map2d<Int> =
        Map2d((0 until request.gridHeight).map {
            (0 until request.gridWidth).map { 0 }.toMutableList()
        }).also { costs ->
            for (enemy in request.enemies) {
                costs.modifyTo(enemy.noDir(), MapConstants.oneGrid, bfsEnemyCost)
            }
        }

    private fun runBreadthFirst(request: RoutingRequest, targets: List<FramePoint>): RoutingResult {
        val passable = allPassable(request)
        val costs = bfsCosts(request)
        val neighborFinder = NeighborFinder(passable, request.halfPassable, request.isLevel).apply {
            costF = costs
        }
        return search(
            request, targets, neighborFinder, passable,
            costs = costs, dangerCost = bfsEnemyCost, normalCost = 1, name = "BFS"
        )
    }

    private fun runZStarBreadthFirst(request: RoutingRequest, targets: List<FramePoint>): RoutingResult {
        val zstar = ZStar(allPassable(request), request.halfPassable, request.isLevel)
        // gives the breadth first search the same cost/passable grid ZStar.routeWithBfs would use
        zstar.setNeighborFinder(zRouteParam(request, targets, goalFor(request, targets)))
        return search(
            request, targets, zstar.neighborFinder, zstar.passable(),
            costs = zstar.costsF, dangerCost = ZStar.nearEnemyCost, normalCost = 1000, name = "ZStar BFS"
        )
    }

    private fun search(
        request: RoutingRequest,
        targets: List<FramePoint>,
        neighborFinder: NeighborFinder,
        passable: Map2d<Boolean>,
        costs: Map2d<Int>,
        dangerCost: Int,
        normalCost: Int,
        name: String
    ): RoutingResult {
        val isGoal = goalFor(request, targets, passable)
        val search = BreadthFirstSearch(isGoal, neighborFinder)
        if (search.isTheGoal(request.start)) {
            return RoutingResult(
                paths = emptyList(), targets = targets, costs = costs,
                dangerCost = dangerCost, normalCost = normalCost,
                summary = "$name: start is already the goal, attack"
            )
        }
        val paths = search.breadthFirstSearch(request.start, targets, request.maxDepth)
        return RoutingResult(
            paths = paths, targets = targets, costs = costs,
            dangerCost = dangerCost, normalCost = normalCost,
            summary = "$name: ${paths.size} path(s), ${targets.size} target(s)"
        )
    }

    private fun runZStar(request: RoutingRequest, targets: List<FramePoint>): RoutingResult {
        val zstar = ZStar(allPassable(request), request.halfPassable, request.isLevel)
        val param = zRouteParam(request, targets, goalFor(request, targets, zstar.passable()))
        val path = zstar.route(param)
        // ZStar strips the direction off of every point, put it back so the arrows render
        val withDirection = withDirections(path)
        val reachedTarget = path.lastOrNull()?.let { it in targets } ?: false
        return RoutingResult(
            paths = if (path.size > 1) listOf(withDirection) else emptyList(),
            targets = targets,
            costs = zstar.costsF,
            dangerCost = ZStar.nearEnemyCost,
            normalCost = 1000,
            summary = "ZStar A*: ${path.size} step(s), ${targets.size} target(s)" +
                    if (reachedTarget) ", reached a target" else ", stopped short of the targets"
        )
    }

    private fun goalFor(
        request: RoutingRequest,
        targets: List<FramePoint>,
        passable: Map2d<Boolean>? = null
    ): (FramePoint, Boolean) -> Boolean =
        goalFor(request.targetMode, request.enemies, targets, request.ableToLongAttack, passable)

    private fun zRouteParam(
        request: RoutingRequest,
        targets: List<FramePoint>,
        isGoal: (FramePoint, Boolean) -> Boolean
    ) = ZStar.ZRouteParam(
        start = request.start,
        targets = targets,
        enemies = request.enemies.map { it.noDir() },
        // where link came from, so the "don't turn around" rule applies like it does in game
        pointBeforeStart = if (request.startDirection == Direction.None) null else
            request.startDirection.opposite().pointModifier(1)(request.start),
        isGoal = isGoal,
        rParam = RouteTo.RoutingParamCommon(
            mapNearest = request.mapNearest,
            finishWithinStrikingRange = request.finishWithinStrikingRange,
            findNearestSafeIfCurrentlyNotSafe = request.findNearestSafeIfCurrentlyNotSafe
        )
    )

    private fun withDirections(path: List<FramePoint>): List<FramePoint> =
        path.mapIndexed { index, point ->
            val next = path.getOrNull(index + 1)
            if (next == null) point else point.copy(direction = point.dirTo(next))
        }
}

data class GridCell(
    val x: Int,
    val y: Int,
    val isStart: Boolean = false,
    val hasEnemy: Boolean = false,
    val enemyDirection: Direction? = null,
    val isTarget: Boolean = false,
    val isPath: Boolean = false,
    val cost: Int = 0,
    val direction: Direction? = null
)

class RoutingTestUIState(initialAlgorithm: RoutingAlgorithm? = null) {
    // Map2d.get clamps every read to MAX_X/MAX_Y, so anything past that is not a real square
    val gridWidth = MapConstants.MAX_X + 1
    val gridHeight = MapConstants.MAX_Y + 1

    var cellSize by mutableStateOf(10)

    var algorithm by mutableStateOf(initialAlgorithm ?: RoutingAlgorithm.BreadthFirst)
    var targetMode by mutableStateOf(TargetMode.EnemyProximity)

    var startPoint: FramePoint? by mutableStateOf(null)
    var startDirection by mutableStateOf(Direction.None)

    /** direction given to the next enemy that gets placed */
    var enemyDirection by mutableStateOf(Direction.None)
    var enemies = mutableStateListOf<FramePoint>()

    var ableToLongAttack by mutableStateOf(false)
    var halfPassable by mutableStateOf(true)
    var isLevel by mutableStateOf(false)
    var mapNearest by mutableStateOf(true)
    var finishWithinStrikingRange by mutableStateOf(true)
    var findNearestSafeIfCurrentlyNotSafe by mutableStateOf(true)

    var showInstructions by mutableStateOf(false)
    var showCosts by mutableStateOf(true)
    var showTargets by mutableStateOf(true)

    var foundPaths = mutableStateListOf<List<FramePoint>>()
    var selectedPathIndex by mutableStateOf(0) // -1 means show all paths
    var summary by mutableStateOf("")

    // lookups rebuilt whenever the scenario changes so drawing a cell is a hash lookup
    private var pathDirections by mutableStateOf<Map<FramePoint, Direction?>>(emptyMap())
    private var targetPoints by mutableStateOf<Set<FramePoint>>(emptySet())
    private var enemyPoints by mutableStateOf<Map<FramePoint, Direction?>>(emptyMap())
    private var costs by mutableStateOf<Map2d<Int>?>(null)
    private var dangerCost by mutableStateOf(ZStar.nearEnemyCost)
    private var normalCost by mutableStateOf(1)

    private val settingsFile = File("breadthfirstsearch_settings.json")

    fun getCellAt(gridX: Int, gridY: Int): GridCell {
        val framePoint = FramePoint(gridX, gridY)
        return GridCell(
            x = gridX,
            y = gridY,
            isStart = framePoint == startPoint,
            hasEnemy = enemyPoints.containsKey(framePoint),
            enemyDirection = enemyPoints[framePoint],
            isTarget = showTargets && framePoint in targetPoints,
            isPath = pathDirections.containsKey(framePoint),
            cost = if (showCosts) costs?.get(framePoint) ?: 0 else 0,
            direction = pathDirections[framePoint]
        )
    }

    fun costColor(cost: Int): Color? = when {
        cost <= 0 -> null
        cost >= dangerCost -> Color.Red.copy(alpha = 0.25f)
        cost < normalCost -> Color.Cyan.copy(alpha = 0.15f) // cheap square, ZStar's highways
        else -> null
    }

    fun setStartPoint(gridX: Int, gridY: Int) {
        startPoint = FramePoint(gridX, gridY, direction = startDirection)
        clearResults()
        saveSettings()
    }

    fun toggleEnemy(gridX: Int, gridY: Int) {
        val existing = enemies.find { it.x == gridX && it.y == gridY }
        if (existing != null) {
            enemies.remove(existing)
        } else {
            enemies.add(FramePoint(gridX, gridY, direction = enemyDirection))
        }
        clearResults()
        saveSettings()
    }

    fun clearAll() {
        enemies.clear()
        startPoint = null
        startDirection = Direction.None
        enemyDirection = Direction.None
        ableToLongAttack = false
        clearResults()
        deleteSavedSettings()
    }

    /** the route is stale as soon as anything about the scenario changes */
    fun clearResults() {
        foundPaths.clear()
        selectedPathIndex = 0
        costs = null
        summary = ""
        refreshLookups()
    }

    fun selectPath(index: Int) {
        selectedPathIndex = index
        refreshLookups()
    }

    fun runSearch() {
        val start = startPoint ?: return
        if (enemies.isEmpty()) return

        foundPaths.clear()
        try {
            val result = RoutingSearchRunner.run(
                RoutingRequest(
                    algorithm = algorithm,
                    targetMode = targetMode,
                    start = start,
                    enemies = enemies.toList(),
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    startDirection = startDirection,
                    ableToLongAttack = ableToLongAttack,
                    halfPassable = halfPassable,
                    isLevel = isLevel,
                    mapNearest = mapNearest,
                    finishWithinStrikingRange = finishWithinStrikingRange,
                    findNearestSafeIfCurrentlyNotSafe = findNearestSafeIfCurrentlyNotSafe
                )
            )
            foundPaths.addAll(result.paths)
            selectedPathIndex = if (result.paths.isNotEmpty()) 0 else -1
            costs = result.costs
            dangerCost = result.dangerCost
            normalCost = result.normalCost
            summary = result.summary
            refreshLookups()
        } catch (e: Exception) {
            summary = "Error running ${algorithm.label}: ${e.message}"
            println(summary)
            e.printStackTrace()
        }
    }

    private fun refreshLookups() {
        enemyPoints = enemies.associate { it.noDir() to it.direction }
        targetPoints = RoutingSearchRunner.targetsFor(enemies.toList(), targetMode).toHashSet()
        val paths = if (selectedPathIndex == -1) foundPaths.toList() else
            listOfNotNull(foundPaths.getOrNull(selectedPathIndex))
        val directions = mutableMapOf<FramePoint, Direction?>()
        for (path in paths) {
            for (point in path) {
                directions[point.noDir()] = point.direction
            }
        }
        pathDirections = directions
    }

    fun saveSettings() {
        try {
            val lines = mutableListOf<String>()
            startPoint?.let { lines.add("start=${it.x},${it.y}") }
            if (enemies.isNotEmpty()) {
                lines.add("enemies=" + enemies.joinToString(";") {
                    "${it.x},${it.y},${(it.direction ?: Direction.None).name}"
                })
            }
            lines.add("longAttack=$ableToLongAttack")
            lines.add("startDirection=${startDirection.name}")
            lines.add("enemyDirection=${enemyDirection.name}")
            lines.add("algorithm=${algorithm.name}")
            lines.add("targetMode=${targetMode.name}")
            lines.add("halfPassable=$halfPassable")
            lines.add("isLevel=$isLevel")
            lines.add("mapNearest=$mapNearest")
            lines.add("finishWithinStrikingRange=$finishWithinStrikingRange")
            lines.add("findNearestSafe=$findNearestSafeIfCurrentlyNotSafe")
            lines.add("showCosts=$showCosts")
            lines.add("showTargets=$showTargets")
            lines.add("cellSize=$cellSize")
            settingsFile.writeText(lines.joinToString("\n"))
            println("Settings saved to ${settingsFile.absolutePath}")
        } catch (e: Exception) {
            println("Error saving settings: ${e.message}")
        }
    }

    /**
     * @param forceAlgorithm keep the algorithm this window was opened with instead of the saved one
     */
    fun loadSettings(forceAlgorithm: RoutingAlgorithm? = null) {
        try {
            if (!settingsFile.exists()) return
            val settings = settingsFile.readLines().mapNotNull { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size != 2) null else parts[0].trim() to parts[1].trim()
            }.toMap()

            settings["longAttack"]?.let { ableToLongAttack = it.toBooleanStrictOrNull() ?: false }
            settings["halfPassable"]?.let { halfPassable = it.toBooleanStrictOrNull() ?: true }
            settings["isLevel"]?.let { isLevel = it.toBooleanStrictOrNull() ?: false }
            settings["mapNearest"]?.let { mapNearest = it.toBooleanStrictOrNull() ?: true }
            settings["finishWithinStrikingRange"]?.let {
                finishWithinStrikingRange = it.toBooleanStrictOrNull() ?: true
            }
            settings["findNearestSafe"]?.let {
                findNearestSafeIfCurrentlyNotSafe = it.toBooleanStrictOrNull() ?: true
            }
            settings["showCosts"]?.let { showCosts = it.toBooleanStrictOrNull() ?: true }
            settings["showTargets"]?.let { showTargets = it.toBooleanStrictOrNull() ?: true }
            settings["cellSize"]?.toIntOrNull()?.let { cellSize = it.coerceIn(4, 24) }
            settings["startDirection"]?.let { startDirection = it.toDirection() }
            settings["enemyDirection"]?.let { enemyDirection = it.toDirection() }
            settings["algorithm"]?.let { name ->
                RoutingAlgorithm.entries.find { it.name == name }?.let { algorithm = it }
            }
            settings["targetMode"]?.let { name ->
                TargetMode.entries.find { it.name == name }?.let { targetMode = it }
            }
            forceAlgorithm?.let { algorithm = it }

            settings["start"]?.split(",")?.let { coords ->
                val x = coords.getOrNull(0)?.toIntOrNull()
                val y = coords.getOrNull(1)?.toIntOrNull()
                if (x != null && y != null) startPoint = FramePoint(x, y, direction = startDirection)
            }
            enemies.clear()
            settings["enemies"]?.takeIf { it.isNotEmpty() }?.split(";")?.forEach { enemy ->
                val coords = enemy.split(",")
                val x = coords.getOrNull(0)?.toIntOrNull()
                val y = coords.getOrNull(1)?.toIntOrNull()
                // older settings files have no direction
                val dir = coords.getOrNull(2)?.toDirection() ?: Direction.None
                if (x != null && y != null) enemies.add(FramePoint(x, y, direction = dir))
            }
            refreshLookups()
            println("Settings loaded from ${settingsFile.absolutePath}")
        } catch (e: Exception) {
            println("Error loading settings: ${e.message}")
        }
    }

    private fun String.toDirection(): Direction =
        Direction.entries.find { it.name == this } ?: Direction.None

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
fun RoutingTestUI(initialAlgorithm: RoutingAlgorithm? = null) {
    val uiState = remember {
        RoutingTestUIState(initialAlgorithm).apply {
            loadSettings(forceAlgorithm = initialAlgorithm)
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { uiState.runSearch() },
                    enabled = uiState.startPoint != null && uiState.enemies.isNotEmpty()
                ) {
                    Text("Run ${uiState.algorithm.label}")
                }

                Button(onClick = { uiState.clearAll() }) {
                    Text("Clear All")
                }

                PickerButton(
                    label = "Algo: ${uiState.algorithm.label}",
                    options = RoutingAlgorithm.entries,
                    optionLabel = { it.label }
                ) {
                    uiState.algorithm = it
                    uiState.clearResults()
                    uiState.saveSettings()
                }

                PickerButton(
                    label = "Target: ${uiState.targetMode.label}",
                    options = TargetMode.entries,
                    optionLabel = { it.label }
                ) {
                    uiState.targetMode = it
                    uiState.clearResults()
                    uiState.saveSettings()
                }

                PickerButton(
                    label = "Link dir: ${uiState.startDirection.name}",
                    options = listOf(
                        Direction.None, Direction.Up, Direction.Down, Direction.Left, Direction.Right
                    ),
                    optionLabel = { it.name }
                ) {
                    uiState.startDirection = it
                    uiState.startPoint = uiState.startPoint?.copy(direction = it)
                    uiState.clearResults()
                    uiState.saveSettings()
                }

                PickerButton(
                    label = "Enemy dir: ${uiState.enemyDirection.name}",
                    options = listOf(
                        Direction.None, Direction.Up, Direction.Down, Direction.Left, Direction.Right
                    ),
                    optionLabel = { it.name }
                ) {
                    uiState.enemyDirection = it
                    uiState.saveSettings()
                }

                if (uiState.foundPaths.isNotEmpty()) {
                    PickerButton(
                        label = if (uiState.selectedPathIndex == -1) "All Paths"
                        else "Path ${uiState.selectedPathIndex + 1}",
                        options = listOf(-1) + uiState.foundPaths.indices.toList(),
                        optionLabel = { index ->
                            if (index == -1) "All Paths"
                            else "Path ${index + 1} (${uiState.foundPaths[index].size} steps)"
                        }
                    ) { uiState.selectPath(it) }
                }

                Button(onClick = { uiState.showInstructions = !uiState.showInstructions }) {
                    Text("Info")
                }

                Spacer(Modifier.weight(1f))

                Button(onClick = {
                    uiState.cellSize = (uiState.cellSize - 2).coerceAtLeast(4)
                    uiState.saveSettings()
                }) { Text("-") }
                Button(onClick = {
                    uiState.cellSize = (uiState.cellSize + 2).coerceAtMost(24)
                    uiState.saveSettings()
                }) { Text("+") }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabeledCheckbox("Long attack", uiState.ableToLongAttack) {
                    uiState.ableToLongAttack = it
                    uiState.clearResults()
                    uiState.saveSettings()
                }
                LabeledCheckbox("Show targets", uiState.showTargets) {
                    uiState.showTargets = it
                    uiState.saveSettings()
                }
                LabeledCheckbox("Show costs", uiState.showCosts) {
                    uiState.showCosts = it
                    uiState.saveSettings()
                }
                LabeledCheckbox("Half passable", uiState.halfPassable) {
                    uiState.halfPassable = it
                    uiState.clearResults()
                    uiState.saveSettings()
                }
                LabeledCheckbox("Is level", uiState.isLevel) {
                    uiState.isLevel = it
                    uiState.clearResults()
                    uiState.saveSettings()
                }
                if (uiState.algorithm == RoutingAlgorithm.ZStarAStar) {
                    LabeledCheckbox("Map nearest", uiState.mapNearest) {
                        uiState.mapNearest = it
                        uiState.clearResults()
                        uiState.saveSettings()
                    }
                    LabeledCheckbox("Finish in range (isGoal)", uiState.finishWithinStrikingRange) {
                        uiState.finishWithinStrikingRange = it
                        uiState.clearResults()
                        uiState.saveSettings()
                    }
                    LabeledCheckbox("Dodge if unsafe", uiState.findNearestSafeIfCurrentlyNotSafe) {
                        uiState.findNearestSafeIfCurrentlyNotSafe = it
                        uiState.clearResults()
                        uiState.saveSettings()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                Column(Modifier.padding(8.dp)) {
                    Text("Coordinates:", fontWeight = FontWeight.Bold)
                    uiState.startPoint?.let {
                        Text("Start Point: (${it.x}, ${it.y}) ${uiState.startDirection.name}")
                    }
                    uiState.enemies.forEachIndexed { index, enemy ->
                        Text("Enemy ${index + 1}: (${enemy.x}, ${enemy.y}) ${enemy.direction?.name ?: "None"}")
                    }
                    if (uiState.summary.isNotEmpty()) {
                        Text(uiState.summary, fontWeight = FontWeight.Bold)
                    }
                    uiState.foundPaths.forEachIndexed { index, path ->
                        Text("Path ${index + 1}: ${path.size} steps, ends at ${path.lastOrNull()}")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.showInstructions) {
                Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Instructions:", fontWeight = FontWeight.Bold)
                        Text("• Left click: Set start point (green)")
                        Text("• Double click: Toggle enemy (red), placed facing the enemy dir")
                        Text("• Blue: Found path, magenta lines are the movement direction")
                        Text("• Orange: Target points (attack points for the attack point modes)")
                        Text("• Red tint: near an enemy, Cyan tint: cheap square (ZStar highway)")
                        Text("• Grid: ${uiState.gridWidth}x${uiState.gridHeight} (Map2d clamps past that)")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

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
                                GridCellView(
                                    cell = uiState.getCellAt(gridX, gridY),
                                    cellSize = uiState.cellSize,
                                    costColor = uiState::costColor,
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

@Composable
private fun <T> PickerButton(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = !expanded }) {
            Text(label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSelect(option)
                    expanded = false
                }) {
                    Text(optionLabel(option))
                }
            }
        }
    }
}

@Composable
private fun LabeledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridCellView(
    cell: GridCell,
    cellSize: Int = 16,
    costColor: (Int) -> Color? = { null },
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    val backgroundColor = when {
        cell.isStart -> Color.Green
        cell.hasEnemy -> Color.Red
        cell.isPath -> Color.Blue
        cell.isTarget -> Color(0xFFFF9800)
        else -> costColor(cell.cost)
            ?: if (cell.x % 8 == 0 || cell.y % 8 == 0) Color.LightGray.copy(alpha = 0.2f) else Color.White
    }

    Box(
        modifier = Modifier
            .size(cellSize.dp)
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
                val lineColor = Color.Magenta

                when (cell.direction) {
                    Direction.Up -> drawLine(
                        color = lineColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = lineThickness
                    )

                    Direction.Down -> drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = lineThickness
                    )

                    Direction.Left -> drawLine(
                        color = lineColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = lineThickness
                    )

                    Direction.Right -> drawLine(
                        color = lineColor,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = lineThickness
                    )

                    Direction.None -> {
                        // No direction line
                    }
                }
            }
        }

        when {
            cell.isStart -> Text("S", color = Color.White, fontSize = 4.sp)
            cell.hasEnemy -> Text(
                cell.enemyDirection?.toArrow() ?: "E", color = Color.White, fontSize = 6.sp
            )

            cell.isPath -> Text("•", color = Color.White, fontSize = 6.sp)
            else -> {
                if (cell.x % 10 == 0 && cell.y % 10 == 0) {
                    Text("${cell.x},${cell.y}", color = Color.Gray, fontSize = 2.sp)
                }
            }
        }
    }
}
