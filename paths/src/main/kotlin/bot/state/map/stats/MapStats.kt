package bot.state.map.stats

import bot.state.MapCoordinates


data class TileAttributeCount(
    val mapCoordinates: MapCoordinates,
    val tileAttribCount: Map<Int, AttributeCount> = mutableMapOf()
)