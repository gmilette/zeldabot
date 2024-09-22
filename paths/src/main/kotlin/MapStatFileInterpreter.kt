import bot.state.MapCoordinates
import bot.state.map.stats.MapStatsTracker
import bot.state.oam.monsterColor
import nintaco.util.BitUtil
import util.d

fun main() {
    println("MapStatFileInterpreter")
    MapStatFileInterpreter().go()
}

class MapStatFileInterpreter {
    fun go() {
        val tracker = MapStatsTracker()
        val limit = 1000
//        val lev = 5 ; val loc = 100
//        val lev = 5 ; val loc = 101
//        val lev = 0 ; val loc = 107
//        val lev = 0 ; val loc = 120
//        val lev = 3 ; val loc = 91
        val lev = 6 ; val loc = 56
        val stats = tracker.readStats(MapCoordinates(lev, loc)) ?: return
        stats.tileAttributeCount.values.filter { it.total() > limit }.let { ct ->
            val tiles = ct.map { it.hex }
            for (at in ct) {
                val tile = at.hex
                val counts = at.attribCount.filter { it.value in 21..<limit }
                d { "**** tile $tile" }
                for (entry in at.attribCount) {
                    val attribute = entry.key
                    val paletteIndex = 0x10 or ((attribute and 0x03) shl 2)
                    val pColor = attribute.monsterColor()
//                    d { " b ${attribute.toString(2)}"}
//                    for (i in 0..7) {
//                        d { "$i: ${BitUtil.getBitBool(attribute, i)}"}
//                    }
//                    for (i in 0..7) {
//                        d { "-- $i: ${getBit(attribute, i)}"}
//                    }
                    val lastBit = getLastBit(attribute)
//                    val damaged = (attribute != 1) || (lastBit == 1 && attribute > 60) // && (lastBit != 1))
                    val notDamaged = attribute == 1 || (attribute != 0 && attribute.toString(2).endsWith("01"))
//                    val damaged = if ()
//                    d {"last = ${getLastBit(attribute)}"}
                    val xFlip = if (BitUtil.getBitBool(attribute, 6)) "xFlip" else ""
                    val yFlip = if (BitUtil.getBitBool(attribute, 7)) "yFlip" else ""
                    d { "e: ${entry.key}, ${entry.value} to $paletteIndex $pColor (${attribute.toString(2)}) $xFlip $yFlip ${if (notDamaged) "*" else "D"}" }
                }
            }
        }
        // 3, 131 c28, Blue
        // 2, 130 c24, Red
        // 1, 129 c20, Blue
        // 0, 128 c16

        // outdoor grunt
        // 24 for red
        // 20 is blue

        // yFlip | xFlip

        // 100ss. have yflip


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

    fun getLastBit(num: Int): Int {
        return num and 1
    }

    fun getBit(value: Int, position: Int): Int {
        return (value shr position) and 1;
    }
}