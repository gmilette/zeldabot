package bot.plan.action

import bot.state.Agent
import bot.state.FramePoint
import util.CalculateDirection

class ProjectileDirectionCalculator {
    // agents must be the same type (projectile or not) maybe same tile

    fun calc(prev: List<Agent>, current: List<Agent>) {
        // only need to calculate it once?
        for (agent in current) {
            prev.findOnDiagonalWith(agent)?.let { diagonalWith ->
                val dir = CalculateDirection.calculateDirection(agent.point, diagonalWith.point)
                // set diagonal direction or normal direction
            }
        }
    }

    private fun List<Agent>.findOnDiagonalWith(agent: Agent): Agent? =
        this.firstOrNull { CalculateDirection.isOnDiagonal(it.point, agent.point) }
}