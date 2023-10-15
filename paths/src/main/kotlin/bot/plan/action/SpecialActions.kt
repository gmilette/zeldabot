package bot.plan.action

import bot.state.FramePoint
import bot.state.GamePad
import bot.state.Inventory
import bot.state.MapLocationState
import bot.state.map.Direction
import bot.state.map.grid
import util.d

class Level3TriggerDoorTrapThenDo(action: Action, exitDirection: Direction = Direction.Left) : WrappedAction(action) {
    private val positionShootActions = mutableListOf(
        InsideNav(
            FramePoint(2.grid, 5.grid)
        ),
//        GoIn(1, GamePad.MoveLeft), // open door
        // dodge trap
        InsideNav(
            FramePoint(5.grid, 5.grid)
        ),
    )

    private val positionShoot = OrderedActionSequence(positionShootActions, restartWhenDone = false)

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    private fun orderedComplete(): Boolean =
        positionShoot.done && positionShoot.lastNull

    override fun nextStep(state: MapLocationState): GamePad {
        d { "Level3TriggerDoorTrapThenDo done=${positionShoot.done}" }
        return if (orderedComplete()) {
            positionShoot.nextStep(state)
        } else {
            super.nextStep(state)
        }
    }

    override val name: String
        get() = "Level3TriggerDoorTrapThenDo ${super.name}"
}