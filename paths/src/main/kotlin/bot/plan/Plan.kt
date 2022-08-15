package bot

import bot.state.MapLocationState
import util.d
import kotlin.math.abs

class Plan {
    private val actions: MutableList<Action> = mutableListOf()

    init {
        push(lootOrKill)
    }

    fun current(): Action =
        actions.first()

    fun push(action: Action) {
        actions.add(action)
    }

    fun pop(): Action =
        actions.removeLast()
}

interface Action {
    fun complete(state: FrameState): Boolean

    fun nextStep(state: MapLocationState): ZeldaBot.GamePad {
        return ZeldaBot.GamePad.MoveUp
    }
}

class AlwaysMoveUp: Action {
    override fun complete(state: FrameState): Boolean =
        false

    override fun nextStep(state: MapLocationState): ZeldaBot.GamePad {
        return ZeldaBot.GamePad.MoveUp
    }
}

val lootOrKill = DecisionAction(GetLoot(), KillAll()) { state ->
    state.frameState.enemies.any { it.isLoot }
}

class DecisionAction(
    private val action1: Action,
    private val action2: Action,
    private val chooseAction1: (state: MapLocationState) -> Boolean
): Action {
    override fun complete(state: FrameState): Boolean =
        false

    override fun nextStep(state: MapLocationState): ZeldaBot.GamePad =
        if (chooseAction1(state)) {
            action1.nextStep(state)
        } else {
            action2.nextStep(state)
        }
}

class GetLoot: Action {
    override fun complete(state: FrameState): Boolean =
        false

    // maybe I should

    override fun nextStep(state: MapLocationState): ZeldaBot.GamePad {
        val loot = state.frameState.enemies.map { it }.sortedBy { it.point
            .distTo(state.frameState.link.point) }

        return NavUtil.moveTowards(state.frameState.link.point, loot.first()
            .point)
    }
}


class AlwaysAttack: Action {
    private var previousAttack = false
    override fun complete(state: FrameState): Boolean =
        false

    override fun nextStep(state: MapLocationState): ZeldaBot.GamePad {
        return (if (previousAttack) ZeldaBot.GamePad.ReleaseA else ZeldaBot
                .GamePad.A).also {
                    previousAttack = ! previousAttack
        }
    }
}

class MoveInCircle: Action {
    override fun complete(state: FrameState): Boolean =
        false

    override fun nextStep(state: MapLocationState): ZeldaBot.GamePad =
         when (state.lastGamePad) {
            ZeldaBot.GamePad.MoveRight -> ZeldaBot.GamePad.MoveUp
            ZeldaBot.GamePad.MoveLeft -> ZeldaBot.GamePad.MoveDown
            ZeldaBot.GamePad.MoveUp -> ZeldaBot.GamePad.MoveLeft
            ZeldaBot.GamePad.MoveDown -> ZeldaBot.GamePad.MoveRight
            else -> ZeldaBot.GamePad.None
    }
}

class KillAll: Action {
    private var previousAttack = false

    override fun complete(state: FrameState): Boolean =
        false

    override fun nextStep(state: MapLocationState): ZeldaBot.GamePad {
        if (previousAttack) {
            // reset it here
            previousAttack = false
            return ZeldaBot.GamePad.ReleaseA
        }
        // go after the first enemy
        return if (state.frameState.enemies.isEmpty()) {
            ZeldaBot.GamePad.MoveUp
        } else {
            // maybe we want to kill closest
            // find the alive enemies
            val aliveEnemies = state.frameState.enemies.filter { it.state ==
                    EnemyState.Alive }.sortedBy { it.point.distTo(state.frameState
                .link.point) }
//                .frameState
//                    .ememyState.zip
//                    (state
//                .frameState
//            .enemies)
//                .mapNotNull {
//                if (it.first == EnemyState.Alive) {
//                    it.second
//                } else {
//                    null
//                }
//            }.sortedBy { it.distTo(state.frameState.link) }

            aliveEnemies.forEach {
                d { "enemy $it dist ${it.point.distTo(state.frameState.link.point)}"}
            }

            if (aliveEnemies.isEmpty()) {
                ZeldaBot.GamePad.MoveUp
            } else {
                val firstEnemy = aliveEnemies.first()
                val link = state.frameState.link
                val dist = abs(firstEnemy.x - link.x) + abs(firstEnemy
                    .y - link.y)
                d { " go find $firstEnemy from $link distance: $dist"}
                when {
                    dist < 18 -> {
                        // is linked turned in the correct direction towards
                        // the enemy?
                        previousAttack = true
                        ZeldaBot.GamePad.A
                    }
                    (link.x.closeTo(firstEnemy.x, 5)) -> {
                        when {
                            (link.y > firstEnemy.y) -> ZeldaBot.GamePad.MoveUp
                            else -> ZeldaBot.GamePad.MoveDown
                        }
                    }
                    else -> when {
                        // not sure aoub
                        (link.x.closeTo(firstEnemy.x, 5)) -> ZeldaBot.GamePad
                            .MoveRight
                        else -> ZeldaBot.GamePad.MoveLeft // test
                    }
                }
            }
        }
    }
}

object NavUtil {
    fun moveTowards(link: FramePoint, target: FramePoint): ZeldaBot.GamePad {
        val dist = abs(target.x - link.x) + abs(target
            .y - link.y)
        d { " go find $target from $link distance: $dist"}
        return when {
//            dist < 18 -> {
//                whenClose()
//            }
            (link.x.closeTo(target.x, 5)) -> {
                when {
                    (link.y > target.y) -> ZeldaBot.GamePad.MoveUp
                    else -> ZeldaBot.GamePad.MoveDown
                }
            }
            else -> when {
                (link.x.closeTo(target.x, 5)) -> ZeldaBot.GamePad
                    .MoveLeft
                else -> ZeldaBot.GamePad.MoveRight
            }
        }
    }
}

fun Int.closeTo(other: Int, tolerance: Int) =
    this > (other - tolerance) && this < other + tolerance

class MoveTo(private val destination: MapLoc): Action {
    override fun complete(state: FrameState): Boolean =
        (state.mapLoc != destination)
}





//class