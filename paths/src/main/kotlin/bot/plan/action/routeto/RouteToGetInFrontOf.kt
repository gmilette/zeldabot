package bot.plan.action.routeto

import bot.state.Agent
import bot.state.EnemyState
import bot.state.FramePoint
import bot.state.MapLocationState
import bot.state.down2
import bot.state.downOneGrid
import bot.state.left2
import bot.state.leftHalfGrid
import bot.state.leftOneGrid
import bot.state.leftTwoGrid
import bot.state.map.MapConstants
import bot.state.map.MovingDirection
import bot.state.map.horizontal
import bot.state.map.pointModifier
import bot.state.oam.swordDir
import bot.state.relativeTo
import bot.state.right2
import bot.state.rightOneGrid
import bot.state.up2
import bot.state.upHalfGrid
import bot.state.upOneGrid
import bot.state.upTwoGrid
import util.d

object RouteToGetInFrontOf {
    fun getInFrontOfGridsForProjectiles(state: MapLocationState): List<FramePoint> =
        state.frameState.enemies.filter { it.state == EnemyState.Projectile }.flatMap { agent ->
            when (agent.moving) {
                MovingDirection.LEFT -> listOf(agent.point.leftTwoGrid.right2,
                    agent.point.leftTwoGrid.right2.upOneGrid)
                MovingDirection.RIGHT -> listOf(agent.point.rightOneGrid.left2)
                MovingDirection.DOWN -> listOf(agent.point.downOneGrid.up2)
                MovingDirection.UP -> listOf(agent.point.upTwoGrid.down2)
                // incorrect most likely
                is MovingDirection.DIAGONAL -> listOf(agent.point.relativeTo(agent.moving.slope))
                else -> emptyList()
            }.also {
                if (it.isNotEmpty()) {
                    d { "${agent.point} is moving ${agent.moving.javaClass.simpleName} in front --> $it" }
                }
            }
        }

    fun getInFrontOfGrids(state: MapLocationState): List<FramePoint> =
        getInFrontOfGridsSword(state) + getInFrontOfGridsForProjectiles(state)

    fun getInFrontOfGridsSword(state: MapLocationState): List<FramePoint> =
        state.frameState.enemies.flatMap { agent: Agent ->
            // sames as just agent.dir
            d { " sword agent at ${agent.point} dir ${agent.dir} calc ${swordDir.dirFront(agent)}"}
            swordDir.dirFront(agent)?.let { dir ->
                val pt = dir.pointModifier(MapConstants.oneGrid)(agent.point)
                listOf(
                    pt,
                    // ok to be next to the enemy, just not half on top of the enemy
                    if (dir.horizontal) pt.upHalfGrid else pt.leftHalfGrid
                )
            } ?: emptyList()
        }

    fun getInFrontOfGridsGhosts(state: MapLocationState): List<FramePoint> =
        // all the frames in front of the ghost are dangerous
        state.frameState.enemies.flatMap { agent: Agent ->
            swordDir.dirFront(agent)?.let { dir ->
                val pt = dir.pointModifier(MapConstants.oneGrid)(agent.point)
                listOf(
                    pt,
                    if (dir.horizontal) pt.upOneGrid else pt.leftOneGrid
                )
            } ?: emptyList()
        }

}