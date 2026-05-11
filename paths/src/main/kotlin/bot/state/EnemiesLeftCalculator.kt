package bot.state

import nintaco.api.API
import util.d

class EnemiesLeftCalculator(api: API) {
    val roomKillCount by lazy { api.readCPU(Addresses.More.objectTypeLinkAndRoomKillCounter) and 0xFF }
    val roomObjCount by lazy { api.readCPU(Addresses.More.roomObjectCount) and 0xFF }

    /**
     * @param tolerance leave this many enemies alive and still mark the room as cleared
     * it should include the number of bubbles and zoras, for example
     */
    fun allEnemiesDead(tolerance: Int): Boolean {
        d { " roomKillCount $roomKillCount roomObjCount $roomObjCount"}
        return roomObjCount > 0 && roomKillCount + tolerance >= roomObjCount
    }
}