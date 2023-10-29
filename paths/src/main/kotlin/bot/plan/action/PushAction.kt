package bot.plan.action

import bot.plan.InLocations
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier

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

private class NavToPushLocation(private val block: FramePoint): Action {

    override fun target(): FramePoint {
        return block
    }

    override fun complete(state: MapLocationState): Boolean {
        return false
    }

    override fun nextStep(state: MapLocationState): GamePad {
        val pushFromUp = Direction.Up.pointModifier(MapConstants.oneGrid)(block)
        val pushFromDown = Direction.Down.pointModifier(MapConstants.oneGrid)(block)
        val distUp = state.link.distTo(pushFromUp)
        val distDown = state.link.distTo(pushFromDown)

        // reset and recalc.. for now just nav

        // route?
        return GamePad.None
    }

    override val name: String
        get() = "PushAction "
}

fun makePushActionMiddle(): Action = CompleteIfMapChanges(PushActionMiddle())

class PushActionMiddle(toB: FramePoint = InLocations.diamondLeftBottomPush,
                       toT: FramePoint = InLocations.middleStair): Action {

    private val pushFrom = toB
    private val block = toB.upOneGrid
    val sequence = OrderedActionSequence(
        mutableListOf(
//            NavToPushLocation(toB),
            InsideNav(pushFrom),
            GoToward(block),
            GoIn(70, GamePad.MoveRight),
            InsideNav(toT),
            GoIn(4, GamePad.MoveRight),
            GoIn(4, GamePad.MoveLeft),
            ), restartWhenDone = true) // restart if timeout

    //    val block = blockIn.upOneGrid
//    val pushFromUp = Direction.Up.pointModifier(MapConstants.oneGrid)(block)
//    val pushFromDown = Direction.Down.pointModifier(MapConstants.oneGrid)(block)
//    add(lastMapLoc, InsideNavAbout(pushFromUp, 4, ignoreProjectiles = ignoreProjectiles, orPoints = listOf(pushFromDown)))
//    // go there
//    add(lastMapLoc, GoToward(block, 70))
//    add(lastMapLoc, GoDirection(GamePad.MoveRight, 70))
//    add(lastMapLoc, InsideNav(toT, ignoreProjectiles = ignoreProjectiles))
//    add(lastMapLoc, GoDirection(GamePad.MoveRight, 4))
//    add(lastMapLoc, GoDirection(GamePad.MoveLeft, 4))


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