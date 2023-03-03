package bot.state

import util.d

/**
 * try to figure out if the enemy is alive or dead
 * or if it is a projectile or not
 */
class EnemyStateReasoner {
    // store some memory of what was happening
    var enemyStateHistory: List<List<EnemyState>> = listOf()

    var history: MutableList<List<AgentData>> = mutableListOf()
    var initialData: List<AgentData> = emptyList()

    // maybe just keep a history of enemyData which has the point and other stuff

    val enemyLocationHistory: MutableList<List<FramePoint>> = mutableListOf()
    val enemyPresenceHistory: MutableList<List<FramePoint>> = mutableListOf()
    var enemyHasMoved: MutableList<Boolean> = mutableListOf()
    var wasLoot: MutableList<Boolean> = mutableListOf()
    var wasEverProjectile: MutableList<Boolean> = mutableListOf()

    // need this high to kill the rhino
    // but if it is stoo high, link just keeps attacking
    private val histSize = 125

    fun add(enemyPoint: List<FramePoint>) {
        enemyLocationHistory.add(enemyPoint)
        if (enemyLocationHistory.size > histSize) {
            enemyLocationHistory.removeFirst()
        }
    }

    fun addData(enemyData: List<AgentData>) {
        if (history.isEmpty()) {
            initialData = enemyData
        }
        history.add(enemyData)
        if (history.size > histSize) {
            history.removeFirst()
        }
    }

    fun clear() {
        history.clear()
        enemyLocationHistory.clear()
        wasLoot = mutableListOf(
                false, false, false, false,
        false, false, false, false,
        false, false, false, false
        )
        // 8 false
        enemyHasMoved = mutableListOf(
            false, false, false, false,
            false, false, false, false,
            false, false, false, false
        )
        wasEverProjectile = mutableListOf(
            false, false, false, false,
            false, false, false, false,
            false, false, false, false
        )
    }

    val numEnemiesSeen: Int
        get() = enemyHasMoved.count { it }

    fun latest(index: Int): AgentData =
        history.last()[index]

    fun makeState(index: Int): EnemyState {
        if (history.isEmpty()) {
            return EnemyState.Unknown
        }

        val first = history.first()[index]
        val last = history.last()[index]

        val lastDropped = history.takeLast(10).map { it[index] }
        val numDistinct = lastDropped.distinct()
        // all distinct
        val isAliveBecauseDroppedId = numDistinct.size == lastDropped.size && numDistinct.size > 5
        if (isAliveBecauseDroppedId) {
            d { " $index Alive because dropped id"}
        }
//        val last3 = history.takeLast(4)
//        var prev: AgentData? = null
//        var allSame = true
//        for (agentData in last3[index]) {
//            prev?.let { prevAgent ->
//                if (prevAgent.droppedId != agentData.droppedId) {
//                    allSame = false
//                }
//            }
//            prev = agentData
//        }
//        val isAliveBecauseDroppedId = !allSame

        //all same
        val ptChanged = history.any {
            it[index].point != first.point
        }
        // only check last 10 or so?
        val animChanged = history.any {
            it[index].animation != first.animation
        }
        val presenceChanged = history.any {
            it[index].presence != first.presence
        }
        val countdownChanged = history.any {
            it[index].countDown != first.countDown
        }
        val hpChanged = history.any {
            it[index].hp == first.hp
        }
        val statusChanged = history.any {
            it[index].status == first.status
        }
        val wasProjectile = history.any {
            it[index].projectileState == ProjectileState.Moving
        }
//        val wasAlive = hpChanged || animChanged || ptChanged || countdownChanged || presenceChanged
        val wasAlive = ptChanged || presenceChanged || animChanged || countdownChanged || isAliveBecauseDroppedId
        // never set it back to false
        if (wasProjectile) {
            wasEverProjectile[index] = true
        }

        val DEBUG = false
        if (DEBUG) {
            d {
                "$wasAlive hp $hpChanged an $animChanged pt $ptChanged cd $countdownChanged pr $presenceChanged st: $statusChanged last hp " +
                        "${last.hp} dropped " +
                        "${last.droppedId} hasM ${enemyHasMoved[index]}" +
                        " wasAlive $wasAlive" +
                        " was loot ${wasLoot[index]}" +
                        " proj state ${last.projectileState}" +
                        " was projectile ${wasProjectile} " +
                        " was ever ${wasEverProjectile[index]} " +
                        " isAliveBecauseDroppedId $isAliveBecauseDroppedId"
            }
        }

        // i dont think we want to change it
        if (wasAlive) {
            enemyHasMoved[index] = wasAlive
        }

                // write this to a special log so I can see the history

//        Unknown,
//        Alive,
//        Dead, // was alive before but now is dead
//        NotSeen,
//        Loot,
//        Projectile
        return when {
            // nope it is a bat
            // it is definately a projectile but also a bat
            // is it a bat too?
            //last.droppedId == 16 && last.isProjectileSlot -> EnemyState.Projectile
            // works, but not always because projectile slot is used by bats probably
//            last.isProjectileSlot -> EnemyState.Projectile
            // TODO: projectiles can be offscreen, maybe that matters?
//            last.projectileState == ProjectileState.Moving -> EnemyState.Projectile

            // this was here, important for ghosts
            isAliveBecauseDroppedId && !wasEverProjectile[index] -> EnemyState.Alive
            // it could have changed to Gone
            // this was the original
//            wasProjectile && wasEverProjectile[index] -> EnemyState.Projectile
            // looks like alive because it finds an id of 0
            wasProjectile && wasEverProjectile[index] -> EnemyState.Projectile
            // changing dropped id means, its alive
            // do this before checking for loot
            //last.droppedId == 128 || last.droppedId == 66 -> EnemyState.Projectile
            // it's not a bat // 1 is for the
            // 2 might be the traps
            //|| last.droppedId == 2
            // 2 might also mean ladder is deployed, i saw the ladder out and
            // had a dropped item 2
            // then when I stopped ladder deployment, it was no drapped item 2
            // 34 is a heart
            last.droppedId != 1 && last.droppedId != 2 && last.droppedId != 16 && last.droppedId != 3 && last.droppedId != 128 && last
                .droppedId > 0 -> {
                if (last.droppedId == 34) {
                    d { " FOUND LOOT heart"}
                }
//                d { " FOUND LOOT ${last.droppedId}"}
                EnemyState.Loot
            } // maybe track
            // the initial value only
            // for sure it is dead
            enemyHasMoved[index] && (last.hp == 128) -> EnemyState.Dead
            // it's not moving any more so it is probably dead
            // my 5 measurements should show some change if the thing is still alive
            enemyHasMoved[index] && !wasAlive -> EnemyState.Dead
            // if it was loot dont make it alive again
            wasAlive && !wasLoot[index] && !wasEverProjectile[index] -> EnemyState.Alive
//            wasAlive -> EnemyState.Alive
//            wasAlive && countdownChanged -> EnemyState.Alive
            else -> EnemyState.NotSeen
        }.also {
            if (it == EnemyState.Loot && wasAlive(index)) {
                // if it was loot done mark it as alive again
                wasLoot[index] = true
            }
        }
    }

    fun log() {
        for (i in 0..9) {
            d { " **Agent $i**"}
            d { "i: ${initialData.get(i)}" }
            history.forEachIndexed { a, agents ->
                d { "$a ${agents.get(i)}"}
            }
        }
    }

    fun enemyState(index: Int): EnemyState {
        if (enemyLocationHistory.isEmpty()) {
            return EnemyState.Unknown
        }
        val first = enemyLocationHistory.first()[index]
        val same = enemyLocationHistory.all {
            it[index].x == first.x && it[index].y == first.y
        }

        // never set it back to false
        if (!same) {
            enemyHasMoved[index] = true
        }

        return when {
            same -> EnemyState.Dead
            else -> EnemyState.Alive
        }
    }

    fun wasAlive(index: Int): Boolean = enemyHasMoved[index]

 // compute isAlive and wasAlive

    // track various histories of values to determine it

    // also is it a projectile or an ememy, this matters
    // we want to avoid projectiles, like an enemy, but we dont
    // want link to swing at it! duh! that would make him look unprofessional
}