package bot.state

import nintaco.api.API
import util.d

class EnemiesLeftCalculator(api: API) {
    val roomKillCount by lazy { api.readCPU(Addresses.More.objectTypeLinkAndRoomKillCounter) and 0xFF }
    val roomObjCount by lazy { api.readCPU(Addresses.More.roomObjectCount) and 0xFF }

    /**
     * Note: does not work for Lamnola and a few other enemies. use [allDead] unless you want to
     * take into account tolerance
     * @param tolerance leave this many enemies alive and still mark the room as cleared
     * it should include the number of bubbles and zoras, for example
     */
    fun allEnemiesDead(tolerance: Int): Boolean {
        d { " roomKillCount $roomKillCount roomObjCount $roomObjCount tolerance $tolerance"}
        return roomObjCount > 0 && roomKillCount + tolerance >= roomObjCount
    }

    private val roomAllDead by lazy {
        api.readCPU(Addresses.More.allEnemiesDead) and 0xFF
    }

    val allDead = roomAllDead != 0
}