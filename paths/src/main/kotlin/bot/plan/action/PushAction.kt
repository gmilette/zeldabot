package bot.plan.action

import bot.plan.InLocations
import bot.state.*
import bot.state.map.*
import util.d

// timeout
fun makePushActionThen(push: InLocations.Push, to: MapLoc, then: Action): Action =
    CompleteIfMapChangesTo(PushAction(push, then), to = to)

fun makeStatuePushGo(statue: FramePoint): Action =
    OrderedActionSequence(listOf(
            InsideNav(statue.upOneGrid, highCost = listOf(statue.downOneGrid, statue), tag = "push position"),
            GoIn(20, GamePad.MoveDown, reset = true),
            GoIn(75, GamePad.None, reset = true),
            Timeout(InsideNav(statue, makePassable = statue, ignoreProjectiles = false, tag = "go in"))
        ), restartWhenDone = false, shouldComplete = true, tag = "pushGo") // fine if this restarts, it will end once user exits

fun makeStatuePush(statue: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem): Action =
    OrderedActionSequence(listOf(
        CompleteIfChangeShopOwner(true, OrderedActionSequence(listOf(
            InsideNav(statue.upOneGrid, highCost = listOf(statue.downOneGrid, statue), tag = "push position"),
            GoIn(20, GamePad.MoveDown, reset = true),
            GoIn(75, GamePad.None, reset = true),
            Timeout(InsideNav(statue, makePassable = statue, ignoreProjectiles = false, tag = "go in"))
        ), restartWhenDone = false, shouldComplete = true, tag = "push") // fine if this restarts, it will end once user exits
//        ), CompleteWhenExitShop(OrderedActionSequence(
        ), CompleteIfChangeShopOwner(false, OrderedActionSequence(
            listOfNotNull(
                GoInConsume(15, GamePad.MoveUp),
                // need to do more here
                if (itemLoc != InLocations.Overworld.centerItem) {
                    InsideNavAbout(itemLoc.downTwoGrid, 4, 2, 1, shop = true)
                } else null,
                InsideNavAbout(itemLoc, 4, 2, 1, shop = true),
                if (itemLoc != Objective.ItemLoc.Enter.point) {
                    ExitShop()
                } else null,
                if (itemLoc != Objective.ItemLoc.Enter.point) {
                    GoIn(5, GamePad.MoveDown)
                } else null,
            ), restartWhenDone = false, shouldComplete = true, tag = "get item"
        ))
    ), restartWhenDone = false, shouldComplete = true)

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
                makePassable = if (push == InLocations.Push.right) push.point else push.point, // was null if right..
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
            InsideNav(out.point, tag = "go to no push point"),
            upTo,
        ), restartWhenDone = false, shouldComplete = true)) // fine if this restarts, it will end once user exits
    ), restartWhenDone = false, shouldComplete = true)

/**
 * robust push sequence
 */
class PushAction(push: InLocations.Push, then: Action): Action {

    val sequence = OrderedActionSequence(
    listOfNotNull(
//        InsideNav(push.position, makePassable = push.point), // fails in level 9
        if (push == InLocations.Push.diamondLeft) navToPush(push, center = true) else null,
        // added the != diamond left
        if (push != InLocations.Push.none && (push != InLocations.Push.diamondLeft)) navToPush(push) else null,
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
        ), restartWhenDone = true, tag = "push sequence")

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
            dirs.add(Direction.Up.pointModifier(MapConstants.oneGrid)(push.point))
            dirs.add(Direction.Down.pointModifier(MapConstants.oneGrid)(push.point))
        } else {
            dirs.add(Direction.Up.pointModifier(MapConstants.oneGrid)(push.point))
            dirs.add(Direction.Down.pointModifier(MapConstants.oneGrid)(push.point))
        }
    }
    return InsideNavAbout(dirs.first(), 4, ignoreProjectiles = ignoreProjectiles,
        makePassable = push.point,
        orPoints = dirs, tag = "navToPush $push ${push.point} center=$center $dirs", highCost = push.highCost)
}

private class PushIt(private val block: FramePoint,
                     private val howMany: Int = MapConstants.twoGrid): Action {
    private var frameCount = 0

    override fun reset() {
        frameCount = 0
    }

    override fun target(): FramePoint {
        return block
    }

    override fun complete(state: MapLocationState): Boolean {
        return frameCount > howMany
    }

    override fun nextStep(state: MapLocationState): GamePad {
        frameCount++
        return state.link.directionTo(block)
    }

    override val name: String
        get() = "PushIt $frameCount"
}

// move in a direction away from the block, hm... routing would help avoid getting stuck
private class AwayFrom(private val block: FramePoint,
                       private val howMany: Int = (MapConstants.twoGrid * 3)): Action {
    private var frameCount = 0

    private var dir: GamePad = GamePad.randomDirection()
    private var dir2: GamePad = GamePad.randomDirection(dir)

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
        if (frameCount == 0) {
            dir = GamePad.randomDirection()
            dir2 = GamePad.randomDirection(dir)
        }

        frameCount++
        return if (frameCount > howMany / 2) {
            dir2
        } else {
            dir
        }
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