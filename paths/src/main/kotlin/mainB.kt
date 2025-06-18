import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import bot.DirectoryConstants
import bot.ZeldaBot
import bot.plan.action.RouteTo
import bot.plan.runner.PlanRunner
import bot.state.*
import bot.state.map.MapConstants
import bot.state.map.MovingDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.FollowView
import util.d

fun main(vararg args: String) = application {
    for (arg in args) {
        d { " ARG: $arg"}
    }
    ZeldaBot.experiment = args.getOrNull(0)
    val isDev = args.contains("dev")
    DirectoryConstants.enableInfo = isDev
    val noUi = args.contains("noui")
    Window(
        visible = !noUi,
        onCloseRequest = ::exitApplication,
        state = WindowState(width = 800.dp, height = 600.dp),
        title = "ZeldaBot"
    ) {
        val model = remember { ZeldaModel() }
        if (!isDev) {
            model.changeDraw(false)
        }
        val debugView = remember { mutableStateOf(true) }
        if (isDev && debugView.value) {
            Debugview(model, debugView)
        } else {
            FollowView(model) {
                if (isDev) {
                    SwitchViewButton(debugView)
                }
            }
        }
    }
}

@Composable
private fun Debugview(model: ZeldaModel, debugView: MutableState<Boolean>) {

    val state = model.plan.value
    var count = remember { mutableStateOf(0) }
    var showMap = remember { mutableStateOf(false) }
    val act = remember { mutableStateOf(ZeldaBot.doAct) }
    val draw = remember { mutableStateOf(ZeldaBot.draw) }
    val ladder = remember { mutableStateOf(true) }
    val log = remember { mutableStateOf(ZeldaBot.log) }
    val inv = remember { mutableStateOf(ZeldaBot.invincible) }
    val max = remember { mutableStateOf(ZeldaBot.maxLife) }
    val allowAttack = remember { mutableStateOf(RouteTo.allowAttack) }
    val zapperLoc = remember { mutableStateOf(ZeldaBot.fixLocationToZapper) }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        SwitchViewButton(debugView)
        Text(
            text = "Plan: ${state?.mapLoc} ${state?.state?.currentMapCell?.mapData?.name ?: ""}",
            fontSize = 20.sp,
        )
        Text(
            text = state?.planRunner?.masterPlan?.toStringCurrentPlanPhase() ?: "None",
            fontSize = 20.sp,
        )
        Text(
            text = "Action (c): ${state?.currentAction}",
            fontSize = 20.sp,
        )
        Text(
            text = "Action (n): ${state?.planRunner?.afterThis()?.name ?: ""}",
            fontSize = 20.sp,
        )
        Text(
            text = "Action (n2): ${state?.planRunner?.afterAfterThis()?.name ?: ""}",
            fontSize = 20.sp,
        )
        Row(
            modifier = Modifier.align(Alignment.Start)
        ) {
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.forceDir(GamePad.A, num = 15)
                }) {
                Text("  A ")
            }
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.forceDir(GamePad.B, num = 15)
                }) {
                Text("  B ")
            }
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.forceDir(GamePad.Start, 2)
                }) {
                Text("  S  ")
            }
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.updateEnemies()
                }) {
                Text("  Update  ")
            }

        }

        Row(
            modifier = Modifier.align(Alignment.Start)
        ) {
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.forceDir(GamePad.MoveRight)
                }) {
                Text("  R  ")
            }
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.forceDir(GamePad.MoveLeft)
                }) {
                Text("  L  ")
            }
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.forceDir(GamePad.MoveUp)
                }) {
                Text("  U  ")
            }
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    model.forceDir(GamePad.MoveDown)
                }) {
                Text("  D  ")
            }
        }

        Row(
            modifier = Modifier.align(Alignment.Start)
        ) {

            Image(
                painter = painterResource("icon_key.png"),
                modifier = Modifier.size(40.dp).background(Color.LightGray).clickable {
                    model.addKey()
                },
                contentDescription = ""
            )

            Image(
                painter = painterResource("icon_bomb.png"),
                modifier = Modifier.size(40.dp).background(Color.LightGray).clickable {
                    model.addBomb()
                },
                contentDescription = ""
            )

            Image(
                painter = painterResource("icon_coin.png"),
                modifier = Modifier.size(40.dp).background(Color.LightGray).clickable {
                    model.addRupee()
                },
                contentDescription = ""
            )
            Image(
                painter = painterResource("icon_candle.png"),
                modifier = Modifier.padding(horizontal = 8.dp).size(40.dp).background(Color.LightGray).clickable {
                    model.addCandle()
                },
                contentDescription = ""
            )
        }

        Row(
            modifier = Modifier.align(Alignment.Start)
        ) {
            Row {
//                Text("Invincible")
//                Checkbox(
//                    checked = inv.value,
//                    onCheckedChange = {
//                        inv.value = it
//                        model.invincible(it)
//                    }
//                )

                Text("Log")
                Checkbox(
                    checked = log.value,
                    onCheckedChange = {
                        log.value = it
                        model.log(it)
                    }
                )

                Text("Attack")
                Checkbox(
                    checked = allowAttack.value,
                    onCheckedChange = {
                        allowAttack.value = it
                        model.allowAttack(it)
                    }
                )

                Text("Zap")
                Checkbox(
                    checked = zapperLoc.value,
                    onCheckedChange = {
                        zapperLoc.value = it
                        model.fixLocationToZapper(it)
                    }
                )

                Text("Max")
                Checkbox(
                    checked = max.value,
                    onCheckedChange = {
                        max.value = it
                        model.max(it)
                    }
                )

                Text("Ladder")
                Checkbox(
                    checked = ladder.value,
                    onCheckedChange = {
                        ladder.value = it
                        model.ladder(it)
                    }
                )

                Text("Act")
                Checkbox(
                    checked = act.value,
                    onCheckedChange = {
                        act.value = it
                        model.changeAct(it)
                    }
                )
                Text("Draw")
                Checkbox(
                    checked = draw.value,
                    onCheckedChange = {
                        draw.value = it
                        model.changeDraw(it)
                    }
                )
            }

            Row {
                Text("Map")
                Checkbox(
                    checked = showMap.value,
                    onCheckedChange = {
                        showMap.value = it
                    }
                )
            }
        }

        Row {
            Text("Enemies", fontSize = 20.sp)
            state?.state?.frameState?.let {
                val alive = it.enemiesClosestToLink(EnemyState.Alive).size
                val dead = it.enemiesClosestToLink(EnemyState.Dead).size
                val loot = it.enemiesClosestToLink(EnemyState.Loot).size
                val proj = it.enemiesClosestToLink(EnemyState.Projectile).size
                Text("Alive: $alive dead: $dead loot $loot $proj", modifier = Modifier.padding(12.dp))
            }
        }

        Row {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Enemies", fontSize = 20.sp)
                state?.enemiesInfo?.let { enemies ->
                    if (enemies.isNotEmpty()) {
                        enemies.sortedBy { it.point.y } .filter { it.state != EnemyState.Dead }.forEachIndexed { index, enemy ->
//                            val tileLine = MapStatsTracker.attribFor(enemy.tile).tileStringLine()
//                            + " " + tileLine
                            val moving = if (enemy.moving == MovingDirection.UNKNOWN_OR_STATIONARY) "" else enemy.moving.toArrow()
                            Text(
                                "$index: (${enemy.tile.toString(16)}_${enemy.attribute.toString(16)}) ${enemy.state.name} ${enemy.point} ${enemy.point.toG} ${enemy.color} ${enemy.dir.toArrow()}" +
                                        enemy.damagedString + " $moving"
                            )
                        }
                    }
                }
            }
        }

//            state?.state?.let {mState ->
//                HyruleMap(mState, state.planRunner)
//            }
        if (showMap.value) {
            state?.stateSnapshot?.let { mState ->
                HyruleMap(mState, state.planRunner)
            }
//                showMap.value = false
        }
    }
}

@Composable
private fun SwitchViewButton(debugView: MutableState<Boolean>) {
    val show = debugView.value
    Button(
        modifier = Modifier.background(Color.LightGray),
        onClick = {
        debugView.value = !show
    }) {
        Text("X")
    }
}

@Composable
private fun HyruleMap(state: MapLocationState, plan: PlanRunner) {
    val link = state.link
    val path: List<FramePoint> = emptyList()
    val enemies: List<Agent> = state.frameState.enemiesSorted.filter { it.state == EnemyState.Alive }
    val projectiles: List<Agent> = state.frameState.enemiesSorted.filter { it.state == EnemyState.Projectile }

    val v = 2
    Canvas(
        modifier = Modifier.width((MapConstants.MAX_X.toFloat() * v).dp)
            .height((MapConstants.MAX_Y.toFloat() * v).dp)
    ) {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            paint.color = Color.Black
            val linkPathPaint = Paint()
            linkPathPaint.style = PaintingStyle.Stroke
            linkPathPaint.strokeWidth = 2.0f
            linkPathPaint.color = Color.Blue
            val targetPaint = Paint()
            targetPaint.color = Color.Cyan
            val passablePaint = Paint()
            passablePaint.color = Color.LightGray
            val notPassable = Paint()
            notPassable.color = Color.Gray
            val enemyPaint = Paint()
            enemyPaint.color = Color.Red
            val projPaint = Paint()
            projPaint.color = Color.Yellow
//            canvas.drawRect(0f, 0f, MapConstants.MAX_X.toFloat()*(v+1), MapConstants.MAX_Y.toFloat()*(v+1), paint)

//            d { " draw map cell"}
//            //for
            val passable = state.currentMapCell.passable

            try {
                for (x in 0..255) {
                    val xa = x * v
                    for (y in 0..167) {
                        val ya = y * v
                        if (passable.get(x, y)) {
                            canvas.drawRect(
                                xa.toFloat(),
                                ya.toFloat(),
                                (xa + 1) * v.toFloat(),
                                (ya + 1) * v.toFloat(),
                                passablePaint
                            )
                        } else {
                            canvas.drawRect(
                                xa.toFloat(),
                                ya.toFloat(),
                                (xa + 1) * v.toFloat(),
                                (ya + 1) * v.toFloat(),
                                notPassable
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                util.e { " map drawing crashed $e" }
            }

            drawPoint(canvas, v, link, paint)
            // draw path: linkPathPaint
//            d { " draw num enemies ${enemies.size} enemies" }
            for (enemy in enemies) {
                drawGridPoint(canvas, v, enemy.point, enemyPaint)
            }
            for (pro in projectiles) {
                drawGridPoint(canvas, v, pro.point, projPaint)
            }

            drawPoint(canvas, v, plan.target(), targetPaint)
            for (target in plan.targets()) {
                drawPoint(canvas, v, target, targetPaint)
            }

            val path = Path()
            val linkPath = plan.path()
            if (linkPath.isNotEmpty()) {
                d { " draw path ${linkPath.size}" }
                path.moveTo(linkPath[0].x.toFloat() * v, linkPath[0].y.toFloat() * v)
                for (pt in linkPath) {
                    path.lineTo(pt.x.toFloat() * v, pt.y.toFloat() * v)
                }
                canvas.drawPath(path, linkPathPaint)
            } else {
                d { " draw no path " }
                //        Toolkit.getDefaultToolkit().beep()
            }
            // top is les than bottom
        }
    }
}

private fun drawPoint(canvas: Canvas, v: Int, pt: FramePoint, paint: Paint) {
    canvas.drawRect(pt.x * v.toFloat(), pt.y * v.toFloat(), (pt.x + 1) * v.toFloat(), (pt.y + 1) * v.toFloat(), paint)
}

private fun drawGridPoint(canvas: Canvas, v: Int, pt: FramePoint, paint: Paint) {
    canvas.drawRect(pt.x * v.toFloat(), pt.y * v.toFloat(), (pt.x + 16) * v.toFloat(), (pt.y + 16) * v.toFloat(), paint)
}

class ZeldaModel : ZeldaBot.ZeldaMonitor {
    val scope = CoroutineScope(Dispatchers.IO)
    val plan = mutableStateOf<ShowState?>(null)
    var enemiesInfo: List<Agent> = emptyList()
    private var updateEnemiesOnNext: Boolean = false

    private var stateSnapshot: MapLocationState? = null

    private var bot: ZeldaBot? = null

    init {
        start()
    }

    override fun update(state: MapLocationState, planRunner: PlanRunner) {
        if (updateEnemiesOnNext) {
            enemiesInfo = state.frameState.enemies.toMutableList()
            stateSnapshot = state
            updateEnemiesOnNext = false
        }
        plan.value = ShowState(
            currentAction = planRunner.action?.name ?: "",
            mapLoc = state.frameState.mapLoc,
            state = state,
            stateSnapshot = stateSnapshot,
            planRunner = planRunner,
            enemiesInfo = enemiesInfo
        )
    }

    fun start() {
        val monitor = this
        scope.launch {
            bot = ZeldaBot.startIt(monitor)
        }
    }

    fun forceDir(forcedDirection: GamePad, num: Int = MapConstants.oneGrid * 4) {
        ZeldaBot.unstick += num
        ZeldaBot.forcedDirection = forcedDirection
    }

    fun updateEnemies() {
        updateEnemiesOnNext = true
    }

    fun changeAct(act: Boolean) {
        ZeldaBot.doAct = act
    }

    fun changeDraw(draw: Boolean) {
        ZeldaBot.draw = draw
    }

    fun addKey() {
        ZeldaBot.addKey = true
    }

    fun addBomb() {
        ZeldaBot.addBomb = true
    }

    fun addRupee() {
        ZeldaBot.addRupee = true
    }

    fun addCandle() {
        ZeldaBot.addCandle = true
    }

    fun ladder(act: Boolean) {
        ZeldaBot.hasLadder = act
    }

    fun invincible(act: Boolean) {
        ZeldaBot.invincible = act
    }

    fun allowAttack(act: Boolean) {
        RouteTo.allowAttack = act
    }

    fun log(act: Boolean) {
        ZeldaBot.log = act
    }

    fun fixLocationToZapper(act: Boolean) {
        ZeldaBot.fixLocationToZapper = act
    }

    fun max(act: Boolean) {
        ZeldaBot.maxLife = act
    }

    data class ShowState(
        val currentAction: String,
        val mapLoc: MapLoc = 0,
        val state: MapLocationState,
        val stateSnapshot: MapLocationState?,
        val planRunner: PlanRunner,
        var enemiesInfo: List<Agent> = emptyList()
    )
}