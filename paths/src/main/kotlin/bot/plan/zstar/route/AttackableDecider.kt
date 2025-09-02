package bot.plan.zstar.route

import bot.plan.action.AttackActionDecider
import bot.plan.zstar.FrameRoute
import bot.plan.zstar.Path
import bot.state.Agent
import bot.state.MapLocationState

object AttackableDecider {
    fun targets(state: MapLocationState): FrameRoute {
        val attackableAgents: List<Agent> = AttackActionDecider.aliveEnemiesCanAttack(state)
        return FrameRoute(attackableAgents.map { it.point })
    }
}