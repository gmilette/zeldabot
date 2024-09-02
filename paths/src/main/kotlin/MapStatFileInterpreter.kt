import bot.state.MapCoordinates
import bot.state.map.stats.MapStatsTracker

class MapStatFileInterpreter {
    fun go() {
        val tracker = MapStatsTracker()
        val limit = 1000
        val stats = tracker.readStats(MapCoordinates(5, 100)) ?: return
        stats.tileAttributeCount.values.filter { it.total() > limit }.let { ct ->
            val tiles = ct.map { it.hex }
            for (at in ct) {
                val tile = at.hex
                val counts = at.attribCount.filter { it.value in 21..<limit }
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