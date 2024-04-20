package bot.state

import bot.state.map.destination.ZeldaItem
import nintaco.api.API
import kotlin.math.max

/**
 * help bot change the game memory
 */
class StateManipulator(
    private val api: API,
    private val updater: FrameStateUpdater
) {
    private val state: MapLocationState
        get() = updater.state

    fun fillTriforce() {
        api.writeCPU(Addresses.triforce, 255)
    }

    fun fillHearts() {
        api.writeCPU(Addresses.heartContainers, 0xCC) // 13 hearts all full
        api.writeCPU(Addresses.heartContainersHalf, 0xFF) // make the half heart full too
    }

    fun setRing(item: ZeldaItem) {
        val ringId = when (item) {
            ZeldaItem.BlueRing -> 1
            ZeldaItem.RedRing -> 2
            else -> 0
        }
        // doesnt change visual but affects damage
        api.writeCPU(Addresses.hasRing, ringId)
    }

    fun setLadderAndRaft(enable: Boolean) {
        api.writeCPU(Addresses.hasLadder, enable.intTrue)
        api.writeCPU(Addresses.hasRaft, enable.intTrue)
    }

    fun setBait() {
        api.writeCPU(Addresses.hasFood, 1)
    }

    fun setLetter() {
        api.writeCPU(Addresses.hasLetter, 1)
    }

    fun setArrow() {
        api.writeCPU(Addresses.hasBow, 1)
        // silver arrow of course, let's be luxurious
        api.writeCPU(Addresses.hasArrow, 2)
    }

    fun deactivateClock() {
        if (state.frameState.clockActivated) {
            api.writeCPU(Addresses.clockActivated, 0)
        }
    }

    fun setRedCandle() {
        api.writeCPU(Addresses.hasCandle, 2)
    }

    fun setMagicShield() {
        api.writeCPU(Addresses.hasShield, 1)
    }

    fun setHaveWhistle() {
        api.writeCPU(Addresses.hasWhistle, 1)
    }

    fun addKey() {
        val current = state.frameState.inventory.numKeys
        api.writeCPU(Addresses.numKeys, current + 1)
    }

    fun addRupee() {
        val current = state.frameState.inventory.numRupees
        val plus100 = max(252, current + 100)
        api.writeCPU(Addresses.numRupees, plus100)
    }

    fun setSword(item: ZeldaItem) {
        when (item) {
            ZeldaItem.WoodenSword -> api.writeCPU(Addresses.hasSword, 1)
            ZeldaItem.WhiteSword -> api.writeCPU(Addresses.hasSword, 2)
            ZeldaItem.MagicSword -> api.writeCPU(Addresses.hasSword, 3)
            else -> {
                api.writeCPU(Addresses.hasSword, 0)
            }
        }
    }
}