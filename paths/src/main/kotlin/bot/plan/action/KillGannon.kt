package bot.plan.action

import bot.state.Agent
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.MapConstants
import bot.state.oam.*
import util.d

class KillGannon : Action {
    companion object {
        private const val MoveDelay = 2
        private const val moveDistance = MapConstants.fourthGrid
    }
    private var noEnemiesFrameCt = 0
    private val positionShoot = OrderedActionSequence(listOf(
        EnoughForArchery,
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
        // it's better just to kill
//        InsideNavAbout(InLocations.Level9.centerGannonAttack, about = 4)
    ), restartWhenDone = true)

    private fun isReadyForDeath(state: MapLocationState): Boolean {
        // doesn't work, no way to tell if gannon is ready for death
        return state.frameState.enemies.any {
            it.attribute == 3 && (it.tile > 200)
        }
    }

    private val criteria = DeadForAWhile(limit = 2000) {
        it.clearedWithMinIgnoreLoot(0)
    }

    override fun complete(state: MapLocationState): Boolean {
        return gannonDefeated(state)
                // it's possible that link could defeat gannon at exact same spot
                // that the triforce appeared and this isGannonTriforce will not trigger, assume victory then
                || noEnemiesFrameCt > 5000
    }

    private fun gannonDefeated(state: MapLocationState): Boolean =
        state.frameState.enemies.any { it.isGannonTriforce() }

    private fun Agent.isGannonTriforce(): Boolean =
        tile in EnemyGroup.triforceTiles

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        if (gannonDefeated(state)) {
            d { "Gannon defeated"}
        } else {
            d {"KillGannon num enemies ${state.numEnemies}"}
        }
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
            d {"KillGannon ${complete(state)}"}
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

private val gannonTargets = listOf(
    GannonParts.body,
    GannonParts.body2,
    GannonParts.moreBody,
    GannonParts.moreBody2,
    GannonParts.moreBody3,
    GannonParts.moreBody4,
)

// it's variable, maybe it depends on gannon's posture
private object GannonParts {
    val arm = 0xe0
    val leg = 0xd4
    val snout = 0xe2
    val snout2 = 0xd2
    val body = 0xe6
    val body2 = 0xd6
    val moreSnout = 0xda
    val moreBody = 0xee
    val moreBody2 = 0xde
    val moreBody3 = 0xce
    val moreBody4 = 0xca
}

// link had trouble targetting the body, kill all is too cautious and looking for
// safe spots. no need for this complicated thing,
val gannonKill = DecisionAction(KillGannon(), KillAll(
    targetOnly = gannonTargets,
    useBombs = true
), completeIf = {
    state -> state.frameState.enemies.any { it.tile == triforceTileLeft }
}) {
    state -> !state.gannonShowing()
}

private fun MapLocationState.gannonShowing(): Boolean = frameState.enemies.any {
    it.tile > 200
}

class GannonAttack : Action {
    private val freq = 5
    private var swordAction = AlwaysAttack(useB = false, freq = freq)
    private var arrowAction = AlwaysAttack(useB = true, freq = freq)

    private var frames = 0

    override fun complete(state: MapLocationState): Boolean {
        // the idea is, if link is showing, the bot should try to shoot in that direction
        // a few times in the hopes of hitting quickly, then it will start switching
        val limit = if (state.gannonShowing()) freq * 2 else freq
        val complete = frames >= limit
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
        return state.gannonShowing()
    }

    override val name: String = "GannonAttack"
}
