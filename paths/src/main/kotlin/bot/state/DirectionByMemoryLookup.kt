package bot.state

import bot.state.map.Direction
import bot.state.map.MapConstants
import nintaco.api.API
import util.d

class DirectionByMemoryLookup(
    private val api: API
){
    private val enemyPoints: Map<String, FramePoint>

    init {
        enemyPoints = readEnemyPointDir().associateBy { it.oneStr }
//        for (enemyPoint in enemyPoints.values) {
//            d { " enemyPoints: $enemyPoint" }
//        }
    }

    fun lookupDirection(point: FramePoint): Direction = enemyPoints[point.oneStr]?.direction ?: Direction.None

    /**
     * it seems the oam locations might be different than memory locations by 1 x value
     */
    private fun List<FramePoint>.expandX(): List<FramePoint> {
        return flatMap {
            listOf(it, it.up.dir(it.direction), it.down.dir(it.direction), it.right.dir(it.direction), it.left.dir(it.direction))
        }
    }

    private fun readEnemyPointDir(): List<FramePoint> {
        val dirs = Addresses.ememyDir.map { api.readCPU(it) }
        val x = Addresses.ememiesX.map { api.readCPU(it) }
        val y = Addresses.ememiesY.map { api.readCPU(it) }
        return x.zip(y).zip(dirs).map { FramePoint(it.first.first, it.first.second - MapConstants.yAdjust, mapDir(it.second)) }.expandX()
    }

    fun readLinkPointDir(): Direction {
        val dirs = api.readCPU(Addresses.linkDir)
        return mapDir(dirs)
    }

    private fun mapDir(dir: Int) = when (dir) {
        2 -> Direction.Left
        1 -> Direction.Right
        8 -> Direction.Up
        4 -> Direction.Down
        else -> Direction.None
    }
}