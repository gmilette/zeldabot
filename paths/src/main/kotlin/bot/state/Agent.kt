package bot.state

import bot.state.map.Direction
import bot.state.oam.LinkDirectionFinder
import bot.state.oam.MonsterDirection
import bot.state.oam.SpriteData

val List<Agent>.points: List<FramePoint>
    get() = this.map { it.point }

val emptyAgent = Agent(0, FramePoint(0, 0), Direction.Down, EnemyState.Unknown,  0)
data class Agent(
    val index: Int = 0,
    val point: FramePoint,
    val dir: Direction = Direction.None,
    val state: EnemyState = EnemyState.Unknown,
    val tile: Int = 0,
    val attribute: Int = 0,
    val tileByte: String = tile.toString(16),
    val attributeByte: String = attribute.toString(16)
) {

    val damaged: Boolean = tile in LinkDirectionFinder.damagedAttribute || tile in MonsterDirection.damagedAttribute

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
    Projectile,
}