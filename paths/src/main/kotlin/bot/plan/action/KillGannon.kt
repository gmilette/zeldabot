package bot.plan.action

import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.MapConstants
import bot.state.oam.arrowHitExplosion
import bot.state.oam.arrowTipShotByEnemy
import bot.state.oam.arrowTipShotByEnemy2
import bot.state.oam.isGannonTriforce
import util.d

class KillGannon : Action {
    companion object {
        private const val MoveDelay = 2
        private const val moveDistance = MapConstants.fourthGrid
    }
    private var noEnemiesFrameCt = 0
    private val positionShoot = OrderedActionSequence(listOf(
        GoIn(moveDistance, GamePad.MoveRight, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        WaitUntilArrowGone(),
        GannonAttack(),

        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(moveDistance, GamePad.MoveUp, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        WaitUntilArrowGone(), // it really has to wait
        GannonAttack(),

        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(moveDistance, GamePad.MoveLeft, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        WaitUntilArrowGone(),
        GannonAttack(),

        GoIn(MoveDelay, GamePad.None, reset = true),
        GoIn(moveDistance, GamePad.MoveDown, reset = true),
        GoIn(MoveDelay, GamePad.None, reset = true),
        WaitUntilArrowGone(),
        GannonAttack(),
        GoIn(MoveDelay, GamePad.None, reset = true),
//        GoIn(MoveDelay, GamePad.None, reset = true),
//        GoIn(moveDistance, GamePad.None, reset = true),
//        WaitUntilArrowGone(),
//        GoIn(MoveDelay, GamePad.None, reset = true),
//        GannonAttack(),
//        GoIn(MoveDelay, GamePad.None, reset = true),
        // it's better just to kill
//        InsideNavAbout(InLocations.Level9.centerGannonAttack, about = 4)
    ), restartWhenDone = true)

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
                // it's possible that link could defeat gannon at exact same spot
                // that the triforce appeared and this isGannonTriforce will not trigger, assume victory then
                || noEnemiesFrameCt > 5000
    }

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {"KillGannon num enemies ${state.numEnemies}"}
        noEnemiesFrameCt++

        // why is this needed
//        state.currentMapCell.zstar.resetPassable()
//        state.currentMapCell.zstar.reset()
        state.frameState.logEnemies()
        if (state.frameState.enemies.isEmpty()) {
            noEnemiesFrameCt++
        }
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

class WaitUntilArrowGone : Action {
    // arrow gone doesn't exactly work
    private val arrowSet = setOf(arrowHitExplosion, arrowTipShotByEnemy2, arrowTipShotByEnemy )

    override fun complete(state: MapLocationState): Boolean {
        return state.frameState.enemies.none { it.tile in arrowSet }.also {
            d { "WaitUntilArrowGone $it"}
        }
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d { " any are arrow: ${state.frameState.enemies.any { it.tile in arrowSet }}"}
        return GamePad.None
    }
}

class GannonAttack : Action {
    private val freq = 5
    private var swordAction = AlwaysAttack(useB = false, freq = freq)
    private var arrowAction = AlwaysAttack(useB = true, freq = freq)

    private var frames = 0

    override fun complete(state: MapLocationState): Boolean {
        val complete = frames >= freq
        if (complete) {
            frames = 0
            swordAction.reset()
            arrowAction.reset()
        }
        return complete
    }

    override fun nextStep(state: MapLocationState): GamePad {
        val action = if (isReadyForDeath(state)) {
            d {"GannonAttack !! READY FOR DEATH !!"}
            arrowAction
        } else {
            d {"GannonAttack sword"}
            swordAction
        }
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
