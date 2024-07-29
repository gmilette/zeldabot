package bot.state.oam

import bot.state.FramePoint
import bot.state.map.Direction

object DirectionLookup {
    fun getDir(tileAttribute: TileAttribute): Direction =
        swordDir.dirFront(tileAttribute.tile) ?: Direction.None

    /**
     * @param point with direction set
     */
    fun mapFromState(pointDirs: List<FramePoint>) {

    }
}