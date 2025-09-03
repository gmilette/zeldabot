package bot.plan.action

import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.Direction
import util.d

class LadderActionDecider {
    fun doLadderAction(state: MapLocationState): Direction {
        return if (state.frameState.ladderDeployed) {
            val dirToGo = state.bestDirection()
            d { " make new route ladder deployed Go dir: $dirToGo"}
            val modifier = if (dirToGo == Direction.None) {
                GamePad.randomDirection(state.link).toDirection()
            } else {
                dirToGo
            }
            modifier
        } else {
            Direction.None
        }
    }
}