package bot.plan.action

import bot.state.Agent
import bot.state.EnemyState
import bot.state.FramePoint
import bot.state.map.MovingDirection
import util.CalculateDirection

object ProjectileDirectionCalculator {
    // agents must be the same type (projectile or not) maybe same tile
    fun calc(point: FramePoint, state: EnemyState, prev: List<Agent>): MovingDirection? {

        return prev.asSequence().filter { it.state == state }.map { it.point }
            .map { CalculateDirection.calculateDirection(it, point) }.firstOrNull { dir ->
                dir != MovingDirection.UNKNOWN_OR_STATIONARY
            }
    }


    fun calc(prev: List<Agent>, current: List<Agent>): List<Agent> {
        // only need to calculate it once?
        return current.map { agent ->
            prev.findOnDiagonalWith(agent)?.let { diagonalWith ->
                val dir = CalculateDirection.calculateDirection(agent.point, diagonalWith.point)
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