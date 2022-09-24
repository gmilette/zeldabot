package bot.plan

import bot.GamePad
import bot.state.*
import util.d
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
    fun complete(state: MapLocationState): Boolean

    fun nextStep(state: MapLocationState): GamePad {
        return GamePad.MoveUp
    }

    fun target() = FramePoint(0,0)
}

class AlwaysMoveUp: Action {
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

private val MapLocationState.hasAnyLoot: Boolean
    get() = frameState.enemies.any { it.isLoot }

private val MapLocationState.cleared: Boolean
    get() = !hasEnemies && !hasAnyLoot

// after kill all, move to next screen
val lootOrKill = DecisionAction(GetLoot(), KillAll()) { state ->
    state.frameState.enemies.any { it.isLoot }.also { d { " num enumes " +
            "${state.frameState.enemies.filter { it.state == EnemyState.Alive }.size}"} }
}

//class MoveToNextScreen: Action {
//    private val moveTo = MoveTowardsUtil()
//    override fun complete(state: MapLocationState): Boolean =
//        false
//
//    override fun nextStep(state: MapLocationState): GamePad {
//
//        if (!state.frameState.isScrolling) {
//            state.masterPlan.advanceIfComplete(state.mapCell)
//        }
//
//        val current = state.masterPlan.current
//        val next = state.masterPlan.next
//        val linkPt = state
//            .frameState
//            .link.point
//
//        // move in
//        val dir = state.masterPlan.nextDirection // move towards exit? (might get
//        // stuck)
//        val exitA = state.mapCell.exitFor(dir) ?: return GamePad.MoveUp
//        val exit = when (dir) {
//            Direction.Left -> exitA // keep
//            Direction.Right -> exitA // keep
//            Direction.Down -> exitA //.up
//            Direction.Up -> exitA //.down
//        }
//        d { " go to exit within ${current.mapLoc} -> ${next.mapLoc} from " +
//                "${linkPt} " +
//                "to ${exit} dir $dir" }
//        // no need to have clever route just route
//        return NavUtil.directionToAvoidingObstacle(state.mapCell, linkPt, exit).also {
//            d { " **go** $it"}
//        }
//
//        // TODO test this
////        return moveTo.moveTowards(linkPt, exit, state.previousMove).also {
////            d { " **go** $it ${state.previousMove}"}
////        }
//
//        // find the exits
////        val exit = state.mapCell.anyExit() ?: return GamePad.MoveUp
////        d { " go to $exit"}
////        NavUtil.directionToAvoidingObstacle(state.mapCell, state.frameState
////            .link.point, exit)
////        return GamePad.MoveUp
//    }
//}

fun opportunityKillOrMove(next: MapCell): Action =
    // if it is close kill all will go kill it
    DecisionAction(lootOrKill, MoveTo(next)) { state ->
        !state.cleared && state.hasEnemies && state.hasNearEnemy()
    }

// move to this location then complete
class InsideNav(private val point: FramePoint): Action {
    override fun complete(state: MapLocationState): Boolean =
        state.frameState.link.point == point

    override fun nextStep(state: MapLocationState): GamePad {
        return navTo(state, point)
    }
}

class Bomb(private val target: FramePoint): Action {
    private var triedToDeployBomb = false
    private var initialBombs = -1
    override fun complete(state: MapLocationState): Boolean =
        state.usedABomb || state.frameState.inventory.numBombs == 0 // no bombs, just complete this

    val MapLocationState.usedABomb: Boolean
        get() = initialBombs > 0 && frameState.inventory.numBombs != initialBombs

    override fun nextStep(state: MapLocationState): GamePad {
        // nav to anywhere near the target
        return when {
            (state.frameState.link.point.distTo(target) < 10) -> {
                initialBombs = state.frameState.inventory.numBombs
                // TODO: also we have to make sure link is pointed towards the target?
                triedToDeployBomb = true
                // should be fine for now if all I have is bombs
                GamePad.B
            }
            // didn't use the bomb yet, keep waiting for it
            (triedToDeployBomb && !state.usedABomb) -> GamePad.B
            else -> navTo(state, target)
        }
    }
}


fun navTo(state: MapLocationState, to: FramePoint): GamePad =
    NavUtil.directionToAvoidingObstacle(state.currentMapCell, state.frameState.link.point, to).also {
        d { " **go** $it"}
    }

class MoveTo(val next: MapCell): Action {
    private val moveTo = MoveTowardsUtil()

    override fun complete(state: MapLocationState): Boolean =
        (state.frameState.mapLoc == next.mapLoc)

    override fun nextStep(state: MapLocationState): GamePad {
        val current = state.currentMapCell
        val next = next
        val linkPt = state
            .frameState
            .link.point

        val dir = NavUtil.directionToDir(current.point.toFrame(), next.point.toFrame())
        val exitA = state.currentMapCell.exitFor(dir) ?: return GamePad.MoveUp
        val exit = when (dir) {
            Direction.Left -> exitA // keep
            Direction.Right -> exitA // keep
            Direction.Down -> exitA //.up
            Direction.Up -> exitA //.down
        }
        d { " go to exit within ${current.mapLoc} -> ${next.mapLoc} from ${linkPt} to ${exit} dir $dir" }
        return NavUtil.directionToAvoidingObstacle(state.currentMapCell, linkPt, exit).also {
            d { " **go** $it"}
        }

        // TODO test this
//        return moveTo.moveTowards(linkPt, exit, state.previousMove).also {
//            d { " **go** $it ${state.previousMove}"}
//        }

        // find the exits
//        val exit = state.mapCell.anyExit() ?: return GamePad.MoveUp
//        d { " go to $exit"}
//        NavUtil.directionToAvoidingObstacle(state.mapCell, state.frameState
//            .link.point, exit)
//        return GamePad.MoveUp
    }
}


class DecisionAction(
    private val action1: Action,
    private val action2: Action, // also use only action 2 for complete
    private val chooseAction1: (state: MapLocationState) -> Boolean
): Action {
    override fun complete(state: MapLocationState): Boolean =
        action2.complete(state)

    private var target: FramePoint = FramePoint(0, 0)

    override fun target(): FramePoint {
        return target
    }

    override fun nextStep(state: MapLocationState): GamePad {
        val choose1 = chooseAction1(state)
        val action: Action
        if (choose1) {
            action = action1
        } else {
            action = action2
        }
        d { "Action -> ${action.javaClass.simpleName}"}
        val gamePad = action.nextStep(state)
        target = action.target()
        return gamePad
    }
}

class GetLoot: Action {
    override fun complete(state: FrameState): Boolean =
        false

    // maybe I should
    private var target: FramePoint = FramePoint(0, 0)

    override fun nextStep(state: MapLocationState): GamePad {
        val loot = state.frameState.enemies.map { it }.sortedBy { it.point
            .distTo(state.frameState.link.point) }
        target = loot.first().point
        return NavUtil.directionToAvoidingObstacle(state.currentMapCell, state.frameState.link.point,
            loot.first().point)
    }

    override fun target() = target
}


class AlwaysAttack: Action {
    private var previousAttack = false
    override fun complete(state: FrameState): Boolean =
        false

    override fun nextStep(state: MapLocationState): GamePad {
        return (if (previousAttack) GamePad.ReleaseA else GamePad.A).also {
                    previousAttack = ! previousAttack
        }
    }
}

class MoveInCircle: Action {
    override fun complete(state: FrameState): Boolean =
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

class KillAll: Action {
    private var previousAttack = false
    private var pressACount = 0
    private var target: FramePoint = FramePoint(0, 0)

    override fun target(): FramePoint {
        return target
    }

    override fun complete(state: FrameState): Boolean =
        false

    override fun nextStep(state: MapLocationState): GamePad {
        when {
            // release on last step
            pressACount == 1 -> {
                pressACount = 0
                return GamePad.ReleaseA
            }
            pressACount > 1 -> {
                pressACount--
                return GamePad.A
            }
        }

//        if (previousAttack) {
//            // reset it here
//            previousAttack = false
//            return GamePad.ReleaseA
//        }
        // go after the first enemy
        return if (state.frameState.enemies.isEmpty()) {
            GamePad.MoveUp
        } else {
            // maybe we want to kill closest
            // find the alive enemies
            val aliveEnemies = state.frameState.enemies.filter { it.state ==
                    EnemyState.Alive }.sortedBy { it.point.distTo(state.frameState
                .link.point) }

            aliveEnemies.forEach {
                d { "enemy $it dist ${it.point.distTo(state.frameState.link.point)}"}
            }

            if (aliveEnemies.isEmpty()) {
                GamePad.MoveUp
            } else {
                val firstEnemy = aliveEnemies.first()
                target = firstEnemy.point
                val link = state.frameState.link
                val dist = abs(firstEnemy.x - link.x) + abs(firstEnemy
                    .y - link.y)
                //d { " go find $firstEnemy from $link distance: $dist"}
                when {
                    dist < 18 -> {
                        // is linked turned in the correct direction towards
                        // the enemy?
                        previousAttack = true
                        pressACount = 3
                        GamePad.A
                    } else -> {
                        NavUtil.directionToAvoidingObstacle(state.currentMapCell, state.frameState.link.point,
                            firstEnemy.point)
                    }
//                    Debug: (Kermit)  go find Agent(point=FramePoint(x=144, y=98), dir=Unknown, state=Alive, isLoot=false, countDown=92) from Agent(point=FramePoint(x=216, y=93), dir=Up, state=Unknown, isLoot=false, countDown=0) distance: 77
//                    Debug: (Kermit) current --> FramePoint(x=144, y=98) link FramePoint(x=216, y=93)
//                    Debug: (Kermit) press MoveRight                114-216 -60

//                    (link.x.closeTo(firstEnemy.x, 5)) -> {
//                        when {
//                            (link.y > firstEnemy.y) -> ZeldaBot.GamePad.MoveUp
//                            else -> ZeldaBot.GamePad.MoveDown
//                        }
//                    }
//                    else -> when {
//                        (link.x > firstEnemy.x ) -> ZeldaBot.GamePad.MoveLeft
//                        else -> ZeldaBot.GamePad.MoveRight
//                    }
                }
            }
        }
    }
}
