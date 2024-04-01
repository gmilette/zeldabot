package bot.state.map

object MapConstants {
    const val MAX_X = 255
    const val MAX_Y = 167
    const val overworld = 0
    const val halfGrid = 8
    const val oneGrid = 16
    const val oneGridF = 16.0f
    const val oneGridPoint5 = 24
    const val swordGrid = 10 // size of sword
    // one grid accounts for size of link, 8 accounts for sword, but it should be 10
    const val swordGridPlusOne = 8 + oneGrid
    const val twoGrid = oneGrid * 2
    const val twoGridPoint5 = 40
    const val threeGrid = oneGrid * 3
    const val gridMaxX = 16
    const val gridMaxY = 16

    /**
     * subtract from any y's to convert to 0 coordinates
     */
    const val yAdjust = 61
}