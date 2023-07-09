package bot.plan.action

import bot.state.GamePad
import bot.state.*
import bot.state.map.*
import nintaco.api.ApiSource
import util.d
import util.i

interface Action {
    fun reset() {
        // empty
    }

    fun complete(state: MapLocationState): Boolean

    fun nextStep(state: MapLocationState): GamePad {
        return GamePad.MoveUp
    }

    fun target() = FramePoint(0, 0)

    fun path(): List<FramePoint> {
        d { " default no path"}
        return emptyList()
    }

    val name: String
        get() = this.javaClass.simpleName
}

interface MapAwareAction : Action {
    val from: MapLoc
    val to: MapLoc
}

class OrderedActionSequence(
    private val actions: List<Action>,
    private val restartWhenDone: Boolean = true
) : Action {
    private var lastComplete = -1
    var stepName: String = ""

    // keep popping the stack
//    private var stack = action
    private var stack = actions.toMutableList()

    var currentAction: Action? = null

    val done: Boolean
        get() = stack.isEmpty()
    val tasksLeft: Int
        get() = stack.size

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

    // never complete
    override fun complete(state: MapLocationState): Boolean =
        false

    private var target: FramePoint = actions.first().target()
    private var path: List<FramePoint> = emptyList()

    override fun target(): FramePoint {
        return target
    }

    override fun path(): List<FramePoint> {
        d { " current action ${currentAction?.name ?: ""}"}
        return currentAction?.path() ?: emptyList()
    }
    override fun nextStep(state: MapLocationState): GamePad {
        val current = currentAction ?: pop() ?: restart() ?: return GamePad.randomDirection(state.link)
        if (current.complete(state)) {
            pop()
            // recur
            return nextStep(state)
        }
        d { " DO --> ${current.name}" }
        stepName = current.name
        return current.nextStep(state)
    }

    override val name: String
        get() = "OrderedAction Sequence ${actions.size}"
}

class DecisionAction(
    private val action1: Action,
    private val action2: Action, // also use only action 2 for complete
    private val completeIf: (state: MapLocationState) -> Boolean = { false },
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
        d { " current action ${currentAction?.name ?: ""}"}
        return currentAction?.path() ?: emptyList()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d { " DO --> next step" }
        val action = actions.firstOrNull { !it.complete(state) }
        currentAction = action
        stepName = action?.name ?: "none"
        d { " DO --> $stepName" }
        return action?.nextStep(state) ?: GamePad.None // GamePad.randomDirection(state.link)
    }

    override val name: String
        get() = "Action Sequence ${actions.size}"
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
    KillInCenter()
) { state ->
    state.numEnemies <= state.numEnemiesAliveInCenter()
}

val clockActivatedKillAllOrKill = DecisionAction(ClockActivatedKillAll(), KillAll()) { state ->
    state.frameState.clockActivated
}

val lootOrKill = DecisionAction(Optional(GetLoot()), Optional(KillAll())) { state ->
    state.frameState.enemies.any { it.isLoot }
}

fun opportunityKillOrMove(next: MapCell): Action =
    // if it is close kill all will go kill it
    DecisionAction(lootOrKill, MoveTo(0, next)) { state ->
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

    override fun complete(state: MapLocationState): Boolean =
        movements >= moves

    override fun nextStep(state: MapLocationState): GamePad {
        if (state.previousLocation != state.link) {
            d { " --> Moved"}
            movements++
        } else {
            d { " --> Moved not effective "}
        }
        return dir
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override val name: String
        get() = "Go In Consume $dir ($movements of $moves)"
}

class GoIn(private val moves: Int = 5, private val dir: GamePad = GamePad.MoveUp, private val reset: Boolean = false) :
    Action {
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

open class Bomb(private val target: FramePoint) : Action {
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
        return routeTo.routeTo(state, exits, overrideMapCell = current)
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

class GetLoot(private val adjustInSideLevel: Boolean = false) : Action {
    // gannon triforce pieces are sometimes projectiles
    private val routeTo = RouteTo(RouteTo.Param(ignoreProjectiles = true))

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
        return routeTo.routeTo(state, targets, forceNew = previousTarget != target)
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
            i { " load save slot $saveSlot"}
            ApiSource.getAPI().quickLoadState(saveSlot)
        }
    }

    override fun complete(state: MapLocationState): Boolean {
        return true
    }
}
