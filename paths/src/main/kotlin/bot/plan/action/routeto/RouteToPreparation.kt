package bot.plan.action.routeto

import bot.plan.action.Action
import bot.plan.action.AlwaysAttack
import bot.plan.action.AttackActionDecider
import bot.plan.action.RouteTo.Param
import bot.plan.action.RouteTo.RouteParam
import bot.plan.action.RouteTo.WhatToAvoid
import bot.plan.action.affectedByBoomerang
import bot.plan.action.aliveEnemies
import bot.plan.action.aliveOrProjectile
import bot.plan.action.arrowKillable
import bot.plan.action.boomerangActive
import bot.plan.action.loot
import bot.plan.action.lootNeeded
import bot.plan.action.projectiles
import bot.state.*
import util.d

/**
 * unchanged data while trying different routes
 */
class RoutePreparation(val params: Param = Param()) {
    var attackable: List<Agent> = emptyList()
    var boomerangable: List<FramePoint> = emptyList()
    var canAttack = false
    var attackPossible = false
//        var attackWithWand
    var useB = false

    var avoid: List<Agent> = emptyList()
    var avoidProjectiles: List<Agent> = emptyList()
    var passable: List<FramePoint> = emptyList()
    var forceHighCost: List<FramePoint> = emptyList()

    fun prepare(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam = RouteParam(),
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList()
    ) {
        canAttack = param.allowAttack && !state.frameState.linkDoingAnAttack() && (param.useB || state.frameState.canUseSword)
        attackPossible = params.whatToAvoid != WhatToAvoid.None && canAttack

        // attack with wand as if it is a sword
        val attackWithWand =
            param.allowAttack && !state.frameState.canUseSword && state.frameState.inventory.selectedItem == Inventory.Selected.wand
        useB = if (attackWithWand) {
            true
        } else {
            param.useB
        }

        val attackableAgents: List<Agent> = AttackActionDecider.aliveEnemiesCanAttack(state)
        attackable = attackableSpec.ifEmpty {
            attackableAgents
        }

        val level = state.frameState.level
        // same with wand? not quite but close
        val specOrAgents: List<Agent> = attackableSpec.ifEmpty {
            attackableAgents
        }

        val affectedByProjectileAgents: List<Agent> = if (state.boomerangActive) {
            specOrAgents.filter { it.affectedByBoomerang(level) }
        } else {
            specOrAgents.filter { it.arrowKillable(level) }
        }
        val affectedByProjectileLoot = state.loot.filter { it.lootNeeded(state) }
        boomerangable =
            (affectedByProjectileAgents + affectedByProjectileLoot)
                .map { it.point }  // won't boomerang for useless stuff like keys, compass, etc.
        val onlyBoomerangagle = (boomerangable - attackable.map { it.point })

        prepareAvoid(state, param)

        //// LOG
        d { " route To attackOrRoute attack=$attackPossible can=$canAttack allowBlock=${param.allowBlock} avoid=${params.whatToAvoid} useB=${useB} canUseSword=${state.frameState.canUseSword} spec`${attackableSpec}"}
        if (state.frameState.linkDoingAnAttack()) {
            d { " xxLink is attackingxx " }
        }

        for (agent in attackableSpec) {
            d { " attackableSpec: $agent" }
        }

        for (framePoint in attackableAgents) {
            d { " attackable agent: $framePoint" }
        }
        for (framePoint in attackable) {
            d { " attackable: $framePoint" }
        }
        for (framePoint in boomerangable) {
            d { " boomerangable: $framePoint" }
        }
        for (framePoint in onlyBoomerangagle) {
            d { " boomerangable: $framePoint" }
        }

        if (attackable.isEmpty()) {
            d { "No attackable" }
        }
    }

    private fun prepareAvoid(state: MapLocationState, routeParam: RouteParam) {
        // nothing to avoid if the clock is activated
        avoid = if (!state.frameState.clockActivated) {
            // this seems to be ok, except link can get hit from the side
            // unless it avoids projectiles
            when (params.whatToAvoid) {
                WhatToAvoid.None -> emptyList()
                WhatToAvoid.JustProjectiles -> state.projectiles
                WhatToAvoid.JustEnemies -> state.aliveEnemies
                else -> state.aliveOrProjectile
            }
        } else {
            emptyList()
        }

        avoidProjectiles = if (!state.frameState.clockActivated) {
            // this seems to be ok, except link can get hit from the side
            // unless it avoids projectiles
            when (params.whatToAvoid) {
                WhatToAvoid.None,
                WhatToAvoid.JustEnemies -> emptyList()
                else -> state.projectiles
            }
        } else {
            emptyList()
        }

        val inFrontOfGrids = RouteToGetInFrontOf.getInFrontOfGrids(state)
        for (point in inFrontOfGrids) {
            d { "in front grid $point"}
        }

        forceHighCost = routeParam.rParam.forceHighCost + inFrontOfGrids

//        val passable = routeParam.rParam.forcePassable.toMutableList()
//        state.frameState.ladder?.let {
//            passable.add(it.point)
//        }
//        this.passable = passable

        passable = state.frameState.ladder?.let {
            routeParam.rParam.forcePassable + listOf(it.point)
        } ?: routeParam.rParam.forcePassable

//        val paramZ = ZStar.ZRouteParam(
//            start = linkPt,
//            targets = to,
//            pointBeforeStart = state.previousMove.from,
//            enemies = avoid.points,
//            projectiles = avoidProjectiles.points, // don't add if there is no dodging
//            rParam = param.rParam.copy(
//                forcePassable = passable,
//                forceHighCost = param.rParam.forceHighCost + inFrontOfGrids
//            )
//        )
    }
}