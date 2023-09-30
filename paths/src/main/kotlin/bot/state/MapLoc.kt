package bot.state

// // Value equals map x location + 0x10 * map y location, that's hex
// so y location * 16 + x
// each screen has unique id.
// 103
// start 119, then right 120, then left 118
// up the number goes down
// 15x2 + 1
// 15 + 2*1
// going up means subtracting 16, down means adding 16
// y = floor(mapLoc / 16)
// x = (mapLoc % 16) * 16
typealias MapLoc = Int

// y values are 0..7
// x values are 0..15
// 127 total
val MapLoc.x
    get() = (this % 16) * 16

val MapLoc.y
    get() = this / 16

val MapLoc.up
    get() = this - 16

val MapLoc.down
    get() = this + 16

val MapLoc.right
    get() = this + 1

val MapLoc.left
    get() = this - 1

