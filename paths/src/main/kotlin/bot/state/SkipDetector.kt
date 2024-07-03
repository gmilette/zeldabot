package bot.state

import bot.state.map.Direction
import bot.state.map.upOrLeft
import nintaco.api.API
import kotlin.math.abs

data class SkipCoordinates(
    val subPixel: Int,
    val subTile: Int,
    val linkDir: Int
)

object SkipDetector {
    fun getSkip(api: API): SkipCoordinates {
        val subPixel = api.readCPU(Addresses.subPixel)
        val subTile = api.readCPU(Addresses.subTile)
        val linkDir = api.readCPU(Addresses.linkDir)
        return SkipCoordinates(subPixel, subTile, linkDir)
    }

    fun willSkip(api: API): Boolean {
        val subPixel = api.readCPU(Addresses.subPixel)
        val subTile = api.readCPU(Addresses.subTile)
        val linkDir = api.readCPU(Addresses.linkDir)
        return willSkip(subPixel, subTile, linkDir)
    }

    fun willSkip(subPixel: Int, subTile: Int, linkDir: Int): Boolean {
        // $08=North, $04=South, $01=East, $02=West
        val dir = when (linkDir) {
            1 -> Direction.Right
            2 -> Direction.Left
            4 -> Direction.Down
            8 -> Direction.Up
            else -> Direction.None
        }

        val subPixelNegative = subPixel < 0
        val absSubTile = abs(subTile)

        val willSkip = if (dir == Direction.Right) {
            absSubTile < 7 && subPixelNegative
        } else {
            false
        }

        // doesnt work for left
        val oneAwayFromTile =
            (absSubTile == 7 && !dir.upOrLeft) ||
                    (absSubTile == 1 && dir.upOrLeft)

        return (subPixelNegative && !oneAwayFromTile)
    }

}