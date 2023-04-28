package bot.plan.action

import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import util.d
import util.w

fun moveHistoryAttackAction(wrapped: Action) =
// really, if there are enemies attack, otherwise, move, or bomb
// if there are pancake enemies
    // hopefully adding some random moves here will also help link get unstuck
    MoveHistoryAction(wrapped, AlwaysAttack(otherwiseRandom = true))


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

    override fun complete(state: MapLocationState): Boolean =
        wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        d { "MoveHistoryAction" }
        return when {
            escapeActionCt > 0 -> {
                d { " ESCAPE ACTION " }
                val action = escapeAction.nextStep(state)
                escapeActionCt--
                action
            }

            state.link in state.aliveEnemies.map { it.point } -> {
                escapeActionCt = escapeActionTimes
                d { " ESCAPE ACTION SAME ENEMY" }
                same.reset()
                wrapped.nextStep(state)
            }

            else -> {
                val nextStep = wrapped.nextStep(state)
                val ct = same.record(nextStep)
                // keep saving link's location
                cycleDetector.save(state.link)
                d { " ESCAPE ACTION not same $nextStep + $ct last ${same.last}" }
                if (ct >= histSize) {
                    escapeActionCt = escapeActionTimes
                    d { " ESCAPE ACTION RESET" }
                    same.reset()
                }
                nextStep
            }
        }.also {
            val inIt = state.link in state.aliveEnemies.map { it.point }
            d { " link at ${state.link} $inIt" }
        }
    }

    override val name: String
        get() = wrapped.name
}
