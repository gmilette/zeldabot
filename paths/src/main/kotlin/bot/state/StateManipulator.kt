package bot.state

import bot.state.map.destination.ZeldaItem
import nintaco.api.API
import util.d
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

    fun setBombs(num: Int) {
        api.writeCPU(Addresses.numBombs, num)
    }

    fun setHearts(num: Int) {
//        val h = (num + 1) + (num + 1) * 16
        d { "set hearts $num" }
//        api.writeCPU(Addresses.heartContainers, 48 + 3) //32 + 2
        api.writeCPU(Addresses.heartContainers, (16 * (num-1)) + (num-1)) //32 + 2
        api.writeCPU(Addresses.heartContainersHalf, 0xFF) // make the half heart full too
    }

    fun fillHeartsToFull() {
        val forFull = state.frameState.inventory.heartCalc.makeHeartsFull()
        api.writeCPU(Addresses.heartContainers, forFull)
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
//        setTunic(item)
    }

    // this is still incorrect
    fun setTunic(item: ZeldaItem) {
        val color = when (item) {
            ZeldaItem.BlueRing -> 22
            ZeldaItem.RedRing -> 50
            else -> 41
        }
        api.writeCPU(Addresses.tunicColor, color)
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

    fun setLetterShownToWoman() {
        api.writeCPU(Addresses.hasLetter, 2)
    }

    fun setArrow() {
        api.writeCPU(Addresses.hasBow, 1)
        // silver arrow of course, let's be luxurious
        // let's not
//        api.writeCPU(Addresses.hasArrow, 2) // silver
        api.writeCPU(Addresses.hasArrow, 1)
    }

    fun setMagicArrow() {
        api.writeCPU(Addresses.hasBow, 1)
        // silver arrow of course, let's be luxurious
        // let's not
        api.writeCPU(Addresses.hasArrow, 2) // silver
    }

    fun setMagicKey() {
        api.writeCPU(Addresses.hasMagicKey, 1)
    }

    fun deactivateClock() {
        if (state.frameState.clockActivated) {
            api.writeCPU(Addresses.clockActivated, 0)
        }
    }

    fun setRedCandle() {
        api.writeCPU(Addresses.hasCandle, 2)
    }

    fun setClock() {
        api.writeCPU(Addresses.clockActivated, 1)
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

    fun addBomb() {
        setBombs(8)
    }

    fun setKeys(num: Int) {
        api.writeCPU(Addresses.numKeys, num)
    }

fun setTriforceAll() {
    // Write to the CPU
    // 15 = 0b1111 (4 pieces)
    // 31 = 0b11111 (5 pieces)
    // 255 = 0b11111111 (8 pieces, full triforce)

    api.writeCPU(Addresses.triforce, 0xFF) // 255 in hexadecimal
}
    fun setPotion(add: Boolean) {
        // if you have potion you have letter too
        if (add) {
            setLetterShownToWoman()
        }
        // set to both potions
        api.writeCPU(Addresses.hasPotion, if (add) 1 else 0)
    }

    fun setBoomerang(item: ZeldaItem) {
        d { " set boomerang $item"}
        when (item) {
            ZeldaItem.MagicalBoomerang -> api.writeCPU(Addresses.hasMagicBoomerang, 1)
            ZeldaItem.Boomerang -> api.writeCPU(Addresses.hasBoomerang, 1)
            else -> {
                api.writeCPU(Addresses.hasMagicBoomerang, 0)
                api.writeCPU(Addresses.hasBoomerang, 0)
            }
        }

    }

    fun setWandNoBook() {
        api.writeCPU(Addresses.hasRod, 1)
        api.writeCPU(Addresses.hasBook, 0)
    }

    fun clearRupee() {
        api.writeCPU(Addresses.numRupees, 0)
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

    fun setRupees(rupees: Int) {
        api.writeCPU(Addresses.numRupees, rupees)
    }
}