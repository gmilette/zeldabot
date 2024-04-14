package bot.plan.action

import bot.plan.zstar.FrameRoute
import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.*
import bot.state.oam.swordDir
import util.Geom
import util.LogFile
import util.d
import util.w
import kotlin.random.Random

class RouteTo(val params: Param = Param()) {
    companion object {
        fun hardlyReplan(dodgeEnemies: Boolean = true, ignoreProjectiles: Boolean = false) = RouteTo(
            Param(
                planCountMax = 100,
                whatToAvoid =
                if (!dodgeEnemies) {
                    WhatToAvoid.None
                } else if (ignoreProjectiles) {
                    WhatToAvoid.JustEnemies
                } else {
                    WhatToAvoid.All
                }
            )
        )
    }

    data class Param(
        var planCountMax: Int = 20,
        /**
         * what obstacles to avoid
         */
        val whatToAvoid: WhatToAvoid = WhatToAvoid.All
    ) {
        companion object {
            /**
             * if ignore projectiles, then just avoid enemies
             * otherwise ignore enemies and projectiles
             */
            fun makeIgnoreProjectiles(ignoreProjectiles: Boolean): WhatToAvoid =
                if (ignoreProjectiles) {
                    WhatToAvoid.JustEnemies // ignore projectiles
                } else {
                    WhatToAvoid.All
                }
        }
    }

    enum class WhatToAvoid {
        None, // avoid nothing even if they exist
        JustProjectiles, // no enemies
        JustEnemies, // no projectiles
        All // include projectiles and enemies
    }

    data class RouteParam(
        /**
         * trigger a new plan
         */
        val forceNew: Boolean = false,
        /**
         * if set, use this map cell, otherwise look up from the current state
         */
        val overrideMapCell: MapCell? = null,
        /**
         * for attacking use B
         */
        val useB: Boolean = false,
        val rParam: RoutingParamCommon = RoutingParamCommon()
    )

    data class RoutingParamCommon(
        val forcePassable: List<FramePoint> = emptyList(),
        // could be used to avoid spots in front of sword guys too
        val forceHighCost: List<FramePoint> = emptyList(),
        val attackTarget: FramePoint? = null,
        val ladderSpec: ZStar.LadderSpec? = null,
        /**
         * move any points to their nearest highway grid spot
         */
        val mapNearest: Boolean = false,
        /**
         * if true, stop searching when route puts link within striking range
         * otherwise route until reach the desired point
         */
        val finishWithinStrikingRange: Boolean = false,
        /**
         * good for the kill all scenario where everything must die
         * if link is just moving around though this will cause
         * too much distraction. Need a long range B weapon or full hearts available
         */
        val finishWithinLongStrikingRange: Boolean = false,
        /**
         * if current spot is not safe, get to a safe spot
         */
        val findNearestSafeIfCurrentlyNotSafe: Boolean = true
    )

    private val routeToFile: LogFile = LogFile("RouteTo")

    var route: FrameRoute? = null
        private set
    private var planCount = 0

    private val attack = AlwaysAttack()
    private val attackB = AlwaysAttack(useB = true)

    fun routeTo(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam = RouteParam(),
    ): GamePad {
        val canAttack = param.useB || state.frameState.canUseSword
        val attackPossible by lazy { params.whatToAvoid != WhatToAvoid.None && canAttack }
        d { " route To attackOrRoute attack=$attackPossible can=$canAttack" }
        val theAttack = if (param.useB) {
            attackB
        } else {
            attack
        }


        val leftCorner = state.link.leftOneGrid.upLeftOneGridALittleLess
        val nearLink = Geom.Rectangle(leftCorner, state.link.downTwoGrid.rightTwoGrid)
        val projectileNear = state.projectiles.any { it.point.toRect().intersect(nearLink) }
        val inRangeOf by lazy { AttackActionDecider.inRangeOf(state) }
        val shouldLongAttack by lazy { AttackLongActionDecider.shouldShootSword(state) }
        val allowAttack = false
        val blockReflex = AttackActionBlockDecider.blockReflex(state)
        return when {
            blockReflex != null -> {
                d { " Route Action -> Block Reflex! $blockReflex" }
                blockReflex
            }
            allowAttack && !projectileNear && attackPossible && attack.isAttacking() -> {
                d { " Route Action -> Keep Attacking" }
                theAttack.nextStep(state)
            }

            allowAttack && !projectileNear && attackPossible && canAttack && shouldLongAttack -> {
                d { " Route Action -> LongAttack" }
                theAttack.nextStep(state)
            }

            !allowAttack ||
                    !attackPossible ||
//                    !canAttack || // redundant
                    (state.frameState.clockActivated && Random.nextInt(10) == 1) ||
                    AttackActionDecider.getInFrontOfGrids(state) ||
                    inRangeOf == GamePad.None -> {
                attack.reset()
                attackB.reset()
                d { " Route Action -> No Attack" }
                doRouteTo(state, to, param)
            }

            else -> {
                d { " Route Action -> RangeAction $inRangeOf" }
                if (inRangeOf.isAttack) {
                    theAttack.nextStep(state)
                } else {
                    inRangeOf
                }
            }
        }
    }

    private fun writeFile(
        to: List<FramePoint>,
        state: MapLocationState,
        gamePad: GamePad
    ) {
        val target = to.minByOrNull { it.distTo(state.link) }
        val distTo = target?.distTo(state.link) ?: 0
        routeToFile.write(
            state.currentMapCell.mapLoc,
            target?.oneStr ?: "0_0",
            state.link.oneStr,
            distTo,
            planCount,
            gamePad.name
        )
    }

    private fun doRouteTo(
        state: MapLocationState,
        to: List<FramePoint>,
        paramIn: RouteParam
    ): GamePad {
        d { " DO routeTo TO ${to.size} points first ${to.firstOrNull()} currently at ${state.currentMapCell.mapLoc}" }
        val param = paramIn.copy(rParam = paramIn.rParam.copy(attackTarget = null))
        var forceNew = true || param.forceNew
        if (to.isEmpty()) {
            w { " no where to go " }
            return NavUtil.randomDir(state.link)
        }
        val linkPt = state.frameState.link.point
        val exitOffScreenAction = exitOfScreen(linkPt, to)
        if (exitOffScreenAction != GamePad.None) {
            return exitOffScreenAction
        }

        val skippedButIsOnRoute = (state.previousMove.skipped && route?.isOn(linkPt, 5) != null)
        if (skippedButIsOnRoute) {
            route?.popUntil(linkPt)
        }

        // getting me suck: && params.planCountMax != 1000
        // there are no enemies so it just keeps forcing replanning
        // make a new boolean force new ONCE
        if (!state.hasEnemiesOrLootOrProjectiles && params.planCountMax != 1000) {
            d { " NO alive enemies, no need to replan just go plan count max: ${params.planCountMax}" }
            // make a plan now though
            forceNew = false // wny is this true?? no enemies!
            params.planCountMax = 1000
        } else {
            d { " alive enemies, keep re planning" }
            // don't reset this if there are no more enemies
            // otherwise this gets into a loop, reset to 20, then force replan
            // set to 1000, then reset
            // TODO needs test, it breaks shop
//            if (state.hasEnemiesOrLoot && params.planCountMax != 1000) {
//                params.planCountMax = 20
//            }
            params.planCountMax = 20
        }

        var nextPoint: FramePoint = route?.pop() ?: FramePoint()
        val routeSize = route?.path?.size ?: 0

        // this is an optimization I dont think is necessary
        val linkPoints = linkPt.corners
        val enemiesNear =
            state.aliveOrProjectile.filter { it.point.minDistToAny(linkPoints) < MapConstants.oneGrid * 5 }

        // nothing to avoid if the clock is activated
        var avoid = if (!state.frameState.clockActivated) {
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

//
//        if (param.ignoreEnemies) {
//            d { "ignore enemies" }
//            avoid = avoid.filter { it.state != EnemyState.Alive }
//        }

        param.rParam.attackTarget?.let { targetAttack ->
            d { " remove enemy from filter $targetAttack" }
            avoid = avoid.filter { it.point != targetAttack }
        }

        for (agent in avoid) {
            d { " enemy avoid $agent" }
        }

        if (forceNew ||
            route == null || // reset
            routeSize <= 2 ||
            planCount >= params.planCountMax || // could have gotten off track
            !state.previousMove.movedNear || // got hit
            enemiesNear.isNotEmpty() // if near an enemy replan, probably not important
        ) {
            val why = when {
                forceNew -> "force new $planCount of ${params.planCountMax}"
                !state.previousMove.movedNear -> "got hit"
                planCount >= params.planCountMax -> "old plan max=${params.planCountMax}"
                route == null -> "no plan"
                routeSize <= 2 -> " 2 sized route"
                !skippedButIsOnRoute -> "skipped and not on route"
                enemiesNear.isNotEmpty() -> "nearby enemies, replan"
                else -> "I donno"
            }

            d { " Plan: ${state.currentMapCell.mapLoc} new plan! because ($why) to $to" }
            nextPoint = makeNewRoute(param, state, to, avoid, nextPoint)
        } else {
            d { " Plan: same plan ct $planCount" }
        }

        planCount++
        d { " go to from $linkPt to next $nextPoint $to" }

        return when {
            nextPoint.isZero && linkPt.x == 0 -> GamePad.MoveLeft
            nextPoint.isZero && linkPt.y == 0 -> GamePad.MoveUp
            // already in a good spot
            nextPoint.isZero -> GamePad.None
            else -> nextPoint.direction?.toGamePad() ?: linkPt.directionTo(nextPoint)
        }.also {
            //        writeFile(to, state, it)
            d { " next point $nextPoint dir: $it ${if (nextPoint.direction != null) "HAS DIR ${nextPoint.direction}" else ""}" }
        }
    }

    private fun makeNewRoute(
        param: RouteParam,
        state: MapLocationState,
        to: List<FramePoint>,
        avoid: List<Agent>,
        nextPoint: FramePoint
    ): FramePoint {
        val linkPt = state.frameState.link.point
        val ladder = state.frameState.ladder
        var nextPoint1 = nextPoint
        val passable = param.rParam.forcePassable.toMutableList()

        if (ladder != null) {
            passable.add(ladder.point)
        }

        val mapCell = param.overrideMapCell ?: state.currentMapCell

        val inFrontOfGrids = getInFrontOfGrids(state)

        route = FrameRoute(
            mapCell.zstar.route(
                ZStar.ZRouteParam(
                    start = linkPt,
                    targets = to,
                    pointBeforeStart = state.previousMove.from,
                    enemies = avoid.points,
                    rParam = param.rParam.copy(
                        forcePassable = passable,
                        forceHighCost = param.rParam.forceHighCost + inFrontOfGrids
                    )
//                    forcePassable = passable,
//                    attackTarget = param.attackTarget,
//                    mapNearest = param.mapNearest,
//                    forceHighCost = param.forceHighCost + inFrontOfGrids,
//                    // could just use this if enemyTarget isn't null
//                    finishWithinStrikingRange = param.finishWithinStrikingRange
                )
            )
        )

//        route?.path?.lastOrNull()?.let { lastPt ->
//            // if it is just projectile then don't try to route towards the projectiles
//            if (param.rParam.mapNearest || lastPt in to || state.frameState.ladderDeployed || !state.hasEnemies) {
//                d { "route to success target" }
//            }
//        }
        route?.next15()
        nextPoint1 = route?.popOrEmpty() ?: FramePoint() // skip first point because it is the current location
        nextPoint1 = route?.popOrEmpty() ?: FramePoint()
        if (nextPoint1.isZero) {
            d { "NO ROUTE" }
        }
        d { " next is $nextPoint1 of ${route?.numPoints ?: 0}" }
        route?.next5()
        planCount = 0
        return nextPoint1
    }

    private fun exitOfScreen(linkPt: FramePoint, to: List<FramePoint>): GamePad {
        // why this? let's go without it and see if it's ok
        // it gets stuck almost about to exit some levels
        // i'm not sure if this fixes it
        val closest = to.minBy { it.distTo(linkPt) }
        return if (linkPt.distTo(closest) <= 1) {
            d { " CLOSE!! $closest" }
            if (closest.y <= 1) {
                d { " CLOSE!! up" }
                GamePad.MoveUp
            } else if (closest.x <= 1) {
                d { " CLOSE!! left" }
                GamePad.MoveLeft
            } else if (closest.x >= MapConstants.MAX_X - 2) {
                d { " CLOSE!! right" }
                GamePad.MoveRight
            } else if (closest.y >= MapConstants.MAX_Y - 2) {
                d { " CLOSE!! down" }
                GamePad.MoveDown
            } else {
                GamePad.None
            }
        } else {
            GamePad.None
        }
    }
    private fun getInFrontOfGridsForProjectiles(state: MapLocationState): List<FramePoint> =
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

    private fun getInFrontOfGrids(state: MapLocationState): List<FramePoint> =
        getInFrontOfGridsSword(state) + getInFrontOfGridsForProjectiles(state)

    private fun getInFrontOfGridsSword(state: MapLocationState): List<FramePoint> =
        state.frameState.enemies.flatMap { agent: Agent ->
            swordDir.dirFront(agent)?.let { dir ->
                val pt = dir.pointModifier(MapConstants.oneGrid)(agent.point)
                listOf(
                    pt,
                    // ok to be next to the enemy, just not half on top of the enemy
                    if (dir.horizontal) pt.upHalfGrid else pt.leftHalfGrid
                )
            } ?: emptyList()
        }

    private fun getInFrontOfGridsGhosts(state: MapLocationState): List<FramePoint> =
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
