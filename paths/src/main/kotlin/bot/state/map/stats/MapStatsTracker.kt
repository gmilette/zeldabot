package bot.state.map.stats

import bot.state.Agent
import bot.state.FramePoint
import bot.state.MapCoordinates
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import util.d
import java.util.*
import kotlin.math.min

var gson = GsonBuilder().setPrettyPrinting().create()

data class MapStatsData(
    val mapCoordinates: MapCoordinates,
    val tileAttributeCount: Map<Int, AttributeCount>
) {
    override fun toString(): String =
        gson.toJson(this)
}

class MapStatsTracker {
    private val DEBUG = false
    private var mapCoordinates: MapCoordinates = MapCoordinates(0, 0)

    // the memory
    private val tileAttribCount = mutableMapOf<Int, AttributeCount>()

    fun isDamaged(enemy: Agent): Boolean = isDamaged(enemy.tile, enemy.attribute)

    fun attribFor(tile: Int): AttributeCount {
        return tileAttribCount.getOrDefault(tile, AttributeCount())
    }

    fun isDamaged(tile: Int, attribute: Int): Boolean {
        val attrib = tileAttribCount.getOrElse(tile) {
            return false
        }
//        val count = attrib.countFor(enemy.attribute)
//        // min distance between observations
//        if (count <= minObservations) {
//            return false
//        }
//        d { "is damaged: ${enemy.tile} ${enemy.attribute} ${count}" }
        return attrib.damaged(attribute).also {
            if (it) {
                if (DEBUG) {
                    d { "check damage DAMAGED Attrib=${attribute}" }
                }
                d { attrib.tileString(tile) }
            } else {
                if (DEBUG) {
                    d { "check damage NOT DAMAGED" }
                }
                d { attrib.tileString(tile) }
            }
        }
    }

    // enemies?
    fun track(mapCoordinates: MapCoordinates, enemies: List<Agent>) {
        if (mapCoordinates != this.mapCoordinates) {
            reset(mapCoordinates)
        }
        for (entry in enemies.groupBy { it.tile }) {
            tileAttribCount.getOrDefault(entry.key, AttributeCount()).apply {
                for (agent in entry.value) {
                    count(agent.attribute)
                }
                tileAttribCount[entry.key] = this
            }
        }
    }

    private fun reset(mapCoordinates: MapCoordinates) {
        val mapStatsData = MapStatsData(mapCoordinates, tileAttribCount)
        if (DEBUG) {
            d { "**Map stats**" }
            d { mapStatsData.toString() }
            writeJson(mapCoordinates)
        }
        tileAttribCount.clear()
        this.mapCoordinates = mapCoordinates
        // read json for this map coordinates
    }

    private fun writeJson(mapCoordinates: MapCoordinates) {
//        val json = gson.toJson(myObject)
//        val file = File("my_file.json")
//        val writer = FileWriter(file)
//        writer.write(json)
//        writer.close()
    }
}

class AttributeCount {
    companion object {
        private const val minObservations = 100
    }

    private val attribCount = mutableMapOf<Int, Int>()

    private fun sorted() =
        attribCount.entries.toList().sortedBy { it.value }

    fun count(attrib: Int) {
        attribCount[attrib] = (attribCount[attrib] ?: 0) + 1
    }

    fun tileString(tile: Int): String {
        var s = ""
        for (entry in sorted()) {
            s = "$s\n$tile\t${entry.key}\t${entry.value}"
        }
        return s
    }

    fun tileStringLine(): String {
        var s = ""
        for (entry in sorted()) {
            s = "$s ${entry.key}=${entry.value}"
        }
        return s
    }

    fun damaged(attrib: Int): Boolean =
        if (attribCount.size <= 1) false else {
            sorted().let {
                val last = it.last()
                val range = last.value - it.first().value
                if (range < minObservations) {
                    return false
                }
                var scanningDamagedValues = true
                var previous: Int? = null
                var damaged = true
                // ghost pattern
                // 39, 41, 470, 828
                // find the biggest difference between values, any values after the big
                // difference are the NOT damaged values
                // for ghosts there could be more than one ok value
                for (entry in it) {
                    if (previous != null) {
                        val diffFromPrevious = entry.value - previous
                        if (diffFromPrevious > minObservations) {
                            scanningDamagedValues = false
                        }
                        if (attrib == entry.key) {
                            damaged = scanningDamagedValues
                        }
                    }
                    previous = entry.value
                }
                return damaged
            }
        }
}

