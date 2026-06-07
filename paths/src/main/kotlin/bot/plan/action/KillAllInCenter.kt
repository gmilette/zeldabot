package bot.plan.action

import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.grid
import util.d

class KillAllInCenter : Action {
    object KillInCenterLocations {
        // should be the middle of the attack area
        val position = FramePoint(3.grid, 8.grid)

        //        val attackFrom = FramePoint(8.grid, 8.grid)
        val attackFrom = FramePoint(8.grid, 8.grid)
    }

    // more debugging
    private val positionShootActions = mutableListOf(
        InsideNavAbout(
            KillInCenterLocations.attackFrom,
            1,
            vertical = 2
        ),
        GoIn(10, GamePad.MoveUp, true)
//        AlwaysAttack()
    )

    init {
        repeat(times = 5) {
            positionShootActions.add(GoIn(10, GamePad.MoveUp, true))
            positionShootActions.add(GoIn(3, GamePad.A, true))
            positionShootActions.add(GoIn(3, GamePad.None, true))
            positionShootActions.add(GoIn(3, GamePad.A, true))
            positionShootActions.add(GoIn(3, GamePad.None, true))
            positionShootActions.add(GoIn(10, GamePad.MoveUp, true))
            positionShootActions.add(GoIn(3, GamePad.A, true))
            positionShootActions.add(GoIn(3, GamePad.None, true))
            positionShootActions.add(GoIn(3, GamePad.A, true))
            positionShootActions.add(GoIn(10, GamePad.MoveUp, true))
            positionShootActions.add(GoIn(3, GamePad.None, true))
            positionShootActions.add(GoIn(3, GamePad.B, true))
            positionShootActions.add(GoIn(3, GamePad.None, true))
            positionShootActions.add(GoIn(3, GamePad.A, true))
            positionShootActions.add(GoIn(3, GamePad.None, true))
        }
    }


    private val positionShoot = OrderedActionSequence(positionShootActions, restartWhenDone = true)

    override fun complete(state: MapLocationState): Boolean =
        state.numEnemiesAliveInCenter() == 0

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d { "KillInCenter" }
        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack

        return positionShoot.nextStep(state)
    }

    override val name: String
        get() = "KillInCenter ${positionShoot.stepName} ${positionShoot.name}"
}