package bot.plan.action

import bot.state.GamePad
import bot.state.MapLocationState
import util.d


class Timeout(action: Action, private val frameLimit: Int = 400) : WrappedAction(action) {
    private var frames = 0

    override fun nextStep(state: MapLocationState): GamePad {
        d { " timeout ct $frames"}
        frames++
        return super.nextStep(state)
    }

    override fun reset() {
        frames = 0
        super.reset()
    }

    override fun complete(state: MapLocationState): Boolean =
        frames > frameLimit || super.complete(state).also {
            if (it) {
                frames = 0
            }
        }

    override val name: String
        get() = "Timeout of $frames ${super.name}"
}

class TimeoutThen(action: Action, private val contingency: Action) : WrappedAction(action) {
    private val frameLimit = 200
    private var frames = 0

    override fun nextStep(state: MapLocationState): GamePad {
        d { " timeout ct $frames"}
        frames++
        return if (frames > frameLimit) {
            d { " timeout do contingency"}
            contingency.nextStep(state)
        } else {
            super.nextStep(state)
        }
    }

    override fun reset() {
        frames = 0
        contingency.reset()
        super.reset()
    }

    override fun complete(state: MapLocationState): Boolean =
        if (frames > frameLimit) {
            contingency.complete(state)
        } else {
            super.complete(state)
        }.also {
            if (it) {
                frames = 0
            }
        }

    override val name: String
        get() = "Timeout Then of $frames ${super.name}"
}

