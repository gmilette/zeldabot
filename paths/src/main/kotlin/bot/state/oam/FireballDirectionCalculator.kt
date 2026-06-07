package bot.state.oam

import bot.state.Addresses
import nintaco.api.API

class FireballDirectionCalculator(private val api: API) {
    fun direction(slot: Int): FireballDirection {
        val dirX = api.readCPU(Addresses.fireballDirX + slot) and 0xFF
        val dirY = api.readCPU(Addresses.fireballDirY + slot) and 0xFF
        return FireballDirection.fromBitmask(dirX or dirY)
    }
}

enum class FireballDirection(val bitmask: Int) {
    RIGHT     (0x01),
    LEFT      (0x02),
    DOWN      (0x04),
    UP        (0x08),
    RIGHT_DOWN(0x05),
    LEFT_DOWN (0x06),
    RIGHT_UP  (0x09),
    LEFT_UP   (0x0A),
    NONE      (0x00);

    companion object {
        private val priority = listOf(RIGHT, LEFT, DOWN, UP)

        fun fromBitmask(value: Int): FireballDirection =
            entries.firstOrNull { it != NONE && it.bitmask == value } ?: NONE
    }
}