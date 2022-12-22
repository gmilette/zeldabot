package bot.state

import bot.GamePad
import bot.plan.action.PreviousMove
import bot.state.map.Hyrule
import bot.state.map.MapCell

class MapLocationState(
    //
    var hyrule: Hyrule,
//    val mapLoc: MapLoc,
    var enemyReasoner: EnemyStateReasoner = EnemyStateReasoner(),

    var lastGamePad: GamePad = GamePad.MoveUp,

    var previousMove: PreviousMove = PreviousMove(
        previous = null,
        from = FramePoint(0, 0),
        to = FramePoint(0, 0),
        actual = FramePoint(0, 0),
        move = GamePad.None,
        triedToMove = true
    ),

    // remember for next interation
    // tried to get from prev to goal
    var previousLocation: FramePoint = FramePoint(),
    var previousGamePad: GamePad = GamePad.None,

    // the last known framestate
    var frameState: FrameState = FrameState(
        5,
        emptyList(),
        0,
        emptyAgent,
        Undefined,
        119,
        Inventory(0, 0, 0, 0, emptySet()),
        false,
        0,
        0,
        false,
        0
    ),

    var framesOnScreen: Int = 0,

    var currentMapCell: MapCell = MapCell.unknown,
) {
    init {
        clearHistory()
    }

    val link: FramePoint
        get() = frameState.link.point

    fun clearHistory() {
        enemyReasoner.clear()
        framesOnScreen = 0
    }

    val numEnemiesSeen: Int
        get() = enemyReasoner.numEnemiesSeen

    fun toStringEnemy(): String {
        var st = ""
        for (enemy in frameState.enemies) {
            st += " enemy ${enemy}\n"
        }
        return st
    }
}