package bot.plan.action

import bot.plan.action.NavUtil.directionToDir
import bot.plan.zstar.ZStar
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapCell
import bot.state.map.MapConstants
import bot.state.map.toGamePad
import util.d
import util.w
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// try to detect when link is stuck and then get unstuck

fun moveHistoryAttackAction(wrapped: Action): Action {
    if (!wrapped.monitorEnabled) return wrapped

    val moveHistoryAction = if (wrapped.escapeActionEnabled) {
//        MoveHistoryAction(wrapped, AlwaysAttack(otherwiseRandom = true))
        MoveHistoryAction(wrapped, CycleAction)
    } else {
        d { " no escape enabled "}
        wrapped
    }
//    val ladderAction = LadderAction()
//    val combinedAction = DecisionAction(ladderAction, StayInCurrentMapCell(moveHistoryAction)) {
//        !ladderAction.complete(it).also {
//            d { "ladder decision $it"}
//        }
//    }

    val combinedAction = StayInCurrentMapCell(moveHistoryAction)

    val usePotion = UsePotion()
    val potionDecision = DecisionAction(usePotion, combinedAction, completeIf = {
        combinedAction.complete(it)
    }) {
        if (usePotion.hasBegun) {
            !usePotion.complete(it)
        } else {
            PotionUsageReasoner.shouldUsePotion(it.frameState)
        }
    }

    return potionDecision
//    return combinedAction
}

class MinDistTotalFramesCount {
    private var count: Int = 0
    private var frames: Int = 0
    private var minX = 0
    private var minY = 0
    private var maxX = 0
    private var maxY = 0

    fun record(point: FramePoint) {
//        frames++
        if (minX == 0 && minY == 0) {
            minX = point.x
            minY = point.y
            maxX = point.x
            maxY = point.y
        }
        minX = min(minX, point.x)
        minY = min(minY, point.y)
        maxY = max(maxY, point.y)
        maxX = max(maxX, point.x)
    }

    fun distance() = abs(maxY - minY) + abs(maxX - minX)

    fun reset() {
        count = 0
        minX = 0
        minY = 0
        maxX = 0
        maxY = 0
    }

    override fun toString(): String {
        return "Dist: ${distance()} Y: $maxY $minY X: $maxX $minX"
    }
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

    fun allSameAndFull(): Boolean =
        allSame() && isFull

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
    val buffer = ArrayDeque<T>(size) // More efficient than MutableList for this use case

    fun clear() {
        buffer.clear()
    }

    val isFull: Boolean
        get() = buffer.size == size

    fun add(pt: T) {
        if (buffer.size >= size) {
            buffer.removeFirst()
        }
        buffer.addLast(pt)
    }

    fun allSame(): Boolean =
        buffer.size == size && buffer.toSet().size == 1

    fun compare(other: MoveBuffer): Boolean {
        if (buffer.size != other.buffer.size) return false
        if (buffer.isEmpty()) return true

        // Check if all elements are not the same and sequences match
        return buffer.toSet().size > 1 &&
               buffer.zip(other.buffer).all { (a, b) -> a == b }
    }
}


class PrevBufferA<T>(val size: Int = 2) {
    var buffer = mutableListOf<T>()

    fun clear() {
        buffer = mutableListOf()
    }

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

// default actions
// if same x,y as enemy, attack
// unstick

class MoveHistoryAction(private val wrapped: Action, private val escapeAction: Action) : Action {
    private val histSize = 250
    private val same = SameCount()
    private val changed = MinDistTotalFramesCount()
    private var escapeActionCt = 0
    private val escapeActionTimes = 50
    private val cycleDetector = CycleDetector()
    private var history: MoveBuffer = MoveBuffer(MapConstants.twoGrid)
    private var whyEscape = ""
    private var frames = 0
    private val framesPerEscape = 2000

    override fun target(): FramePoint {
        return wrapped.target()
    }

    override fun targets(): List<FramePoint> {
        return wrapped.targets()
    }

    override fun reset() {
        changed.reset()
        same.reset()
    }

    override fun path(): List<FramePoint> =
        wrapped.path()

    override fun zstar(): ZStar? = wrapped.zstar()

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
//        d { "MoveHistoryAction" }
        frames++
        return when {
            escapeActionCt > 0 -> {
                d { " ESCAPE ACTION $escapeActionCt left because $whyEscape" }
                val action = escapeAction.nextStep(state)
                escapeActionCt--
                action
            }

            // what? dont escape when on enemy? makes no sense
//            state.link in state.aliveEnemies.map { it.point } -> {
//                whyEscape = "on enemy"
//                escapeActionCt = escapeActionTimes
//                d { " ESCAPE ACTION reset" }
//                same.reset()
//                wrapped.nextStep(state)
//            }

            else -> {
                val nextStep = wrapped.nextStep(state)
                val ct = same.record(nextStep)
                //val notChanged = changed.record(state.link)
                history.add(state.link)

                val minDist = MinDistTotalFramesCount()
                for (framePoint in history.buffer) {
                    minDist.record(framePoint)
                }
                val dists = history.buffer.map { it.oneStr }.joinToString(" ")
//                d { "min dist for escape: $frames $minDist $dists"}
                val notChanged = if (frames > framesPerEscape) { // stuck
                    val notChangedResult = (minDist.distance() < MapConstants.twoGrid)

                    if (notChangedResult) {
                        d { " ESCAPE ACTION NOT CHANGED $frames dist=${minDist.distance()}" }
                    }
                    notChangedResult
                } else {
                    false
                }
                // keep saving link's location
//                cycleDetector.save(state.link)
//                d { " ESCAPE ACTION not same $nextStep + $ct last ${same.last}" }
                if (ct >= histSize || notChanged) { // || notChanged
                    escapeActionCt = escapeActionTimes
                    // if not changed to the reset screen
                    whyEscape = "histsize=$histSize ct: ${ct >= histSize} notChanged=$notChanged"
                    d { " ESCAPE ACTION RESET" }
                    frames = 0
                    same.reset()
                    changed.reset()
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

/**
 * say you tried to burn a tree and missed, the only thing you can do is leave
 * the screen and come back
 */
class ExitReturnIfNecessary(private val wrapped: Action): Action {
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

class ReBurnIfNecessary(private val wrapped: Action): Action {
    private var frameCt = 0
    private var attack = AlwaysAttack(useB = true)

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        frameCt++

        // it worked! it took a while though at 1000
        return if (frameCt > 600) {
            d { "Reburn!!!" }
            if (frameCt > 700) {
                frameCt = 0
                d { "ah well restart" }
            }
            // TODO: this requires leaving the spot and returning
            attack.nextStep(state)
        } else {
            wrapped.nextStep(state)
        }
    }
}


class StayInCurrentMapCell(private val wrapped: Action) : Action {
    private val routeTo = RouteTo(params = RouteTo.Param())

    private var initialLevel: Int = -1

    private var failureCt = 0

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

    var disabled = false
    override fun nextStep(state: MapLocationState): GamePad {
        // don't do any of this if the screen is scrolling
        // if it is scrolling just reset the framect after the scene
        if (state.frameState.isScrolling || state.movedTo == 0 || state.levelTo == -1) {
            d { "StayInCurrentMapCell dont move = ${state.movedTo} level = ${state.levelTo}" }
            failureCt = 0
            return wrapped.nextStep(state)
        }

        val isInCurrent = state.currentMapCell.mapLoc == state.movedTo && state.frameState.level == state.levelTo
        if (isInCurrent) {
            failureCt = 0
        } else {
            failureCt++
        }

        return if (isInCurrent || failureCt < 70) {
            wrapped.nextStep(state)
        } else {
            d { " should be at ${state.movedTo} but am at ${state.currentMapCell.mapLoc}"}
            val dir = state.currentMapCell.mapLoc.directionToDir(state.movedTo)
//
//            // this should handle the case where link accidentally moves into
//            // stair
            val exits = when {
                dir == Direction.None -> state.currentMapCell.allExits()
                else -> state.currentMapCell.exitsFor(dir) ?: state.currentMapCell.allExits()
            }
//            if (dir == Direction.None) {
////                // the only way to go is up and out I think, there isn't a way to warp
////                // somewhere else
////                dir = Direction.Up
//            }
            d { " closest to ${exits.minOf { it.distTo(state.link) }}"}
//            d { "all "}
//            exits.forEach {
//                d { "   $it d:${it.distTo(state.link)}" }
//            }
            // maybe add parameter for NO attacking while moving, so link doesn't get pulled into the level.
            // it happened in level 7 for sure
            routeTo.routeTo(state, exits)
        }
    }

    override fun reset() {
        initialLevel = -1
        failureCt = 0
    }

    override val name: String
        get() = "${if (failureCt>70) "! " else "" } ${wrapped.name}"
}

class LadderAction: Action {
    // let link try to escape on its own for a bit
    private var ladderDeployedForFrames = 0
    override fun complete(state: MapLocationState): Boolean =
        (!state.frameState.ladderDeployed || (ladderDeployedForFrames < LADDER_ESCAPE_MOVEMENTS) || ladderOnPassable(state)).also {
            d { "!! ladder deployed ${state.frameState.ladderDeployed} frames $ladderDeployedForFrames passable ${ladderOnPassable(state)}" }
            if (state.frameState.ladderDeployed) {
                ladderDeployedForFrames++
            } else {
                ladderDeployedForFrames = 0
            }
        }

    private fun ladderOnPassable(state: MapLocationState) =
        state.frameState.ladder?.point?.let { ladder ->
            // try all makes sense but it also might disable the ladder action
            listOf(ladder, ladder.justRightEnd, ladder.justLeftBottom, ladder.justRightEndBottom).all {
                state.currentMapCell.passable.get(it.x, it.y).also { res ->
                    //d { "on passable: $it $res"}
                }
            }
//            listOf(ladder).all {
//                state.currentMapCell.passable.get(it.x, it.y)
//            }
        } ?: false

    private var ladderDirection: GamePad? = GamePad.None
    private var ladderDirectionCount = 0

    companion object {
        private const val LADDER_ESCAPE_MOVEMENTS = 26
    }

    override fun nextStep(state: MapLocationState): GamePad {
        if (!state.frameState.ladderDeployed) {
            d { " !! ladder not deployed "}
            ladderDeployedForFrames = 0
//            ladderDirection = null
//            ladderDirectionCount = 0
            return GamePad.None
        } else {
            d { " !! ladder deployed "}
            // not needed I think
//            ladderDeployedForFrames++
        }

        return if (ladderDirectionCount < LADDER_ESCAPE_MOVEMENTS) {
            // what direction is link facing?
            if (ladderDirection == null) {
                ladderDirection = state.frameState.link.dir.toGamePad()
                if (ladderDirection == GamePad.None) {
                    ladderDirection = GamePad.randomDirection()
                }
                d {"!! Ladder direction!! start at $ladderDirection"}
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

class Timeout(action: Action, private val frameLimit: Int = 400) : WrappedAction(action) {
    private var frames = 0

    override fun nextStep(state: MapLocationState): GamePad {
        d { " timeout ct $frames"}
        frames++
        return super.nextStep(state)
    }

    override fun reset() {
        frames = 0
        super.reset()
    }

    override fun complete(state: MapLocationState): Boolean =
        frames > frameLimit || super.complete(state).also {
            if (it) {
                frames = 0
            }
        }

    override val name: String
        get() = "Timeout of $frames ${super.name}"
}

class TimeoutThen(action: Action, private val contingency: Action) : WrappedAction(action) {
    private val frameLimit = 200
    private var frames = 0

    override fun nextStep(state: MapLocationState): GamePad {
        d { " timeout ct $frames"}
        frames++
        return if (frames > frameLimit) {
            d { " timeout do contingency"}
            contingency.nextStep(state)
        } else {
            super.nextStep(state)
        }
    }

    override fun reset() {
        frames = 0
        super.reset()
    }

    override fun complete(state: MapLocationState): Boolean =
        if (frames > frameLimit) {
            contingency.complete(state)
        } else {
            super.complete(state)
        }.also {
            if (it) {
                frames = 0
            }
        }

    override val name: String
        get() = "Timeout Then of $frames ${super.name}"
}

