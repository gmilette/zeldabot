package bot.state

val emptyAgent = Agent(FramePoint(0, 0), Dir.Right, EnemyState.Unknown,  0,0)
data class Agent(
    val point: FramePoint,
    val dir: Dir,
    val state: EnemyState = EnemyState.Unknown,
    val countDown: Int = 0,
    val hp: Int = 0,
) {
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
    val dir: Dir,
    val countDown: Int = 0,
    val status: Int = 0,
    val hp: Int = 0,
    val velocity: Int = 0,
    val animation: Int = 0,
    val presence: Int = 0,
    val droppedId: Int = 0,
    val droppedEnemyItem: Int = 0,
    val droppedItem: DroppedItem = DroppedItem.Unknown
)
//val xStatus = api.readCPU(Addresses.enemyStatus[i])
//val xHp = api.readCPU(Addresses.enemyHp[i])
//val xVel = api.readCPU(Addresses.velocity[i])
//// check out all these indicators of aliveness, and if you see it change
//// the guy is still alive, as soon as it doesn't change at all
//// it's dead
//val xAnimation = api.readCPU(Addresses.enemyAnimationOnOff[i])
//val xPresence = api.readCPU(Addresses.enemyPresence[i])
//
//// type 15: i think it's the clock
////  dropped item type 24 i think it's a bomb
//val droppedId = api.readCPU(Addresses.dropItemType[i])
//val droppedEnemyItem = api.readCPU(Addresses.dropEnemyItem[i])
//val dropped = droppedItemMap(droppedId)
//// if this changes, it is an indication that the enemy was alive
//val countDown = api.readCPU(Addresses.enemyCountdowns[i])