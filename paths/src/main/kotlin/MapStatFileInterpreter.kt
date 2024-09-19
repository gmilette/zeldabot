import bot.state.MapCoordinates
import bot.state.map.stats.MapStatsTracker
import util.d

fun main() {
    println("MapStatFileInterpreter")
    MapStatFileInterpreter().go()
}

class MapStatFileInterpreter {
    fun go() {
        val tracker = MapStatsTracker()
        val limit = 1000
        val stats = tracker.readStats(MapCoordinates(6, 56)) ?: return
        stats.tileAttributeCount.values.filter { it.total() > limit }.let { ct ->
            val tiles = ct.map { it.hex }
            for (at in ct) {
                val tile = at.hex
                val counts = at.attribCount.filter { it.value in 21..<limit }
                d { "**** tile $tile" }
                for (entry in at.attribCount) {
                    val paletteIndex = 0x10 or ((entry.value and 0x03) shl 2)
                    d { "e: ${entry.key} to ${paletteIndex} (${entry.value.toString(2)})" }
                }
            }
        }
        // calculate damaged

        // could just modify the stats, and output it
        // then read it again

        // want to create a uber mapStats data for the levels containing everything I need
        // but i still need a lyonel to hex code mapping so I can lookup
        // that is a separate lookup
        // all tiles
        // damaged tiles
        // boomerang
    }
}