package bot.plan.action

import bot.plan.zstar.FrameRoute
import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.*
import bot.state.oam.EnemyGroup
import bot.state.oam.swordDir
import util.Geom
import util.LogFile
import util.d
import util.w
import kotlin.random.Random

class RouteTo(val params: Param = Param()) {
    private var boomerangCt = 0

    companion object {
        /**
         * dont shoot the boomerang too much
         */
        private val WAIT_BETWEEN_BOOMERANG = 20
        private val WAIT_BETWEEN_NOT_BOOMERANG = 2
        var allowAttack = true
        fun hardlyReplan(dodgeEnemies: Boolean = true,
                         /** don't try to block or route around projectiles **/
                         ignoreProjectiles: Boolean = false) = RouteTo(
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
        val allowBlock: Boolean = true,
        val allowAttack: Boolean = true,
        val allowRangedAttack: Boolean = true,
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
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList()
    ): GamePad {
        val canAttack = param.allowAttack && (param.useB || state.frameState.canUseSword)

        // attack with wand as if it is a sword
        val attackWithWand =
            param.allowAttack && !state.frameState.canUseSword && state.frameState.inventory.selectedItem == Inventory.Selected.wand
        val useB = if (attackWithWand) {
            true
        } else {
            param.useB
        }

        val attackPossible by lazy { params.whatToAvoid != WhatToAvoid.None && canAttack }
        d { " route To attackOrRoute attack=$attackPossible can=$canAttack allowBlock=${param.allowBlock} avoid=${params.whatToAvoid} waitBoom=$boomerangCt useB=${useB} canUseSword=${state.frameState.canUseSword} spec`${attackableSpec}" }
        val theAttack = if (useB) {
            attackB
        } else {
            attack
        }

        val attackableAgents: List<Agent> = AttackActionDecider.aliveEnemiesCanAttack(state)
        val attackable = attackableSpec.ifEmpty {
            attackableAgents
        }

        val level = state.frameState.level
        // same with wand? not quite but close
        val specOrAgents: List<Agent> = attackableSpec.ifEmpty {
            attackableAgents
        }
        val boomerangable: List<FramePoint> =
            (specOrAgents.filter { it.affectedByBoomerang(level) } +
                state.loot.filter { it.lootNeeded(state) }).map { it.point }  // won't boomerang for useless stuff like keys, compass, etc.
//        val boomerangable: List<FramePoint> = attackableSpec.ifEmpty {
//            (attackableAgents.filter { it.affectedByBoomerang(level) } +
//                    state.loot.filter { it.lootNeeded(state) }  // won't boomerang for useless stuff like keys, compass, etc.
//            ).map { it.point }
//        }
        for (framePoint in attackableAgents) {
            d { " attackable agent: $framePoint" }
        }
        for (framePoint in attackable) {
            d { " attackable: $framePoint" }
        }
        for (framePoint in boomerangable) {
            d { " boomerangable: $framePoint" }
        }
        val onlyBoomerangagle = (boomerangable - attackable)
        for (framePoint in onlyBoomerangagle) {
            d { " boomerangable: $framePoint" }
        }

        if (attackable.isEmpty()) {
            d { "No attackable" }
        }

        val dodgeReflex: GamePad = GamePad.None //idea
        val blockReflex: GamePad? = if (param.allowBlock && this.params.whatToAvoid != WhatToAvoid.JustEnemies) AttackActionBlockDecider.blockReflex(state) else null
        val leftCorner = state.link.upLeftOneGridALittleLess
//        val nearLink = Geom.Rectangle(leftCorner, state.link.downTwoGrid.rightTwoGrid)
//        val nearLink = Geom.Rectangle(leftCorner, state.link.downOneGrid.rightOneGrid)
        // should only do this for fireball projectiles
        val projectileNear = false // state.projectiles.filter { !state.frameState.isLevel || it.tile !in EnemyGroup.projectilesAttackIfNear }.any { it.point.toRect().intersect(nearLink) }
        val inRangeOf by lazy { AttackActionDecider.inRangeOf(state, attackable.map { it.point } , useB) }
        val shouldLongAttack by lazy { param.allowRangedAttack && AttackLongActionDecider.shouldShootSword(state, attackable.map { it.point }) }
        val shouldLongBoomerang by lazy { param.allowRangedAttack && boomerangCt <= 0 && AttackLongActionDecider.shouldBoomerang(state, boomerangable) }
        boomerangCt--

        val considerAttacks = allowAttack && !projectileNear && attackPossible
        return when {
            blockReflex != null -> {
                d { " Route Action -> Block Reflex! $blockReflex" }
                blockReflex
            }
            considerAttacks && (attack.isAttacking()) -> {
                d { " Route Action -> Keep Attacking" }
                theAttack.nextStep(state)
            }

            considerAttacks && canAttack && shouldLongAttack -> {
                d { " Route Action -> LongAttack" }
                theAttack.nextStep(state)
            }

            considerAttacks && canAttack && shouldLongBoomerang -> {
                d { " Route Action -> LongAttack Boomerang" }
                boomerangCt = if (state.arrowActive) {
                    // shoot the arrow alot when active
                    WAIT_BETWEEN_NOT_BOOMERANG
                } else {
                    // it's possible to get stuck doing the boomerang over and over never attacking
                    typically(WAIT_BETWEEN_BOOMERANG, WAIT_BETWEEN_BOOMERANG * 2) // was 4
                }
                attackB.nextStep(state)
            }

            !allowAttack ||
                    !attackPossible ||
                    projectileNear || // ignore sun
                    (inRangeOf.isAttack && theAttack.attackWaiting()) || //rhino
//                    !canAttack || // redundant
                    (state.frameState.clockActivated && Random.nextInt(10) == 1) ||
                    // this is weird, no need to do this yet
//                    AttackActionDecider.getInFrontOfGrids(state) ||
                    inRangeOf == GamePad.None -> {
                attack.reset()
                attackB.reset()
                d { " Route Action -> No Attack allow=${allowAttack} possible=${attackPossible} projNear=$projectileNear clock=${state.frameState.clockActivated} inRangeOf=${inRangeOf == GamePad.None} rh=${(inRangeOf.isAttack && theAttack.attackWaiting())}" }
                doRouteTo(state, to, param)
            }

            else -> {
                d { " Route Action -> RangeAction $inRangeOf use ${theAttack.gameAction} is=${inRangeOf.isAttack}" }
                if (inRangeOf.isAttack) {
                    theAttack.nextStep(state)
                } else {
                    inRangeOf
                }
            }
        }
    }

    private fun typically(typical: Int, everySoOften: Int): Int =
        if (Random.nextInt(6) == 1) {
            everySoOften
        } else {
            typical
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
        d { " DO routeTo TO ${to.size} points first ${to.firstOrNull()} currently at ${state.currentMapCell.mapLoc} what to avoid: ${params.whatToAvoid}" }
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

        var avoidProjectiles = if (!state.frameState.clockActivated) {
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
            nextPoint = makeNewRoute(param, state, to, avoid, avoidProjectiles, nextPoint)
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
        avoidProjectiles: List<Agent>,
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

        if (state.frameState.ladderDeployed) {
            d { " make new route ladder deployed "}
            val dirToGo = state.bestDirection()
            val modifier = if (dirToGo == Direction.None) {
                GamePad.randomDirection(state.link).toDirection().pointModifier()
            } else {
                dirToGo.pointModifier()
            }
            return modifier(linkPt)
        }

        route = FrameRoute(
            mapCell.zstar.route(
                ZStar.ZRouteParam(
                    start = linkPt,
                    targets = to,
                    pointBeforeStart = state.previousMove.from,
                    enemies = avoid.points,
                    projectiles = avoidProjectiles.map { it.point }, // don't add if there is no dodging
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
        ) //.cornering(state.link, state.frameState.link.dir) //.cornerSoon(state.link.oneStr, state.frameState.link.dir)


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
        route?.next5()
        planCount = 0
        val pointDir = nextPoint1.direction ?:
            route?.decideDirection(linkPt, state.frameState.link.dir)
        d { " next is $nextPoint1 of ${route?.numPoints ?: 0}" }
        return nextPoint1.copy(direction = pointDir)
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
            // sames as just agent.dir
//            d { " sword agent dir ${agent.dir} calc ${swordDir.dirFront(agent)}"}
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
