package bot.state.oam

import bot.state.map.Direction
import util.d

/**
 * figure out direction link is facing
 */
object LinkDirectionFinder {
    val damagedAttribute = setOf(41, 42, 43)

    private val down = setOf(0x16, 0x14, 0x5A, 0x08, 0x5A, 0x58, 0x0A, 0x60) //5A, attribute 00 is down. 60, attribute 00, 08 attribute 40
    private val up = setOf(0x0C, 0x0E, 0x18, 0x1A) //43 was hit (dark color), //41 was hit light color
    //42 is hit
    private val right = setOf((0x02).toInt(), (0x00).toInt(), (0x06).toInt(), (0x04).toInt()) // attribute 00, could be 02 with attrib 00
    private val left = setOf((0x02).toInt(), (0x00).toInt(), (0x10).toInt(), (0x12).toInt()) // attribute 40 or 43

    private val downInt = down.map { it.toInt() }
    private val upInt = up.map { it.toInt() }
    private val rightInt = right.map { it.toInt() }
    private val leftInt = left.map { it.toInt() }

    data class DirectionDamage(val direction: Direction, val damaged: Boolean)

    fun direction(sprites: List<SpriteData>): DirectionDamage {
        val linkMatch = sprites.firstOrNull { it.toDir() != Direction.None }
        val dir = linkMatch?.toDir()
        val isDamaged = damagedAttribute.contains(linkMatch?.attribute)
        d { "link match $linkMatch $dir damaged $isDamaged"}
//        if (linkMatch == null) {
//            d { " sprites link!" }
//            sprites.forEachIndexed { index, sprite ->
//                d { "$index: $sprite ${LinkDirection.dirFor(sprite)}" }
//            }
//        }
        return DirectionDamage((dir ?: Direction.None), isDamaged)
    }

    fun dirFor(data: SpriteData): Direction = data.toDir()

    private fun SpriteData.toDir(): Direction =
        when {
            downInt.contains(tile) && attribute == 0 -> Direction.Down
            upInt.contains(tile) -> Direction.Up
            leftInt.contains(tile) ||
                    rightInt.contains(tile) -> {
                if (attribute == 40 || attribute == 64 || attribute == 43) Direction.Left else Direction.Right
            }
            else -> Direction.None
//                .also {
//                d { " tile is null $tile ${leftInt} ${rightInt} $downInt $upInt"}
//            }
        }
}