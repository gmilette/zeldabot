package ui

import ZeldaModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bot.state.map.destination.DestType
import bot.state.map.destination.ZeldaItem

@Composable
fun FollowView(
    model: ZeldaModel,
    switch: @Composable () -> Unit
) {
    val state = model.plan.value ?: return

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        with(state.planRunner.masterPlan) {
            FollowCard(
                title = "Phase",
                width = 160.dp,
                height = 40.dp
            ) {
                Text(
                    text = toStringCurrentPhase(),
                    modifier = Modifier.padding(4.dp),
                    fontSize = 14.sp,
                )
            }
            FollowCard(
                title = "Segment",
                height = 40.dp,
                width = 160.dp,
                text = ""
            ) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    fontSize = 12.sp,
                    text = toStringCurrentSeg()
                )
            }
        }
        ImageActionObjective(state)
        // current action (icon)
        // current objective (icon)
        Row {
            FollowCard(title = "Location", text = "${state.state.frameState.level} : ${state.state.frameState.mapLoc}")
            TimeElapsed(state)
        }

        Progress(state)

        Row {
            val hits = state.planRunner.runLog.totalHits
            val bombs = state.planRunner.runLog.bombsUsed.total
            FollowCard(title = "Hits", text = "$hits")
            FollowCard(title = "Bombs", text = "$bombs")
//            FollowCard(width = 100.dp, title = "Potions Refills", text = "1")
//            FollowCard(width = 100.dp, title = "Bomb Refills", text = "1")
//            FollowCard(width = 100.dp, title = "Rupee Refills", text = "1")
        }

        Row {
            Spacer(modifier = Modifier.weight(1.0f))
            switch()
        }
    }
}

@Composable
private fun ImageActionObjective(state: ZeldaModel.ShowState) {
    val obj = state.planRunner.masterPlan.currentSeg().objective
    val type = obj.type
    val image = when (type) {
        is DestType.Level -> "icon_tri.png"
        is DestType.Heart -> "icon_heart.png"
        is DestType.SecretToEverybody -> "icon_coin.png"
        is DestType.Item -> {
            when (type.item) {
                ZeldaItem.Raft -> "icon_raft.png"
                ZeldaItem.WoodenSword -> "icon_woodensword.png"
                ZeldaItem.WhiteSword -> "icon_whitesword.png"
                ZeldaItem.MagicSword -> "icon_sword.png"
                else -> "icon_empty.png"
            }
        }
        is DestType.Triforce -> "icon_linktri.png"
        is DestType.Princess -> "icon_linktri.png"
        is DestType.Woman -> "icon_empty.png"
        is DestType.Shop -> "icon_empty.png"
        else -> "icon_empty.png"
    }

    Row {
        FollowCard(title = "objective", background = Color.Green) {
            Image(
                painter = painterResource(image),
        modifier = Modifier.size(40.dp).align(Alignment.Center).background(Color.LightGray),
                contentDescription = ""
            )
        }

//        FollowCard(title = "objective", text = obj.type.name, background = Color.Green)

        val act = state.currentAction.lowercase()
        val imageAction = when {
            // KILL ALL
            act.contains("move") -> "icon_move.png"
            act.contains("kill all") -> "icon_kill.png"
            act.contains("push") -> "icon_block.png"
            else -> "icon_empty.png"
        }

        FollowCard(title = "action", background = Color.Black) {
            Image(
                painter = painterResource(imageAction),
                modifier = Modifier.size(40.dp).align(Alignment.Center).background(Color.LightGray),
                contentDescription = ""
            )
        }
    }
}

@Composable
private fun TimeElapsed(state: ZeldaModel.ShowState) {
    val time = (System.currentTimeMillis() - state.planRunner.runLog.started) / 1000
    val minutes = (time / 60)
    val seconds = time - (minutes * 60)
    FollowCard(title = "Time", text = "${minutes.pad()}:${seconds.pad()}")
}

@Composable
private fun Progress(state: ZeldaModel.ShowState) {
    val numLeft = state.planRunner.masterPlan.actionsLeft
    FollowCard(
        title = "Progress",
        width = 160.dp,
        text = "$numLeft (${state.planRunner.masterPlan.percentDoneInt}%)"
    ) {
//            LinearProgressIndicator(modifier = Modifier.padding(bottom = 2.dp).height(4.dp),
//                progress = state.planRunner.masterPlan.percentDone)

        CustomLinearProgressIndicator(
            modifier = Modifier.padding(bottom = 2.dp).height(4.dp),
            progress = state.planRunner.masterPlan.percentDone
        )
    }
}

private fun Long.pad(): String =
    if (this < 10) "0$this" else this.toString()

@Composable
fun CustomLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    progressColor: Color = Color.Blue,
    backgroundColor: Color = Color.Gray,
    clipShape: Shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .clip(clipShape)
            .background(backgroundColor)
            .height(8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(progressColor)
                .fillMaxHeight()
                .fillMaxWidth(progress)
        ) {
            Text("H")
        }
    }
}

@Composable
private fun FollowCard(
    width: Dp = 80.dp, height: Dp = 80.dp,
    title: String, text: String = "",
    background: Color = Color.White,
    footer: @Composable BoxScope.() -> Unit = {}
) {
    Card(
        modifier = Modifier.width(width).height(height).padding(8.dp),
        border = BorderStroke(2.dp, Color.Black),
        backgroundColor = background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.padding(top = 8.dp, start = 8.dp),
                text = title,
                color = Color.Gray,
                textAlign = TextAlign.Start,
                fontSize = 10.sp
            )
            Box(modifier = Modifier.fillMaxSize()) {
                if (text.isNotEmpty()) {
                    Text(
                    modifier = Modifier.align(Alignment.Center),
                        color = Color.Black,
                        text = text,
                        textAlign = TextAlign.Start,
                        fontSize = 16.sp
                    )
                }
                Box(
                    modifier = if (text.isEmpty()) Modifier.align(Alignment.BottomCenter) else Modifier.align(Alignment.BottomStart)
                ) {
                    footer()
                }
            }
        }
    }
}