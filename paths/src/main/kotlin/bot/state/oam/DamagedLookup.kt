package bot.state.oam

import bot.state.oam.DamagedLookup.add128
import util.d

object DamagedLookup {
    private val h67 = 0x43
    private val h66 = 0x42
    private val h65 = 0x41
    private val h64 = 0x40

    private val h131 = 0x83
    private val h130 = 0x82
    private val h129 = 0x81
    private val h128 = 0x80

    private val damagedPairs = mutableSetOf(
        0xc2 to 0,
        0xc2 to 1,
        0xc2 to h67,
        0xc2 to h66,
        0xc4 to 0,
        0xc4 to 1,
        0xc4 to h67,
        0xc4 to h66,
        0xf4 to 0,
        0xf4 to 1,
        0xfe to h65,
        0xfe to h64,
        0xf6 to h65,
        0xf6 to h64,
        0xf8 to h65,
        0xf8 to h64,
        0xf8 to 0,
        0xf8 to 1,
        0xf2 to h64,
        0xf2 to h65,
        0xf0 to 1,
        0xf0 to 0,
        0xb6 to h64,
        0xb6 to h67,
        0xb6 to h66,
        0xb0 to 0,
        0xb0 to 2,
        0xb0 to 3,
        0xb8 to 0,
        0xb8 to 2,
        0xb4 to 3,
        0xb4 to 0,
        0xb4 to 2,
        0xfc to 0,
        0xfc to 1,
        0xba to h67,
        0xba to h66,
        0xba to h64,
        0xb8 to 3,
//        0xb0 to h128,
//        0xb0 to h131,
//        0xb0 to h130,
//        0xb2 to 0,
//        0xb2 to 2,
//        0xb2 to 3,
//        0xb2 to h128,
//        0xb2 to h131,
//        0xb2 to h130,
    )
        // forest level stuff
        .add128(0xb0)
        .add023(0xb2)
        .add128(0xb2)
        .add(0x28, h130) // statue i think
        // got these from white sword room
        .add64(0xd0)
        .add64(0xd4)
        .add64(0xd8)
        .add64(0xdc)
        .add023(0xc3)
        .add023(0xd6)
        .add02(0xd2)
        .add02(0xda)


    init {
        for (damagedPair in damagedPairs) {
            d { " damaged pair ${damagedPair.tile} to ${damagedPair.attribute}"}
        }
    }

    fun build() {
        val sets = mutableSetOf<TileAttribute>()
        sets.add023(0xb2)
    }

    private fun MutableSet<TileAttribute>.same(tile: Int) {
        // use the same set as last time
    }

    private fun MutableSet<TileAttribute>.add(tile: Int, attrib: Int): MutableSet<TileAttribute> {
        add(tile to attrib)
        return this
    }

    private fun MutableSet<TileAttribute>.add128(tile: Int): MutableSet<TileAttribute> {
        add(tile to h128)
        add(tile to h131)
        add(tile to h130)
        return this
    }

    private fun MutableSet<TileAttribute>.add64(tile: Int): MutableSet<TileAttribute> {
        add(tile to h64)
        add(tile to h67)
        add(tile to h66)
        return this
    }

    private fun MutableSet<TileAttribute>.add023(tile: Int): MutableSet<TileAttribute> {
        add(tile to 0)
        add(tile to 2)
        add(tile to 3)
        return this
    }
    private fun MutableSet<TileAttribute>.add02(tile: Int): MutableSet<TileAttribute> {
        add(tile to 0)
        add(tile to 2)
        return this
    }

    private infix fun MutableSet<Pair<Int, Int>>.add(tile: Int) {

    }

    fun isDamaged(tile: Int, attribute: Int): Boolean {
        return damagedPairs.contains(tile to attribute)
    }
}