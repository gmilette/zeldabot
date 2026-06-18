package bot.plan.action

import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.Direction
import bot.state.map.horizontal
import util.d

/**
 * if ladder is deployed perform the ladder action to get off of it
 */
class LadderActionDecider {
    fun doLadderAction(state: MapLocationState): Direction {
        return if (state.frameState.ladderDeployed) {
            val allowed = allowed(state)
            val dirToGo = state.bestDirection(allowed)
            d { " make new route ladder deployed Go dir: $dirToGo from $allowed ladder is ${state.frameState.ladder?.dir}"}
            if (dirToGo == Direction.None) {
                GamePad.randomDirection(state.link).toDirection()
            } else {
                dirToGo
            }
        } else {
            Direction.None
        }
    }

    private fun allowed(state: MapLocationState): Set<Direction> {
        val dir = state.frameState.ladder?.dir ?: return emptySet<Direction>()
        return if (dir.horizontal) Direction.horizontalSet else Direction.verticalSet
    }
}