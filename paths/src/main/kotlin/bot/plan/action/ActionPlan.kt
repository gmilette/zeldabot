package bot.plan.action

import bot.GamePad
import bot.plan.action.NavUtil.directionToDir
import bot.plan.gastar.FrameRoute
import bot.state.*
import bot.state.map.*
import util.d
import java.awt.Point
import kotlin.math.abs

// set of actions for a given map cell
//class ActionPlan {
//    private val actions: MutableList<Action> = mutableListOf()
//
//    init {
//        push(MoveToNextScreen()) //lootOrKill
////        push(lootOrKill)
//    }
//
//    fun current(): Action =
//        actions.first()
//
//    fun push(action: Action) {
//        actions.add(action)
//    }
//
//    fun pop(): Action =
//        actions.removeLast()
//}

interface Action {
    fun reset() {
        // empty
    }

    fun complete(state: MapLocationState): Boolean

    fun nextStep(state: MapLocationState): GamePad {
        return GamePad.MoveUp
    }

    fun target() = FramePoint(0, 0)

    fun path(): List<FramePoint> = emptyList()

    val name: String
        get() = this.javaClass.simpleName
}

class EndAction : Action {
    override fun complete(state: MapLocationState): Boolean =
        true

    override fun nextStep(state: MapLocationState): GamePad {
        d { " DONE! " }
        return GamePad.MoveUp
    }
}

class AlwaysMoveUp : Action {
    override fun complete(state: MapLocationState): Boolean =
        false

    override fun nextStep(state: MapLocationState): GamePad {
        return GamePad.MoveUp
    }
}

//val killAllOrMoveToNext = DecisionAction(KillAll()) {
//        state -> state.hasEnemies
//}

private fun MapLocationState.hasNearEnemy(threshold: Int = 32): Boolean =
    frameState.enemies.any { it.state == EnemyState.Alive && frameState.link.point.distTo(it.point) < threshold }

private val MapLocationState.hasEnemies: Boolean
    get() = frameState.enemies.any { it.state == EnemyState.Alive }

private val MapLocationState.numEnemies: Int
    get() = frameState.enemies.count { it.state == EnemyState.Alive }

//private val MapLocationState.hasEnemies: Boolean
//    get() = frameState.killedEnemyCount < numEnemiesSeen.also {
//        d { " killed ${frameState.killedEnemyCount} moved: ${numEnemiesSeen}"}
//    }

private val MapLocationState.hasAnyLoot: Boolean
    get() = frameState.enemies.any { it.isLoot }

private val MapLocationState.cleared: Boolean
    get() = !hasEnemies && !hasAnyLoot

private fun MapLocationState.clearedWithMin(min: Int): Boolean =
    numEnemies <= min && !hasAnyLoot

class Optional(val action: Action, private val must: Boolean = true) : Action {
    override fun reset() {
        action.reset()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return action.nextStep(state)
    }

    override val name: String
        get() = "O(${action.name})"

    override fun target() = action.target()
    override fun complete(state: MapLocationState): Boolean =
        true
}

class WaitUntilActive(val action: Action, private val waitTime: Int = 30) : Action {
    private var stepsWaited = 0
    override fun reset() {
        stepsWaited = 0
        action.reset()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        stepsWaited++
        return action.nextStep(state)
    }

    override val name: String
        get() = action.name

    override fun target() = action.target()
    override fun complete(state: MapLocationState): Boolean =
        stepsWaited > waitTime && action.complete(state)
}

class Must(val action: Action, private val must: Boolean = true) : Action {
    override fun reset() {
        action.reset()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return action.nextStep(state)
    }

    override fun target() = action.target()
    override fun complete(state: MapLocationState): Boolean =
        must && action.complete(state)
}

// after kill all, move to next screen
val lootAndKill = DecisionAction(GetLoot(), KillAll()) { state ->
    state.frameState.enemies.any { it.isLoot }
}

//.also {
//    d { " num enumes " +
//            "${state.frameState.enemies.filter { it.state == EnemyState.Alive }.size}"
//    }
//WaitUntilActive(
val clockActivatedKillAllOrKill = DecisionAction(ClockActivatedKillAll(), KillAll()) { state ->
    state.frameState.clockActivated
}

val lootOrKill = DecisionAction(Optional(GetLoot()), Optional(KillAll())) { state ->
    state.frameState.enemies.any { it.isLoot }
}

fun opportunityKillOrMove(next: MapCell): Action =
    // if it is close kill all will go kill it
    DecisionAction(lootOrKill, MoveTo(next)) { state ->
        !state.cleared && state.hasEnemies && state.hasNearEnemy()
    }

fun killThenLootThenMove(next: MapCell): Action =
    // if it is close kill all will go kill it
    DecisionAction(lootAndKill, MoveTo(next)) { state ->
        !state.cleared && state.hasEnemies
    }

fun lootThenMove(next: MapCell): Action =
    // if it is close kill all will go kill it
    DecisionAction(GetLoot(), MoveTo(next)) { state ->
        state.hasAnyLoot
    }

// assume lined up at the entrance, so link just has to go up
class GoIn(private val moves: Int = 5, private val dir: GamePad = GamePad.MoveUp) : Action {
    private var movements = 0

    override fun complete(state: MapLocationState): Boolean =
        movements == moves

    override fun nextStep(state: MapLocationState): GamePad {
        movements++
        return dir
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override val name: String
        get() = "Go In"
}

class GoDirection(private val dir: GamePad, private val moves: Int = 10) : Action {
    private var movements = 0

    override fun complete(state: MapLocationState): Boolean =
        movements == moves

    override fun nextStep(state: MapLocationState): GamePad {
        d { " MOVE $movements" }
        movements++
        return dir
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override val name: String
        get() = "Go Up $movements"
}

fun FramePoint.toLineOf(length: Int = 8): List<FramePoint> {
    val pts = mutableListOf<FramePoint>()
    for (i in 0..length) {
        pts.add(FramePoint(this.x + i, this.y))
    }

    return pts
}

// move to this location then complete
class InsideNavShop(private val point: FramePoint) : Action {
    private val routeTo = RouteTo.hardlyReplan()

    private val extendedPoint = point.toLineOf(10)
    override fun complete(state: MapLocationState): Boolean =
        // until inventory changed.. how do I know?
        extendedPoint.contains(state.frameState.link.point)

    override fun nextStep(state: MapLocationState): GamePad {
        // need to specify mapcell
        return routeTo.routeTo(state, extendedPoint, overrideMapCell = state.hyrule.shopMapCell)
        // use the shop map cell
//        return NavUtil.directionToAvoidingObstacle(state.hyrule.shopMapCell, state.frameState.link.point, extendedPoint)
    }

    override fun target(): FramePoint {
        return point
    }

    override fun path(): List<FramePoint> {
        return routeTo.route?.path ?: emptyList()
    }

    override val name: String
        get() = "InsideNavShop to ${point}"
}


// move to this location then complete
class InsideNav(private val point: FramePoint) : Action {
    private val routeTo = RouteTo.hardlyReplan()
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
        get() = "Nav to ${point}"
}

class RepeatIfFail() {

}

// do moveTo(100,100) then moveDown(100) while !reached(pushPoint)

class InsideNavAbout(private val point: FramePoint, about: Int, vertical: Int = 1, negVertical: Boolean = false) : Action {
    private val routeTo = RouteTo.hardlyReplan()
    private val points: List<FramePoint>

    init {
        val pts = mutableListOf<FramePoint>()
        if (negVertical) {
            pts.addAll(point.copy(y = point.y - 1).toLineOf(about))
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
        routeTo.routeTo(state, points)

    override fun target(): FramePoint {
        return point
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override val name: String
        get() = "Nav to ${point}"
}

class Bomb(private val target: FramePoint) : Action {
    private val routeTo = RouteTo()
    private val stretchedTarget: List<FramePoint> = target.toLineOf(3)
    private var triedToDeployBomb = false
    private var initialBombs = -1
    override fun complete(state: MapLocationState): Boolean =
        state.usedABomb || state.frameState.inventory.numBombs == 0 // no bombs, just complete this

    val MapLocationState.usedABomb: Boolean
        get() = initialBombs > 0 && frameState.inventory.numBombs != initialBombs

    override fun nextStep(state: MapLocationState): GamePad {
        // nav to anywhere near the target
        return when {
            (state.frameState.link.point.minDistToAny(stretchedTarget) < 10) -> {
                initialBombs = state.frameState.inventory.numBombs
                // TODO: also we have to make sure link is pointed towards the target?
                triedToDeployBomb = true
                // should be fine for now if all I have is bombs
                GamePad.B
            }
            // didn't use the bomb yet, keep waiting for it
            (triedToDeployBomb && !state.usedABomb) -> GamePad.B
            else -> {
                routeTo.routeTo(state, stretchedTarget)
                // changed from this
//                navTo(state, stretchedTarget)
            }
        }
    }
}

fun navTo(state: MapLocationState, to: FramePoint): GamePad =
    navTo(state, listOf(to))

fun navTo(state: MapLocationState, to: List<FramePoint>): GamePad =
    NavUtil.directionToAvoidingObstacle(state.currentMapCell, state.frameState.link.point, to).also {
        d { " **go** $it (from ${state.frameState.link.point} to ${to} at ${state.currentMapCell.mapLoc}" }
    }

class ExitShop() : Action {
    private val routeTo = RouteTo.hardlyReplan()
    override fun complete(state: MapLocationState): Boolean =
        (state.hyrule.shopMapCell.exitsFor(Direction.Down)?.contains(state.frameState.link.point.down7) == true).also {
            if (it) {
                d { " EXIT DONE " }
            }
        }

    override fun target(): FramePoint {
        return super.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        val current = state.hyrule.shopMapCell
        val dir = Direction.Down
        val link = state.frameState.link.point
        val exits = current.exitsFor(dir) ?: return NavUtil.randomDir(link)
        d { " try to exit shop ${state.currentMapCell.mapLoc} ${current.mapLoc} ${exits.firstOrNull()}" }
//        exits.forEach {
//            d { " exit -> $it"}
//        }
        return routeTo.routeTo(state, exits, overrideMapCell = current)
//        return NavUtil.directionToAvoidingObstacle(current, link, exits)
    }
}


private class RouteTo(val params: Param = Param()) {
    companion object {
        fun hardlyReplan() = RouteTo(RouteTo.Param(planCountMax = 100))
    }

    data class Param(
        var planCountMax: Int = 20,
        // not used
        val considerLiveEnemies: Boolean = true
    )

    var route: FrameRoute? = null
        private set
    private var planCount = 0

    fun routeTo(state: MapLocationState, to: List<FramePoint>, forceNewI: Boolean = false, overrideMapCell: MapCell? = null):
            GamePad {
        d { " DO routeTo TO ${to.size}" }
        var forceNew = forceNewI
        if (to.isEmpty()) {
            d { " no where to go " }
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
            // could handle down and right
        }

        val skippedButIsOnRoute = (state.previousMove.skipped && route?.isOn(linkPt, 5) != null)
        if (skippedButIsOnRoute) {
            route?.popUntil(linkPt)
        }

        // getting me suck: && params.planCountMax != 1000
        if (!state.hasEnemies && params.planCountMax != 1000) {
            d { " NO alive enemies, no need to replan just go" }
            // make a plan now though
            forceNew = true
            params.planCountMax = 1000
        } else {
//            d { " oops there are alive enemies, keep it" }
//            for (enemy in state.frameState.enemies) {
//                d { " ememy: $enemy" }
//            }
            params.planCountMax = 20
        }

        var nextPoint: FramePoint = route?.pop() ?: FramePoint()
        val routeSize = route?.path?.size ?: 0

        if (forceNew ||
            route == null || // reset
            routeSize <= 2 ||
            planCount >= params.planCountMax || // could have gotten off track
            !state.previousMove.movedNear // got hit
        ) {
            val why = when {
                !state.previousMove.movedNear -> "got hit"
                planCount >= params.planCountMax -> "old plan"
                route == null -> "no plan"
                forceNew -> "force new ${params.planCountMax}"
                routeSize <= 2 -> " 2 sized route"
                !skippedButIsOnRoute -> "skipped and not on route"
                else -> "I donno"
            }

            val mapCell = overrideMapCell ?: state.currentMapCell
            // of if the expected point is not where we should be
            // need to re route
            route = FrameRoute(NavUtil.routeToAvoidingObstacle(mapCell, linkPt, to))
            d { " ${state.currentMapCell.mapLoc} new plan! because ($why)" }
            route?.next5()
            nextPoint = route?.popOrEmpty() ?: FramePoint() // skip first point because it is the current location
            nextPoint = route?.popOrEmpty() ?: FramePoint()
            d { " next is ${nextPoint}" }
            route?.next5()
            planCount = 0
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
        } else if (nextPoint.direction != null) {
            nextPoint.direction?.toGamePad() ?: GamePad.MoveUp
        } else {
            // change everywher
            NavUtil.directionToDist(linkPt, nextPoint)
        }.also {
            d { " next point $nextPoint dir: $it" }
        }
    }
}

// do both at the same time
class PickupDroppedDungeonItemAndKill : Action {
    private val routeTo = RouteTo()

    private var enemies = mutableListOf<Agent>()

    private var keysNeeded = -1

    private val killAll = KillAll()

    override val name: String
        get() = "pickup dropped item and kill"

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun target(): FramePoint {
        return killAll.target()
    }

    override fun complete(state: MapLocationState): Boolean =
        state.frameState.inventory.numKeys == keysNeeded

    // need to visit all the enemy locations until it is done
    override fun nextStep(state: MapLocationState): GamePad {
        if (keysNeeded == -1) {
            keysNeeded = state.frameState.inventory.numKeys + 1
        }

        return if (state.hasEnemies) {
            killAll.nextStep(state)
        } else {
            if (enemies.isEmpty()) {
                enemies = state.frameState.enemies.toMutableList()
            }

            enemies = enemies.filter { it.topCenter.distTo(state.frameState.link.topCenter) > 8 }.toMutableList()

            d { " remaining enemies: ${enemies.size} ${state.frameState.inventory.numKeys} need ${keysNeeded}" }
            for (enemy in enemies) {
                d { " remaining enemy $enemy" }
            }

            routeTo.routeTo(state, enemies.map { it.topCenter })
        }
    }
}

// TODO:
class ClockActivatedKillAll : Action {
    private val routeTo = RouteTo()
    private val criteria = KillAllCompleteCriteria()

    private var target: FramePoint = FramePoint()

    private var enemies = mutableListOf<Agent>()

    private var pressedACt = 0

    override val name: String
        get() = "pickup dropped item and kill"

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun target(): FramePoint {
        return target
    }

    override fun complete(state: MapLocationState): Boolean =
    // needs another criteria like number killed is number seen or something
        // need to add another "is alive" criteria like some of the presence
        criteria.complete(state)

    // need to visit all the enemy locations until it is done
    override fun nextStep(state: MapLocationState): GamePad {
        criteria.update(state)

        if (enemies.isEmpty()) {
            enemies = state.frameState.enemies.toMutableList()
        }

        state.frameState.logEnemies()

        target = state.frameState.enemiesSorted.first().point

        // todo; debug
        return if (pressedACt > 0) {
            pressedACt--
            GamePad.A
        } else if (enemies.any { it.topCenter.distTo(state.frameState.link.topCenter) < 8 }) {
            // press A for a bit
            pressedACt = 3
            GamePad.A
        } else {
            routeTo.routeTo(state, enemies.map { it.topCenter })
        }
    }
}

class Unstick(private val wrapped: Action, private val howLong:Int = 5000) : Action {
    private var frames = 0
    private var randomMoves = 250
    private var randomMovesCt = 250
    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        frames++
        return when {
            (randomMovesCt <= 0) -> {
                randomMovesCt = randomMoves
                frames = 0
                NavUtil.randomDir(state.link)
            }
            (frames > howLong) -> {
                d { " UNSTICK!!"}
                randomMovesCt--
                NavUtil.randomDir(state.link)
            }
            else -> {
                randomMovesCt = randomMoves
                wrapped.nextStep(state)
            }
        }
    }

    override val name: String
        get() = "Wait for ${wrapped.name}"
}

// complete when link is at this square, otherwise route back to this square
// this helps
//class GetBackTo(val next: MapCell, val forceDirection: Direction? = null) : Action {
//    private val routeTo = RouteTo(params = RouteTo.Param(considerLiveEnemies = false))
//
//}

class MoveTo(val next: MapCell, val forceDirection: Direction? = null) : Action {
    private val routeTo = RouteTo(params = RouteTo.Param(considerLiveEnemies = false))

    init {
        next.gstar.clearAvoid()
    }

    override fun reset() {
        next.gstar.clearAvoid()
        route = null
    }

    override fun complete(state: MapLocationState): Boolean =
        (state.frameState.mapLoc == next.mapLoc).also {
            if (it) route = null
//            d { " Move to complete $it"}
        }

    override fun target(): FramePoint {
        return super.target()
    }

    override fun path(): List<FramePoint> {
        return route?.path ?: emptyList()
    }

    private var route: FrameRoute? = null
    private var planCount = 0

    private var dir: Direction = Direction.Down

    private var start: MapCell? = null

    override fun nextStep(state: MapLocationState): GamePad {
        d { " DO MOVE TO cell ${next.mapLoc}" }

        for (enemy in state.frameState.enemies) {
            d { " enemy: $enemy" }
        }

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
        dir = forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)


//        val exit = state.currentMapCell.exitFor(dir) ?: return NavUtil.randomDir(linkPt)
//        val exit = when (dir) {
//            Direction.Left -> exitA // keep
//            Direction.Right -> exitA // keep
//            Direction.Down -> exitA //.up
//            Direction.Up -> exitA //.down
//        }

//        d { "exits "}
//        exits.forEach {
//            d { " exit -> $it ${linkPt.distTo(it)}"}
//        }

//        goingTo = exits.firstOrNull() ?: FramePoint(0, 0)

        if (state.currentMapCell.exitsFor(dir) == null) {
            d { " default move " }
        }
        val exits = state.currentMapCell.exitsFor(dir) ?: return NavUtil.randomDir()

        return routeTo.routeTo(state, exits)
    }

    override val name: String
        get() = "Move to ${this.next.mapLoc} ${dir ?: ""}"
}

class DecisionAction(
    private val action1: Action,
    private val action2: Action, // also use only action 2 for complete
    private val chooseAction1: (state: MapLocationState) -> Boolean
) : Action {
    override fun complete(state: MapLocationState): Boolean =
        action1.complete(state) && action2.complete(state)
//            .also {
//            d { " decision complete $it ${action1.complete(state)} ${action2.complete(state)}"}
//        }

    private var target: FramePoint = FramePoint(0, 0)
    private var path: List<FramePoint> = emptyList()

    override fun target(): FramePoint {
        return target
    }

    override fun path(): List<FramePoint> = path

    override fun nextStep(state: MapLocationState): GamePad {
        val choose1 = chooseAction1(state)
        val action: Action
        if (choose1) {
            action = action1
        } else {
            action = action2
        }
//        d { "Action -> ${action.javaClass.simpleName}" }
        val gamePad = action.nextStep(state)
        target = action.target()
        path = action.path()
        return gamePad
    }

    override val name: String
        get() = "${action1.name} or ${action2.name}"
}

class GetLoot : Action {
    private val routeTo = RouteTo()

    override fun complete(state: MapLocationState): Boolean =
        state.hasAnyLoot

    // maybe I should
    private var target: FramePoint = FramePoint(0, 0)

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun nextStep(state: MapLocationState): GamePad {
        d { " GET LOOT" }
        val loot = state.frameState.enemies.filter { it.isLoot }.sortedBy {
            it.point.distTo(state.link)
        }

        if (loot.isEmpty()) {
            d { " no loot " }
            for (enemy in state.frameState.enemies) {
                d { " enemy ${enemy}" }
            }
            return NavUtil.randomDir(state.link)
        }

        loot.forEach {
            d { "loot $it dist ${it.point.distTo(state.frameState.link.point)}" }
        }

        val previousTarget = target
        target = loot.first().point

        // untested, try to make it easier to navigate to the loot
        val targets = target.about()

        d { " get loot $target" }
        return routeTo.routeTo(state, targets, forceNewI = previousTarget != target)
    }

    override fun target() = target

    override val name: String
        get() = "Get loot"
}

fun FramePoint.about(horizontal: Int = 4, vertical: Int = 2): List<FramePoint> {
    val targets = mutableListOf<FramePoint>()
    repeat(vertical) {
        val line = this.copy(y = this.y + it).toLineOf(horizontal)
        targets.addAll(line)
    }
    return targets
}

class AlwaysAttack : Action {
    private var previousAttack = false
    override fun complete(state: MapLocationState): Boolean =
        false

    override fun nextStep(state: MapLocationState): GamePad {
        return (if (previousAttack) GamePad.ReleaseA else GamePad.A).also {
            previousAttack = !previousAttack
        }
    }
}

class MoveInCircle : Action {
    override fun complete(state: MapLocationState): Boolean =
        false

    override fun nextStep(state: MapLocationState): GamePad =
        when (state.lastGamePad) {
            GamePad.MoveRight -> GamePad.MoveUp
            GamePad.MoveLeft -> GamePad.MoveDown
            GamePad.MoveUp -> GamePad.MoveLeft
            GamePad.MoveDown -> GamePad.MoveRight
            else -> GamePad.None
        }
}

class Wait(val howLong: Int) : Action {
    private var frames = 0
    override fun complete(state: MapLocationState): Boolean =
        frames < howLong

    override fun nextStep(state: MapLocationState): GamePad {
        frames++
        return GamePad.None
    }

    override val name: String
        get() = "Wait for $howLong"
}

private class KillAllCompleteCriteria {
    private var count = 0
    private var waitAfterAllKilled = 0

    fun update(state: MapLocationState) {
        count++
        if (state.hasEnemies) {
            waitAfterAllKilled = 110
        } else {
            waitAfterAllKilled--
        }
    }

    fun complete(state: MapLocationState): Boolean =
        (waitAfterAllKilled <= 0 && count > 33 && state.cleared).also {
            d { " kill all complete $it" }
            d { " kill all status ${state.frameState.enemies}" }
            state.frameState.enemies.forEach {
                d { "loot $it dist ${it.point.distTo(state.link)}" }
            }
        }
}

class KillAll(
    private val sameEnemyFor: Int = 60,
    private val useBombs: Boolean = false,
    private val waitAfterAttack: Boolean = false,
    private val numberLeftToBeDead: Int = 0,
) :
    Action {
    private val routeTo = RouteTo()
    private val criteria = KillAllCompleteCriteria()

    private var sameEnemyCount = 0

    private var previousAttack = false
    private var pressACount = 0
    private var target: FramePoint = FramePoint(0, 0)

    private var count = 0
    private var waitAfterPressing = 0

    // just be sure everything is dead and not just slow to move
    private var waitAfterAllKilled = 200

    override fun reset() {
        super.reset()
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun target(): FramePoint {
        return target
    }

    override val name: String
        get() = "KILL ALL"

    private fun killedAllEnemies(state: MapLocationState): Boolean {
        return state.clearedWithMin(numberLeftToBeDead)
    }

    override fun complete(state: MapLocationState): Boolean =
//        criteria.complete(state)
        (waitAfterAllKilled <= 0 && count > 33 && killedAllEnemies(state)).also {
            d { " kill all complete $it ${state.numEnemies} or ${numberLeftToBeDead}" }
            d { "result $it ${state.clearedWithMin(numberLeftToBeDead)} ct $count wait $waitAfterAllKilled" }
            state.frameState.enemies.filter { it.state == EnemyState.Alive }.forEach {
                d { "enemy $it dist ${it.point.distTo(state.link)}" }
            }
        }

    override fun nextStep(state: MapLocationState): GamePad {
        d { " KILL ALL step ${state.currentMapCell.mapLoc} count $count wait $waitAfterAllKilled" }

        for (enemy in state.frameState.enemies) {
            d { " enemy: $enemy" }
        }
        criteria.update(state)

        count++
        when {
            // release on last step
            pressACount == 1 -> {
                pressACount = 0
                d { "Press A last time" }
                return if (useBombs) GamePad.ReleaseB else GamePad.ReleaseA
            }

            pressACount > 1 -> {
                pressACount--
                d { "Press A" }
                return if (useBombs) GamePad.B else GamePad.A
            }
            // only for boss
            pressACount == 0 && waitAfterPressing > 0 -> {
                d { "Press A WAIT" }
                waitAfterPressing--
                return GamePad.None
            }
        }

        return if (killedAllEnemies(state)) {
            d { " no enemies" }
            waitAfterAllKilled--
            return GamePad.None // just wait
        } else {
            // maybe we want to kill closest
            // find the alive enemies
            val aliveEnemies = state.frameState.enemiesClosestToLink()
            aliveEnemies.forEach {
                d { "alive enemy $it dist ${it.point.distTo(state.frameState.link.point)}" }
            }

            if (killedAllEnemies(state)) {
                waitAfterAllKilled--
                return GamePad.None // just wait
//                NavUtil.randomDir(state.link)
            } else {
                // 110 too low for bats
                waitAfterAllKilled = 210
                val firstEnemy = aliveEnemies.first()
                val previousTarget = target
                target = firstEnemy.point
                val link = state.frameState.link
                val dist = firstEnemy.point.distTo(link.point)
                //d { " go find $firstEnemy from $link distance: $dist"}
                when {
                    dist < 18 && state.frameState.canUseSword -> {
                        // is linked turned in the correct direction towards
                        // the enemy?
                        previousAttack = true
                        pressACount = 3
                        // for the rhino
                        if (waitAfterAttack) {
                            waitAfterPressing = 60
                        }
                        if (useBombs) GamePad.B else GamePad.A
                    }

                    else -> {
                        // changed enemies
                        sameEnemyCount++
                        val sameEnemyForTooLong = sameEnemyCount > sameEnemyFor
                        if (sameEnemyForTooLong) {
                            sameEnemyCount = 0
                        }
                        val forceNew = previousTarget != target && sameEnemyForTooLong
                        routeTo.routeTo(state, listOf(target), forceNew)
                    }
                }
            }
        }
    }
}
