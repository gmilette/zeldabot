package bot.state.oam

import bot.state.map.Direction

object DirectionLookup {
    fun getDir(tileAttribute: TileAttribute): Direction =
        swordDir.dirFront(tileAttribute.tile) ?: Direction.None
}