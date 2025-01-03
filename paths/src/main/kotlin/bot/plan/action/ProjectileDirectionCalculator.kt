package bot.plan.action

import bot.state.Agent
import bot.state.EnemyState
import bot.state.FramePoint
import bot.state.map.MovingDirection
import util.CalculateDirection
import util.d

object ProjectileDirectionCalculator {
    private val DEBUG = false
    // agents must be the same type (projectile or not) maybe same tile
    fun calc(currentPoint: FramePoint, state: EnemyState, prev: List<Agent>, tile: Int): MovingDirection? {
        if (DEBUG) {
            d { " current point $currentPoint" }
            d { " agents " }
            for (agent in prev) {
                d { " prev ${agent.point} ${agent.state}" }
            }
            for (prevPoint in prev.filter { it.state == state && !it.damaged && it.tile == tile }) { // && it.tile == tile  don't check tile because it changes
                val dir = CalculateDirection.calculateDirection(prevPoint.point, currentPoint)
                d { "  to ${prevPoint.point} ${dir.toArrow()}" }
            }
        }
        return prev.asSequence()
            .filter { it.state == state && !it.damaged && it.tile == tile}
            .map { it.point }
            .map { prevPoint -> CalculateDirection.calculateDirection(prevPoint, currentPoint) }
            .firstOrNull { it != MovingDirection.UNKNOWN_OR_STATIONARY }
            .also {
                if (DEBUG) {
                    if (state == EnemyState.Projectile) {
                        if (it == null) {
                            d { " no moving dir $it" }
                        } else {
                            d { "moving dir current: $currentPoint moving ${it.toArrow()}" }
                        }
                    }
                }
            }
    }

    fun calc(prev: List<Agent>, current: List<Agent>): List<Agent> {
        // only need to calculate it once?
        return current.map { agent ->
            prev.findOnDiagonalWith(agent)?.let { diagonalWith ->
                val dir = CalculateDirection.calculateDirection(diagonalWith.point, agent.point)
                // set diagonal direction or normal direction
                agent.copy(moving = dir)
            } ?: agent
        }
//        for (agent in current) {
//            prev.findOnDiagonalWith(agent)?.let { diagonalWith ->
//                val dir = CalculateDirection.calculateDirection(agent.point, diagonalWith.point)
//                // set diagonal direction or normal direction
//            }
//        }
    }

    private fun List<Agent>.findOnDiagonalWith(agent: Agent): Agent? =
        this.firstOrNull { CalculateDirection.isOnDiagonal(it.point, agent.point) }
}