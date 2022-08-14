package bot.state

import bot.EnemyState
import bot.FrameState
import bot.ZeldaBot

class MapLocationState(
    //
//    val mapLoc: MapLoc,
    // store some memory of what was happening
    var enemyState: List<EnemyState> = listOf<EnemyState>(),

//    val mapData: List<List<MapCell>>
    var lastGamePad: ZeldaBot.GamePad = ZeldaBot.GamePad.MoveUp,

    // the last known framestate
    var frameState: FrameState = FrameState()
)