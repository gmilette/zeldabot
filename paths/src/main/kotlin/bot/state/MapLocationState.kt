package bot.state

import bot.plan.action.PreviousMove
import bot.state.map.Hyrule
import bot.state.map.MapCell
import nintaco.api.ApiSource

/**
 * persists between frames
 */
class MapLocationState(
    var hyrule: Hyrule,

    var lastGamePad: GamePad = GamePad.MoveUp,

    var previousMove: PreviousMove = PreviousMove(
        previous = null,
        from = FramePoint(0, 0),
        to = FramePoint(0, 0),
        actual = FramePoint(0, 0),
        move = GamePad.None,
        triedToMove = true
    ),

    // remember for next interaction
    var previousLocation: FramePoint = FramePoint(),
    var previousHeart: Double = -1.0,
    var previousDamageNumber: Int = -1,
    var previousNumBombs: Int = 0,
    var previousGamePad: GamePad = GamePad.None,

    // null: not deployed
    // true/false: can move horizontal / vertical
    var ladderStateHorizontal: Boolean? = null,

    // the last known framestate
    var frameState: FrameState = FrameState(ApiSource.getAPI(),
        emptyList(),
        0,
        0,
        emptyAgent,
        emptyAgent
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
        framesOnScreen = 0
    }

    fun toStringEnemy(): String {
        var st = ""
        for (enemy in frameState.enemies) {
            st += " enemy ${enemy}\n"
        }
        return st
    }
}