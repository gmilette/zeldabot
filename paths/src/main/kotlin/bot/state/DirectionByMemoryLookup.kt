package bot.state

import bot.state.map.Direction
import bot.state.map.MapConstants
import nintaco.api.API
import util.d

class DirectionByMemoryLookup(
    private val api: API
){
    companion object {
        val DEBUG = false
    }
    data class PointAndDamage(
        val point: FramePoint,
        val damaged: Int = 0,
        val hp: Int = 0,
        val type: Int = 0
    )

    private val enemyPoints: Map<String, PointAndDamage>

    init {
        enemyPoints = readEnemyPointDir().associateBy { it.point.oneStr }
        if (DEBUG) {
            d { toString() }
        }
    }

    fun lookupDirection(point: FramePoint): Direction = enemyPoints[point.oneStr]?.point?.direction ?: Direction.None

    // add type too
    fun lookupDamaged(point: FramePoint): Boolean = (enemyPoints[point.oneStr]?.damaged ?: 0) != 0

    fun lookupHp(point: FramePoint): Int = (enemyPoints[point.oneStr]?.hp ?: 0)

    fun lookupType(point: FramePoint): Int = (enemyPoints[point.oneStr]?.type ?: 0)

    /**
     * it seems the oam locations might be different than memory locations by 1 x value
     */
    private fun List<PointAndDamage>.expandX(): List<PointAndDamage> {
        return flatMap {
            listOf(it,
                PointAndDamage(it.point.up.dir(it.point.direction), it.damaged, it.hp, it.type),
                PointAndDamage(it.point.down.dir(it.point.direction), it.damaged, it.hp, it.type),
                PointAndDamage(it.point.right.dir(it.point.direction), it.damaged, it.hp, it.type),
                PointAndDamage(it.point.left.dir(it.point.direction), it.damaged, it.hp, it.type)
            )
        }
    }

    private fun readEnemyPointDir(): List<PointAndDamage> {
        val dirs = Addresses.ememyDir.map { api.readCPU(it) }
        val damaged = Addresses.enemyDamaged.map { api.readCPU(it) }
        val hp = Addresses.enemyHp.map { api.readCPU(it) }
        val types = Addresses.More.objType.map { api.readCPU(it) }
        val x = Addresses.ememiesX.map { api.readCPU(it) }
        val y = Addresses.ememiesY.map { api.readCPU(it) }

        val info = mutableListOf<PointAndDamage>()
        for (i in x.indices) {
            val pt = FramePoint(x[i], y[i] - MapConstants.yAdjust, mapDir(dirs[i]))
            val damage = damaged[i]
            val hpVal = hp[i]
            val typeVal = types[i]
            val all = PointAndDamage(pt, damage, hpVal, typeVal)
            info.add(all)
            d(DEBUG) { "readEnemyPointDir info: $i: $pt $all" }
        }
        return info.expandX()
    }

    fun readLinkPointDir(): Direction {
        val dirs = api.readCPU(Addresses.linkDir)
        return mapDir(dirs)
    }

    private fun mapDir(dir: Int) = Direction.fromBitmask(dir)

    override fun toString(): String {
        val builder = StringBuilder()
        for (enemyPoint in enemyPoints.values) {
            builder.append(" enemyPoints: $enemyPoint\n")
        }
        return builder.toString()
    }
}