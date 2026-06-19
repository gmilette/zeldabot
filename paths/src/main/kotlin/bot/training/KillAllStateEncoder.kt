package bot.training

import bot.state.EnemyState
import bot.state.MapLocationState
import bot.state.distTo
import bot.state.map.Direction

object KillAllStateEncoder {
    fun encode(state: MapLocationState, episodeFrameCount: Int = 0): FloatArray {
        val fs = state.frameState
        val link = fs.link
        val buf = FloatArray(113)
        var i = 0

        // Link position (2 floats)
        buf[i++] = link.point.x / 255f
        buf[i++] = link.point.y / 167f
        // Link direction (5 floats)
        i = encodeDirection(link.dir, buf, i)
        // Link stats (2 floats)
        buf[i++] = fs.inventory.heartCalc.heartContainers() / 16f
        buf[i++] = if (fs.canUseSword) 1f else 0f

        // Up to 8 enemies, sorted by distance to link (12 floats each = 96 floats total)
        val enemies = fs.enemies
            .filter { it.state == EnemyState.Alive || it.state == EnemyState.Projectile }
            .sortedBy { it.point.distTo(link.point) }
            .take(8)
        for (e in enemies) {
            buf[i++] = e.point.x / 255f
            buf[i++] = e.point.y / 167f
            i = encodeDirection(e.dir, buf, i)
            buf[i++] = if (e.state == EnemyState.Alive) 1f else 0f
            buf[i++] = if (e.state == EnemyState.Projectile) 1f else 0f
            buf[i++] = e.tile / 255f
            buf[i++] = if (e.damaged) 1f else 0f
            buf[i++] = e.point.distTo(link.point) / 422f
        }
        // Zero-pad remaining enemy slots
        i += (8 - enemies.size) * 12

        // Inventory (5 floats)
        buf[i++] = fs.inventory.numBombs / 8f
        buf[i++] = fs.inventory.numKeys / 8f
        buf[i++] = fs.inventory.numRupees / 255f
        buf[i++] = fs.inventory.heartCalc.lifeInHearts().toFloat() / 16f
        buf[i++] = if (fs.inventory.hasBoomerang) 1f else 0f

        // Global (3 floats)
        buf[i++] = enemies.count { it.state == EnemyState.Alive } / 12f
        buf[i++] = if (fs.isLevel) 1f else 0f
        buf[i++] = episodeFrameCount.toFloat() / 1500f

        return buf
    }

    private fun encodeDirection(dir: Direction, buf: FloatArray, idx: Int): Int {
        var i = idx
        buf[i++] = if (dir == Direction.Up) 1f else 0f
        buf[i++] = if (dir == Direction.Down) 1f else 0f
        buf[i++] = if (dir == Direction.Left) 1f else 0f
        buf[i++] = if (dir == Direction.Right) 1f else 0f
        buf[i++] = if (dir == Direction.None) 1f else 0f
        return i
    }
}
