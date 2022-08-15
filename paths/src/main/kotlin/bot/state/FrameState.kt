package bot

import bot.state.Agent
import kotlin.math.abs

typealias MapLoc = Int

enum class EnemyState {
    // might be alive or dead
    Unknown,
    Alive, Dead
}

enum class Dir {
    Up, Down, Left, Right, Unknown
}

data class FrameState (
    val enemies: List<Agent> = emptyList(),
    val link: Agent = Agent(FramePoint(0, 0), Dir.Right),
//    val link: FramePoint = Undefined,
//    val linkDir: Dir = Dir.Unknown,
//    val enemies: List<FramePoint> = emptyList(),
//    val ememyState: List<EnemyState> = emptyList(),
//    val ememyCountdowns: List<Int> = emptyList(),
//    val enemyDirs: List<Dir> = emptyList(),
    val subPoint: FramePoint = Undefined, // might not be frame point
    // // Value equals map x location + 0x10 * map y location
    // each screen has unique id.
    // 103
    // start 119, then right 120, then left 118
    // up the number goes down
    val mapLoc: MapLoc = 119,
    // check this, and if it exists, then go to the enemy x,y location to
    // collect it
//    val droppedItems: List<Int> = emptyList()
    // has to have all the locations on the map
//    val map.
)

data class FramePoint(val x: Int, val y: Int)

fun FramePoint.distTo(other: FramePoint) =
    abs(x - other.x) + abs(y - other.y)

val Undefined = FramePoint(0, 0)

