package bot.plan.action

import bot.plan.action.RouteTo.RouteParam
import bot.plan.action.routeto.PointMoveAction
import bot.plan.action.routeto.RouteExecution
import bot.plan.action.routeto.RouteToGetInFrontOf
import bot.plan.zstar.FrameRoute
import bot.plan.zstar.ZStar
import bot.plan.zstar.route.AttackableDecider
import bot.plan.zstar.route.BreadthFirstSearch
import bot.plan.zstar.route.BreadthFirstSearch.ActionRoute
import bot.plan.zstar.route.GoalFunction
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapCell
import bot.state.map.toGamePad
import util.LogFile
import util.d

class RouteTo(val params: Param = Param()) {
    private var boomerangCt = 0
    private var routeAction = RouteExecution(params)

    companion object {
        /**
         * dont shoot the boomerang too much
         */
        private const val WAIT_BETWEEN_BOOMERANG = 20
        private const val WAIT_BETWEEN_NOT_BOOMERANG = 2
        var allowAttack = true
        fun hardlyReplan(dodgeEnemies: Boolean = true,
                         /** don't try to block or route around projectiles **/
                         ignoreProjectiles:  Boolean = false) = RouteTo(
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
        val breadthFirst: Boolean = false,
        val rParam: RoutingParamCommon = RoutingParamCommon()
    )

    data class RoutingParamCommon(
        val forcePassable: List<FramePoint> = emptyList(),
        // could be used to avoid spots in front of sword guys too
        val forceHighCost: List<FramePoint> = emptyList(),
        /**
         * ignored
         */
        @Deprecated("Ignored")
        val attackTarget: FramePoint? = null,
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

    private val routeToFile: LogFile = LogFile("RouteTo", devType = true)

    var route: FrameRoute? = null
        private set
    private var planCount = 0

    fun needsRoute(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam = RouteParam(),
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList()
    ): GamePad {
        return GamePad.None
    }

    fun routeToBest(
        state: MapLocationState,
        param: RouteParam = RouteParam(),
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList()
    ): GamePad {
        d { "BFS START"}
        val linkPt = state.link
        val paramZ = ZStar.ZRouteParam(
            start = linkPt,
            targets = emptyList(),
            pointBeforeStart = state.previousMove.from,
            enemies = emptyList(),
            projectiles = emptyList(),
            rParam = param.rParam
        )
        state.currentMapCell.zstar.setNeighborFinder(paramZ)
        val ableToShoot = AttackLongActionDecider.ableToShoot(state)
        val isGoal = { point: FramePoint ->
            GoalFunction(ableToLongAttack = ableToShoot, ableToAttack = true, state.currentMapCell.zstar.neighborFinder)
                .isGoal(point, emptyList(), false)
        }
        val search = BreadthFirstSearch(isGoal, state.currentMapCell.zstar.neighborFinder)
        val attackableAgents: List<Agent> = AttackableDecider.aliveEnemiesCanAttack(state)
        val attackable = attackableSpec.ifEmpty {
            attackableAgents
        }
        val linkDir = state.frameState.link.dir
        val result = search.bestRoute(linkPt.withDir(linkDir), attackable.map { it.point })
        d { " BFS result action $result"}
        // refactor to one function
        return when (result) {
            is ActionRoute.Attack -> GamePad.aOrB(result.useB)
            is ActionRoute.Route -> {
                route = FrameRoute(result.route)
                var nextPoint = route?.path?.getOrNull(1) ?: FramePoint()
                val pointDir = route?.decideDirection(linkPt, state.frameState.link.dir)
                d { " next is $nextPoint of ${route?.numPoints ?: 0} chose direction $pointDir" }
                nextPoint = nextPoint.copy(direction = pointDir)
                getActionFromRoute(nextPoint, linkPt)
            }
        }
    }

    fun getActionFromRoute(nextPoint: FramePoint, linkPt: FramePoint): GamePad {
        return when {
            nextPoint.isZero && linkPt.x == 0 -> GamePad.MoveLeft
            nextPoint.isZero && linkPt.y == 0 -> GamePad.MoveUp
            // already in a good spot
            nextPoint.isZero -> GamePad.None
            else -> nextPoint.direction?.toGamePad() ?: linkPt.directionTo(nextPoint)
        }.also {
            //        writeFile(to, state, it)
            d { " link point $linkPt next point $nextPoint dir: $it ${if (nextPoint.direction != null) "HAS DIR ${nextPoint.direction}" else ""}" }
        }
    }

    fun routeTo(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam = RouteParam(),
        attackableSpec: List<Agent> = emptyList()
    ): GamePad {
        return routeAction.route(state, to, param, attackableSpec, this)
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

    fun makeNewRoute(
        param: RouteParam,
        state: MapLocationState,
        to: List<FramePoint>,
        avoid: List<Agent>,
        avoidProjectiles: List<Agent>,
        nextPoint: FramePoint,
        attackableSpec: List<Agent> = emptyList()
    ): FramePoint {
        val linkPt = state.frameState.link.point
        val ladder = state.frameState.ladder
        var nextPoint1 = nextPoint
        val passable = param.rParam.forcePassable.toMutableList()

        if (ladder != null) {
            passable.add(ladder.point)
        }

        val mapCell = param.overrideMapCell ?: state.currentMapCell

        val inFrontOfGrids = RouteToGetInFrontOf.getInFrontOfGrids(state)
        for (point in inFrontOfGrids) {
            d { "in front grid $point"}
        }

        // don't need I think
//        if (state.frameState.ladderDeployed) {
//            val dirToGo = state.bestDirection()
//            d { " make new route ladder deployed Go dir: $dirToGo"}
//            val modifier = if (dirToGo == Direction.None) {
//                GamePad.randomDirection(state.link).toDirection().pointModifier()
//            } else {
//                dirToGo.pointModifier()
//            }
//            return modifier(linkPt)
//        }

        val goalFunction = IsGoal(routeAction, state, param, attackableSpec)
        val bypassGoal = { point: FramePoint -> false}
        val paramZ = ZStar.ZRouteParam(
            start = linkPt,
            targets = to,
            pointBeforeStart = state.previousMove.from,
            enemies = avoid.points,
            projectiles = avoidProjectiles.map { it.point }, // don't add if there is no dodging
            isGoal = goalFunction::isGoal,
            rParam = param.rParam.copy(
                forcePassable = passable,
                forceHighCost = param.rParam.forceHighCost + inFrontOfGrids
            )
        )

        val routePoints = if (param.breadthFirst) {
            val goalFunction = IsGoal(routeAction, state, param, attackableSpec)
            mapCell.zstar.routeWithBfs(paramZ, goalFunction::isGoal)
        } else {
            mapCell.zstar.route(paramZ)
        }
        route = FrameRoute(routePoints)

        route?.next15()
        nextPoint1 = route?.popOrEmpty() ?: FramePoint() // skip first point because it is the current location
        nextPoint1 = route?.popOrEmpty() ?: FramePoint()
        if (nextPoint1.isZero) {
            d { "NO ROUTE" }
        } else {
            d { " router size: ${route?.numPoints ?: 0}" }
            route?.next5()
//            d { " next 100 "}
//            route?.next100()
        }
        planCount = 0
        val pointDir = nextPoint1.direction ?:
            route?.decideDirection(linkPt, state.frameState.link.dir)
        d { " next is $nextPoint1 of ${route?.numPoints ?: 0}" }
        return nextPoint1.copy(direction = pointDir)
    }
}

private class IsGoal(
    routeAction: RouteExecution,
    private val state: MapLocationState,
    private val param: RouteParam,
    attackableSpec: List<Agent> = emptyList()
) {
    val determine = routeAction.getDetermine(state, param, attackableSpec)
    fun isGoal(point: FramePoint): Boolean {
        val action = determine.nextAttackAction(state, emptyList(), param, 0, false, link = point, linkDir = point.direction ?: Direction.None)
//            action == PointMoveAction.LongAttack || action == PointMoveAction.ShortAttack || action == PointMoveAction.BoomerangAttack
        return action != PointMoveAction.Route
    }
}