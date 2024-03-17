package bot.state

import bot.state.map.Direction
import bot.state.map.MovingDirection
import bot.state.oam.EnemyGroup
import bot.state.oam.TileAttribute
import util.CalculateDirection

val List<Agent>.points: List<FramePoint>
    get() = this.map { it.point }

val emptyAgent = Agent(0, FramePoint(0, 0), Direction.Down, EnemyState.Unknown, 0)

data class Agent(
    val index: Int = 0,
    val point: FramePoint,
    val dir: Direction = Direction.None,
    val state: EnemyState = EnemyState.Unknown,
    val tile: Int = 0,
    val attribute: Int = 0,
    val tileByte: String = tile.toString(16),
    val attributeByte: String = attribute.toString(16),
    val damaged: Boolean = false,
    val blockable: Blockable = Blockable.No,
    val moving: MovingDirection = MovingDirection.UNKNOWN_OR_STATIONARY
) {
    val tileAttrib = TileAttribute(tile, attribute)

    val damagedString: String = if (damaged) "*D*" else ""

    val isLoot: Boolean
        get() = state == EnemyState.Loot

    val topCenter = FramePoint(point.x + 8, point.y + 0)
    val x: Int
        get() = point.x

    val y: Int
        get() = point.y
}

enum class EnemyState {
    Unknown,
    Alive,
    Dead,
    Loot,
    Projectile
}

sealed class EnemyStates {
    object Unknown : EnemyStates()
    object Alive : EnemyStates()
    object Dead : EnemyStates()
    object Loot : EnemyStates()
    sealed class Projectile(val blockable: Boolean, val magicShieldBlockable: Boolean) : EnemyStates() {
        object Unblockable : Projectile(blockable = false, magicShieldBlockable = false)
        object Blockable : Projectile(blockable = true, magicShieldBlockable = false)
        object BlockableWithMagicShield : Projectile(blockable = true, magicShieldBlockable = true)
    }
}

sealed class Blockable(val blockable: Boolean, val requiresMagicShield: Boolean) {
    object No : Blockable(blockable = false, requiresMagicShield = false)
    object WithSmallShield : Blockable(blockable = true, requiresMagicShield = false)
    object WithMagicShield : Blockable(blockable = true, requiresMagicShield = true)
}
