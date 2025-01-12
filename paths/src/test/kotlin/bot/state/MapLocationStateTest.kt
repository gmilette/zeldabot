package bot.state

import bot.plan.action.MoveBuffer
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.map.MapConstants
import io.kotest.matchers.be
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.kotlin.mock
import util.d
import kotlin.random.Random

class MapLocationStateTest {
    @Test
    fun `test best direction`() {
        //val mapLocationState = MapLocationState(Hyrule())
        //lastPoints
        val p32 = FramePoint(32, 88)
        val p33 = FramePoint(33, 88)
        //mapLocationState.lastPoints.addAll(listOf(p32, p33, p32, p32, p32, p33, p32, p32, p32, p33))
        val lastPoints = MoveBuffer(10)
        //32_88, 33_88, 32_88, 32_88, 32_88, 33_88, 32_88, 32_88, 32_88, 33_88
        lastPoints.addAll(listOf(p32, p33, p32, p32, p32, p33, p32, p32, p32, p33))
        val bestDirection = bestDirection(lastPoints)
        bestDirection shouldBe Direction.Right
    }

    @Test
    fun `test dirto`() {
        val p32 = FramePoint(32, 88)
        val p33 = FramePoint(33, 88)
        p32.dirTo(p32) shouldBe Direction.None
    }

    @Test
    fun `test one`() {
        val x = 32
        val res = x < MapConstants.MAX_X - MapConstants.oneGrid - 2
        val b = Random.nextInt(4)
        val b2 = Random.nextInt(4)
        val b3 = Random.nextInt(4)
        val b4 = Random.nextInt(4)
        val b54 = Random.nextInt(4)
        val a = 1
    }

    fun bestDirection(lastPoints: MoveBuffer): Direction {
        // this is required, otherwise link will get stuck
        // why is it commented out??
        if (lastPoints.allSameAndFull()) {
            return Direction.randomDirection()
        }
        // don't keep the NONE
        val lastDirections = lastPoints.buffer.zipWithNext { a, b -> a.dirTo(b) }.filter { it != Direction.None }
        val keyWithMostItems = lastDirections.groupBy { it.ordinal }.maxByOrNull { it.value.size }?.key ?: 0
        // idea: if there are two directions counts that are equal, link is oscillating, maybe do something different
        val direction = Direction.entries[keyWithMostItems]
        d { " bestdirction sorted dirs $keyWithMostItems $direction moves $lastPoints dirs: $lastDirections"}
        return direction
    }
}