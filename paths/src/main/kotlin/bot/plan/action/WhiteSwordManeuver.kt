package bot.plan.action

import bot.state.Agent
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.oam.lynelsTile
import util.d

private fun isLyonel(tile: Int) = tile in lynelsTile

private const val deathRowY = 45

private fun Agent.isLyonelInSafeSpot(): Boolean =
    point.y > deathRowY

/**
 * eventually the lyonel will respawn in a place where link can walk around
 */
fun whiteSwordManeuverSneakManeuver(moveAway: MoveTo, moveBack: MoveTo): Action =
    CompleteIfLyonelSafe(resetScreen(moveAway, moveBack))

fun resetScreen(moveAway: MoveTo, moveBack: MoveTo): Action =
    OrderedActionSequence(
        listOf(
            GoIn(30, GamePad.MoveUp, true),
            moveAway,
            GoIn(30, GamePad.MoveDown, true),
            moveBack,
            GoIn(30, dir = GamePad.MoveUp, true)
        ), restartWhenDone = true, shouldComplete = true
    )

class CompleteIfLyonelSafe(wrapped: Action) : WrappedAction(wrapped) {
    //  && state.currentMapCell == moveBack.next
    override fun complete(state: MapLocationState): Boolean =
        state.frameState.enemies.firstOrNull { isLyonel(it.tile) }?.isLyonelInSafeSpot() ?: false.also {
            d { " lyonel: ${state.frameState.enemies.firstOrNull { isLyonel(it.tile) }?.point }"}
            state.frameState.logAliveEnemies()
            for (aliveEnemy in state.aliveEnemies) {
                d { " enemy: ${aliveEnemy.tile} is: ${isLyonel(aliveEnemy.tile)}"}
            }
        }

    override val name: String
        get() = "CompleteIfLyonelSafe"
}
