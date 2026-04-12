package bot.state

import bot.state.map.Direction
import bot.state.map.MapConstants
import nintaco.api.API
import org.apache.commons.math3.analysis.function.Add
import util.d

class DirectionByMemoryLookup(
    private val api: API
){
    data class PointAndDamage(
        val point: FramePoint,
        val damaged: Int = 0
    )

    private val enemyPoints: Map<String, PointAndDamage>

    init {
        enemyPoints = readEnemyPointDir().associateBy { it.point.oneStr }
        for (enemyPoint in enemyPoints.values) {
            d { " enemyPoints: $enemyPoint" }
        }
    }

    fun lookupDirection(point: FramePoint): Direction = enemyPoints[point.oneStr]?.point?.direction ?: Direction.None

    // add type too
    fun lookupDamaged(point: FramePoint): Boolean = (enemyPoints[point.oneStr]?.damaged ?: 0) != 0

    /**
     * it seems the oam locations might be different than memory locations by 1 x value
     */
    private fun List<PointAndDamage>.expandX(): List<PointAndDamage> {
        return flatMap {
            listOf(it,
                PointAndDamage(it.point.up.dir(it.point.direction), it.damaged),
                PointAndDamage(it.point.down.dir(it.point.direction), it.damaged),
                PointAndDamage(it.point.right.dir(it.point.direction), it.damaged),
                PointAndDamage(it.point.left.dir(it.point.direction), it.damaged)
            )
        }
    }

    private fun readEnemyPointDir(): List<PointAndDamage> {
        val dirs = Addresses.ememyDir.map { api.readCPU(it) }
        val damaged = Addresses.enemyDamaged.map { api.readCPU(it) }
        val x = Addresses.ememiesX.map { api.readCPU(it) }
        val y = Addresses.ememiesY.map { api.readCPU(it) }
//        val enemyDirs = x.zip(y).zip(dirs).map { FramePoint(it.first.first, it.first.second - MapConstants.yAdjust, mapDir(it.second)) }.expandX()
        val enemyDirsD = x.zip(y).zip(dirs).zip(damaged).map {
            val pt = it .first.first
            val dir = it.first.second
            val damage = it.second
            if (damage != 0) {
                d { "the damage $damage at $pt" }
            }
            val point = FramePoint(pt.first, pt.second - MapConstants.yAdjust, mapDir(dir))
            PointAndDamage(point, damage) }.expandX()
        return enemyDirsD
    }

    fun readLinkPointDir(): Direction {
        val dirs = api.readCPU(Addresses.linkDir)
        return mapDir(dirs)
    }

    private fun mapDir(dir: Int) = Direction.fromBitmask(dir)
}