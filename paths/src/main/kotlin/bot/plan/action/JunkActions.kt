import bot.plan.action.Action
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import util.d

class MoveHistoryActionQueue(private val wrapped: Action, private val escapeAction: () -> GamePad) : Action {
    private val histSize = 75

    private var queue = mutableListOf<GamePad>()

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        val same = queue.distinct().size == 1
        d { "same $same $histSize"}
        queue.take(10).forEach {
            d { " queue $it"}
        }
        val bigger = queue.size >= histSize
        return if (same && bigger) {
            d { " queue ESCAPE ACTION "}
            escapeAction()
        } else {
            d { " queue normal action "}
            val nextStep = wrapped.nextStep(state)
            queue.add(0, nextStep)
            if (queue.size > histSize) {
                queue.removeLast()
            }
            d { " queue size ${queue.size} "}
            nextStep
        }
    }

    override val name: String
        get() = "Unstick for ${wrapped.name}"
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

//val killAllAndInCenter = DecisionAction(moveToKillAllInCenterSpot, KillAll()) { state ->
//    state.numEnemies <= state.numEnemiesAliveInCenter()
//}


class CrashAction : Action {
    override fun complete(state: MapLocationState): Boolean = false

    override fun nextStep(state: MapLocationState): GamePad {
        throw RuntimeException("Fail")
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun path(): List<FramePoint> {
        return emptyList()
    }

    override val name: String
        get() = "crash"
}
