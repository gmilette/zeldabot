package bot.state

import bot.Dir
import bot.EnemyState
import bot.FramePoint

data class Agent(
    val point: FramePoint,
    val dir: Dir,
    val state: EnemyState = EnemyState.Unknown,
    val isLoot: Boolean = false,
    val countDown: Int = 0
) {
    val x: Int
        get() = point.x

    val y: Int
        get() = point.y
}