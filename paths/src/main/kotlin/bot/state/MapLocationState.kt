package bot.state

import bot.plan.action.MoveBuffer
import bot.plan.action.PrevBuffer
import bot.plan.action.PreviousMove
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.map.MapCell
import bot.state.map.pointModifier
import nintaco.api.ApiSource
import util.d

/**
 * persists between frames
 */
class MapLocationState(
    var hyrule: Hyrule,

    var lastGamePad: GamePad = GamePad.MoveUp,

    val lastPoints: MoveBuffer = MoveBuffer(10),

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
        emptyList(),
        emptyList(),
        0,
        0,
        emptyAgent,
        emptyAgent,
        false
    ),

    var framesOnScreen: Int = 0,

    var currentMapCell: MapCell = MapCell.unknown,

    var movedTo: Int = 0,
    var levelTo: Int = 0
) {
    init {
        clearHistory()
    }

    val link: FramePoint
        get() = frameState.link.point

    /**
     * get most frequent direction, it will be one that was successful
     * if link is not moving, then it will be forced to try something random
     */
    fun bestDirection(): Direction {
        val lastDirections = lastPoints.buffer.zipWithNext { a, b -> a.dirTo(b) }
        val keyWithMostItems = lastDirections.groupBy { it.ordinal }.maxByOrNull { it.value.size }?.key ?: 0
        // idea: if there are two directions counts that are equal, link is oscillating, maybe do something different
        val direction = Direction.entries[keyWithMostItems]
        d { " sorted dirs $keyWithMostItems $direction"}
        return direction
    }

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