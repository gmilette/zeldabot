package bot.plan.action

import bot.plan.zstar.ZStar
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.distTo
import bot.state.map.MapCell
import util.d
import util.w

// try to detect when link is stuck and then get unstuck

fun moveHistoryAttackAction(wrapped: Action): Action {
// really, if there are enemies attack, otherwise, move, or bomb
// if there are pancake enemies
    // hopefully adding some random moves here will also help link get unstuck
    //MoveHistoryAction(wrapped, AlwaysAttack(otherwiseRandom = true))
    val moveHistoryAction = MoveHistoryAction(wrapped, AlwaysAttack(otherwiseRandom = true))
    val ladderAction = LadderAction()
    val combinedAction = DecisionAction(ladderAction, StayInCurrentMapCell(moveHistoryAction)) {
        !ladderAction.complete(it)
    }

    return combinedAction
}


private class SameCount {
    var last: GamePad = GamePad.None
    private var count: Int = 0

    fun record(pad: GamePad): Int {
        if (last == pad) {
            count++
        } else {
            // need it?
            reset()
        }
        last = pad
        return count
    }

    fun reset() {
        count = 0
    }
}

class MoveBuffer(val size: Int = 2) {
    val buffer = mutableListOf<FramePoint>()

    val isFull: Boolean
        get() = buffer.size == size

    fun add(pt: FramePoint) {
        buffer.add(pt)
        if (buffer.size > size) {
            buffer.removeFirst()
        }
    }

    fun allSame(): Boolean =
        buffer.distinct().size == 1

    fun allDifferent(): Boolean =
        buffer.distinct().size == size

    fun compare(other: MoveBuffer): Boolean =
        other.buffer == buffer // also check that they are all not same
//        if (other.buffer.size != buffer.size) {
//            false
//        } else {
//            var allEqual = true
//            buffer.forEachIndexed { index, _ ->
//                allEqual = allEqual && other.buffer[index] == buffer[index]
//            }
//            allEqual
//        }

    override fun toString(): String {
        var moves = ""
        for (framePoint in buffer) {
            moves = "$moves, ${framePoint.oneStr}"
        }
        return moves
    }
}

class PrevBuffer<T>(val size: Int = 2) {
    val buffer = mutableListOf<T>()

    val isFull: Boolean
        get() = buffer.size == size

    fun add(pt: T) {
        buffer.add(pt)
        if (buffer.size > size) {
            buffer.removeFirst()
        }
    }

    fun allSame(): Boolean =
        buffer.distinct().size == size

    fun compare(other: MoveBuffer): Boolean =
        other.buffer == buffer // also check that they are all not same
//        if (other.buffer.size != buffer.size) {
//            false
//        } else {
//            var allEqual = true
//            buffer.forEachIndexed { index, _ ->
//                allEqual = allEqual && other.buffer[index] == buffer[index]
//            }
//            allEqual
//        }
}

/**
 * when link has done the last same set of 3-4 moves X times
 * keep history of moves, iterate looking for a loop
 */
private class CycleDetector(val cyclesLimit: Int = 20) {
    data class BufferCount(val size: Int) {
        var ct: Int = 0
        var prevBuffer: MoveBuffer = MoveBuffer(size)
        var buffer: MoveBuffer = MoveBuffer(size)
        fun reset() {
            move()
            ct = 0
        }

        fun move() {
            prevBuffer = buffer
            buffer = MoveBuffer(size)
        }
    }

    val buffers = listOf(
        BufferCount(2),
        BufferCount(3),
        BufferCount(4),
        BufferCount(5)
    )

    fun save(pt: FramePoint) {
        for (buffer in buffers) {
            buffer.buffer.add(pt)
            // if it is full then
            if (buffer.buffer.isFull) {
                if (buffer.buffer.compare(buffer.prevBuffer)) {
                    buffer.ct++
                    buffer.move()
                    if (buffer.ct == cyclesLimit) {
                        // it's wiggling!
                        w { " It's Wiggling! ${buffer.size}" }
                    }
                } else {
                    buffer.reset()
                }
            }
        }
    }
}

/**
 * when link has done the last same set of 3-4 moves X times
 * keep history of moves, iterate looking for a loop
 */
private class CycleDetectorInList() {
    val buffer = MoveBuffer(100)

    fun save(pt: FramePoint) {
        buffer.add(pt)
        buffer.buffer.forEachIndexed { index, framePoint ->
            buffer.buffer[index % 2] == framePoint
            buffer.buffer[index % 3] == framePoint
            buffer.buffer[index % 4] == framePoint
            buffer.buffer[index % 5] == framePoint
        }
    }
}

// default actions
// if same x,y as enemy, attack
// unstick

class MoveHistoryAction(private val wrapped: Action, private val escapeAction: Action) : Action {
    private val histSize = 250
    private val same = SameCount()
    private var escapeActionCt = 0
    private val escapeActionTimes = 50
    private val cycleDetector = CycleDetector()

    override fun target(): FramePoint {
        return wrapped.target()
    }

    override fun targets(): List<FramePoint> {
        return wrapped.targets()
    }

    override fun path(): List<FramePoint> =
        wrapped.path()

    override fun zstar(): ZStar? = wrapped.zstar()

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
//        d { "MoveHistoryAction" }
        return when {
            escapeActionCt > 0 -> {
                d { " ESCAPE ACTION " }
                val action = escapeAction.nextStep(state)
                escapeActionCt--
                action
            }

            state.link in state.aliveEnemies.map { it.point } -> {
                escapeActionCt = escapeActionTimes
                d { " ESCAPE ACTION reset" }
                same.reset()
                wrapped.nextStep(state)
            }

            else -> {
                val nextStep = wrapped.nextStep(state)
                val ct = same.record(nextStep)
                // keep saving link's location
                cycleDetector.save(state.link)
//                d { " ESCAPE ACTION not same $nextStep + $ct last ${same.last}" }
                if (ct >= histSize) {
                    escapeActionCt = escapeActionTimes
                    d { " ESCAPE ACTION RESET" }
                    same.reset()
                }
                nextStep
            }
        }//.also {
//            val inIt = state.link in state.aliveEnemies.map { it.point }
//            d { " link at ${state.link} $inIt" }
//        }
    }

    override val name: String
        get() = wrapped.name
}

class ReBombIfNecessary(private val wrapped: Action): Action {
    private var frameCt = 0
    private var attack = AlwaysAttack(useB = true)

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        frameCt++

        // it worked! it took a while though at 1000
        return if (frameCt > 600) {
            d { "REBOMB!!!" }
            if (frameCt > 700) {
                frameCt = 0
                d { "ah well restart" }
            }
            attack.nextStep(state)
        } else {
            wrapped.nextStep(state)
        }
    }
}

class StayInCurrentMapCell(private val wrapped: Action) : Action {
    private val routeTo = RouteTo(params = RouteTo.Param(considerLiveEnemies = false))

    private var initialMapCell: MapCell? = null
    private var initialLevel: Int = -1

    private var frameCt = 0
    private var frameCtAfterScene = 0

    companion object {
        private const val FRAMES_BEFORE_SET_INITIAL = 300
    }

    override fun target(): FramePoint =
        wrapped.target()

    override fun targets(): List<FramePoint> {
        return wrapped.targets()
    }

    override fun path(): List<FramePoint> =
        wrapped.path()

    override fun zstar(): ZStar? = wrapped.zstar()

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)
//        initialMapCell == null ||
//                (state.frameState.isDoneScrolling && initialMapCell?.mapLoc == state.currentMapCell.mapLoc)

    override fun nextStep(state: MapLocationState): GamePad {
        frameCt++
        d { "StayInCurrentMapCell ${state.frameState.isDoneScrolling} ${initialMapCell != null}" }
        if (initialMapCell == null && !state.frameState.isScrolling && frameCt > FRAMES_BEFORE_SET_INITIAL) {
            initialMapCell = state.currentMapCell
            d { "StayInCurrentMapCell set to ${state.currentMapCell.mapLoc} level = $initialLevel" }
            initialLevel = state.frameState.level
        }

        // don't do any of this if the screen is scrolling
        // if it is scrolling just reset the framect after the scene
        if (state.frameState.isScrolling || state.currentMapCell.mapLoc == 0 ||
            // never switch between level and no
            state.frameState.level != initialLevel) {
            frameCtAfterScene = 0
            return wrapped.nextStep(state)
        }

        frameCtAfterScene++

        val isInCurrent = onCorrectMap(state)

        if (!isInCurrent) {
            val fromLoc = initialMapCell?.mapLoc ?: 0
            d { " should be at $fromLoc"}
        }

        if (true) return wrapped.nextStep(state)

        return if (isInCurrent) {
            wrapped.nextStep(state)
        } else {
            val fromLoc = initialMapCell?.mapLoc ?: 0
            d { " should be at $fromLoc"}
//            var dir = state.currentMapCell.mapLoc.directionToDir(fromLoc)
//
//            // this should handle the case where link accidentally moves into
//            // stair
//            if (dir == Direction.None) {
//                // the only way to go is up and out I think, there isn't a way to warp
//                // somewhere else
//                dir = Direction.Up
//            }
//
//            // either the selected exit or just go to any exit, which is probably the closest
//            // actually maybe just always go to the closest exit!
//            val exits = state.currentMapCell.exitsFor(dir) ?: state.currentMapCell.allExits()
            val exits = state.currentMapCell.allExits()
            // what is the nearest point
            // waste of computation just for debug but why not
            d { " closest to ${exits.minOf { it.distTo(state.link) }}"}
            // maybe add parameter for NO attacking while moving, so link doesn't get lou
            routeTo.routeTo(state, exits)
        }
    }

    private fun onCorrectMap(state: MapLocationState): Boolean {
        val current = state.currentMapCell
        val fromLoc = initialMapCell?.mapLoc ?: return true
        val isInCurrent = current.mapLoc == fromLoc
        if (isInCurrent || frameCtAfterScene < 100) {
            d { " STAY-> ON THE RIGHT ROUTE on ${current.mapLoc} == $fromLoc frames=$frameCt sc=$frameCtAfterScene wrapped = ${wrapped.name}"}
        } else {
            d { " STAY-> ON THE WRONG ROUTE on ${current.mapLoc} but should be on $fromLoc frames=$frameCt sc=$frameCtAfterScene wrapped = ${wrapped.name}"}
            // go in this direction
        }

        return isInCurrent
    }

    override val name: String
        get() = wrapped.name
}

class LadderAction: Action {
    // let link try to escape on its own for a bit
    private var ladderDeployedForFrames = 0
    override fun complete(state: MapLocationState): Boolean =
        (!state.frameState.ladderDeployed || ladderDeployedForFrames < LADDER_ESCAPE_MOVEMENTS).also {
            if (state.frameState.ladderDeployed) {
                ladderDeployedForFrames++
            }
        } //|| ladderOnPassable(state)

    private fun ladderOnPassable(state: MapLocationState) =
        state.frameState.ladder?.let { ladder ->
            state.currentMapCell.passable.get(ladder.x, ladder.y)
        } ?: false

    private var ladderDirection: GamePad? = GamePad.None
    private var ladderDirectionCount = 0

    companion object {
        private const val LADDER_ESCAPE_MOVEMENTS = 30
    }

    override fun nextStep(state: MapLocationState): GamePad {
        if (!state.frameState.ladderDeployed) {
            ladderDeployedForFrames = 0
            return GamePad.None
        }

        if (ladderOnPassable(state)) {
            d { "!! ladder on passable ${state.frameState.ladder?.point}"}
        }

        return if (ladderDirectionCount < LADDER_ESCAPE_MOVEMENTS) {
            if (ladderDirection == null) {
                ladderDirection = GamePad.randomDirection(state.link)
            }
            d {"!! Ladder direction!! $ladderDirection"}
            ladderDirectionCount++
            ladderDirection ?: GamePad.None
        } else {
            d {"!! Ladder no ladder"}
            ladderDirectionCount = 0
            ladderDirection = null
            GamePad.None
        }
    }

    override val name: String
        get() = ""
}
