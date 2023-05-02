package bot.plan.action

import bot.state.FramePoint
import bot.state.FrameState
import bot.state.MapLocationState
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import util.d

object AttackAction {
    fun shouldAttack(state: MapLocationState) =
        shouldAttack(
            state.frameState.link.dir, // ?: state.previousMove.dirActual,
            state.link,
            state.aliveEnemies.map { it.point })

    private fun shouldAttack(from: Direction, link: FramePoint, enemiesClose: List<FramePoint>, dist: Int = MapConstants.oneGrid): Boolean {
        val attackDirectionGrid = from.pointModifier(dist - 1)(link) // -1 otherwise the sword is just out of reach

        d { "should attack dir = $from link = $link dirGrid = $attackDirectionGrid" }

        // if it is on top of link ALWAYS attack, if it is in the direction link is facing, also attack
//        return enemiesClose.any { it.isInGrid(link) || it.isInGrid(attackDirectionGrid) }
        return enemiesClose.any { link.isInGrid(it) || attackDirectionGrid.isInGrid(it) }
    }
}