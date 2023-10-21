package bot.plan.action

import bot.plan.InLocations
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.oam.isGannonTriforce
import util.d

class KillGannon : Action {
    companion object {
        private const val MoveDelay = 2
    }
    private val positionShoot = OrderedActionSequence(listOf(
        GoIn(5, GamePad.MoveRight, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GannonAttack(),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(5, GamePad.MoveUp, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GannonAttack(),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(5, GamePad.None, reset = true),
        GoIn(5, GamePad.MoveLeft, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GannonAttack(),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(5, GamePad.MoveDown, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GannonAttack(),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(5, GamePad.None, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GannonAttack(),
        GoIn(MoveDelay, GamePad.None, reset = true),
        // it's better just to kill
//        InsideNavAbout(InLocations.Level9.centerGannonAttack, about = 4)
    ))

    private fun isReadyForDeath(state: MapLocationState): Boolean {
//        212, attribute=3 //dark
        // attribute 43 is light
        return state.frameState.enemies.any {
            it.attribute == 3 && (it.tile > 200)
        }
    }

    private val criteria = DeadForAWhile(limit = 2000) {
        it.clearedWithMinIgnoreLoot(0)
    }

    override fun complete(state: MapLocationState): Boolean {
        return state.frameState.enemies.any { it.isGannonTriforce() }
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {"KillGannon num enemies ${state.numEnemies}"}
        state.currentMapCell.zstar.resetPassable()
        state.currentMapCell.zstar.reset()
        state.frameState.logEnemies()
        if (isReadyForDeath(state)) {
            d {"KillGannon !! READY FOR DEATH !!"}
        } else {
            d {"KillGannon"}
        }
        criteria.nextStep(state)

        return positionShoot.nextStep(state)
    }

    override val name: String
        get() = "KillGannon ${criteria.frameCount}"
}

class GannonAttack : Action {
    private val freq = 10
    private var swordAction = AlwaysAttack(useB = false, freq = freq)
    private var arrowAction = AlwaysAttack(useB = true, freq = freq)

    private var frames = 0

    override fun complete(state: MapLocationState): Boolean {
        val complete = frames >= freq
        if (complete) {
            frames = 0
        }
        return complete
    }

    override fun nextStep(state: MapLocationState): GamePad {
        val action = if (isReadyForDeath(state)) arrowAction else swordAction
        frames++
        return action.nextStep(state)
    }

    private fun isReadyForDeath(state: MapLocationState): Boolean {
//        212, attribute=3 //dark
        // attribute 43 is light
        return state.frameState.enemies.any {
            it.attribute == 3 && (it.tile > 200)
        }
    }
}
