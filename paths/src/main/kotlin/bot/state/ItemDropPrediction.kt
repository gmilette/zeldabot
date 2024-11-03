package bot.state

import bot.state.oam.*
import nintaco.api.API
import nintaco.api.ApiSource

class ItemDropPrediction(
    private val api: API = ApiSource.getAPI(),
) {
    val killCt: Int by lazy { api.readCPU(Addresses.enemiesKilledCount) }

    // see https://www.zeldaspeedruns.com/loz/generalknowledge/item-drops-chart
    private val bombDropCounts = setOf(0, 5, 7)

    // depends on the type indeed
    private val heartDropCounts = setOf(0)

    // type D
    private val fairyDropCounts = setOf(1, 4)

    // type C
    private val coinDropCounts = setOf(3, 9)

    // cheap hack is, outside, color is blue, it can give a bomb

    fun bombsLikely(): Boolean {
        // monster must be of type B
        val isTypeB = true

        return isTypeB && (killCt in bombDropCounts)
    }

    fun fairyLikely(): Boolean {
        // monster must be of type B
        val isTypeD = true

        return isTypeD && (killCt in fairyDropCounts)
    }

    fun heartLikely(): Boolean {
        // monster must be of type B
        val isTypeD = true

        return isTypeD && (killCt in heartDropCounts)
    }

    fun bluecoinLikely(): Boolean {
        val isTypeC = true

        return isTypeC && (killCt in coinDropCounts)
    }

    fun whatLikely(): String =
        when {
            bombsLikely() -> "b"
//            fairyLikely() -> "f"
//            heartLikely() -> "h"
//            bluecoinLikely() -> "c"
            else -> ""
    }

    val coin = 0x01
    // draw the type on target enemy instead of dot?
    val typeDSequence = listOf(heart, fairy, coin, heart, fairy, heart, heart, heart, coin, heart)
    val typeCSequence = listOf(coin, heart, coin, bigCoin, heart, clockTile, coin, coin, coin, bigCoin)
    val typeBSequence = listOf(bomb, coin, clockTile, coin, heart, bomb, coin, bomb, heart, heart)
    val typeASequence = listOf(coin, heart, coin, fairy, coin, heart, heart, coin, coin, heart)
}