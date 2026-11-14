package bot.state

import nintaco.api.API

class LikeLikeLogic(private val api: API) {
    val holding by lazy { isLikeLikeHolding()}

    fun isLikeLikeHolding(): Boolean =
        api.readCPU(Addresses.stunnedByLikeLike) and 0xFF != 0

    fun isAnyLikeLikeHolding(): Boolean =
        (1..11).any { slot ->
            api.readCPU(0x034F + slot) and 0xFF == 0x17 &&
                    api.readCPU(0x042C + slot) and 0xFF != 0
        }

    fun framesUntilShieldEaten(api: API, slot: Int): Int {
        val t = api.readCPU(0x042C + slot) and 0xFF
        return if (t >= 0x60) 0 else 0x60 - t
    }
}