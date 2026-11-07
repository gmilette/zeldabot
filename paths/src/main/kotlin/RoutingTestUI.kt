import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
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
 * What the placed enemies are. Sword carriers cannot be hit from the front, and the game decides
 * that from their TILE, so picking Sword also moves the synthetic state to a level that has them.
 */
enum class EnemyKind(val label: String) {
    Generic("Generic"),
    Sword("Sword (no front)")
}

data class RoutingRequest(
    val algorithm: RoutingAlgorithm,
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
    val maxDepth: Int = 300,
    val mapLoc: Int = 0,
    val enemyKind: EnemyKind = EnemyKind.Generic,
    /** price the squares in front of sword enemies out of the route, as RouteTo does */
    val avoidFrontOfEnemies: Boolean = false,
    /** replaces production's IsGoal, same shape as ZStar.ZRouteParam.isGoal */
    val goalOverride: ((FramePoint, Boolean) -> Boolean)? = null
)

data class GoalReached(
    val point: FramePoint,
    /** what the search passed to the goal test; the plain BFS always claims true */
    val askedSafe: Boolean,
    val actuallySafe: Boolean,
    val rules: List<String>,
    val nearestEnemy: FramePoint?,
    val distToNearestEnemy: Int
) {
    val reached: Boolean
        get() = rules.isNotEmpty()

    override fun toString(): String {
        val where = "(${point.x},${point.y})${point.direction?.let { " ${it.name}" } ?: ""}"
        val safety = if (askedSafe == actuallySafe) "safe=$askedSafe"
        else "safe=$askedSafe(asked)/$actuallySafe(grid)"
        val enemy = nearestEnemy?.let { " nearest enemy (${it.x},${it.y}) d=$distToNearestEnemy" } ?: ""
        return if (reached) "GOAL $where $safety via ${rules.joinToString(" + ")}$enemy"
        else "NOT a goal $where $safety$enemy"
    }
}

data class RoutingResult(
    val paths: List<List<FramePoint>>,
    val targets: List<FramePoint>,
    val costs: Map2d<Int>? = null,
    /** cost at or above this is "near an enemy" */
    val dangerCost: Int = ZStar.nearEnemyCost,
    /** cost below this is a cheap (highway) square */
    val normalCost: Int = 1,
    val goals: List<GoalReached> = emptyList(),
    val summary: String = ""
)

/**
 * Runs any of the [RoutingAlgorithm]s against a scenario built in the UI. Everything here is
 * plain (non compose) code so it can be called from tests too.
 */
object RoutingSearchRunner {
    /** the cost the plain BFS grid uses around an enemy, matches what the BFS UI always did */
    private const val bfsEnemyCost = 1000

    fun targetsFor(enemies: List<FramePoint>): List<FramePoint> =
        enemies.flatMap { AttackActionDecider.attackPointsNoCorner(it.noDir()) }.distinct()

    /**
     * The goal every search stops at is production's own IsGoal from RouteTo, run against a
     * synthetic MapLocationState. The targets are only what ZStar aims at and what is drawn.
     */
    private class GoalForRun(request: RoutingRequest) {
        private val override = request.goalOverride
        private val production = RoutingTestGoal.productionGoal(
            start = request.start,
            startDirection = request.startDirection,
            enemies = request.enemies,
            isLevel = request.isLevel,
            mapLoc = request.mapLoc,
            fullHealth = request.ableToLongAttack,
            swordEnemies = request.enemyKind == EnemyKind.Sword
        )

        val test: (FramePoint, Boolean) -> Boolean = override ?: production::isGoal

        val inFrontOf: List<FramePoint> =
            if (request.avoidFrontOfEnemies) production.inFrontOfEnemies() else emptyList()

        fun rulesAt(point: FramePoint, isSafe: Boolean): List<String> =
            if (!test(point, isSafe)) emptyList()
            else listOf(if (override == null) production.describe(point, isSafe) else "injected isGoal")
    }

    private fun frontNote(request: RoutingRequest, goal: GoalForRun): String = when {
        !request.avoidFrontOfEnemies -> ""
        goal.inFrontOf.isEmpty() ->
            " - don't attack front priced out nothing: no sword enemies (needs Enemy: ${EnemyKind.Sword.label})"
        else -> " - avoiding ${goal.inFrontOf.size} square(s) in front"
    }

    private fun describeGoal(
        request: RoutingRequest,
        goal: GoalForRun,
        point: FramePoint,
        askedSafe: Boolean,
        actuallySafe: Boolean
    ): GoalReached {
        val nearest = request.enemies.map { it.noDir() }.minByOrNull { point.distTo(it) }
        return GoalReached(
            point = point,
            askedSafe = askedSafe,
            actuallySafe = actuallySafe,
            rules = goal.rulesAt(point, askedSafe),
            nearestEnemy = nearest,
            distToNearestEnemy = nearest?.let { point.distTo(it) } ?: -1
        )
    }

    fun run(request: RoutingRequest): RoutingResult {
        // these are on by default and make the search crawl (they loop the open list every iteration)
        ZStar.DEBUG = false
//        ZStar.DEBUG_B = false
        ZStar.DEBUG_V = false

        val targets = targetsFor(request.enemies)
        val goal = GoalForRun(request)
        return when (request.algorithm) {
            RoutingAlgorithm.BreadthFirst -> runBreadthFirst(request, targets, goal)
            RoutingAlgorithm.ZStarAStar -> runZStar(request, targets, goal)
            RoutingAlgorithm.ZStarBreadthFirst -> runZStarBreadthFirst(request, targets, goal)
        }
    }

    private fun allPassable(request: RoutingRequest): Map2d<Boolean> =
        Map2d((0 until request.gridHeight).map {
            (0 until request.gridWidth).map { true }.toMutableList()
        })

    private fun bfsCosts(request: RoutingRequest, inFrontOf: List<FramePoint>): Map2d<Int> =
        // the base must be nonzero: a cost-ordered search over an all-zero grid has no notion of
        // "further", so the priority queue is arbitrary and it never expands outward
        Map2d((0 until request.gridHeight).map {
            (0 until request.gridWidth).map { 1 }.toMutableList()
        }).also { costs ->
            for (enemy in request.enemies) {
                costs.modifyTo(enemy.noDir(), MapConstants.oneGrid, bfsEnemyCost)
            }
            // the plain BFS takes no rParam, so forceHighCost has to land on its grid by hand
            for (point in inFrontOf) {
                costs.set(point, bfsEnemyCost)
            }
        }

    private fun runBreadthFirst(
        request: RoutingRequest, targets: List<FramePoint>, goal: GoalForRun
    ): RoutingResult {
        val passable = allPassable(request)
        val costs = bfsCosts(request, goal.inFrontOf)
        val neighborFinder = NeighborFinder(passable, request.halfPassable, request.isLevel).apply {
            costF = costs
        }
        return search(
            request, targets, goal, neighborFinder,
            costs = costs, dangerCost = bfsEnemyCost, normalCost = 1, name = "BFS"
        )
    }

    private fun runZStarBreadthFirst(
        request: RoutingRequest, targets: List<FramePoint>, goal: GoalForRun
    ): RoutingResult {
        val zstar = ZStar(allPassable(request), request.halfPassable, request.isLevel)
        // gives the breadth first search the same cost/passable grid ZStar.routeWithBfs would use
        zstar.setNeighborFinder(zRouteParam(request, targets, goal.test, goal.inFrontOf))
        return search(
            request, targets, goal, zstar.neighborFinder,
            costs = zstar.costsF, dangerCost = ZStar.nearEnemyCost, normalCost = 1000, name = "ZStar BFS"
        )
    }

    private fun search(
        request: RoutingRequest,
        targets: List<FramePoint>,
        goal: GoalForRun,
        neighborFinder: NeighborFinder,
        costs: Map2d<Int>,
        dangerCost: Int,
        normalCost: Int,
        name: String
    ): RoutingResult {
        val search = BreadthFirstSearch(goal.test, neighborFinder)
        if (search.isTheGoal(request.start)) {
            return RoutingResult(
                paths = emptyList(), targets = targets, costs = costs,
                dangerCost = dangerCost, normalCost = normalCost,
                goals = listOf(
                    describeGoal(request, goal, request.start, true, neighborFinder.isSafe(request.start))
                ),
                summary = "$name: start is already the goal, attack"
            )
        }
        val paths = search.breadthFirstSearch(request.start, targets, request.maxDepth)
        val goals = paths.map { path ->
            val end = path.last()
            describeGoal(request, goal, end, true, neighborFinder.isSafe(end))
        }
        return RoutingResult(
            paths = paths, targets = targets, costs = costs,
            dangerCost = dangerCost, normalCost = normalCost, goals = goals,
            summary = "$name: ${paths.size} path(s), ${targets.size} target(s)" +
                    frontNote(request, goal)
        )
    }

    private fun runZStar(
        request: RoutingRequest, targets: List<FramePoint>, goal: GoalForRun
    ): RoutingResult {
        val zstar = ZStar(allPassable(request), request.halfPassable, request.isLevel)
        val param = zRouteParam(request, targets, goal.test, goal.inFrontOf)
        val path = zstar.route(param)
        // ZStar strips the direction off of every point, put it back so the arrows render
        val withDirection = withDirections(path)
        // not "is the end IN targets": a goal is a place to attack FROM, and in striking range
        // mode the targets are the enemies, which link never stands on
        val reached = path.lastOrNull()?.let { end ->
            val safe = zstar.neighborFinder.isSafe(end)
            describeGoal(request, goal, withDirection.lastOrNull() ?: end, safe, safe)
        }
        return RoutingResult(
            paths = if (path.size > 1) listOf(withDirection) else emptyList(),
            targets = targets,
            costs = zstar.costsF,
            dangerCost = ZStar.nearEnemyCost,
            normalCost = 1000,
            goals = if (path.size > 1 && reached != null) listOf(reached) else emptyList(),
            summary = "ZStar A*: ${path.size} step(s), ${targets.size} target(s)" +
                    (if (reached?.reached == true) ", reached the goal" else ", stopped short of the goal") +
                    frontNote(request, goal)
        )
    }

    private fun zRouteParam(
        request: RoutingRequest,
        targets: List<FramePoint>,
        isGoal: (FramePoint, Boolean) -> Boolean,
        inFrontOf: List<FramePoint> = emptyList()
    ) = ZStar.ZRouteParam(
        start = request.start,
        targets = targets,
        enemies = request.enemies.map { it.noDir() },
        // where link came from, so the "don't turn around" rule applies like it does in game
        pointBeforeStart = if (request.startDirection == Direction.None) null else
            request.startDirection.opposite().pointModifier(1)(request.start),
        isGoal = isGoal,
        rParam = RouteTo.RoutingParamCommon(
            forceHighCost = inFrontOf,
            mapNearest = request.mapNearest,
            finishWithinStrikingRange = request.finishWithinStrikingRange,
            findNearestSafeIfCurrentlyNotSafe = request.findNearestSafeIfCurrentlyNotSafe
        )
    )

    private fun withDirections(path: List<FramePoint>): List<FramePoint> =
        path.mapIndexed { index, point ->
            val next = path.getOrNull(index + 1)
            // link arrives facing the way he walked in, so the last point keeps the incoming
            // direction rather than none at all: with no direction inRangeOf can never say
            // "can swing from here" and every A* route looked like it stopped short
            val dir = if (next != null) point.dirTo(next)
            else path.getOrNull(index - 1)?.dirTo(point)
            if (dir == null) point else point.copy(direction = dir)
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

    var algorithm by mutableStateOf(initialAlgorithm ?: RoutingAlgorithm.ZStarBreadthFirst)
    var enemyKind by mutableStateOf(EnemyKind.Generic)
    var avoidFrontOfEnemies by mutableStateOf(false)

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
    var showResults by mutableStateOf(false)
    var showSteps by mutableStateOf(false)
    var showCosts by mutableStateOf(true)
    var showTargets by mutableStateOf(true)

    var foundPaths = mutableStateListOf<List<FramePoint>>()

    var foundGoals = mutableStateListOf<GoalReached>()
    var selectedPathIndex by mutableStateOf(0) // -1 means show all paths
    var summary by mutableStateOf("")

    fun goalOf(index: Int): GoalReached? = foundGoals.getOrNull(index)

    // the grid cost the search accumulates: every point entered after the start. Turn cost is not
    // included, it lives in the search and is not reported back
    fun pathCost(index: Int): Int? {
        val grid = costs ?: return null
        val path = foundPaths.getOrNull(index) ?: return null
        return path.drop(1).sumOf { grid.get(it) }
    }

    fun pathTurns(index: Int): Int {
        val path = foundPaths.getOrNull(index) ?: return 0
        return path.zipWithNext().count { (a, b) -> a.direction != null && a.direction != b.direction }
    }

    fun pathLabel(index: Int): String {
        val cost = pathCost(index)?.let { ", cost $it" } ?: ""
        val turns = pathTurns(index).let { if (it > 0) ", $it turn(s)" else "" }
        return "Path ${index + 1} (${foundPaths[index].size} steps$cost$turns)"
    }

    var goalOverride: ((FramePoint, Boolean) -> Boolean)? = null

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
        foundGoals.clear()
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
        foundGoals.clear()
        try {
            val result = RoutingSearchRunner.run(
                RoutingRequest(
                    algorithm = algorithm,
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
                    findNearestSafeIfCurrentlyNotSafe = findNearestSafeIfCurrentlyNotSafe,
                    enemyKind = enemyKind,
                    avoidFrontOfEnemies = avoidFrontOfEnemies,
                    goalOverride = goalOverride
                )
            )
            foundPaths.addAll(result.paths)
            foundGoals.addAll(result.goals)
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
        targetPoints = RoutingSearchRunner.targetsFor(enemies.toList()).toHashSet()
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
            lines.add("halfPassable=$halfPassable")
            lines.add("isLevel=$isLevel")
            lines.add("mapNearest=$mapNearest")
            lines.add("finishWithinStrikingRange=$finishWithinStrikingRange")
            lines.add("findNearestSafe=$findNearestSafeIfCurrentlyNotSafe")
            lines.add("enemyKind=$enemyKind")
            lines.add("avoidFront=$avoidFrontOfEnemies")
            lines.add("showResults=$showResults")
            lines.add("showSteps=$showSteps")
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
            settings["enemyKind"]?.let { name ->
                EnemyKind.entries.find { it.name == name }?.let { enemyKind = it }
            }
            settings["avoidFront"]?.let { avoidFrontOfEnemies = it.toBooleanStrictOrNull() ?: false }
            settings["showResults"]?.let { showResults = it.toBooleanStrictOrNull() ?: false }
            settings["showSteps"]?.let { showSteps = it.toBooleanStrictOrNull() ?: false }
            settings["showCosts"]?.let { showCosts = it.toBooleanStrictOrNull() ?: true }
            settings["showTargets"]?.let { showTargets = it.toBooleanStrictOrNull() ?: true }
            settings["cellSize"]?.toIntOrNull()?.let { cellSize = it.coerceIn(4, 24) }
            settings["startDirection"]?.let { startDirection = it.toDirection() }
            settings["enemyDirection"]?.let { enemyDirection = it.toDirection() }
            settings["algorithm"]?.let { name ->
                RoutingAlgorithm.entries.find { it.name == name }?.let { algorithm = it }
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
                    label = "Enemy: ${uiState.enemyKind.label}",
                    options = EnemyKind.entries,
                    optionLabel = { it.label }
                ) {
                    uiState.enemyKind = it
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
                            else uiState.pathLabel(index)
                        }
                    ) { uiState.selectPath(it) }
                }

                Button(onClick = { uiState.showInstructions = !uiState.showInstructions }) {
                    Text("Info")
                }

                Button(onClick = { uiState.showResults = !uiState.showResults }) {
                    Text(if (uiState.showResults) "Hide results" else "Show results")
                }

                if (uiState.showResults) {
                    Button(onClick = { uiState.showSteps = !uiState.showSteps }) {
                        Text(if (uiState.showSteps) "Hide steps" else "Show steps")
                    }
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
                LabeledCheckbox("Don't attack front", uiState.avoidFrontOfEnemies) {
                    uiState.avoidFrontOfEnemies = it
                    uiState.saveSettings()
                    uiState.runSearch()
                }

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

            if (uiState.showResults) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                SelectionContainer {
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
                        val goal = uiState.goalOf(index)
                        Text(
                            "${uiState.pathLabel(index)} ends at ${path.lastOrNull()}",
                            fontWeight = if (index == uiState.selectedPathIndex) FontWeight.Bold
                            else FontWeight.Normal
                        )
                        Text(
                            "    ${goal?.toString() ?: "goal not reported"}",
                            color = when {
                                goal == null -> Color.Gray
                                goal.reached -> Color(0xFF1B5E20)
                                else -> Color.Red
                            }
                        )
                        if (uiState.showSteps &&
                            (uiState.selectedPathIndex == -1 || uiState.selectedPathIndex == index)
                        ) {
                            path.forEachIndexed { step, point ->
                                val last = step == path.size - 1
                                Text(
                                    "      ${step + 1}. (${point.x}, ${point.y}) " +
                                            (point.direction?.name ?: "none") +
                                            if (last) "  <- ${goal?.rules?.firstOrNull() ?: "no goal"}" else "",
                                    color = if (last && goal?.reached == true) Color(0xFF1B5E20)
                                    else Color.DarkGray
                                )
                            }
                        }
                    }
                    if (uiState.foundPaths.isEmpty()) {
                        uiState.goalOf(0)?.let { Text("Start: $it") }
                    }
                }
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
