package bot.plan.action

import androidx.compose.runtime.structuralEqualityPolicy
import bot.plan.action.AttackActionDecider.isInUpPointPosition
import bot.plan.action.AttackActionDecider.upPoints
import bot.plan.zstar.FrameRoute
import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.*
import bot.state.oam.swordDir
import util.LogFile
import util.d
import util.w

class RouteTo(val params: Param = Param()) {
    companion object {
        fun hardlyReplan(dodgeEnemies: Boolean = true, ignoreProjectiles: Boolean = false) = RouteTo(
            Param(
                planCountMax = 100,
                dodgeEnemies = dodgeEnemies,
                ignoreProjectiles = ignoreProjectiles,
            )
        )
    }

    data class Param(
        var planCountMax: Int = 20,
        // not used
        val considerLiveEnemies: Boolean = true,
        val dodgeEnemies: Boolean = true, // always dodge I think, unless i'm in a shop or something
        val ignoreProjectiles: Boolean = false,
    )

    data class RouteParam(
        val forceNew: Boolean = false,
        val overrideMapCell: MapCell? = null,
        val makePassable: FramePoint? = null,
        val enemyAvoid: List<FramePoint> = emptyList(),
        val useB: Boolean = false,
        val attackTarget: FramePoint? = null,
        // if false, dont bother avoiding any enemies, avoid projectiles though
        val ignoreEnemies: Boolean = false,
        val mapNearest: Boolean = false,
        val forceHighCost: List<FramePoint> = emptyList(),
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
        return attackOrRoute(state, to, param)
    }

    private fun attackOrRoute(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam,
    ): GamePad {
        d { " route To attackOrRoute" }
        val canAttack = param.useB || state.frameState.canUseSword

//        val closest = state.aliveEnemies.minBy { it.point.distTo(state.link) }
//        d { " route To link at ${state.link} closest=${closest}" }
//        for (upPoint in closest.point.upPoints()) {
//            d { " point $upPoint"}
//        }
        // if it's in the up position for ANY enemy
        val inUpPosition = state.aliveEnemies.any { enemy ->
            enemy.point.upPoints().contains(state.link)
        }
        if (inUpPosition) {
            d { " route to in position at direction ${state.frameState.link.dir}"}
            // go up
            if (state.frameState.link.dir != Direction.Up) {
                // make sure it is on the highway, otherwise don't
                if (state.link.onHighway) {
                    d { " route to go up to correct direction"}
                    return GamePad.MoveUp
                } else {
                    d { " route to no on highway don't try to correct position"}
                }
            }
        } else {
            d { " not in position ${state.frameState.link.dir}"}
        }

        val theAttack = if (param.useB) {
            attackB
        } else {
            attack
        }
        // TODO: do not move if in middle of an attack
        return if (attack.isAttacking() || AttackActionDecider.shouldAttack(state) && canAttack
        ) {
            d { " Route Action -> ATTACK" }
//            val att = if (param.useB) GamePad.B else GamePad.A
//            writeFile(to, state, att)
            theAttack.nextStep(state)
        } else {
            attack.reset()
            attackB.reset()
            d { " Route Action -> No Attack" }
            doRouteTo(state, to, param)
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

//        return if (params.dodgeEnemies && AttackActionDecider.shouldAttack(state) && (state.frameState.canUseSword && !useB)
//        ) {
//            d { " Route Action -> ATTACK" }
//            if (useB) {
//                attackB.nextStep(state)
//            } else {
//                attack.nextStep(state)
//            }
//        } else {
//            if (params.dodgeEnemies) {
//                val pad = AttackActionDecider.shouldDodgeDepending(state)
//                if (pad != GamePad.None) {
//                    d { " Route Action -> DODGE" }
//                    pad
//                } else {
//                    d { " Route Action -> No Attack (no dodge)" }
//                    attack.reset()
//                    attackB.reset()
//                    doRouteTo(state, to, forceNewI, overrideMapCell, makePassable)
//                }
//            } else {
//                attack.reset()
//                attackB.reset()
//                d { " Route Action -> No Attack" }
//                doRouteTo(state, to, forceNewI, overrideMapCell, makePassable)
//            }
//        }
//}

    private fun doRouteTo(
        state: MapLocationState,
        to: List<FramePoint>,
        paramIn: RouteParam
    ): GamePad {
        d { " DO routeTo TO ${to.size} points first ${to.firstOrNull()} currently at ${state.currentMapCell.mapLoc}" }
        val param = paramIn.copy(attackTarget = null)
        var forceNew = param.forceNew
        if (to.isEmpty()) {
            w { " no where to go " }
            return NavUtil.randomDir(state.link)
        }
        val linkPt = state.frameState.link.point
        // why this? let's go without it and see if it's ok
//        val closest = to.minBy { it.distTo(linkPt) }
//        if (linkPt.distTo(closest) <= 1) {
//            d { " CLOSE!! $closest" }
//            if (closest.y <= 1) {
//                d { " CLOSE!! up" }
//                return GamePad.MoveUp
//            }
//            if (closest.x <= 1) {
//                d { " CLOSE!! left" }
//                return GamePad.MoveLeft
//            }
//            if (closest.x >= MapConstants.MAX_X - 2) {
//                d { " CLOSE!! right" }
//                return GamePad.MoveRight
//            }
//            if (closest.y >= MapConstants.MAX_Y - 2) {
//                d { " CLOSE!! down" }
//                return GamePad.MoveDown
//            }
//        }

        val skippedButIsOnRoute = (state.previousMove.skipped && route?.isOn(linkPt, 5) != null)
        if (skippedButIsOnRoute) {
            route?.popUntil(linkPt)
        }

        // getting me suck: && params.planCountMax != 1000
        // there are no enemies so it just keeps forcing replanning
        // make a new boolean force new ONCE
        if (!state.hasEnemiesOrLoot && params.planCountMax != 1000) {
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
        val linkPoints = listOf(linkPt, linkPt.justRightEnd, linkPt.justRightEndBottom, linkPt.justLeftDown)
        val enemiesNear =
            state.aliveOrProjectile.filter { it.point.minDistToAny(linkPoints) < MapConstants.oneGrid * 5 }

        // nothing to avoid if the clock is activated
        var avoid = if (params.dodgeEnemies && !state.frameState.clockActivated) {
            // this seems to be ok, except link can get hit from the side
            // unless it avoids projectiles
//            state.aliveEnemies
            when {
                params.ignoreProjectiles -> state.aliveEnemies
                param.ignoreEnemies -> state.projectiles
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

        param.attackTarget?.let { targetAttack ->
            d { " remove enemy from filter $targetAttack" }
            avoid = avoid.filter { it.point != targetAttack }
        }

        d { " avoid attack target ${param.attackTarget}" }
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

        return if (nextPoint.isZero && linkPt.x == 0) {
            GamePad.MoveLeft
        } else if (nextPoint.isZero && linkPt.y == 0) {
            GamePad.MoveUp
        } else {
            nextPoint.direction?.toGamePad() ?: linkPt.directionTo(nextPoint)
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
        val passable = mutableListOf<FramePoint>()

        if (ladder != null) {
            passable.add(ladder.point)
        }
        if (param.makePassable != null) {
            passable.add(param.makePassable)
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
                    forcePassable = passable,
                    enemyTarget = param.attackTarget,
                    mapNearest = param.mapNearest,
                    forceHighCost = param.forceHighCost + inFrontOfGrids
                )
            )
        )

        route?.path?.lastOrNull()?.let { lastPt ->
            // if it is just projectile then don't try to route towards the projectiles
            if (param.mapNearest || lastPt in to || state.frameState.ladderDeployed || !state.hasEnemies) {
                d { "route to success target" }
            }
        }
        route?.next15()
        nextPoint1 = route?.popOrEmpty() ?: FramePoint() // skip first point because it is the current location
        nextPoint1 = route?.popOrEmpty() ?: FramePoint()
        d { " next is $nextPoint1" }
        route?.next5()
        planCount = 0
        return nextPoint1
    }

    private fun getInFrontOfGrids(state: MapLocationState): List<FramePoint> =
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
