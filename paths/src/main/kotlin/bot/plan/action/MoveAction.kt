package bot.plan.action

import bot.plan.action.NavUtil.directionToDir
import bot.plan.zstar.FrameRoute
import bot.state.*
import bot.state.map.*
import bot.state.oam.*
import util.d

class CompleteIfAction(
    private val action: Action,
    private val completeIf: (state: MapLocationState) -> Boolean = { false },
) : Action {
    override fun complete(state: MapLocationState): Boolean =
        completeIf(state) || (action.complete(state))

    override fun nextStep(state: MapLocationState): GamePad {
        return action.nextStep(state)
    }

    override val name: String
        get() = action.name
}

class CompleteWhenExitShop(wrapped: Action) : WrappedAction(wrapped) {
    override fun complete(state: MapLocationState): Boolean {
        d { " state game mode ${state.frameState.gameMode}"}
        return wrapped.complete(state) || state.frameState.gameMode != 11
    }
}

class CompleteIfChangeShopOwner(private val changeTo: Boolean, private val wrapped: Action) : Action {
    private var initial: Boolean? = null
    private var completeCt: Int = 0

    private fun changedOwnerAppearance(state: MapLocationState): Boolean =
        initial != null && (inShop(state) == changeTo)
//        state.frameState.enemies.isNotEmpty() && initial != null && (inShop(state) == changeTo)

    private fun inShop(state: MapLocationState): Boolean = state.frameState.enemiesRaw.any {
        (it.tile == shopOwner.first) || (it.tile == shopkeeperAndBat.first) || (it.tile == wizard)
    }

    override fun reset() {
        completeCt = 0
        initial = null
    }

    override fun complete(state: MapLocationState): Boolean =
        completeCt > 100 || wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        d { " CompleteIfChangeShopOwner initial $initial current ${inShop(state)} shouldBe $changeTo" }
        state.frameState.logEnemies()
        if (initial == null) {
            initial = inShop(state)
        }
        if (changedOwnerAppearance(state)) {
            completeCt++
        } else {
            completeCt = 0
        }
        return wrapped.nextStep(state)
    }

    override val name: String
        get() = "Until Change Shop ${wrapped.name}"
}

class CompleteIfMapChanges(private val wrapped: Action) : Action {
    private var initialMapLoc: MapLoc = -1;

    private fun changedMapLoc(state: MapLocationState): Boolean =
        initialMapLoc > 0 && state.frameState.mapLoc != initialMapLoc

    override fun complete(state: MapLocationState): Boolean =
        changedMapLoc(state) || wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        d { " CompleteIfMapChanges initial $initialMapLoc current ${state.frameState.mapLoc}" }
        if (initialMapLoc == -1) {
            initialMapLoc = state.frameState.mapLoc
        }
        return wrapped.nextStep(state)
    }

    override val name: String
        get() = "Until Change Map from $initialMapLoc ${wrapped.name}"
}

class CompleteIfMapChangesTo(private val wrapped: Action, val to: MapLoc) : Action {
    private var initialMapLoc: MapLoc = -1;

    private fun changedMapLoc(state: MapLocationState): Boolean =
        initialMapLoc > 0 && state.frameState.mapLoc == to

    override fun complete(state: MapLocationState): Boolean =
        changedMapLoc(state) || wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        d { " CompleteIfMapChanges initial $initialMapLoc current ${state.frameState.mapLoc}" }
        if (initialMapLoc == -1) {
            initialMapLoc = state.frameState.mapLoc
        }
        return wrapped.nextStep(state)
    }

    override val name: String
        get() = "Until Change Map from $initialMapLoc to $to ${wrapped.name}"
}

class CompleteIfHaveBombs(wrapped: Action) : WrappedAction(wrapped) {
    override fun complete(state: MapLocationState): Boolean =
        state.frameState.inventory.numBombs != 0 || super.complete(state)
}

// move to this location then complete
class InsideNav(
    private val point: FramePoint,
    ignoreProjectiles: Boolean = false,
    private val makePassable: FramePoint? = null,
    private val tag: String = "",
    private val highCost: List<FramePoint> = emptyList()
) : Action {
    private val routeTo = RouteTo.hardlyReplan(ignoreProjectiles = ignoreProjectiles)
    override fun complete(state: MapLocationState): Boolean =
        state.frameState.link.point == point

    override fun nextStep(state: MapLocationState): GamePad {
        return routeTo.routeTo(state, listOf(point),
            RouteTo.RouteParam(
                rParam = RouteTo.RoutingParamCommon(
                    forcePassable = makePassable?.let { listOf(makePassable) } ?: emptyList(),
                    forceHighCost = highCost)
            )
        )
    }

    override fun target(): FramePoint {
        return point
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override val name: String
        get() = "Nav to inside $point $tag"
}

class InsideNavAbout(
    private val point: FramePoint, about: Int, vertical: Int = 1, negVertical: Int = 0,
    val shop: Boolean = false, ignoreProjectiles: Boolean = false,
    private val makePassable: FramePoint? = null,
    orPoints: List<FramePoint> = emptyList(),
    private val setMonitorEnabled: Boolean = true,
    private val tag: String = "",
    private val highCost: List<FramePoint> = emptyList()
) : Action {
    private val routeTo = RouteTo.hardlyReplan(dodgeEnemies = !shop, ignoreProjectiles)
    private val points: List<FramePoint>

    override val monitorEnabled: Boolean
        get() = setMonitorEnabled

    init {
        val pts = mutableListOf<FramePoint>()
        repeat(negVertical) {
            pts.addAll(point.copy(y = point.y - it).toLineOf(about))
        }
        repeat(vertical) {
            pts.addAll(point.copy(y = point.y + it).toLineOf(about))
        }
        for (orPoint in orPoints) {
            repeat(negVertical) {
                pts.addAll(orPoint.copy(y = orPoint.y - it).toLineOf(about))
            }
            repeat(vertical) {
                pts.addAll(orPoint.copy(y = orPoint.y + it).toLineOf(about))
            }
        }
        points = pts
    }

    override fun complete(state: MapLocationState): Boolean =
        (state.link.minDistToAny(points) < 2).also {
            d { "MoveAction! ${state.link} in $points isComplete=$it" }
        }
//        points.contains(state.frameState.link.point).also {
//            d { "! ${state.link} not in ${points} "}
//        }

    override fun nextStep(state: MapLocationState): GamePad =
        routeTo.routeTo(
            state,
            to = points,
            RouteTo.RouteParam(
                overrideMapCell = if (shop) state.hyrule.shopMapCell else null,
                allowBlock = !shop,
                rParam = RouteTo.RoutingParamCommon(
                    makePassable?.let { listOf(makePassable) } ?: emptyList(),
                    forceHighCost = highCost
                ),
                allowAttack = !shop
            )
        )

    override fun target(): FramePoint {
        return point
    }

    override fun targets(): List<FramePoint> {
        return listOf(point)
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override val name: String
        get() = "Nav to about $point $tag avoid ${routeTo.params.whatToAvoid}"
}

class StartAtAction(val at: MapLoc = 0, val atLevel: Int = -1) : Action {
    override fun complete(state: MapLocationState): Boolean =
        true.also {
            d { "set start to $at" }
            state.movedTo = at
            state.levelTo = atLevel
        }
}

class MoveTo(
    private val fromLoc: MapLoc = 0,
    val next: MapCell,
    val toLevel: Int,
    private val forceDirection: Direction? = null,
    ignoreProjectiles: Boolean = false,
    private val allowBlocking: Boolean = true
) : MapAwareAction {
    private val routeTo = RouteTo(params = RouteTo.Param(
        whatToAvoid = RouteTo.Param.makeIgnoreProjectiles(ignoreProjectiles)))

    init {
        next.zstar.clearAvoid()
    }

    override val from: MapLoc = fromLoc
    override val to: MapLoc = next.mapLoc

    override val actionLoc: MapLoc
        get() = to

    override val levelLoc: Int
        get() = toLevel

    private var arrived = false
    private var movedIn = 0
    private var previousDir: Direction = Direction.None
    private var arrivedDir: Direction = Direction.None

    override fun reset() {
        next.zstar.clearAvoid()
        route = null
        arrived = false
        movedIn = 0
    }

    override fun complete(state: MapLocationState): Boolean =
        (arrived && movedIn >= 5).also {
            if (it) {
                onArrived(state)
                route = null
            }
        }

    private fun onArrived(state: MapLocationState) {
        state.movedTo = to
        state.levelTo = toLevel
    }

    private fun checkArrived(state: MapLocationState, dir: Direction) {
        if (!arrived) {
            arrived = state.frameState.mapLoc == next.mapLoc
            if (arrived) {
                onArrived(state)
                movedIn = 0
                arrivedDir = dir
                d { " arrived! $arrived dir $arrivedDir" }
            } else {
                d { " not arrived! $arrived dir $arrivedDir" }
            }
        }
    }

    override fun target(): FramePoint {
        return targets.firstOrNull() ?: FramePoint()
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    private var targets = listOf<FramePoint>()

    override fun targets(): List<FramePoint> {
        return targets
    }

    private var route: FrameRoute? = null

    private var dir: Direction = Direction.Down

    private var start: MapCell? = null

    override fun nextStep(state: MapLocationState): GamePad {
        d { " DO MOVE TO cell ${next.mapLoc} arrived=${arrived} movedIn=$movedIn" }

        val current = state.currentMapCell
        if (start == null) {
            start = current
        }
        val next = next
        previousDir = dir

        dir = forceDirection ?: current.mapLoc.directionToDir(next.mapLoc)

        if (state.currentMapCell.exitsFor(dir) == null) {
            d { " default move " }
        }
        val secretRoomExit = secretRoomExit(state)
        val exits = secretRoomExit ?: state.currentMapCell.exitsFor(dir) ?: return NavUtil.randomDir()
        val overrideMapCell = if (secretRoomExit != null) {
            if (levelLoc == MapConstants.overworld) {
                state.hyrule.shopMapCell
            } else {
                state.hyrule.level1EntranceCell
            }
        } else {
            null
        }

        targets = exits

        // next.. need an escape plan
        // detect if link is inside a secret room, but somewhere else, in which case, push an escape action
        // also can I start from after first fairy rather than rerunning it
        // two 5e or two 5c tiles indicate it

        checkArrived(state, previousDir)
        d { " arrived! is $arrived"}

        return if (!arrived) {
            routeTo.routeTo(state, exits, RouteTo.RouteParam(allowBlock = allowBlocking,
                overrideMapCell = overrideMapCell)
            )
        } else {
            movedIn++
            arrivedDir.toGamePad()
        }
    }

    override val name: String
        get() = "Move from ${this.fromLoc} to ${this.next.mapLoc}"

    private fun secretRoomExit(state: MapLocationState): List<FramePoint>? =
        when {
            (state.frameState.isLevel && toLevel == MapConstants.overworld) -> {
                d { " in level but shouldnt be "}
                state.hyrule.level1EntranceCell.exitsFor(Direction.Down)
            }
            (state.frameState.isLevel) -> null
            (state.frameState.enemiesRaw.count { it.tile in EnemyGroup.flame} >= 2) -> {
                d { " in secret room cave=${state.frameState.isInCave}"}
                state.hyrule.shopMapCell.exitsFor(Direction.Down)
            }
            (state.frameState.isInCave) -> {
                d { " in cave, get out "}
                state.hyrule.shopMapCell.exitsFor(Direction.Down)
            }
            else -> null
        }
}
