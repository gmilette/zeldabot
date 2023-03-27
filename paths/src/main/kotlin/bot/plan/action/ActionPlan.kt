package bot.plan.action

import bot.GamePad
import bot.plan.action.NavUtil.directionToDir
import bot.plan.gastar.FrameRoute
import bot.state.*
import bot.state.map.*
import util.d
import kotlin.random.Random

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
        return GamePad.None
    }
}

class DoNothing : Action {
    override fun complete(state: MapLocationState): Boolean =
        false

    override fun nextStep(state: MapLocationState): GamePad {
        return GamePad.None
    }
}

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

// after kill all, move to next screen
val lootAndKill = DecisionAction(GetLoot(), KillAll()) { state ->
    state.frameState.enemies.any { it.isLoot }
}

val moveToKillAllInCenterSpot = DecisionAction(
    InsideNavAbout(FramePoint(5.grid, 5.grid), 2),
    KillInCenter()) { state ->
    state.numEnemies <= state.numEnemiesAliveInCenter()
}

class KillInCenter : Action {
    private var frames = 0

    // need to wait to make sure they are killed maybe
    override fun complete(state: MapLocationState): Boolean =
        state.numEnemiesAliveInCenter() == 0

    override fun nextStep(state: MapLocationState): GamePad {
        val move = if (frames < 10) {
            GamePad.MoveUp
        } else {
            if (frames % 10 < 5) {
                GamePad.A
            } else {
                GamePad.None
            }
        }
        frames++
        return move
    }

    override val name: String
        get() = "KillInCenter"
}


object KillHandsLevel7Data {
    val safe = FramePoint(3.grid, 6.grid)
    val attackFrom = FramePoint(3.grid, 6.grid)
    val attractFrom = FramePoint(2.grid-1, 6.grid)
}

// TODO: dropped id not used anymore
val MapLocationState.handActive: Boolean
    get() = this.frameState.enemies.any { it.droppedId == 1 && it.state == EnemyState.Alive }

val still = AlwaysDo(GamePad.None)

class DeadForAWhile(private val limit: Int = 450, val completeCriteria: (MapLocationState) -> Boolean) {
    var frameCount = 0

    operator fun invoke(state: MapLocationState): Boolean {
        return completeCriteria(state) && frameCount > limit
    }

    fun nextStep(state: MapLocationState) {
        d { "DEAD for a while $frameCount"}
        if (completeCriteria(state)) {
            frameCount++
        } else {
//            frameCount = 0
        }
    }

    fun seenEnemy() {
        frameCount = 0
    }
}
class KillHandsInLevel7 : Action {
    private val safe = ActionSequence(InsideNavAbout(KillHandsLevel7Data.safe, 4), still)
    private val positionAttack = ActionSequence(InsideNavAbout(KillHandsLevel7Data.attackFrom, 0),
        GoIn(5, GamePad.MoveLeft, reset = true), AlwaysAttack())
    private val attract = ActionSequence(InsideNavAbout(KillHandsLevel7Data.attractFrom, 0), GoIn(5, GamePad.MoveLeft, reset = true), still)

    private var lastAction = ""
    private var lastTarget = FramePoint(0, 0)

    private val criteria = DeadForAWhile {
        lastAction != "SAFE" && it.clearedWithMinIgnoreLoot(3)
    }

    // need to wait to make sure they are killed maybe
    override fun complete(state: MapLocationState): Boolean = criteria(state)

    private val MapLocationState.handActive: Boolean
        get() = this.frameState.enemies.any { it.droppedId == 1 && it.state == EnemyState.Alive }

    override fun target(): FramePoint {
        return super.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {"KillHandsInLevel7 has hands ${state.handActive}"}
        criteria.nextStep(state)

        return when {
            !state.frameState.canUseSword -> safe.nextStep(state).also {
                d {" --> SAFE"}
                lastAction = "SAFE"
                lastTarget = KillHandsLevel7Data.safe
            }
            state.handActive -> positionAttack.nextStep(state).also {
                d {" --> ATTACK"}
                lastAction = "ATTACK"
                criteria.seenEnemy()
                lastTarget = KillHandsLevel7Data.attackFrom
            }
            !state.handActive -> attract.nextStep(state).also {
                d {" --> ATTRACT"}
                lastAction = "ATTRACT"
                lastTarget = KillHandsLevel7Data.attractFrom
            }
            else -> GamePad.randomDirection().also {
                d {" --> RANDOM"}
                lastTarget = FramePoint()
            }
        }
    }

    override val name: String
        get() = "killHandsInLevel7 $lastAction ${criteria.frameCount}"
}

class SwitchToItemConditionally(private val inventoryPosition: Int = Inventory.Selected.candle) : Action {
    private val switchSequence = mutableListOf<Action>(
        GoIn(2, GamePad.Start),
        GoIn(30, GamePad.None),
        SwitchToItem(inventoryPosition),
        GoIn(100, GamePad.None),
        GoIn(2, GamePad.Start),
        GoIn(30, GamePad.None),
    )

    private val positionShoot = OrderedActionSequence(switchSequence)


    override fun complete(state: MapLocationState): Boolean {
        return positionShoot.finished() || startedWithItem
    }

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    private var firstStep = true
    private var startedWithItem = false

    override fun nextStep(state: MapLocationState): GamePad {
        d {"SwitchToItemConditionally"}
        return if (firstStep && state.frameState.inventory.selectedItem == inventoryPosition) {
            d {"Already have"}
            startedWithItem = true
            GamePad.None
        } else {
            firstStep = false
            positionShoot.nextStep(state)
        }
    }

    override val name: String
        get() = "SwitchToItemConditionally ${inventoryPosition}"
}


// assume switched to arrow
class KillArrowSpider : Action {
    object KillArrowSpiderData {
        // should be the middle of the attack area
        val attackAreaLeftSideMiddleVertical = FramePoint(8.grid, 7.grid)
        val attackAreaLeftSideMiddleVerticalBottom = FramePoint(8.grid, 8.grid)
    }
    // go in attack area
    // move up, then shoot
    private val positionShootO = ActionSequence(
        // todo: if at top of the grid, move to bottom first
        // but this works well enough
        InsideNavAbout(KillArrowSpiderData.attackAreaLeftSideMiddleVertical,
            MapConstants.oneGrid / 2, // stay in middle
            vertical = MapConstants.oneGrid,
            negVertical = MapConstants.oneGrid),
        GoIn(3, GamePad.MoveUp, reset = true),
//        GoIn(3, GamePad.B, reset = true),
//        GoIn(3, GamePad.None, reset = true),
        AlwaysAttack(useB = true, 5)
    )

    // more debugging
    private val positionShootActions = mutableListOf<Action>(
        InsideNavAbout(KillArrowSpiderData.attackAreaLeftSideMiddleVerticalBottom,
            MapConstants.oneGrid / 2, // stay in middle
            vertical = 4,
            negVertical = 4)
    ).also { list ->
        repeat(5) {
            list.add(GoIn(3, GamePad.MoveUp, reset = true))
            list.add(GoIn(3, GamePad.B, reset = true))
            list.add(GoIn(3, GamePad.None, reset = true))
        }
    }

    private val positionShoot = OrderedActionSequence(positionShootActions)

    private val criteria = DeadForAWhile(limit = 200) {
        it.clearedWithMinIgnoreLoot(0)
    }

    override fun complete(state: MapLocationState): Boolean = criteria(state)

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {"KillArrowSpider"}
        criteria.nextStep(state)

        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack

        return positionShoot.nextStep(state)
    }

    override val name: String
        get() = "killHandsInLevel7KillArrowSpider ${criteria.frameCount}"
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

class AlwaysDo(private val dir: GamePad = GamePad.MoveUp) : Action {

    override fun complete(state: MapLocationState): Boolean = false

    override fun nextStep(state: MapLocationState): GamePad {
        return dir
    }

    override val name: String
        get() = "AlwaysDo $dir"
}
// assume lined up at the entrance, so link just has to go up
class GoIn(private val moves: Int = 5, private val dir: GamePad = GamePad.MoveUp, private val reset: Boolean = false) : Action {
    private var movements = 0

    override fun complete(state: MapLocationState): Boolean {
        val complete = movements >= moves
        if (complete && reset) {
//            d { " --> RESET $name $movements"}
            movements = 0
        }
        return complete
    }

    override fun nextStep(state: MapLocationState): GamePad {
//        d { " --> Move $name movements $movements"}
        movements++
        return dir
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override val name: String
        get() = "Go In $dir ($movements of $moves)"
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
        get() = "Go Up $movements of $moves"
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
        get() = "InsideNavShop to $point"
}

class SwitchToItem(private val inventoryPosition: Int = Inventory.Selected.candle) : Action {

    private var pressCountdown = 0
    private var last: GamePad = GamePad.None

    override fun complete(state: MapLocationState): Boolean =
        selectedItem(state)// && pressCountdown <= 0

    private fun selectedItem(state: MapLocationState): Boolean =
        state.frameState.inventory.selectedItem == inventoryPosition // how does this map

    private fun directionToSelection(state: MapLocationState): GamePad =
        if (state.frameState.inventory.selectedItem < inventoryPosition) {
            GamePad.MoveRight
        } else {
            GamePad.MoveLeft
        }

    override fun nextStep(state: MapLocationState): GamePad {
        d { " selecting item selected=${selectedItem(state)} state=${state.frameState.inventory
            .selectedItem}"}
        // this is weird, link cannot just keep pressing left for right, it has to
        // press none inbetween sometimes. Whatever this works
        return if (selectedItem(state) || Random.nextBoolean()) {
                GamePad.None
            } else {
                directionToSelection(state)
            }
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun path(): List<FramePoint> {
        return emptyList()
    }

    override val name: String
        get() = "SwitchToItem to ${inventoryPosition}"
}

class UseItem : Action {
    private var used = 0
    override fun complete(state: MapLocationState): Boolean =
        used > 5

    override fun nextStep(state: MapLocationState): GamePad {
        return GamePad.B.also {
            used++
        }
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun path(): List<FramePoint> {
        return emptyList()
    }

    override val name: String
        get() = "Use item $used"
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

class InsideNavAbout(private val point: FramePoint, about: Int, vertical: Int = 1, negVertical: Int = 0,
                     val shop: Boolean = false) : Action {
    private val routeTo = RouteTo.hardlyReplan()
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
        routeTo.routeTo(state, points, overrideMapCell = if (shop) state.hyrule.shopMapCell else null)

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

    override val name: String
        get() = "Bomb $target $triedToDeployBomb"
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
        d { " DO routeTo TO ${to.size} first ${to.firstOrNull()?.toG} currently at ${state.currentMapCell.mapLoc}" }
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
            val avoid = state.aliveEnemies.map { it.point }
            route = FrameRoute(NavUtil.routeToAvoidingObstacle(mapCell, linkPt, to, avoid))
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
        d { " keys needed $keysNeeded keysHave = ${state.frameState.inventory.numKeys}"}
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


fun moveHistoryAttackAction(wrapped: Action) =
    // really, if there are enemies attack, otherwise, move, or bomb
    // if there are pancake enemies
    MoveHistoryAction(wrapped, AlwaysAttack())

private class SameCount {
    var last: GamePad = GamePad.None
    private var count: Int = 0

    fun record(pad: GamePad): Int {
        if (last == pad) {
            count++
        }
        last = pad
        return count
    }

    fun reset() {
        count = 0
    }
}

// default actions
// if same x,y as enemy, attack
// unstick

class MoveHistoryAction(private val wrapped: Action, private val escapeAction: Action) : Action {
    private val histSize = 250
    private val same = SameCount()
    private var escapeActionCt = 0
    private val escapeActionTimes = 50

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad =
        when {
            escapeActionCt > 0 -> {
                d { " ESCAPE ACTION "}
                val action = escapeAction.nextStep(state)
                escapeActionCt--
                action
            }
            else -> {
                val nextStep = wrapped.nextStep(state)
                val ct = same.record(nextStep)
                d { " ESCAPE ACTION not same $nextStep + $ct last ${same.last}"}
                if (ct >= histSize) {
                    escapeActionCt = escapeActionTimes
                    d { " ESCAPE ACTION RESET"}
                    same.reset()
                }
                nextStep
            }
        }

    override val name: String
        get() = wrapped.name
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
        get() = "Unstick for ${wrapped.name}"
}

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

class ActionSequence(
    private vararg val actions: Action
): Action {
    // never complete
    override fun complete(state: MapLocationState): Boolean =
        false

    private var target: FramePoint = actions.first().target()
    private var path: List<FramePoint> = emptyList()

    override fun target(): FramePoint {
        return target
    }

    override fun path(): List<FramePoint> = path

    override fun nextStep(state: MapLocationState): GamePad {
        val action = actions.firstOrNull { !it.complete(state) }
        d { " DO --> ${action?.name}"}
        return action?.nextStep(state) ?: GamePad.randomDirection()
    }

    override val name: String
        get() = "Action Sequence ${actions.size}"
}

class OrderedActionSequence(
    private val actions: List<Action>,
    private val restartWhenDone: Boolean = true
): Action {
    private var lastComplete = -1

  // keep popping the stack
//    private var stack = action
    private var stack = actions.toMutableList()

    private var currentAction: Action? = null

    private fun pop(): Action? = stack.removeFirstOrNull().also {
        currentAction = it
    }

    fun restart(): Action? {
        return if (restartWhenDone) {
            stack = actions.toMutableList()
            pop()
        } else {
            null
        }
    }

    fun finished(): Boolean = stack.isEmpty()

    // never complete
    override fun complete(state: MapLocationState): Boolean =
        false

    private var target: FramePoint = actions.first().target()
    private var path: List<FramePoint> = emptyList()

    override fun target(): FramePoint {
        return target
    }

    override fun path(): List<FramePoint> = path

    override fun nextStep(state: MapLocationState): GamePad {
        val current = currentAction ?: pop() ?: restart() ?: return GamePad.randomDirection()
        if (current.complete(state)) {
            pop()
            // recur
            return nextStep(state)
        }
        d { " DO --> ${current.name}"}
        return current.nextStep(state)
    }

    override val name: String
        get() = "OrderedAction Sequence ${actions.size}"
}

class DecisionAction(
    private val action1: Action,
    private val action2: Action, // also use only action 2 for complete
    private val completeIf: (state: MapLocationState) -> Boolean = {false},
    private val chooseAction1: (state: MapLocationState) -> Boolean,
) : Action {
    override fun complete(state: MapLocationState): Boolean =
        completeIf(state) || (action1.complete(state) && action2.complete(state))
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
        d { "Action -> ${action.javaClass.simpleName}" }
        val gamePad = action.nextStep(state)
        target = action.target()
        path = action.path()
        return gamePad
    }

    override val name: String
        get() = "${action1.name} or ${action2.name}"
}

class GetLoot(private val adjustInSideLevel: Boolean = false) : Action {
    private val routeTo = RouteTo()

    override fun complete(state: MapLocationState): Boolean =
        !state.hasAnyLoot

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

        // the target cannot actually be beyond the bottom two
        // lines that would be obsurd
        // need this only for gannon I think
        if (adjustInSideLevel && target.y > 8.grid) {
            target = FramePoint(target.x, 8.grid)
        }

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

class AlwaysAttack(useB: Boolean = false, private val freq: Int = 5) : Action {
    private var frames = 0
    private val gameAction = if (useB) GamePad.B else GamePad.A

    override fun nextStep(state: MapLocationState): GamePad {
        d { " dir ${state.frameState.link.dir}"}
        val move = if (frames < 10) {
            GamePad.None
        } else {
            when {
                frames % 10 < freq -> gameAction
                else -> GamePad.None
            }
        }
        frames++
        return move
    }

    override fun complete(state: MapLocationState): Boolean =
        false

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

/**
 * just use to mark where the plan should start
 */
class StartHereAction: Action {
    companion object {
        val name = "StartHereAction"
    }
    override fun complete(state: MapLocationState): Boolean {
        return true
    }
}

class KillAll(
    private val sameEnemyFor: Int = 60,
    private val useBombs: Boolean = false,
    private val waitAfterAttack: Boolean = false,
    private val numberLeftToBeDead: Int = 0,
    /**
     * everything else is a potential projectile
     */
    private val numEnemies: Int = -1,
    // do not try to kill the enemies in the center
    private val considerEnemiesInCenter: Boolean = false,
    private val ifCantMoveAttack: Boolean = false
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
        get() = "KILL ALL ${if (numberLeftToBeDead > 0) "until $numberLeftToBeDead" else ""}"

    private fun killedAllEnemies(state: MapLocationState): Boolean {
        return state.clearedWithMinIgnoreLoot(numberLeftToBeDead + centerEnemies(state))
//        return state.clearedWithMin(numberLeftToBeDead)
    }

    private fun centerEnemies(state: MapLocationState): Int =
        if (considerEnemiesInCenter) state.numEnemiesAliveInCenter() else 0

    private fun killedAllEnemiesIgnoreLoot(state: MapLocationState): Boolean {
        return state.clearedWithMinIgnoreLoot(numberLeftToBeDead)
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
        val numEnemiesInCenter = state.numEnemiesAliveInCenter()
        d { " KILL ALL step ${state.currentMapCell.mapLoc} count $count wait $waitAfterAllKilled center: $numEnemiesInCenter" }

        for (enemy in state.frameState.enemies.filter { it.state != EnemyState.Dead }) {
            d { " enemy: $enemy" }
        }
        criteria.update(state)

        count++
        when {
            // reset on the last count
            pressACount == 1 -> {
                pressACount = 0
                // have to release for longer than 1
                d { "Press A last time" }
                return GamePad.None
            }

            pressACount > 4 -> {
                pressACount--
                d { "Press A" }
                return if (useBombs) GamePad.B else GamePad.A
            }

            // release for a few steps
            pressACount > 1 -> {
                pressACount--
                return if (useBombs) GamePad.ReleaseB else GamePad.ReleaseA
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
            val aliveEnemies = state.frameState.enemiesClosestToLink().filter {
                // test
                it.index >= numberLeftToBeDead
            }
//            aliveEnemies.forEach {
//                d { "alive enemy $it dist ${it.point.distTo(state.frameState.link.point)}" }
//            }

            if (killedAllEnemies(state)) {
                waitAfterAllKilled--
                return GamePad.None // just wait
//                NavUtil.randomDir(state.link)
            } else {
                // 110 too low for bats
                waitAfterAllKilled = 250
                val firstEnemyOrNull = aliveEnemies.firstOrNull()
                // handle the null?? need to test
                firstEnemyOrNull?.let { firstEnemy ->
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
                            pressACount = 6
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
                } ?: GamePad.None
            }
        }
    }
}
