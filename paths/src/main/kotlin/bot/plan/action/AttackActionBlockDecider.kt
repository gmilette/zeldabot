package bot.plan.action

import bot.state.*
import bot.state.map.*
import bot.state.oam.EnemyGroup
import bot.state.oam.swordDir
import util.Geom
import util.d

object AttackActionBlockDecider {

    /**
     * return null if no action should be taken
     */
    fun blockReflex(state: MapLocationState): GamePad? {
        val hasMagicShield = state.frameState.inventory.inventoryItems.hasMagicShield
        val blockableProjectiles = state.projectiles.filter { blockable(hasMagicShield, it) }
        for (projectile in blockableProjectiles) {
            val reflexAction = check(state.frameState.link, projectile)
            if (reflexAction != null) {
                return reflexAction
            }
        }
        return null
    }

    private fun blockable(magicShield: Boolean, projectile: Agent) =
        when (projectile.blockable) {
            Blockable.No -> false
            Blockable.WithSmallShield -> true
            Blockable.WithMagicShield -> magicShield
        }

    private fun check(link: Agent, projectile: Agent): GamePad? {
        if (projectile.dir == Direction.None) return null

        val modifier = projectile.dir.pointModifier(MapConstants.halfGrid)
        val projectileTarget = modifier(projectile.point).toRect()
        val facingProjectileDirection = projectile.dir.opposite()

        if (!projectileTarget.intersect(link.point.toRect())) {
            return null
        }

        // it's going to hit us!
        d { "Block reflex: about to get hit by ${projectile.point}"}
        return if (link.dir == facingProjectileDirection) {
            d { "Block reflex: wait and block"}
            GamePad.None
        } else {
            d { "Block reflex: face $facingProjectileDirection"}
            facingProjectileDirection.toGamePad()
        }
    }

}
