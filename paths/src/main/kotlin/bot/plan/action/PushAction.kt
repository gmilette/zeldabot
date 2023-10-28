package bot.plan.action

import bot.plan.InLocations
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.MapConstants

fun makePushAction(toB: FramePoint): Action = CompleteIfMapChanges(PushAction(toB,
    InsideNavAbout(InLocations.rightTop, 2)))

fun makePushActionThen(toB: FramePoint, then: Action): Action = CompleteIfMapChanges(PushAction(toB, then))

/**
 * robust push sequence
 */
class PushAction(toB: FramePoint, then: Action): Action {
    val sequence = OrderedActionSequence(
    mutableListOf(
        // line up correctly so dont come at it from above or below
        InsideNavAbout(FramePoint(toB.x - MapConstants.twoGrid, toB.y - MapConstants.twoGrid), 4),
        InsideNavAbout(toB, 2),
        GoIn(100, GamePad.MoveDown, reset = true),
        Wait(300),
        GoIn(20, GamePad.MoveUp, reset = true),
        StartAtAction(0, -1),
        then
    ), restartWhenDone = true)

    override fun target(): FramePoint {
        return sequence.target()
    }

    override fun complete(state: MapLocationState): Boolean {
        return false
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return sequence.nextStep(state)
    }

    override val name: String
        get() = "PushAction ${sequence.name} "
}