package sequence.findpaths

import bot.state.map.destination.DestType

data class Destination(
    val point: Point,
    val type: DestType
)