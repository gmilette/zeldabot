package bot.plan.action

import bot.plan.InLocations
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.grid
import util.d

fun level6TriggerDoorTrapThenDo(action: Action) = Level3SequenceThenDo(
    OrderedActionSequence(
        mutableListOf(
            InsideNav(
                FramePoint(7.grid, 3.grid)
            ),
            InsideNav(
                FramePoint(7.grid, 2.grid)
            ),
            InsideNav(
                FramePoint(7.grid, 5.grid)
            ),
        ), restartWhenDone = false
    ), action
)

fun level3TriggerDoorTrapThenDo(action: Action) = Level3SequenceThenDo(
    OrderedActionSequence(
        mutableListOf(
            // line up correctly so dont come at it from above or below
            InsideNav(
                FramePoint(3.grid, 5.grid)
            ),
            InsideNav(
                FramePoint(2.grid, 5.grid)
            ),
            InsideNav(
                FramePoint(4.grid, 5.grid)
            ),
        ), restartWhenDone = false
    ), action
)
fun level3TriggerBombThenDo(action: Action) = Level3SequenceThenDo(
    OrderedActionSequence(
        mutableListOf(
            Bomb(InLocations.BombDirection.right),
            InsideNav(
                FramePoint(11.grid, 5.grid)
            ),
        ), restartWhenDone = false
    ), action
)


class Level3TriggerDoorTrapThenDoZ(action: Action) : WrappedAction(action) {
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

class Level3SequenceThenDo(private val sequence: OrderedActionSequence, action: Action) : WrappedAction(action) {
    private var frameCt = 0
    override fun target(): FramePoint {
        return sequence.target()
    }

    private fun orderedComplete(): Boolean =
        sequence.done && sequence.lastNull

    override fun nextStep(state: MapLocationState): GamePad {
        frameCt++
        return if (frameCt > 10 && orderedComplete()) {
            d { "Level3SequenceThenDo done seq ${sequence.done} last ${sequence.lastNull}" }
            super.nextStep(state)
        } else {
            d { "Level3SequenceThenDo not done" }
            sequence.nextStep(state)
        }
    }

    override val name: String
        get() = "Level3SequenceThenDo ${super.name}"
}