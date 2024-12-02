package bot.plan.action

import bot.state.Agent
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.MapConstants
import bot.state.oam.MonstersOverworld
import bot.state.oam.lynelsTile
import util.d
import util.ifTrue

private fun isLyonel(tile: Int) = tile in MonstersOverworld.lynel.tile

private const val deathRowY = 45

private fun Agent.isLyonelInSafeSpot(): Boolean =
    point.y > deathRowY

/**
 * eventually the lyonel will respawn in a place where link can walk around
 */
fun whiteSwordManeuverSneakManeuver(moveAway: MoveTo, moveBack: MoveTo): Action =
    ForAWhile(CompleteIfLyonelSafe(resetScreen(moveAway, moveBack)))

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
    // make sure lyonel doesn't wander up
    //  && state.currentMapCell == moveBack.next
    override fun complete(state: MapLocationState): Boolean =
        (state.frameState.enemies.firstOrNull { isLyonel(it.tile) }?.isLyonelInSafeSpot() ?: false).also {
            d { " lyonel: ${state.frameState.enemies.firstOrNull { isLyonel(it.tile) }?.point } ${it.ifTrue("Safe")} "}
        }

    override val name: String
        get() = "CompleteIfLyonelSafe"
}

class ForAWhile(wrapped: Action) : WrappedAction(wrapped) {
    private var timesComplete = 0

    override fun reset() {
        timesComplete = 0
        super.reset()
    }

    override fun complete(state: MapLocationState): Boolean {
        val complete = wrapped.complete(state)
        if (complete) {
            d { " Completed so far $timesComplete"}
            timesComplete++
        }
        return timesComplete > 10
    }
}
