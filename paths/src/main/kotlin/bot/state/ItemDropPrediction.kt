package bot.state

import nintaco.api.API
import nintaco.api.ApiSource

class ItemDropPrediction(
    private val api: API = ApiSource.getAPI(),
) {
    val killCt: Int by lazy { api.readCPU(Addresses.enemiesKilledCount) }

    // see https://www.zeldaspeedruns.com/loz/generalknowledge/item-drops-chart
    private val bombDropCounts = setOf(0, 5, 7)

    // cheap hack is, outside, color is blue, it can give a bomb

    fun bombsLikely(): Boolean {
        // monster must be of type B
        val isTypeB = true

        return isTypeB && (killCt in bombDropCounts)
    }
}