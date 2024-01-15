package bot.plan.action

import bot.plan.action.NavUtil.directionToDir
import bot.plan.zstar.FrameRoute
import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.*
import bot.state.oam.shopOwner
import bot.state.oam.shopkeeperAndBat
import bot.state.oam.swordDir
import util.LogFile
import util.d
import util.e
import util.w


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

class CompleteIfChangeShopOwner(private val changeTo: Boolean, private val wrapped: Action) : Action {
    private var initial: Boolean? = null
    private var completeCt: Int = 0

    private fun changedOwnerAppearance(state: MapLocationState): Boolean =
        state.frameState.enemies.isNotEmpty() && initial != null && (inShop(state) == changeTo)

    private fun inShop(state: MapLocationState): Boolean = state.frameState.enemies.any {
        (it.tile == shopOwner.first && it.attribute == shopOwner.second) ||
                (it.tile == shopkeeperAndBat.first && it.attribute == shopkeeperAndBat.second)
    }

    override fun reset() {
        completeCt = 0
        initial = null
    }

    override fun complete(state: MapLocationState): Boolean =
        completeCt > 100 || wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        d { " CompleteIfChangeShopOwner initial $initial current ${inShop(state)} shouldBe $changeTo" }
        state.frameState.logEnemies()
        if (initial == null) {
            initial = inShop(state)
        }
        if (changedOwnerAppearance(state)) {
            completeCt++
        } else {
            completeCt = 0
        }
        return wrapped.nextStep(state)
    }

    override val name: String
        get() = "Until Change Shop ${wrapped.name}"
}

class CompleteIfMapChanges(private val wrapped: Action) : Action {
    private var initialMapLoc: MapLoc = -1;

    private fun changedMapLoc(state: MapLocationState): Boolean =
        initialMapLoc > 0 && state.frameState.mapLoc != initialMapLoc

    override fun complete(state: MapLocationState): Boolean =
        changedMapLoc(state) || wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        d { " CompleteIfMapChanges initial $initialMapLoc current ${state.frameState.mapLoc}" }
        if (initialMapLoc == -1) {
            initialMapLoc = state.frameState.mapLoc
        }
        return wrapped.nextStep(state)
    }

    override val name: String
        get() = "Until Change ${wrapped.name}"
}

// move to this location then complete
class InsideNav(
    private val point: FramePoint,
    ignoreProjectiles: Boolean = false,
    private val makePassable: FramePoint? = null,
    private val tag: String = "",
    private val highCost: List<FramePoint> = emptyList()
) : Action {
    private val routeTo = RouteTo.hardlyReplan(ignoreProjectiles = ignoreProjectiles)
    override fun complete(state: MapLocationState): Boolean =
        state.frameState.link.point == point

    override fun nextStep(state: MapLocationState): GamePad {
        return routeTo.routeTo(state, listOf(point), makePassable = makePassable, highCost = highCost)
    }

    override fun target(): FramePoint {
        return point
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override val name: String
        get() = "Nav to inside $point $tag"
}

class InsideNavAbout(
    private val point: FramePoint, about: Int, vertical: Int = 1, negVertical: Int = 0,
    val shop: Boolean = false, ignoreProjectiles: Boolean = false,
    private val makePassable: FramePoint? = null,
    orPoints: List<FramePoint> = emptyList(),
    private val setMonitorEnabled: Boolean = true,
    private val tag: String = "",
    private val highCost: List<FramePoint> = emptyList()
) : Action {
    private val routeTo = RouteTo.hardlyReplan(dodgeEnemies = !shop, ignoreProjectiles)
    private val points: List<FramePoint>

    override val monitorEnabled: Boolean
        get() = setMonitorEnabled

    init {
        val pts = mutableListOf<FramePoint>()
        repeat(negVertical) {
            pts.addAll(point.copy(y = point.y - it).toLineOf(about))
        }
        repeat(vertical) {
            pts.addAll(point.copy(y = point.y + it).toLineOf(about))
        }
        for (orPoint in orPoints) {
            repeat(negVertical) {
                pts.addAll(orPoint.copy(y = orPoint.y - it).toLineOf(about))
            }
            repeat(vertical) {
                pts.addAll(orPoint.copy(y = orPoint.y + it).toLineOf(about))
            }
        }
        points = pts
    }

    override fun complete(state: MapLocationState): Boolean =
        state.link.minDistToAny(points) < 2.also {
            d { "! ${state.link} in $points isComplete=$it" }
        }
//        points.contains(state.frameState.link.point).also {
//            d { "! ${state.link} not in ${points} "}
//        }

    override fun nextStep(state: MapLocationState): GamePad =
        routeTo.routeTo(
            state,
            to = points,
            RouteTo.RouteParam(
                overrideMapCell = if (shop) state.hyrule.shopMapCell else null,
                makePassable = makePassable,
                forceHighCost = highCost
            )
        )

    override fun target(): FramePoint {
        return point
    }

    override fun targets(): List<FramePoint> {
        return listOf(point)
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override val name: String
        get() = "Nav to about $point $tag"
}

class StartAtAction(val at: MapLoc = 0, val atLevel: Int = -1) : Action {
    override fun complete(state: MapLocationState): Boolean =
        true.also {
            d { "set start to $at" }
            state.movedTo = at
            state.levelTo = atLevel
        }
}

class MoveTo(
    val fromLoc: MapLoc = 0,
    val next: MapCell,
    val toLevel: Int,
    val forceDirection: Direction? = null,
    ignoreProjectiles: Boolean = false
) : MapAwareAction {
    private val routeTo =
        RouteTo(params = RouteTo.Param(considerLiveEnemies = false, ignoreProjectiles = ignoreProjectiles))

    init {
        next.zstar.clearAvoid()
    }

    override val from: MapLoc = fromLoc
    override val to: MapLoc = next.mapLoc

    override val actionLoc: MapLoc
        get() = to

    override val levelLoc: Int
        get() = toLevel

    private var arrived = false
    private var movedIn = 0
    private var previousDir: Direction = Direction.None
    private var arrivedDir: Direction = Direction.None

    override fun reset() {
        next.zstar.clearAvoid()
        route = null
        arrived = false
        movedIn = 0
    }

    override fun complete(state: MapLocationState): Boolean =
        (arrived && movedIn >= 5).also {
            if (it) {
                onArrived(state)
                route = null
            }
//            d { " Move to complete $it"}
        }

    private fun onArrived(state: MapLocationState) {
        state.movedTo = to
        state.levelTo = toLevel
    }

    private fun checkArrived(state: MapLocationState, dir: Direction) {
        if (!arrived) {
            arrived = state.frameState.mapLoc == next.mapLoc
            if (arrived) {
                onArrived(state)
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
            routeTo.routeTo(state, exits, RouteTo.RouteParam())
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
        forceNew: Boolean = false,
        overrideMapCell: MapCell? = null,
        makePassable: FramePoint? = null,
        useB: Boolean = false,
        highCost: List<FramePoint> = emptyList()
    ): GamePad {
        return routeTo(
            state, to, RouteParam(
                forceNew, overrideMapCell, makePassable, emptyList(), useB,
                forceHighCost = highCost
            )
        )
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
        d { " route To" }
        val canAttack = param.useB || state.frameState.canUseSword

        return if (AttackActionDecider.shouldAttack(state) && canAttack
        ) {
            d { " Route Action -> ATTACK" }
//            val att = if (param.useB) GamePad.B else GamePad.A
//            writeFile(to, state, att)
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
