package bot.plan.gastar

import bot.state.*
import bot.state.map.Direction
import bot.state.map.vertical

object SkipLocations {
    private val skips: MutableMap<Direction, Set<Int>> = mutableMapOf()

    private fun has(pt: Int, direction: Direction): Boolean =
        skips[direction]?.contains(pt) == true

    fun hasPt(point: FramePoint) =
        skips.any {
            if (it.key.vertical) {
                it.value.contains(point.y)
            } else {
                it.value.contains(point.y)
            }
        }

    fun getNext(start: FramePoint, direction: Direction): FramePoint =
        when (direction) {
            Direction.Up ->
                if (has(start.y, direction)) {
                    start.up2
                } else {
                    start.up
                }
            Direction.Down ->
                if (has(start.y, direction)) {
                    start.down2
                } else {
                    start.down
                }
            Direction.Right ->
                if (has(start.x, direction)) {
                    start.right2
                } else {
                    start.right
                }
            Direction.Left ->
                if (has(start.x, direction)) {
                    start.left2
                } else {
                    start.left
                }
            else -> {
                start
            }
        }

    init {
        skips[Direction.Down] = setOf(
            142, 145, 150, 153, 158, 161, 166, 169, 174, 177, 182, 185, 190, 193, 198, 201, 206, 209, 63, 66, 69, 72, 75, 78, 81, 86,
            89, 94, 97, 102, 105, 110, 113, 118, 121, 126, 129, 134, 137, 125, 128, 131
        )
        skips[Direction.Up] = setOf(
            156,
            153,
            148,
            145,
            213,
            210,
            207,
            204,
            201,
            196,
            193,
            188,
            185,
            180,
            177,
            172,
            169,
            164,
            161,
            140,
            137,
            132,
            129,
            124,
            121,
            116,
            113,
            108,
            105,
            100,
            97,
            92,
            89,
            84,
            81,
            76,
            73,
            68,
            65,
            211,
            208,
            205,
            202,
            199,
            141,
            138,
            135
        )
        skips[Direction.Right] = setOf(
            1,
            4,
            9,
            12,
            17,
            20,
            25,
            28,
            33,
            36,
            41,
            44,
            49,
            52,
            57,
            60,
            65,
            68,
            73,
            76,
            81,
            84,
            89,
            92,
            97,
            100,
            105,
            108,
            113,
            116,
            121,
            124,
            129,
            132,
            137,
            140,
            145,
            148,
            153,
            156,
            161,
            164,
            169,
            172,
            177,
            180,
            185,
            188,
            193,
            196,
            201,
            204,
            209,
            212,
            217,
            220,
            225,
            228,
            233,
            236,
            0,
            3,
            6,
            2,
            5,
            8,
            11,
            14,
            48,
            51,
            54
        )
        skips[Direction.Left] = setOf(
            240,
            237,
            234,
            231,
            228,
            223,
            220,
            215,
            212,
            207,
            204,
            199,
            196,
            191,
            188,
            183,
            180,
            175,
            172,
            167,
            164,
            159,
            156,
            151,
            148,
            143,
            140,
            135,
            132,
            127,
            124,
            119,
            116,
            111,
            108,
            103,
            100,
            95,
            92,
            87,
            84,
            79,
            76,
            71,
            68,
            63,
            60,
            55,
            52,
            47,
            44,
            39,
            36,
            31,
            28,
            23,
            20,
            15,
            12,
            7,
            4,
            238,
            235,
            232,
            229,
            226,
            48,
            45,
            42
        )
        initTwo()
    }

    private fun initTwo() {
//        skips[Direction.Right] = setOf(32, 35, 38, 41, 44, 49, 52, 57, 60, 65, 68, 73, 76, 81, 84, 89, 92, 97, 100, 105, 108, 113, 116, 121,
//            124, 129, 132, 137, 140, 145, 148, 153, 156, 161, 164, 169, 172, 177, 180, 185, 188, 193, 196, 201, 204)
        // from desert
        skips[Direction.Right] = setOf(1, 4, 9, 12, 17, 20, 25, 28, 33, 36, 41, 44, 49, 52, 57, 60, 65, 68, 73, 76, 81, 84, 89, 92, 97,
            100, 105, 108, 113, 116, 121, 124, 129, 132, 137, 140, 145, 148, 153, 156, 161, 164, 169, 172, 177, 180, 185, 188, 193, 196,
                201, 204, 209, 212, 217, 220, 225, 228, 233, 236)
//        skips[Direction.Left] = setOf(208, 205, 202, 199, 196, 191, 188, 183, 180, 175, 172, 167, 164, 159, 156, 151, 148, 143, 140, 135,
//            132, 127, 124, 119, 116, 111, 108, 103, 100, 95, 92, 87, 84, 79, 76, 71, 68, 63, 60, 55, 52, 47, 44, 39, 36)
        skips[Direction.Left] = setOf(240, 237, 234, 231, 228, 223, 220, 215, 212, 207, 204, 199, 196, 191, 188, 183, 180, 175, 172, 167,
            164, 159, 156, 151, 148, 143, 140, 135, 132, 127, 124, 119, 116, 111, 108, 103, 100, 95, 92, 87, 84,
            79, 76, 71, 68, 63, 60, 55, 52, 47, 44, 39, 36, 31, 28, 23, 20, 15, 12, 7, 4)
    }
}