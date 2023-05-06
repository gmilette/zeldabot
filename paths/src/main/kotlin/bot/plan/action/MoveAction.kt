package bot.plan.action

import bot.plan.action.NavUtil.directionToDir
import bot.plan.gastar.FrameRoute
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLoc
import bot.state.MapLocationState
import bot.state.map.Direction
import bot.state.map.MapCell
import util.d

class MoveTo(val fromLoc: MapLoc = 0, val next: MapCell, val forceDirection: Direction? = null) : MapAwareAction {
    private val routeTo = RouteTo(params = RouteTo.Param(considerLiveEnemies = false))

    init {
        next.gstar.clearAvoid()
    }

    override val from: MapLoc = fromLoc
    override val to: MapLoc = next.mapLoc

    override fun reset() {
        next.gstar.clearAvoid()
        route = null
    }

    override fun complete(state: MapLocationState): Boolean =
        (state.frameState.mapLoc == next.mapLoc).also {
            if (it) route = null
//            d { " Move to complete $it"}
        }

    override fun target(): FramePoint {
        return super.target()
    }

    override fun path(): List<FramePoint> {
        return route?.path ?: emptyList()
    }

    private var route: FrameRoute? = null
    private var planCount = 0

    private var dir: Direction = Direction.Down

    private var start: MapCell? = null

    override fun nextStep(state: MapLocationState): GamePad {
        d { " DO MOVE TO cell ${next.mapLoc}" }

//        for (enemy in state.frameState.enemies) {
//            d { " enemy: $enemy" }
//        }
//        val current = state.hyrule.levelMap.cell(1, state.currentMapCell.mapLoc)
        val current = state.currentMapCell
        if (start == null) {
            start = current
        }
        val next = next
        val linkPt = state
            .frameState
            .link.point

        // TODO: need to figure out how to get back on track if get off the plan for some reason
//        val isInCurrent = false
//        dir = when {
//            !isInCurrent -> current.mapLoc.directionToDir(start!!.mapLoc)
//            else -> forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)
//        }

        // always assume on current need this? for the state.frameState.isLevel &&
//        val isInCurrent = current.mapLoc == start?.mapLoc //|| (current.mapLoc != (start?.mapLoc ?: 0))
        val isInCurrent = state.frameState.isLevel || current.mapLoc == from //|| (current.mapLoc != (start?.mapLoc ?: 0))
        // need a little routing alg
        dir = when {
            !isInCurrent -> current.mapLoc.directionToDir(fromLoc)
            else -> forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)
        }

        if (isInCurrent) {
            d { " ON THE RIGHT ROUTE on ${current.mapLoc} but should be on ${fromLoc} or start ${start?.mapLoc} go $dir"}
        } else {
            d { " ON THE WRONG ROUTE on ${current.mapLoc} but should be on ${fromLoc} or start ${start?.mapLoc} go $dir"}
        }
//        dir = forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)


        if (state.currentMapCell.exitsFor(dir) == null) {
            d { " default move " }
        }
        val exits = state.currentMapCell.exitsFor(dir) ?: return NavUtil.randomDir()

        return routeTo.routeTo(state, exits)
    }

    override val name: String
        get() = "Move from ${this.fromLoc} to ${this.next.mapLoc} ${dir ?: ""}"
}
