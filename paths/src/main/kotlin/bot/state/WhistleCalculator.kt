package bot.state

import nintaco.api.API

class WhistleCalculator(private val api: API) {
    val fluteTimer by lazy {
        api.readCPU(Addresses.fluteTimer) and 0xFF
    }

    val isFlutePlaying: Boolean by lazy {
        fluteTimer != 0
    }

    val fluteWasUsed: Boolean by lazy {
        api.readCPU(Addresses.usedFlute) and 0xFF != 0
    }

    fun fluteWasUsedAndDone(): Boolean = fluteWasUsed && !isFlutePlaying
}