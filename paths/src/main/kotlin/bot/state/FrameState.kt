package bot.state

import bot.GamePad
import bot.state.map.Direction
import bot.state.map.MapConstants
import sequence.ZeldaItem
import sequence.zeldaItemsRequired
import util.d
import kotlin.math.abs

typealias MapLoc = Int

enum class EnemyState {
    // might be alive or dead
    Unknown,
    Alive,
    Dead, // was alive before but now is dead
//    ProbablyDead,
    NotSeen,
    Loot,
    Projectile,
    InactiveProjectile
}

enum class Dir {
    Up, Down, Left, Right, Unknown
}

data class FrameState(
    /**
     *  0=Title/transitory    1=Selection Screen
    5=Normal              6=Preparing Scroll
    7=Scrolling           4=Finishing Scroll;
    E=Registration        F=Elimination
     */
    val gameMode: Int = 5,
    val enemies: List<Agent> = emptyList(),
    val killedEnemyCount: Int = 0,
    val link: Agent = Agent(0, FramePoint(0, 0), Dir.Right),
//    val link: FramePoint = Undefined,
//    val linkDir: Dir = Dir.Unknown,
//    val enemies: List<FramePoint> = emptyList(),
//    val ememyState: List<EnemyState> = emptyList(),
//    val ememyCountdowns: List<Int> = emptyList(),
//    val enemyDirs: List<Dir> = emptyList(),
    val subPoint: FramePoint = Undefined, // might not be frame point
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
    val mapLoc: MapLoc = 119,
    // check this, and if it exists, then go to the enemy x,y location to
    // collect it
//    val droppedItems: List<Int> = emptyList(),
    // has to have all the locations on the map
//    val map.
    /**
     * if the enemy has dropped the dungeon item, like the skeleton holding the key
     */
    val inventory: Inventory = Inventory(0, 0, 0, 0, emptySet()),
    val hasDungeonItem: Boolean = false,
    val tenth: Int = 0,
    val level: Int = 0,
    val clockActivated: Boolean,
    val swordUseCountdown: Int
) {
    val canUseSword: Boolean = swordUseCountdown == 0
    val isScrolling: Boolean
        get() = gameMode == 7 || gameMode == 6 || gameMode == 4

    val isDoneScrolling: Boolean
        get() = gameMode == 4

    fun enemiesClosestToLink(stateOfEnemy: EnemyState = EnemyState.Alive): List<Agent> {
        return enemies.filter {
            it.state == stateOfEnemy
        }.sortedBy {
            it.point.distTo(link.point)
        }
    }

    fun logEnemies() {
        d { " remaining enemies: ${enemies.size}" }
        for (enemy in enemiesSorted) {
            d { " remaining enemy $enemy" }
        }
    }

    val enemiesSorted: List<Agent>
        get() = enemies.sortedBy { it.point.distTo(link.point) }
}

data class Inventory(
    val selectedItem: Int,
    val numBombs: Int,
    val numKeys: Int,
    val numRupees: Int,
    val items: Set<ZeldaItem>
) {
    val percentComplete: Float
        get() = items.size.toFloat() / zeldaItemsRequired.size.toFloat()

    val missingItems: Set<ZeldaItem>
        get() = zeldaItemsRequired - items

    val acquiredItems: Set<ZeldaItem>
        get() = zeldaItemsRequired.intersect(items)

    val hasCandle: Boolean
        get() = items.contains(ZeldaItem.BlueCandle) || items.contains((ZeldaItem.RedCandle))

    object Selected {
        val boomerang = 0
        val bomb = 1
        val arrow = 2
        val candle = 4
        val whistle = 5
        val bait = 6 //?
        val potionletter = 7
        val wand = 8 //?
    }
}

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


fun MapLocFromPoint(x: Int, y: Int): MapLoc =
    x + 16 * y

data class MapCellPoint(val x: Int, val y: Int) {
    override fun toString(): String {
        return "$x, $y"
    }
}

fun MapCellPoint.toFrame() = FramePoint(this.x, this.y)

data class FramePoint(val x: Int = 0, val y: Int = 0, val direction: Direction? = null) : Graph.Vertex {
    constructor(x: Int = 0, y: Int = 0) : this(x, y, null)

    override fun equals(other: Any?): Boolean {
        return if (other is FramePoint) {
            other.x == x && other.y == y
        } else false
    }

    val oneStr: String
        get() = "${x}_$y"

    override fun toString(): String {
        return "($x, $y)"
    }
}

val FramePoint.toG: FramePoint
    get() = FramePoint(x / 16, y / 16)

fun FramePoint.addDirection(dir: Direction) =
    this.copy(direction = dir)

fun FramePoint.directionTo(to: FramePoint): GamePad {
    return when {
        x == to.x -> {
            if (y < to.y) GamePad.MoveDown else GamePad.MoveUp
        }

        y == to.y -> {
            if (x < to.x) GamePad.MoveRight else GamePad.MoveLeft
        }

        else -> GamePad.MoveLeft
    }
}

fun FramePoint.dirTo(to: FramePoint): Direction {
    return when {
        x == to.x -> {
            if (y < to.y) Direction.Down else Direction.Up
        }

        y == to.y -> {
            if (x < to.x) Direction.Right else Direction.Left
        }

        else -> Direction.Down
    }
}

fun FramePoint.adjustBy(pad: GamePad) =
    when (pad) {
        GamePad.MoveUp -> this.up
        GamePad.MoveDown -> this.down
        GamePad.MoveRight -> this.right
        GamePad.MoveLeft -> this.left
        else -> this
    }

val FramePoint.toScreenY
    get() = FramePoint(x, y + 61)

val FramePoint.isTop
    get() = y == 0
val FramePoint.isBottom
    get() = y == MapConstants.MAX_Y
val FramePoint.isRight
    get() = x == MapConstants.MAX_X
val FramePoint.isLeft
    get() = x == 0

val FramePoint.justMid
    get() = FramePoint(x, y + 8)
val FramePoint.justMidEnd
    get() = FramePoint(x + 15, y + 8)

val FramePoint.upEnd
    get() = FramePoint(x, y + 8 - 1)
val FramePoint.upEndRight
    get() = FramePoint(x + 16, y + 8 - 1)
val FramePoint.upOneGrid
    get() = FramePoint(x, y - 16)
val FramePoint.up
    get() = FramePoint(x, y - 1)
val FramePoint.up2
    get() = FramePoint(x, y - 2)
val FramePoint.down
    get() = FramePoint(x, y + 1)
val FramePoint.down7
    get() = FramePoint(x, y + 7)
val FramePoint.down2
    get() = FramePoint(x, y + 2)
val FramePoint.downEnd
    get() = FramePoint(x, y + 16 + 1)
val FramePoint.downEndRight
    get() = FramePoint(x + 16, y + 16 + 1) // extra 1 just like downEnd
val FramePoint.right
    get() = FramePoint(x + 1, y)
val FramePoint.right2
    get() = FramePoint(x + 2, y)

val FramePoint.rightEnd
    get() = FramePoint(x + 16 + 1, y)
val FramePoint.rightEndDown
    get() = FramePoint(x + 16 + 1, y + 15) // why is this 15????

val FramePoint.left
    get() = FramePoint(x - 1, y)
val FramePoint.left2
    get() = FramePoint(x - 2, y)
val FramePoint.leftDown
    get() = FramePoint(x - 1, y + 15)


val FramePoint.justLeftDown
    get() = FramePoint(x, y + 15)
val FramePoint.justRightEnd
    get() = FramePoint(x + 15, y)
val FramePoint.justRightEndBottom
    get() = FramePoint(x + 15, y + 15)
val FramePoint.justLeftBottom
    get() = FramePoint(x, y + 15)


fun FramePoint.distTo(other: FramePoint) =
    abs(x - other.x) + abs(y - other.y)

fun FramePoint.minDistToAny(other: List<FramePoint>) =
    other.minOf { this.distTo(it) }

val Undefined = FramePoint(0, 0)

