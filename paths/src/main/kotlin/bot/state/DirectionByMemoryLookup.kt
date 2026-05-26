package bot.state

import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.MovingDirection
import nintaco.api.API
import util.d

class DirectionByMemoryLookup(
    private val api: API
){
    companion object {
        private const val DEBUG = false
    }
    data class PointAndDamage(
        val index: Int, // which array index in the various lists of addresses
        val point: FramePoint,
        val move: MovingDirection = MovingDirection.UnknownOrStationary,
        val damaged: Int = 0,
        val stunned: Int = 0,
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

    fun closest(point: FramePoint, maxDistance: Int = 5): PointAndDamage? {
        val nearest = enemyPoints.values.minByOrNull { it.point.distTo(point) } ?: return null
        val dist = nearest.point.distTo(point)
        d(DEBUG) { "dist to closest to $point is ${nearest.point} x dist: ${point.x - nearest.point.x} y dist: ${point.y - nearest.point.y} dist: $dist" }
        return nearest.takeIf { dist <= maxDistance }
    }

    private fun readEnemyPointDir(): List<PointAndDamage> {
        val dirs = Addresses.ememyDir.map { api.readCPU(it) }
        val damaged = Addresses.enemyDamaged.map { api.readCPU(it) }
        val hp = Addresses.enemyHp.map { api.readCPU(it) }
        val types = Addresses.More.objType.map { api.readCPU(it) }
        val x = Addresses.ememiesX.map { api.readCPU(it) }
        val y = Addresses.ememiesY.map { api.readCPU(it) }
        val stunned = Addresses.More.objStunTimer.map { api.readCPU(it) }

        val info = mutableListOf<PointAndDamage>()
        for (i in x.indices) {
            val pt = FramePoint(x[i], y[i] - MapConstants.yAdjust, mapDir(dirs[i]))
            val moveDirection = Direction.fromBitmaskWithDiagonal(dirs[i])
            val damage = damaged[i]
            val hpVal = hp[i]
            val typeVal = types[i]
            val stunned = stunned[i]
            val all = PointAndDamage(i, pt, moveDirection, damage, stunned, hpVal, typeVal)
            info.add(all)
            d(DEBUG) { "readEnemyPointDir info: $i: $pt $all" }
        }
        return info
    }

    fun readLinkPointDir(): Direction {
        val dirs = api.readCPU(Addresses.linkDir)
        return mapDir(dirs)
    }

    private fun mapDir(dir: Int) = Direction.fromBitmask(dir)

    override fun toString(): String {
        val builder = StringBuilder()
        for (enemyPoint in enemyPoints.values) {
            builder.append(" enemyPoints: $enemyPoint \n")
        }
        return builder.toString()
    }
}