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
            InsideNavAbout(
                FramePoint(7.grid, 3.grid),
                about = 2
            ),
            InsideNavAbout(
                FramePoint(7.grid, 2.grid),
                about = 2
            ),
            InsideNavAbout(
                FramePoint(7.grid, 5.grid),
                about = 2
            ),
        ), restartWhenDone = false
    ), action
)

fun level3TriggerDoorTrapThenDo(action: Action) = Level3SequenceThenDo(
    OrderedActionSequence(
        mutableListOf(
            // line up correctly so dont come at it from above or below
            InsideNavAbout(
                FramePoint(3.grid, 5.grid),
                about = 2
            ),
            InsideNavAbout(
                FramePoint(2.grid, 5.grid),
                about = 2
            ),
            InsideNavAbout(
                FramePoint(4.grid, 5.grid),
                about = 2
            ),
        ), restartWhenDone = false
    ), action
)
fun level3TriggerBombThenDo(action: Action) = Level3SequenceThenDo(
    OrderedActionSequence(
        mutableListOf(
            Bomb(InLocations.BombDirection.right),
            InsideNavAbout(
                FramePoint(11.grid, 4.grid),
                about = 2,
                vertical = 2
            ),
        ), restartWhenDone = false
    ), action
)


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
            d { "Level3SequenceThenDo done seq ${sequence.done} last ${sequence.lastNull} $frameCt" }
            super.nextStep(state)
        } else {
            d { "Level3SequenceThenDo not done ${sequence.done} last ${sequence.lastNull} $frameCt" }
            sequence.nextStep(state)
        }
    }

    override val name: String
        get() = "Level3SequenceThenDo ${super.name}"
}