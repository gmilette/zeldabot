package bot.state.map.stats

import bot.DirectoryConstants
import bot.plan.action.PrevBuffer
import bot.plan.action.ProjectileDirectionCalculator
import bot.state.*
import bot.state.map.MapConstants
import bot.state.map.MovingDirection
import bot.state.oam.EnemyGroup.boomerangs
import com.google.gson.GsonBuilder
import util.Map2d
import util.d
import java.io.File
import java.io.FileWriter

var gson = GsonBuilder().setPrettyPrinting().create()

data class PointAction(
    val point: FramePoint,
    val action: GamePad
)

data class MapStatsData(
    val mapCoordinates: MapCoordinates,
    val tileAttributeCount: MutableMap<Int, AttributeCount> = mutableMapOf()
) {
    fun add(other: MapStatsData) {
        for (entry in other.tileAttributeCount) {
            val otherCount = entry.value
            val thisCount = tileAttributeCount[entry.key]
            if (thisCount == null) {
                tileAttributeCount[entry.key] = otherCount
            } else {
                thisCount.add(otherCount)
            }
        }
    }

    override fun toString(): String =
        gson.toJson(this)
}

class MapStatsTracker {
    private val DEBUG = true
    private var mapCoordinates: MapCoordinates = MapCoordinates(0, 0)
    private var visits: Map2d<Boolean> = Map2d(
        MutableList(MapConstants.MAX_Y) { MutableList(MapConstants.MAX_X) { false } })
    private var movements = mutableListOf<PointAction>()
    private var moveLog: List<FramePoint> = emptyList()

    // the memory
    private val tileAttribCount = mutableMapOf<Int, AttributeCount>()
    // where was the enemy size frames ago, so I can imagine where it will be size frames from now
    var previousEnemyLocations: PrevBuffer<List<Agent>> = PrevBuffer(size = MapConstants.halfGrid)

    val seenBoomerang: Boolean
        get() = boomerangs.any { tileAttribCount.contains(it) }

    fun isDamaged(enemy: Agent): Boolean = isDamaged(enemy.tile, enemy.attribute)

    fun attribFor(tile: Int): AttributeCount {
        return tileAttribCount.getOrDefault(tile, AttributeCount(tile))
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
                    d { "check damage DAMAGED tile=$tile Attrib=${attribute}" }
                }
                d { attrib.tileString(tile) }
            } else {
                if (DEBUG) {
                    d { "check damage NOT DAMAGED tile=$tile " }
                }
                d { attrib.tileString(tile) }
            }
        }
    }

    private fun savePrevious(enemies: List<Agent>) {
        val enemyCopies = mutableListOf<Agent>()
        for (agent: Agent in enemies) {
            enemyCopies.add(agent.copy())
        }
        previousEnemyLocations.add(enemyCopies)
    }

    private fun saveLink(state: FrameState) {
        d { " saveq link state ${state.link.point}"}
        visits.set(state.link.point, true)
    }

    fun trackDecision(link: FramePoint, pad: GamePad) {
        movements.add(PointAction(link, pad))
    }

    fun track(mapCoordinates: MapCoordinates, enemies: List<Agent>, state: FrameState) {
        if (mapCoordinates != this.mapCoordinates) {
            reset(this.mapCoordinates)
        }
        saveLink(state)
        savePrevious(enemies)
        for (entry in enemies.groupBy { it.tile }) {
            tileAttribCount.getOrDefault(entry.key, AttributeCount(entry.key)).apply {
//                d { "attrib ct for ${entry.key}"}
                for (agent in entry.value) {
                    count(agent.attribute)
                }
                tileAttribCount[entry.key] = this
            }
        }
        this.mapCoordinates = mapCoordinates
    }

    private fun reset(mapCoordinates: MapCoordinates) {
        writeMapStats(mapCoordinates)
        writeVisits(mapCoordinates)
        appendMovements(mapCoordinates)
    }

    private fun writeMapStats(mapCoordinates: MapCoordinates) {
        // it is writing the wrong coordinates
        val mapStatsData = MapStatsData(mapCoordinates, tileAttribCount)
        if (DEBUG) {
            d { "**Map stats**" }
            d { mapStatsData.toString() }
        }
        // read in the old stats and add these new stats
        val currentMapStats = readStats(mapCoordinates)
        if (currentMapStats != null) {
            d { " read stats: $currentMapStats"}
            for (entry in currentMapStats.tileAttributeCount) {
                d { " ${entry.key} h: ${entry.key.toString(16)}"}
            }
            d { " current $mapStatsData" }
            mapStatsData.add(currentMapStats)
            d { " added $mapStatsData" }
        }
        write(mapCoordinates, mapStatsData.toString())
        previousEnemyLocations.clear()
        tileAttribCount.clear()
        // read json for this map coordinates
    }

    private fun writeVisits(mapCoordinates: MapCoordinates) {
        d { " write visits "}
        val fileName = "${mapCoordinates.level}_${mapCoordinates.loc}_visits.json"
        val fileNameCsv = "${mapCoordinates.level}_${mapCoordinates.loc}_visits.csv"
        val fileNameCsvDir = DirectoryConstants.file("visits", fileNameCsv)
        val dir = DirectoryConstants.file("visits", fileName)

//        val fileNameCsvC = "${mapCoordinates.level}_${mapCoordinates.loc}_visitsC.csv"
//        val fileNameCsvDirC = DirectoryConstants.file("visits", fileNameCsvC)

        // read it
        val current = readMap(dir)
        // merge it
        if (current != null) {
            visits.mapXyCurrent { x, y, t ->
                t || current.get(x, y)
            }
        }

        // write the merged visits, not just the most recent run
        val data = gson.toJson(visits)
        val writer = FileWriter(dir)
        writer.write(data)
        writer.close()

        visits.writeAt(fileNameCsvDir) { v, _, _ ->
            if (v) "X" else "_"
        }

//        visits.writeAtCondensed(fileNameCsvDirC) { v, x, y ->
//            if (v) "X" else "_"
//        }

        visits = Map2d(
            MutableList(MapConstants.MAX_Y) { MutableList(MapConstants.MAX_X) { false } })
    }

    private fun appendMovements(mapCoordinates: MapCoordinates) {
        d { " appendMovements "}
        val fileNameCsv = "${mapCoordinates.level}_${mapCoordinates.loc}_movements.csv"
        val fileNameCsvDir = DirectoryConstants.file("visits", fileNameCsv)

        val writer = FileWriter(fileNameCsvDir, true)
        for (movement in movements) {
            writer.append("${movement.point.x},${movement.point.y},${movement.action.name}\n")
        }
        writer.close()

        movements = mutableListOf()
    }

    private fun statFileName(mapCoordinates: MapCoordinates): String {
        val fileName = "${mapCoordinates.level}_${mapCoordinates.loc}_mapstats.json"
        return DirectoryConstants.file("mapStats", fileName)
    }

    private fun readStats(mapCoordinates: MapCoordinates): MapStatsData? {
        val file = File(statFileName(mapCoordinates))
        if (!file.exists()) {
            return null
        }
        val json = file.readText()
       return gson.fromJson(json, MapStatsData::class.java)
    }

    private fun readMap(name: String): Map2d<Boolean>? {
        val file = File(name)
        if (!file.exists()) {
            return null
        }
        val json = file.readText()
        return gson.fromJson(json, Map2d::class.java) as Map2d<Boolean>
    }

    private fun write(mapCoordinates: MapCoordinates, data: String) {
        val writer = FileWriter(statFileName(mapCoordinates))
        writer.write(data)
        writer.close()
    }

    private fun writeJson(mapCoordinates: MapCoordinates) {
//        val json = gson.toJson(myObject)
//        val file = File("map_stats_db.json")
//        val writer = FileWriter(file)
//        writer.write(json)
//        writer.close()
    }

    fun calcDirection(currentPoint: FramePoint, state: EnemyState, tile: Int): MovingDirection =
        previousEnemyLocations.buffer.firstOrNull()?.let { previousPoint ->
            ProjectileDirectionCalculator.calc(currentPoint, state, previousPoint, tile)
        } ?: MovingDirection.UNKNOWN_OR_STATIONARY
}

class AttributeCount(tile: Int = 0, val hex: String = tile.toString(16)) {
    companion object {
        private const val minObservations = 100
    }

    val attribCount = mutableMapOf<Int, Int>()

    fun add(other: AttributeCount) {
        for (count in other.attribCount) {
            attribCount[count.key] = (attribCount[count.key] ?: 0) + count.value
        }
    }

    private fun sorted() =
        attribCount.entries.toList().sortedBy { it.value }

    fun count(attrib: Int) {
        attribCount[attrib] = (attribCount[attrib] ?: 0) + 1
    }

    fun tileString(tile: Int): String {
        var s = ""
        for (entry in sorted()) {
            s = "$s\n$tile\t${tile.toString(16)}\t${entry.key}\t${entry.value}"
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

