package bot.plan.action

import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapCell
import bot.state.map.MapConstants
import bot.state.map.grid
import bot.state.oam.*
import nintaco.api.ApiSource
import util.d
import util.i

interface Action {
    /**
     * where this action should take place
     */
    val actionLoc: MapLoc
        get() = -1

    val levelLoc: MapLoc
        get() = -1

    val escapeActionEnabled: Boolean
        get() = true
    val monitorEnabled: Boolean
        get() = true

    fun reset() {
        // empty
    }

    fun complete(state: MapLocationState): Boolean

    fun nextStep(state: MapLocationState): GamePad {
        return GamePad.MoveUp
    }

    fun target() = FramePoint(0, 0)

    fun targets() = listOf(target())

    fun path(): List<FramePoint> {
        d { " default no path" }
        return emptyList()
    }

    fun zstar(): ZStar? = null

    val name: String
        get() = this.javaClass.simpleName
}

abstract class WrappedAction(val wrapped: Action) : Action {
    override fun reset() {
        wrapped.reset()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return wrapped.nextStep(state)
    }

    override fun complete(state: MapLocationState): Boolean = wrapped.complete(state)

    override fun target() = wrapped.target()

    override fun targets() = wrapped.targets()

    override fun path(): List<FramePoint> = wrapped.path()

    override fun zstar(): ZStar? = wrapped.zstar()

    override val name: String
        get() = wrapped.name
}

interface MapAwareAction : Action {
    val from: MapLoc
    val to: MapLoc
}

class OrderActionBuilder() {
    fun build(block: OrderActionBuilder.() -> Unit) {

    }

    // plan builder?
//    fun untilMapChange() {
//
//    }
    fun untilMapChange(vararg actions: Action) {

    }
}

fun make() {
    OrderActionBuilder().build {
        untilMapChange(

        )
    }
}

/**
 * maintains a stack of actions, and moves to the next, once one is complete
 */
class OrderedActionSequence(
    private val actions: List<Action>,
    private val restartWhenDone: Boolean = true,
    private val shouldComplete: Boolean = false,
    private val tag: String = ""
) : Action {
    private var lastComplete = -1
    var stepName: String = ""

    // keep popping the stack
//    private var stack = action
    private var stack = actions.toMutableList()

    var currentAction: Action? = null
    val lastNull: Boolean
        get() = currentAction == null

    val done: Boolean
        get() = hasBegun && stack.isEmpty()
    val tasksLeft: Int
        get() = stack.size

    private fun pop(): Action? = stack.removeFirstOrNull().also {
        currentAction = it
    }

    override fun reset() {
        restart()
    }

    fun restart(): Action? {
        return if (restartWhenDone) {
            stack = actions.toMutableList()
            for (action in actions) {
                d { " reset ${action.name}" }
                action.reset()
            }
            d { " restart ${stack.size}" }
            pop()
        } else {
            null
        }
    }

    // never complete
    override fun complete(state: MapLocationState): Boolean =
        shouldComplete && done && currentAction?.complete(state) == true

    private var target: FramePoint = actions.first().target()
    private var path: List<FramePoint> = emptyList()

    override fun target(): FramePoint {
        return currentAction?.target() ?: stack.firstOrNull()?.target() ?: target
    }

    override fun path(): List<FramePoint> {
        d { " current action ${currentAction?.name ?: ""}" }
        return currentAction?.path() ?: emptyList()
    }

    private var hasBegun = false

    override fun nextStep(state: MapLocationState): GamePad {
        hasBegun = true
        d { "OrderedActionSequence begin ${currentAction?.name ?: "none"}" }
        val current = currentAction ?: pop() ?: restart() ?: return GamePad.randomDirection(state.link)
        // check all complete to prevent infinite loop
        if (current.complete(state)) { // causes bomb one to fail && !allComplete(state)
            d { " sequence complete ${stack.size}" }
//            for (action in stack) {
//                d { "is complete ${action.name} = ${action.complete(state)}" }
//            }
            pop()
            // recur
            return nextStep(state)
        }
        d { " DO --> ${current.name}" }
        stepName = current.name
        return current.nextStep(state)
    }

    private fun allComplete(state: MapLocationState): Boolean =
        actions.all { it.complete(state) }

    override val name: String
        get() = "oSeq: $tag doing ${currentAction?.name}"
}

class CompleteIfGetItem(
    action: Action,
    val select: Inventory.() -> Int = { this.numKeys }
) : WrappedAction(action) {
    private var frameCt = 0
    private var startingQuantity = -1
    override fun complete(state: MapLocationState): Boolean {
        return ((startingQuantity >= 0) && (quantity(state) != startingQuantity)) ||
                super.complete(state)
    }

    private fun quantity(state: MapLocationState) =
        state.frameState.inventory.select()

    override fun nextStep(state: MapLocationState): GamePad {
        d { " CompleteIfGetItem " }
        if (startingQuantity < 0) {
            startingQuantity = quantity(state)
        }
        frameCt++
        return super.nextStep(state)
    }

}

class DecisionAction(
    private val action1: Action,
    private val action2: Action, // also use only action 2 for complete
    private val completeIf: (state: MapLocationState) -> Boolean = { false },
    private val chooseAction1: (state: MapLocationState) -> Boolean,
) : Action {
    override val actionLoc: MapLoc
        get() = if (action1 is MoveTo) action1.to else if (action2 is MoveTo) action2.to else -2

    override val levelLoc: MapLoc
        get() = if (action1 is MoveTo) action1.levelLoc else if (action2 is MoveTo) action2.levelLoc else -2

    override fun complete(state: MapLocationState): Boolean {
        val completeIf = completeIf(state)
        val complete1 = action1.complete(state)
        val complete2 = action2.complete(state)
        return completeIf || (complete1 && complete2)
    }
//            .also {
//            d { " decision complete $it ${action1.complete(state)} ${action2.complete(state)}"}
//        }

    private var target: FramePoint = FramePoint(0, 0)
    private var path: List<FramePoint> = emptyList()

    override fun target(): FramePoint {
        return target
    }

    override fun path(): List<FramePoint> = path

    override fun zstar(): ZStar? = action1.zstar()

    override fun nextStep(state: MapLocationState): GamePad {
        val choose1 = chooseAction1(state)
        val action: Action = if (choose1) {
            action1
        } else {
            action2
        }
        d { "Decision Action -> ${action.name}" }
        val gamePad = action.nextStep(state)
        target = action.target()
        path = action.path()
        return gamePad
    }

    override val name: String
        get() = "${action1.name}${if (action1.name.isEmpty()) "" else " or "}${action2.name}"
}


class ActionSequence(
    private vararg val actions: Action
) : Action {
    var stepName: String = ""
    var currentAction: Action? = null

    // never complete
    override fun complete(state: MapLocationState): Boolean =
        false

    private var target: FramePoint = actions.first().target()
    private var path: List<FramePoint> = actions.first().path()

    override fun target(): FramePoint {
        return currentAction?.target() ?: FramePoint()
    }

    override fun path(): List<FramePoint> {
        d { " current action ${currentAction?.name ?: ""}" }
        return currentAction?.path() ?: emptyList()
    }

    override fun zstar(): ZStar? = currentAction?.zstar()

    override fun nextStep(state: MapLocationState): GamePad {
        d { " DO --> next step" }
        val action = actions.firstOrNull { !it.complete(state) }
        currentAction = action
        stepName = action?.name ?: "none"
        d { " DO --> $stepName" }
        return action?.nextStep(state) ?: GamePad.None // GamePad.randomDirection(state.link)
    }

    override val name: String
        get() = "Action Sequence ${actions.size} ${currentAction?.name ?: ""}"
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

    override fun path(): List<FramePoint> =
        action.path()

    override val name: String
        get() = "O(${action.name})"

    override fun target() = action.target()
    override fun complete(state: MapLocationState): Boolean =
        true
}

val moveToKillAllInCenterSpot = DecisionAction(
    InsideNavAbout(FramePoint(5.grid, 5.grid), 2),
    KillInCenter()
) { state ->
    state.numEnemies <= state.numEnemiesAliveInCenter()
}

fun lootAndKill(kill: Action) = DecisionAction(Optional(GetLoot()), kill) { state ->
    neededReachableLoot(state).isNotEmpty()
}

val lootOrKill = DecisionAction(Optional(GetLoot()), Optional(KillAll())) { state ->
    neededReachableLoot(state).isNotEmpty()
}

fun lootAndMove(moveTo: Action) = DecisionAction(Optional(GetLoot()), moveTo) { state ->
    neededReachableLoot(state).isNotEmpty()
}

//fun lootAndMove(moveTo: Action) = DecisionAction(GetLoot(), moveTo) { state ->
//    neededReachableLoot(state).isNotEmpty()
//}

// do this for all move tasks that are after kill tasks
// in case the kill all didnt work and there are enemies
// around, in that case go back to kill mode
fun killAndMove(moveTo: MoveTo) = DecisionAction(Optional(KillAll()), moveTo) { state ->
    neededReachableLoot(state).isNotEmpty()
}

fun opportunityKillOrMove(next: MapCell, level: Int): Action =
    // if it is close kill all will go kill it
    DecisionAction(lootOrKill, MoveTo(0, next, level)) { state ->
        !state.cleared && state.hasEnemies && state.hasNearEnemy()
    }

val still = AlwaysDo(GamePad.None)

class AlwaysDo(private val dir: GamePad = GamePad.MoveUp) : Action {

    override fun complete(state: MapLocationState): Boolean = false

    override fun nextStep(state: MapLocationState): GamePad {
        return dir
    }

    override val name: String
        get() = "AlwaysDo $dir"
}

class GoInConsume(private val moves: Int = 5, private val dir: GamePad = GamePad.MoveUp) :
    Action {
    private var movements = 0

    override val escapeActionEnabled: Boolean
        get() = false

    override fun complete(state: MapLocationState): Boolean =
        movements >= moves

    override fun nextStep(state: MapLocationState): GamePad {
        if (state.previousLocation != state.link) {
            d { " --> Moved" }
            movements++
        } else {
            d { " --> Moved not effective " }
        }
        return dir
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override val name: String
        get() = "Go In Consume $dir ($movements of $moves)"
}

class GoIn(
    private val moves: Int = 5,
    private val dir: GamePad = GamePad.MoveUp,
    private val reset: Boolean = false,
    private val setMonitorEnabled: Boolean = true,
    /**
     * dodge if necessary
     */
    private val defensive: Boolean = true,
    private val condition: (MapLocationState) -> Boolean = { true }
) :
    Action {
    private var movements = 0

    override val monitorEnabled: Boolean
        get() = setMonitorEnabled

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
        // if about to get hit, go it
        return when (
            val reflex = AttackActionBlockDecider.blockReflex(state)) {
            null -> if (condition(state)) dir else GamePad.None
            else -> reflex.also { d { "!! Reflex while go in !!" } }
        }
//        return if (condition(state)) dir else GamePad.None
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

class GoToward(private val target: FramePoint, private val moves: Int = 10) : Action {
    private var movements = 0

    override fun complete(state: MapLocationState): Boolean =
        movements == moves

    override fun nextStep(state: MapLocationState): GamePad {
        val gamePad = state.link.directionTo(target)
        d { " MOVE $movements" }
        movements++
        return gamePad
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override val name: String
        get() = "Go Toward $target $movements of $moves"
}

open class Bomb(private val target: FramePoint) : Action {
    private val routeTo = RouteTo()
    private val stretchedTarget: List<FramePoint> = target.toLineOf(3)
    private var triedToDeployBomb = false
    private var initialBombs = -1

    override fun reset() {
        triedToDeployBomb = false
        initialBombs = -1
    }

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
//                routeTo.routeTo(state, stretchedTarget)
            }
        }
    }

    override val name: String
        get() = "Bomb $target $triedToDeployBomb"
}

class Bomb2(private val target: FramePoint) : Bomb(target) {
    private var sawBombClose = false
    private val stretchedTarget: List<FramePoint> = target.toLineOf(3)
//        private val routeTo = RouteTo()
//        private val attack = AlwaysAttack(useB = true)
//        private var usedBombCt = 0

    override fun complete(state: MapLocationState): Boolean =
        sawBombClose //super.complete(state) &&

    override fun nextStep(state: MapLocationState): GamePad {
//            if (usedBombCt < 5 && state.frameState.link.point.minDistToAny(stretchedTarget) < 10) {
//                attack.nextStep(state)
//                usedBombCt++
//            }
        // once detected, no need to check more
        if (!sawBombClose) {
            val bomb = state.frameState.enemies.firstOrNull { it.tile == bomb && it.y > 0 }
            bomb?.let {
                sawBombClose = (it.point.distTo(target) < MapConstants.oneGridPoint5)
                d { " saw bomb! dist = ${it.point.distTo(target)} CLOSE!=$sawBombClose bomb = $it" }
            }
        }

        return super.nextStep(state)
    }

    override val name: String
        get() = "Bomb2 $target"
}

class ExitShop : Action {
    private val routeTo = RouteTo.hardlyReplan(dodgeEnemies = false)
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
        return routeTo.routeTo(
            state, exits, RouteTo.RouteParam(
                overrideMapCell = current
            )
        )
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
        d { " keys needed $keysNeeded keysHave = ${state.frameState.inventory.numKeys}" }
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

val MapLocationState.neededLoot: List<Agent>
    get() = frameState.enemies.filter { it.isLoot }.filter { LootKnowledge.needed(this, it) }

fun neededReachableLoot(state: MapLocationState): List<Agent> {
    return state.neededLoot.filter {
        // always get the fairy, it will move into accessible area
        it.tile == fairy || halfInsidePassable(state, it.point, it.tile in LootKnowledge.halfSize)
    }
}

// some items are half sized, like hearts
// should take into account half passable
private fun halfInsidePassable(state: MapLocationState, pt: FramePoint, small: Boolean): Boolean {
    return with(state.currentMapCell) {
        val midPassable = passable.get(pt.downHalf) && passable.get(pt.downHalf.justRightHalf)
        val topPassable = passable.get(pt) && passable.get(pt.justRightHalf)
        d { " mid pass: $pt mid=$midPassable top=$topPassable small=${small}"}
        if (small) {
            // whole thing needs to be showing
            topPassable && midPassable
        } else {
            // have of it needs to be showing
            val bottomPassable = passable.get(pt.downOneGrid) && passable.get(pt.downOneGrid.justRightHalf)
            d { "         bottom=$bottomPassable"}
            (topPassable && midPassable && bottomPassable) || bottomPassable && midPassable
        }
    }
}


object LootKnowledge {
    val keepSet = setOf(heart, fairy, fairy2, bomb, bigCoin) //bigCoin,

    val halfSize = setOf(heart, bomb)

    fun keep(tile: Int): Boolean =
        tile in keepSet

    fun needed(state: MapLocationState, agent: Agent) =
        // get all the loot
        when (agent.tile) {
            bomb -> (state.frameState.inventory.numBombs < 8)
            bigCoin -> (state.frameState.inventory.numRupees < 250)
            fairy,
            heart -> !state.frameState.inventory.heartCalc.noDamage()
            // heart
            // fairy
            else -> true
        }
}

class GetLoot(
    private val adjustInSideLevelBecauseGannon: Boolean = false,
    private val onlyIfYouNeedIt: Boolean = false
) : Action {
    // gannon triforce pieces are sometimes projectiles
    // yea but then we are going to route link into the projectiles
    // so now I parameterize this so that it only ignores projectiles when in gannon
    private val routeTo =
        RouteTo(params = RouteTo.Param(whatToAvoid = RouteTo.Param.makeIgnoreProjectiles(adjustInSideLevelBecauseGannon)))

    override fun complete(state: MapLocationState): Boolean =
        neededReachableLoot(state).isEmpty()

    // it cant be too close to the not passable spot
    // maybe just make sure all of it is passable

    private var target: FramePoint = FramePoint(0, 0)

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()


    override fun nextStep(state: MapLocationState): GamePad {
        d { " GET LOOT" }
        val loot = state.neededLoot.sortedBy {
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
            d { "loot to get: loot $it dist ${it.point.distTo(state.frameState.link.point)}" }
        }

        val previousTarget = target
        target = loot.first().point

        // the target cannot actually be beyond the bottom two
        // lines that would be obsurd
        // need this only for gannon I think
        if (adjustInSideLevelBecauseGannon && target.y > 8.grid) {
            target = FramePoint(target.x, 8.grid)
        }

        // untested, try to make it easier to navigate to the loot
        // this is the wrong way to target an item, let's try the new way
//        val targets = target.about()
        val targets = target.lootTargets.filter {
            halfInsidePassable(state, it, false)
        }

//        val targets = NearestSafestPoint.mapNearest(state, target.lootTargets)
//        // i think this is ok, at least for vis
//        target = NearestSafestPoint.mapNearest(state, listOf(loot.first().point)).first()

        // map nearest can cause zelda to get stuck sometimes
        d { " get loot for $target from targets $targets" }
        return routeTo.routeTo(
            state, targets,
            RouteTo.RouteParam(forceNew = previousTarget != target) // mapNearest = false
        )
    }

    override fun target() = target

    override val name: String
        get() = "Get loot"
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

/**
 * just use to mark where the plan should start
 */
class StartHereAction(private val saveSlot: Int? = null) : Action {
    companion object {
        val name = "StartHereAction"
    }

    fun restoreSaveSlot() {
        saveSlot?.let {
            i { " load save slot $saveSlot" }
            ApiSource.getAPI().quickLoadState(saveSlot)
        }
    }

    override fun complete(state: MapLocationState): Boolean {
        return true
    }
}
