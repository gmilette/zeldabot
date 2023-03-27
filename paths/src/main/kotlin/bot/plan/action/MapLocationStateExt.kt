package bot.plan.action

import bot.state.*
import bot.state.map.grid


val MapLocationState.hasAnyLoot: Boolean
    get() = frameState.enemies.any { it.isLoot }

val MapLocationState.cleared: Boolean
    get() = !hasEnemies && !hasAnyLoot

fun MapLocationState.clearedWithMin(min: Int): Boolean =
    numEnemies <= min && !hasAnyLoot

fun MapLocationState.clearedWithMinIgnoreLoot(min: Int): Boolean =
    numEnemies <= min

fun MapLocationState.hasNearEnemy(threshold: Int = 32): Boolean =
    frameState.enemies.any { it.state == EnemyState.Alive && frameState.link.point.distTo(it.point) < threshold }

val MapLocationState.hasEnemies: Boolean
    get() = frameState.enemies.any { it.state == EnemyState.Alive }

val MapLocationState.numEnemies: Int
    get() = frameState.enemies.count { it.state == EnemyState.Alive }

val MapLocationState.aliveEnemies: List<Agent>
    get() = frameState.enemies.filter { it.state == EnemyState.Alive }

val middleGrids = listOf(
    FramePoint(7.grid, 5.grid),
    FramePoint(8.grid, 5.grid),
    FramePoint(9.grid, 5.grid),
    FramePoint(8.grid, 4.grid),
    FramePoint(8.grid, 6.grid),
)

fun MapLocationState.numEnemiesAliveInCenter(): Int {
    return frameState.enemies.filter { it.state == EnemyState.Alive } .count { agent ->
        middleGrids.any { it.isInGrid(agent.point) }
    }
}