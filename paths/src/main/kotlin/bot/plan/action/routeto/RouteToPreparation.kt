package bot.plan.action.routeto

import bot.plan.action.*
import bot.plan.action.RouteTo.Param
import bot.plan.action.RouteTo.RouteParam
import bot.plan.action.RouteTo.WhatToAvoid
import bot.plan.zstar.route.AttackableDecider
import bot.state.Agent
import bot.state.FramePoint
import bot.state.Inventory
import bot.state.MapLocationState
import util.d

/**
 * unchanged data while trying different routes
 */
class RoutePreparation(val params: Param = Param()) {
    var attackable: List<Agent> = emptyList()
    var boomerangable: List<FramePoint> = emptyList()
    // can be stopped by a bubble
    var canAttack = false
    // link can still attack even if hit with bubble
    var canLongAttack = false
    var attackPossible = false
//        var attackWithWand
    var useB = false

    var avoid: List<Agent> = emptyList()
    var avoidProjectiles: List<Agent> = emptyList()
    var passable: List<FramePoint> = emptyList()
    var forceHighCost: List<FramePoint> = emptyList()

    fun prepare(
        state: MapLocationState,
        param: RouteParam = RouteParam(),
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList()
    ) {
        // Just changed linkDoingAnAttack to be more specific
        // NEED TO TEST THIS
        canAttack = param.allowAttack && !state.frameState.linkDoingAnAttack() && (param.useB || state.frameState.canUseSword)
        canLongAttack = param.allowAttack && !state.frameState.linkDoingAnAttack()
        attackPossible = params.whatToAvoid != WhatToAvoid.None // && canAttack // Test this comment

        // attack with wand as if it is a sword
        val attackWithWand =
            param.allowAttack && !state.frameState.canUseSword && state.frameState.inventory.selectedItem == Inventory.Selected.wand
        useB = if (attackWithWand) {
            true
        } else {
            param.useB
        }

        val attackableAgents: List<Agent> = AttackableDecider.aliveEnemiesCanAttack(state)
        val specOrAgents: List<Agent> = attackableSpec.ifEmpty {
            attackableAgents
        }
        attackable = specOrAgents

        val level = state.frameState.level

        val affectedByProjectileAgents: List<Agent> = if (state.boomerangActive) {
            specOrAgents.filter { it.affectedByBoomerang(level) }
        } else {
            specOrAgents.filter { it.arrowKillable(level) }
        }
        val affectedByProjectileLoot = state.loot.filter { it.lootNeeded(state) }
        boomerangable =
            (affectedByProjectileAgents + affectedByProjectileLoot)
                .map { it.point }  // won't boomerang for useless stuff like keys, compass, etc.

        // what is only boomerangable?
//        onlyBoomerangable = (boomerangable - attackable.map { it.point }.toSet())

        prepareAvoid(state, param)

        //// LOG
        log(param, state, attackableSpec, attackableAgents)
    }

    private fun log(
        param: RouteParam,
        state: MapLocationState,
        attackableSpec: List<Agent>,
        attackableAgents: List<Agent>
    ) {
        d { " route To attackOrRoute attack=$attackPossible can=$canAttack allowBlock=${param.allowBlock} avoid=${params.whatToAvoid} useB=${useB} canUseSword=${state.frameState.canUseSword} spec`${attackableSpec}" }
        if (state.frameState.linkDoingAnAttack()) {
            // observation: This always lasts 15 frames
            d { " xxLink is attackingxx " }
        }

        for (agent in attackableSpec) {
            d { " attackableSpec: $agent" }
        }

        for (framePoint in attackableAgents) {
            d { " attackable agent: $framePoint" }
        }
        if (attackable.isEmpty()) {
            d { "No attackable" }
        } else {
            for (framePoint in attackable) {
                d { " attackable: $framePoint" }
            }
        }
        for (framePoint in boomerangable) {
            d { " boomerangable: $framePoint" }
        }
    }

    private fun prepareAvoid(state: MapLocationState, routeParam: RouteParam) {
        avoid = if (state.frameState.clockActivated) {
            emptyList()
        } else {
            // this seems to be ok, except link can get hit from the side
            // unless it avoids projectiles
            when (params.whatToAvoid) {
                WhatToAvoid.None -> emptyList()
                WhatToAvoid.JustProjectiles -> state.projectiles
                WhatToAvoid.JustEnemies -> state.aliveEnemies
                else -> state.aliveOrProjectile
            }
        }

        avoidProjectiles = if (state.frameState.clockActivated) {
            emptyList()
        } else {
            when (params.whatToAvoid) {
                WhatToAvoid.None,
                WhatToAvoid.JustEnemies -> emptyList()
                else -> state.projectiles
            }
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