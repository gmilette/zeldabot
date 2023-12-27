package bot.state.map.stats

import bot.state.MapLoc

data class MapStats(val level: Int, val cell: MapLoc) {
}

data class TileAttributeCount(val tile: Int, val attrib: Int)