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

    fun add(enemyPoint: List<FramePoint>) {
        enemyLocationHistory.add(enemyPoint)
        if (enemyLocationHistory.size > 75) {
            enemyLocationHistory.removeFirst()
        }
    }

    fun addData(enemyData: List<AgentData>) {
        if (history.isEmpty()) {
            initialData = enemyData
        }
        history.add(enemyData)
        if (history.size > 75) {
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
//        val wasAlive = hpChanged || animChanged || ptChanged || countdownChanged || presenceChanged
        val wasAlive = ptChanged || presenceChanged || animChanged || countdownChanged
//        d { "$wasAlive hp $hpChanged an $animChanged pt $ptChanged cd $countdownChanged pr $presenceChanged st: $statusChanged last hp " +
//                "${last.hp} dropped " +
//                "${last.droppedId} hasM ${enemyHasMoved[index]}" +
//                " wasAlive $wasAlive" }

        enemyHasMoved[index] = wasAlive

        // write this to a special log so I can see the history

//        Unknown,
//        Alive,
//        Dead, // was alive before but now is dead
//        NotSeen,
//        Loot,
//        Projectile
        return when {
            // nope it is a bat
//            last.droppedId == 16 -> EnemyState.Projectile
            last.droppedId == 128 || last.droppedId == 66 -> EnemyState.Projectile
            // it's not a bat
            last.droppedId != 2 && last.droppedId != 16 && last.droppedId != 3 && last.droppedId > 0 -> EnemyState.Loot // maybe track
            // the initial value only
            // for sure it is dead
            enemyHasMoved[index] && (last.hp == 128) -> EnemyState.Dead
            // it's not moving any more so it is probably dead
            // my 5 measurements should show some change if the thing is still alive
            enemyHasMoved[index] && !wasAlive -> EnemyState.Dead
            // if it was loot dont make it alive again
            wasAlive && !wasLoot[index] -> EnemyState.Alive
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