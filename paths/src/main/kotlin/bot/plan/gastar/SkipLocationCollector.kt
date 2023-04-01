package bot.plan.gastar

import bot.state.*
import bot.state.map.Direction
import util.d
import util.e

class SkipLocationCollector {
    private val skips: MutableMap<Direction, LinkedHashSet<Int>> = mutableMapOf()

    init {
        skips[Direction.Down] = LinkedHashSet()
        skips[Direction.Up] = LinkedHashSet()
        skips[Direction.Right] = LinkedHashSet()
        skips[Direction.Left] = LinkedHashSet()
    }

    fun collect(now: FramePoint, prev: FramePoint) {
        val dir = prev.dirTo(now)
        val dist = now.distTo(prev)
        if (dist > 2) {
            e { "too much dist!!! $prev to $now" }
        } else if (dist == 2) {
            when (dir) {
                Direction.Up,
                Direction.Down -> {
                    d { "$dir add y ${prev.y}" }
                    skips[dir]?.add(prev.y)
                }
                else -> {
                    d { "$dir add x ${prev.x}" }
                    skips[dir]?.add(prev.x)
                }
            }
            d { "***" }
//            d {" ${toStringAll()}" }
            d {" ${skips}" }
        } else {
            d { " dist $dist" }
        }
    }

    fun toStringAll(): String {
        return "d ${skips[Direction.Down]?.sorted()}\nu ${skips[Direction.Up]?.sorted()}\nr ${skips[Direction.Right]?.sorted()}\nl " +
                "${
                    skips[Direction
                        .Left]?.sorted()
                }\n"
    }

    override fun toString(): String {
        return "d ${skips[Direction.Down]?.sorted()?.size}\nu ${skips[Direction.Up]?.sorted()?.size}\nr ${skips[Direction.Right]?.sorted()?.size}\nl " +
                "${
                    skips[Direction
                        .Left]?.sorted()?.size
                }\n"
    }
}