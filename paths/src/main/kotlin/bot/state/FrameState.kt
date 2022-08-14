package bot

typealias MapLoc = Int

enum class EnemyState {
    // might be alive or dead
    Unknown,
    Alive, Dead
}

data class FrameState (
    val link: FramePoint = Undefined,
    val enemies: List<FramePoint> = emptyList(),
    val ememyState: List<EnemyState> = emptyList(),
    val subPoint: FramePoint = Undefined, // might not be frame point
    // // Value equals map x location + 0x10 * map y location
    // each screen has unique id.
    // 103
    // start 119, then right 120, then left 118
    // up the number goes down
    val mapLoc: MapLoc = 119
    // has to have all the locations on the map
//    val map.
)

data class FramePoint(val x: Int, val y: Int)

val Undefined = FramePoint(0, 0)

