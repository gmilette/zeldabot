package bot.plan.action

import bot.plan.InLocations
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.grid
import bot.state.map.pointModifier
import util.d

// timeout
fun makePushActionThen(push: InLocations.Push, then: Action): Action =
    CompleteIfMapChanges(PushAction(push, then))

fun makeGo(to: FramePoint) = InsideNav(to)

// redo whole map if this goes on too long

//fun makeCenterPush(startAt: MapLoc, upTo: Action, goTo: FramePoint = InLocations.getItem): Action =
//    OrderedActionSequence(listOf(
//        // restart map, leave, then return. its extreme
//        CompleteIfMapChanges(
//            PushAction(
//                InLocations.Push.diamondLeft,
//                InsideNav(InLocations.middleStair)) // avoiding the spot in front of push
//        ),
//        // nav inside
//        CompleteIfMapChanges(OrderedActionSequence(listOf(
//            StartAtAction(startAt),
//            InsideNav(goTo),
//            upTo,
//        ), restartWhenDone = false, shouldComplete = true))
//    ), restartWhenDone = false, shouldComplete = true)

fun makeCenterPush(startAt: MapLoc,
                   upTo: Action,
                   out: InLocations.OutLocation = InLocations.OutLocation.item): Action =
    makePush(push = InLocations.Push.diamondLeft,
        upTo,
        startAt,
        stairs = InLocations.StairsLocation.center,
        out = out)

fun makeStairsItemPush(startAt: MapLoc,
                   upTo: Action,
                   out: InLocations.OutLocation = InLocations.OutLocation.item): Action =
    makePush(push = InLocations.Push.right,
        upTo,
        startAt,
        stairs = InLocations.StairsLocation.corner,
        out = out)

fun makePush(push: InLocations.Push = InLocations.Push.diamondLeft,
             upTo: Action,
             startAt: MapLoc,
             /**
                     * point where the stairs is
                     */
             stairs: InLocations.StairsLocation,
             out: InLocations.OutLocation = InLocations.OutLocation.item): Action =
    OrderedActionSequence(listOf(
        CompleteIfMapChanges(
            PushAction(push, InsideNav(stairs.point,
                push.ignoreProjectiles,
                makePassable = if (push == InLocations.Push.right) null else push.point,
                highCost = push.highCost
                ))),
        CompleteIfMapChanges(OrderedActionSequence(listOf(
            StartAtAction(startAt),
            InsideNav(out.point, ignoreProjectiles = true),
            upTo,
        ), restartWhenDone = false, shouldComplete = true)) // fine if this restarts, it will end once user exits
    ), restartWhenDone = false, shouldComplete = true)

fun goNoPush(upTo: Action,
            startAt: MapLoc,
            stairs: InLocations.StairsLocation,
            out: InLocations.OutLocation = InLocations.OutLocation.item): Action =
    OrderedActionSequence(listOf(
        CompleteIfMapChanges(InsideNav(stairs.point)),
        CompleteIfMapChanges(OrderedActionSequence(listOf(
            StartAtAction(startAt),
            InsideNav(out.point),
            upTo,
        ), restartWhenDone = false, shouldComplete = true)) // fine if this restarts, it will end once user exits
    ), restartWhenDone = false, shouldComplete = true)

/**
 * robust push sequence
 */
class PushAction(push: InLocations.Push, then: Action): Action {

    val sequence = OrderedActionSequence(
    mutableListOf(
//        InsideNav(push.position, makePassable = push.point), // fails in level 9
        if (push == InLocations.Push.diamondLeft) navToPush(push, center = true) else null,
        if (push != InLocations.Push.none) navToPush(push) else null,
        if (push != InLocations.Push.none) PushIt(push.point) else null,
        // optional some push
        if (push.needAway) AwayFrom(push.point) else null,
//        Wait(10),
        // // move away from block otherwise link will be on unpassable
        StartAtAction(0, -1),
        // avoid horizontal spot
        Timeout(then),
        KillAll(),
        InsideNav(push.position) // if we are going to retry reposition link
        ).filterNotNull(), restartWhenDone = true)

    override fun reset() {
        d { " reset seq " }
        sequence.reset()
    }

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

data class PushDirection(
    val horizontal: Boolean = false, val vertical: Boolean = true
)

private fun navToPush(push: InLocations.Push, center: Boolean = false, ignoreProjectiles: Boolean = false): Action {
    val dirs = mutableListOf<FramePoint>()
    if (push.dir.horizontal) {
        dirs.add(Direction.Left.pointModifier(MapConstants.oneGrid)(push.point))
        dirs.add(Direction.Right.pointModifier(MapConstants.oneGrid)(push.point))
    }
    if (push.dir.vertical) {
        if (center) {
            dirs.add(Direction.Up.pointModifier(MapConstants.oneGrid)(push.point).leftOneGrid)
            dirs.add(Direction.Down.pointModifier(MapConstants.oneGrid)(push.point).leftOneGrid)
        } else {
            dirs.add(Direction.Up.pointModifier(MapConstants.oneGrid)(push.point))
            dirs.add(Direction.Down.pointModifier(MapConstants.oneGrid)(push.point))
        }
    }
    return InsideNavAbout(dirs.first(), 4, ignoreProjectiles = ignoreProjectiles,
        makePassable = push.point,
        orPoints = dirs, tag = "navToPush $push ${push.point}", highCost = push.highCost)
}

private class PushIt(private val block: FramePoint,
                     private val howMany: Int = MapConstants.twoGrid,
                    private val howManyAway: Int = 0): Action {
    private var frameCount = 0
    private var frameCountAway = 0
    private var previousDir: GamePad = GamePad.None

    override fun reset() {
        frameCount = 0
    }

    override fun target(): FramePoint {
        return block
    }

    override fun complete(state: MapLocationState): Boolean {
        return frameCount > (howMany + howManyAway)
    }

    override fun nextStep(state: MapLocationState): GamePad {
        frameCount++
        return if (frameCount > (howMany + howManyAway)) {
            d { " DIR is OPPOSITE $previousDir"}
            previousDir.opposite()
        } else {
            if (state.link != block) {
                previousDir = state.link.directionTo(block)
                d { " DIR is $previousDir link: ${state.link} to $block"}
            }
            previousDir
        }
//        return previousDir
//        return state.link.directionTo(block).also {
//            d { " DIR $it link: ${state.link} to $block"}
//        }
    }

    override val name: String
        get() = "PushIt $frameCount"
}

// move in a direction away from the block, ignoring routing
private class AwayFrom(private val block: FramePoint,
                       private val howMany: Int = MapConstants.twoGrid): Action {
    private var frameCount = 0

    private var dir: GamePad = GamePad.randomDirection()

    override fun reset() {
        frameCount = 0
    }

    override fun target(): FramePoint {
        return block
    }

    override fun complete(state: MapLocationState): Boolean {
        return frameCount > howMany || state.link.distTo(block) > MapConstants.twoGrid
    }

    override fun nextStep(state: MapLocationState): GamePad {
        if (frameCount > howMany || frameCount == 0) {
            dir = GamePad.randomDirection()
        }
        frameCount++
        return dir
    }

    override val name: String
        get() = "PushAway From $frameCount $dir ${super.name}"
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
            NavToPushLocation(toB),
            InsideNav(pushFrom),
            GoToward(block),
            GoIn(70, GamePad.MoveRight),
            InsideNav(toT),
            GoIn(4, GamePad.MoveRight),
            GoIn(4, GamePad.MoveLeft),
            ), restartWhenDone = true) // restart if timeout

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

class PushInLevelAnyBlock(inMapLoc: MapLoc = 88,
    // target to push
                        pushTarget: FramePoint? = null,
    // where the stairs are
                        stairsTarget: FramePoint,
    // where to go back to
                        upTo: MapLoc,
    // where to go inside
                        outLocation: FramePoint = InLocations.getItem,
                        directionFrom: Direction = Direction.Down,
                        position: FramePoint = FramePoint(3.grid, 8.grid), // for down
                        thenGo: GamePad = GamePad.None): Action {

                            // add KillAll to this
    val sequence = OrderedActionSequence(
        mutableListOf(
            // line up correctly so dont come at it from above or below
//            InsideNavAbout(FramePoint(toB.x - MapConstants.twoGrid, toB.y - MapConstants.twoGrid), 4),
//            InsideNavAbout(toB, 2),
//            GoIn(100, GamePad.MoveDown, reset = true),
//            Wait(300),
//            GoIn(20, GamePad.MoveUp, reset = true),
//            StartAtAction(0, -1),
//            then
        ), restartWhenDone = true)

    override fun complete(state: MapLocationState): Boolean {
        TODO("Not yet implemented")
    }

//    goTo(position)

    // dynamic direction?

//    // go to stairs, maybe not always ignore projectiles
//    add(lastMapLoc, InsideNav(stairsTarget, ignoreProjectiles = true))
//    if (thenGo != GamePad.None) {
//        goIn(thenGo, 15)
//    }
//
//    startAt(inMapLoc)
//    go(outLocation)
//    upTo(upTo)
}