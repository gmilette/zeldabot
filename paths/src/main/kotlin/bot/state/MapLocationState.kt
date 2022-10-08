package bot.state

import bot.GamePad
import bot.plan.PreviousMove

class MapLocationState(
    //
    var hyrule: Hyrule,
//    val mapLoc: MapLoc,
    // store some memory of what was happening
    var enemyStateHistory: List<List<EnemyState>> = listOf(),

    val enemyLocationHistory: MutableList<List<FramePoint>> = mutableListOf(),

//    val mapData: List<List<MapCell>>
    var lastGamePad: GamePad = GamePad.MoveUp,

    var previousMove: PreviousMove = PreviousMove(),

    // remember for next interation
    // tried to get from prev to goal
    var previousLocation: FramePoint = FramePoint(),
    var previousGamePad: GamePad = GamePad.None,

    // the last known framestate
    var frameState: FrameState = FrameState(),

    var framesOnScreen: Int = 0,

    var currentMapCell: MapCell = MapCell.unknown
) {
    fun clearHistory() {
        enemyLocationHistory.clear()
        framesOnScreen = 0
    }
}