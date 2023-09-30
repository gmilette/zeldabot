package sequence.findpaths

import bot.state.map.destination.DestType

object ZeldaDestinations {
    val levels = listOf(
        // or 199
        Destination(
            Point(37, 119, ZeldaMapCode.Cave.code),
            DestType.Level(1)
        ),
        Destination(Point(37, 199, ZeldaMapCode.Cave.code), DestType.Level(2)),
        Destination(Point(81, 72, ZeldaMapCode.Cave.code), DestType.Level(3)),
        Destination(Point(48, 88, ZeldaMapCode.Cave.code), DestType.Level(4)),
        Destination(Point(4, 183, ZeldaMapCode.Cave.code), DestType.Level(5)),
        Destination(Point(26, 39, ZeldaMapCode.Cave.code), DestType.Level(6)),
        Destination(Point(50, 39, ZeldaMapCode.Cave.code), DestType.Level(7)),
        Destination(Point(68, 217, ZeldaMapCode.Cave.code), DestType.Level(8)
        ), // to left, but i guess it could be to right too
    )

    // potion shop
    // 67 71 bt SPOT

    val level9 = Destination(Point(7, 91, "02"), DestType.Level(9))

    fun level(number: Int) = levels[number-1]

    val leftShopNotSure = Destination(Point(78, 11, "02"), DestType.Shop())

    val start = Destination(Point(82, 114, "02"), DestType.Level(9))
    val near = Destination(Point(82, 116, "02"), DestType.Level(9))
    val near1 = Destination(Point(50, 127, "02"), DestType.Level(9))

//    val start = Destination(Point( 114, 82,"02"), DestType.LEVEL(9))
//    val near = Destination(Point(116,82,  "02"), DestType.LEVEL(9))
//    val near1 = Destination(Point( 127, 50,"02"), DestType.LEVEL(9))

    val spots = Destination(Point(67, 71, "12"), DestType.Level(9))

    val spots2 = Destination(Point(56, 71, "12"), DestType.Level(9))


    val all = levels
}