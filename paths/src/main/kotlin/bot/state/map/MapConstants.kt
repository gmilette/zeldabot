package bot.state.map

/**
 * any constants related to the map
 */
object MapConstants {
    const val MAX_X = 255
    const val MAX_Y = 167
    const val overworld = 0

    // each grid is 16 pixels. One tile occupies one grid usually. For example link is 1 grid big
    const val oneGrid = 16
    const val fourthGrid = 4
    const val halfGrid = 8
    const val oneGridF = 16.0f
    const val oneGridPoint5 = 24
    const val swordGrid = 8 // width of a sword I think
    // one grid accounts for size of link, 8 accounts for sword, but it should be 10
    // no i think it should be 8, maybe 10 because that is what we use for the sword
    // length
    const val swordGridPlusOne = swordGrid + oneGrid
    const val twoGrid = oneGrid * 2
    const val twoGridPoint5 = 40
    const val threeGrid = oneGrid * 3
    const val fourGrid = oneGrid * 4
    const val sixGrid = oneGrid * 6
    const val gridMaxX = 16
    const val gridMaxY = 16

    /**
     * subtract from any y's to convert to 0 coordinates
     */
    const val yAdjust = 61

    fun frac(percent: Float): Int = (oneGrid.toFloat() * percent).toInt()
}

val Int.isOverworld: Boolean
    get() = this == MapConstants.overworld