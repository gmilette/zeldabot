package bot.plan.action

import bot.plan.action.NavUtil.directionToDir
import bot.plan.gastar.FrameRoute
import bot.plan.gastar.GStar
import bot.state.*
import bot.state.map.*
import util.LogFile
import util.d


//val navUntil = CompleteIfAction(InsideNav()), completeIf = { state ->
//    // first action is one
//)

class CompleteIfAction(
    private val action: Action,
    private val completeIf: (state: MapLocationState) -> Boolean = { false },
) : Action {
    override fun complete(state: MapLocationState): Boolean =
        completeIf(state) || (action.complete(state))

    override fun nextStep(state: MapLocationState): GamePad {
        return action.nextStep(state)
    }

    override val name: String
        get() = action.name
}

class CompleteIfMapChanges(private val wrapped: Action) : Action {
    private var initialMapLoc: MapLoc = -1;

    private fun changedMapLoc(state: MapLocationState): Boolean =
        initialMapLoc > 0 && state.frameState.mapLoc != initialMapLoc

    override fun complete(state: MapLocationState): Boolean =
        changedMapLoc(state) || wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        if (initialMapLoc == -1) {
            initialMapLoc = state.frameState.mapLoc
        }
        return wrapped.nextStep(state)
    }

    override val name: String
        get() = "Until Change ${wrapped.name}"
}

// move to this location then complete
class InsideNav(private val point: FramePoint, ignoreProjectiles: Boolean = false) : Action {
    private val routeTo = RouteTo.hardlyReplan(ignoreProjectiles = ignoreProjectiles)
    override fun complete(state: MapLocationState): Boolean =
        state.frameState.link.point == point

    override fun nextStep(state: MapLocationState): GamePad {
        val pad = routeTo.routeTo(state, listOf(point))
        return pad
    }

    override fun target(): FramePoint {
        return point
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override val name: String
        get() = "Nav to inside $point"
}

class InsideNavAbout(
    private val point: FramePoint, about: Int, vertical: Int = 1, negVertical: Int = 0,
    val shop: Boolean = false, ignoreProjectiles: Boolean = false, private val makePassable: FramePoint? = null
) : Action {
    private val routeTo = RouteTo.hardlyReplan(dodgeEnemies = !shop, ignoreProjectiles)
    private val points: List<FramePoint>

    init {
        val pts = mutableListOf<FramePoint>()
        repeat(negVertical) {
            pts.addAll(point.copy(y = point.y - it).toLineOf(about))
        }
        repeat(vertical) {
            pts.addAll(point.copy(y = point.y + it).toLineOf(about))
        }
        points = pts
    }

    override fun complete(state: MapLocationState): Boolean =
        state.link.minDistToAny(points) < 2.also {
            d { "! ${state.link} not in ${points} " }
        }
//        points.contains(state.frameState.link.point).also {
//            d { "! ${state.link} not in ${points} "}
//        }

    override fun nextStep(state: MapLocationState): GamePad =
        routeTo.routeTo(
            state, points, overrideMapCell = if (shop) state.hyrule.shopMapCell else null,
            makePassable = makePassable
        )

    override fun target(): FramePoint {
        return point
    }

    override fun targets(): List<FramePoint> {
        return listOf(point)
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override val name: String
        get() = "Nav to about $point"
}

class MoveTo(val fromLoc: MapLoc = 0, val next: MapCell, val forceDirection: Direction? = null) : MapAwareAction {
    private val routeTo = RouteTo(params = RouteTo.Param(considerLiveEnemies = false))

    init {
        next.gstar.clearAvoid()
    }

    override val from: MapLoc = fromLoc
    override val to: MapLoc = next.mapLoc

    private var arrived = false
    private var movedIn = 0
    private var previousDir: Direction = Direction.None
    private var arrivedDir: Direction = Direction.None

    override fun reset() {
        next.gstar.clearAvoid()
        route = null
    }

    override fun complete(state: MapLocationState): Boolean =
        (arrived && movedIn >= 5).also {
            if (it) route = null
//            d { " Move to complete $it"}
        }

    private fun checkArrived(state: MapLocationState, dir: Direction) {
        if (!arrived) {
            arrived = state.frameState.mapLoc == next.mapLoc
            if (arrived) {
                movedIn = 0
                arrivedDir = dir
                d { " arrived! $arrived dir $arrivedDir" }
            }
        }
    }

    override fun target(): FramePoint {
        return targets.firstOrNull() ?: FramePoint()
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    private var targets = listOf<FramePoint>()

    override fun targets(): List<FramePoint> {
        return targets
    }

    private var route: FrameRoute? = null
    private var planCount = 0

    private var dir: Direction = Direction.Down

    private var start: MapCell? = null

    override fun nextStep(state: MapLocationState): GamePad {
        d { " DO MOVE TO cell ${next.mapLoc} arrived=${arrived} $movedIn" }
//        for (enemy in state.frameState.enemies) {
//            d { " enemy: $enemy" }
//        }
//        val current = state.hyrule.levelMap.cell(1, state.currentMapCell.mapLoc)
        val current = state.currentMapCell
        if (start == null) {
            start = current
        }
        val next = next
        val linkPt = state
            .frameState
            .link.point

        // TODO: need to figure out how to get back on track if get off the plan for some reason
//        val isInCurrent = false
//        dir = when {
//            !isInCurrent -> current.mapLoc.directionToDir(start!!.mapLoc)
//            else -> forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)
//        }

        // always assume on current need this? for the state.frameState.isLevel &&
//        val isInCurrent = current.mapLoc == start?.mapLoc //|| (current.mapLoc != (start?.mapLoc ?: 0))

        // do it for levels too
        val isInCurrent =
            state.frameState.isLevel || current.mapLoc == from //|| (current.mapLoc != (start?.mapLoc ?: 0))
//        val isInCurrent = current.mapLoc == from //|| (current.mapLoc != (start?.mapLoc ?: 0))
        // need a little routing alg
        previousDir = dir

        // use this to retrace
//        dir = when {
//            !isInCurrent -> current.mapLoc.directionToDir(fromLoc)
//            else -> forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)
//        }
//
//        if (isInCurrent) {
//            d { " ON THE RIGHT ROUTE on ${current.mapLoc} but should be on ${fromLoc} or start ${start?.mapLoc} go $dir"}
//        } else {
//             d { " ON THE WRONG ROUTE on ${current.mapLoc} but should be on ${fromLoc} or start ${start?.mapLoc} go $dir"}
//        }
        dir = forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)


        if (state.currentMapCell.exitsFor(dir) == null) {
            d { " default move " }
        }
        val exits = state.currentMapCell.exitsFor(dir) ?: return NavUtil.randomDir()

        targets = exits

        checkArrived(state, previousDir)

        return if (!arrived) {
            routeTo.routeTo(state, exits)
        } else {
            movedIn++
            arrivedDir.toGamePad()
        }
    }

    override val name: String
        get() = "Move from ${this.fromLoc} to ${this.next.mapLoc} $dir $movedIn"
}

class RouteTo(val params: Param = Param()) {
    companion object {
        fun hardlyReplan(dodgeEnemies: Boolean = true, ignoreProjectiles: Boolean = false) = RouteTo(
            RouteTo.Param(
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
        val ignoreEnemies: Boolean = false
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
        forceNew: Boolean = false,
        overrideMapCell: MapCell? = null,
        makePassable: FramePoint? = null,
        useB: Boolean = false
    ): GamePad {
        return routeTo(state, to, RouteParam(forceNew, overrideMapCell, makePassable, emptyList(), useB))
    }

    fun routeTo(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam,
    ): GamePad {
        return attackOrRoute(state, to, param)
    }

    private fun attackOrRoute(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam,
    ): GamePad {
        // is this direction correct?
        // i tried dirActual
        d { " route To" }
        val canAttack = param.useB || state.frameState.canUseSword

        return if (AttackActionDecider.shouldAttack(state) && canAttack
        ) {
            d { " Route Action -> ATTACK" }
            val att = if (param.useB) GamePad.B else GamePad.A
            writeFile(to, state, param, att)
            if (param.useB) {
                attackB.nextStep(state)
            } else {
                attack.nextStep(state)
            }
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
        param: RouteParam,
        gamePad: GamePad
    ) {
        val target = to.minByOrNull { it.distTo(state.link) }
        val distTo = target?.distTo(state.link) ?: 0
        routeToFile.write(state.currentMapCell.mapLoc, target?.oneStr ?: "0_0", state.link.oneStr, distTo, planCount, gamePad.name)
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
    param: RouteParam
): GamePad {
    d { " DO routeTo TO ${to.size} first ${to.firstOrNull()} currently at ${state.currentMapCell.mapLoc}" }
    var forceNew = param.forceNew
    if (to.isEmpty()) {
        d { " no where to go " }
        // dont do this because it could cause link to go off scren
//            throw RuntimeException("no where to bo")
        return NavUtil.randomDir(state.link)
    }
    val linkPt = state.frameState.link.point
    if (linkPt.minDistToAny(to) <= 1) {
        d { " CLOSE!! " }
        if (to.first().y <= 1) {
            d { " CLOSE!! up" }
            return GamePad.MoveUp
        }
        if (to.first().x <= 1) {
            d { " CLOSE!! left" }
            return GamePad.MoveLeft
        }
        if (to.first().x >= MapConstants.MAX_X - 2) {
            d { " CLOSE!! right" }
            return GamePad.MoveRight
        }
        if (to.first().y >= MapConstants.MAX_Y - 2) {
            d { " CLOSE!! down" }
            return GamePad.MoveDown
        }
    }

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
        forceNew = false // wny is this true?? no enemies! it caises
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

    val linkPoints =  listOf(linkPt, linkPt.justRightEnd, linkPt.justRightEndBottom, linkPt.justLeftDown)
    // it's ignoring the rhino!!
    val enemiesNear = state.aliveOrProjectile.filter { it.point.minDistToAny(linkPoints) < MapConstants.oneGrid * 5 }
    val projectilesNear = state.projectile.filter { it.point.minDistToAny(linkPoints) < MapConstants.oneGrid * 5 }
    val ladder = state.frameState.ladder

    var avoid = if (params.dodgeEnemies) {
        when {
            enemiesNear.isNotEmpty() -> enemiesNear
            // I can see why I set this to > 1, in the case of there being an enemy
            // we should just boldly move towards it because when we get close
            // we will attack, but it's not always the case, at least for rhino
//            (enemiesNear.size > 1) -> enemiesNear
            // one projectile
            projectilesNear.isNotEmpty() -> projectilesNear
            else -> emptyList()
        }
    } else {
        emptyList()
    }

    if (params.ignoreProjectiles) {
        avoid = avoid.filter { it.state != EnemyState.Projectile }
    } else {
        ////
//        avoid = avoid.filter { it.state == EnemyState.Projectile }
    }
    if (param.ignoreEnemies) {
        d { "ignore enemies" }
        avoid = avoid.filter { it.state != EnemyState.Alive }
    }

    param.attackTarget?.let { targetAttack ->
        d { " remove enemy from filter $targetAttack"}
        avoid = avoid.filter { it.point != targetAttack }
    }
    //
//    if (param.attackTarget != null) {
//        d { " there is attack target why?"}
//        avoid = emptyList()
//    }

    d { " enemies near ${enemiesNear.size} projectiles ${projectilesNear.size} avoid: ${avoid.size}" }
        for (agent in enemiesNear) {
            d { " enemy $agent"}
        }
    d { " avoid attack target ${param.attackTarget}"}
    for (agent in avoid) {
        d { " enemy $agent"}
    }


    // faster, but i have to do tradeoff between dodging
//    avoid = emptyList()

    if (forceNew ||
        route == null || // reset
        routeSize <= 2 ||
        planCount >= params.planCountMax || // could have gotten off track
        !state.previousMove.movedNear || // got hit
        enemiesNear.isNotEmpty()
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

        val passable = mutableListOf<FramePoint>()
        if (ladder != null) {
            passable.add(ladder.point)
        }
        if (param.makePassable != null) {
            passable.add(param.makePassable)
        }

        val mapCell = param.overrideMapCell ?: state.currentMapCell

        // of if the expected point is not where we should be
        // need to re route
        route = FrameRoute(
            NavUtil.routeToAvoidingObstacle(
                mapCell = mapCell,
                from = linkPt,
                to = to,
                before = state.previousMove.from,
                enemies = avoid.points,
                forcePassable = passable,
                ladderSpec = state.frameState.ladder?.let {
                    d { " has ladder "}
                    GStar.LadderSpec(false, it.point)
                }
            )
        )

        d { " Plan: ${state.currentMapCell.mapLoc} new plan! because ($why) to ${to}" }
        route?.path?.lastOrNull()?.let { lastPt ->
            if (lastPt in to || ladder != null) {
                d { "route to success target" }
            } else {
                // what if can't find a route here?
                d { "route to fail, route towards enemies"}
                d { "ended at $lastPt which is ${lastPt.distTo(to.first())}"}
                // never go after projectiles
                val withoutClosestEnemy = avoid.filter { it.state == EnemyState.Alive } .sortedBy { it.point.distTo(state.link) }.toMutableList()
                val onlyProjectiles = avoid.filter { it.state == EnemyState.Projectile }.map { it.point }
                val closest = withoutClosestEnemy.removeFirst()
                d { "closest is ${closest.point}"}
                route = FrameRoute(
                    NavUtil.routeToAvoidingObstacle(
                        mapCell = mapCell,
                        from = linkPt,
                        to = avoid.points,
                        before = state.previousMove.from,
                        enemies = onlyProjectiles, // withoutClosestEnemy.points,
                        forcePassable = passable,
                        enemyTarget = closest.point
                    )
                )
                d { "found route of size ${route?.path?.size ?: 0}"}
            }
        }
        route?.next15()
//            route?.adjustCorner()
        nextPoint = route?.popOrEmpty() ?: FramePoint() // skip first point because it is the current location
        nextPoint = route?.popOrEmpty() ?: FramePoint()
        d { " next is $nextPoint" }
        route?.next5()
        planCount = 0
    } else {
        d { " Plan: same plan ct $planCount" }
    }

    planCount++
//within ${current.mapLoc} -> ${next.mapLoc}
    d { " go to from ${linkPt} to next ${nextPoint} ${to}" }
//        route?.next5()
    return if (nextPoint == null) {
        NavUtil.randomDir(state.link)
    } else if (nextPoint == FramePoint(0, 0) && linkPt.x == 0) {
        GamePad.MoveLeft
    } else if (nextPoint == FramePoint(0, 0) && linkPt.y == 0) {
        GamePad.MoveUp
    } else {
        // what nav to direction, that's weird
        val gamepad = nextPoint.direction?.toGamePad() ?: NavUtil.directionToDist(linkPt, nextPoint)

        // it's the wrong point
        if (ladder != null) {
            d { "Ladder! $ladder horiz = ${state.ladderStateHorizontal}"}
        }
        if (true || ladder == null) gamepad else
            if (state.previousMove.didntMove) {
                // at least this allows link to escape sometimes, but not all times
                d { "ladder didnt move ${state.previousMove.move} ${state.previousMove.dir}"}
                if (state.previousMove.dir.horizontal || state.previousMove.move == GamePad.None) {
                    // force vertical
                    d { " ladder should go vertical " }
                    GamePad.randomDirection()
                } else {
                    d { " ladder should go hori " }
                    // force left
                    GamePad.randomDirection()
                }
            } else {
                gamepad
            }
//            state.ladderStateHorizontal == true && gamepad.isHorizontal -> gamepad
//            state.ladderStateHorizontal == false && !gamepad.isHorizontal -> gamepad
//            else -> GamePad.MoveUp
//        }
    }.also {
        writeFile(to, state, param, it)
        d { " next point $nextPoint dir: $it ${if (nextPoint.direction != null) "HAS DIR ${nextPoint.direction}" else ""}" }
    }
}
}
