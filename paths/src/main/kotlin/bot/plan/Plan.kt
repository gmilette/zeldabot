package bot

import bot.state.MapLocationState
import util.d

class Plan {
    private val actions: MutableList<Action> = mutableListOf()

    init {
        push(KillAll())
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
            return ZeldaBot.GamePad.ReleaseA
        }
        // go after the first enemy
        return if (state.frameState.enemies.isEmpty()) {
            ZeldaBot.GamePad.MoveUp
        } else {
            val firstEnemy = state.frameState.enemies.first()
            val link = state.frameState.link
            val dist = Math.abs(firstEnemy.x - link.x) + Math.abs(firstEnemy
                .y - link.y)
            d { " go find $firstEnemy from $link distance: $dist"}
            when {
                dist < 18 -> {
                    previousAttack = true
                    ZeldaBot.GamePad.A
                }
                (link.x == firstEnemy.x) -> {
                    when {
                        (link.y > firstEnemy.y) -> ZeldaBot.GamePad.MoveUp
                        else -> ZeldaBot.GamePad.MoveDown
                    }
                }
                else -> when {
                    (link.x > firstEnemy.x) -> ZeldaBot.GamePad.MoveLeft
                    else -> ZeldaBot.GamePad.MoveRight
                }
            }
        }
    }
}

class MoveTo(private val destination: MapLoc): Action {
    override fun complete(state: FrameState): Boolean =
        (state.mapLoc != destination)
}





//class