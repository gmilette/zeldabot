package bot.state

import bot.state.map.Direction

val emptyAgent = Agent(0, FramePoint(0, 0), Direction.Down, EnemyState.Unknown,  0,0)
data class Agent(
    val index: Int = 0,
    val point: FramePoint,
    val dir: Direction = Direction.None,
    val state: EnemyState = EnemyState.Unknown,
    val countDown: Int = 0,
    val hp: Int = 0,
    val projectileState: ProjectileState = ProjectileState.NotProjectile,
    val droppedId: Int = 0
) {
    val tile = hp
    val attribute = droppedId

    val damaged: Boolean = tile in LinkDirection.damagedAttribute || tile in MonsterDirection.damagedAttribute

    val damagedString: String = if (damaged) "*D*" else ""

    val isLoot: Boolean
        get() = state == EnemyState.Loot

    val topCenter = FramePoint(point.x + 8, point.y + 0)
    val x: Int
        get() = point.x

    val y: Int
        get() = point.y
}

// everything I can track about the enemy so far
data class AgentData(
    val point: FramePoint,
    val dir: Direction,
    val countDown: Int = 0,
    val status: Int = 0,
    val hp: Int = 0,
    val velocity: Int = 0,
    val animation: Int = 0,
    val presence: Int = 0,
    val droppedId: Int = 0,
    val droppedEnemyItem: Int = 0,
    val droppedItem: DroppedItem = DroppedItem.Unknown,
    val projectileState: ProjectileState = ProjectileState.NotProjectile
)
