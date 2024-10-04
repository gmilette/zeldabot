package bot.state

import bot.state.map.destination.ZeldaItem
import util.d

/**
 * it turns out that this calculation of how much life link has isn't a single calculation.
 * I also want to know when lint has taken damage and how much
 */
class HeartsStateCalculator(private val inventory: Inventory) {
    fun downToOne() = lifeInHearts() <= 1.0f

    fun calc() {
        val damage = damageInHearts()
        val life = lifeInHearts(damage)
        val life2 = lifeInHearts2()
        val damageNumber = damageNumber()
        d {" heart info: life $life life: $life2 damage $damage damageNumber = $damageNumber"}
    }

    private fun logOtherHeartInfo() {
        // useless
//        val heart1 = api.readCPU(Addresses.heart1)
//        val heart1f = api.readCPU(Addresses.heart1Full)
//        val heart2 = api.readCPU(Addresses.heart2)
//        val heart2f = api.readCPU(Addresses.heart2Full)
//        val heart3 = api.readCPU(Addresses.heart3)
//        val heart3f = api.readCPU(Addresses.heart3Full)
//        d { "1 $heart1 $heart1f"}
//        d { "2 $heart2 $heart2f"}
//        d { "3 $heart3 $heart3f"}
    }

    // this is just a decreasing number
    fun damageNumber(): Int {
        val damage = inventory.damage
        val whole = damage.secondNibble()
        val fraction = damage.firstNibble()
        // will be a decreasing number, it's not completely equal when comparing values
        // the max is F, but then D, B, 8, 7, 5, 3, 1
        val reversed = "${whole.padIfLessThan()}${fraction.padIfLessThan()}"
        d { "aaaa $damage $whole $fraction $reversed"}
//        255 15 15 1515
        return Integer.parseInt(reversed, 16)
    }

    private fun Int.padIfLessThan(): String =
        if (this < 10) "0$this" else this.toString()

    fun lifeInHearts2(): Double {
        val full = heartContainersFull().toDouble()
        val damage = damageDecimal()
//        d { " full: $full decimal $damage"}
        return full + damage
    }

    fun lifeInHearts(damage: Double = damageInHearts()): Double =
        heartContainers().toDouble() - damage

    private fun damageDecimal(): Double {
        val half = inventory.damage

        val fraction = half.firstNibble()
//        d { " fraction = $fraction"}

        val decimal = when (fraction) {
            13 -> 0.875
            11 -> 0.75
            9 -> 0.625
            7 -> 0.5
            3 -> 0.25
            1 -> 0.125
            else -> 0.0
        }

        return decimal
    }

    fun damageInHearts(): Double {
        val half = inventory.damage
        val heartContain = inventory.hearts
//        d { " half hex = ${half.toString(16)} container = ${heartContain.toString(16)}"}

        val whole = half.secondNibble()
        val fraction = half.firstNibble()
//        d { " whole = $whole fraction = $fraction"}

        val halfHeartShowing = fraction <= 7
        val decimal = when (fraction) {
            13 -> 0.875
            11 -> 0.75
            9 -> 0.625
            7 -> 0.5
            3 -> 0.25
            1 -> 0.125
            else -> 0.0
        }

        // need special handling for lower numbers maybe this has
        // to be 16 somtimes
        val wholeDamage = 15 - whole
        val lifeTotalDecimal = wholeDamage.toDouble() + decimal
        return lifeTotalDecimal
    }

    fun noDamage(): Boolean {
        val containers = heartContainers()
        val full = heartContainersFull()
        val damage = damageDecimal()
        return damage == 0.0 && containers == full
    }

    fun full(state: FrameState): Boolean {
        return full(state.inventory.inventoryItems.whichRing())
    }

    fun full(state: MapLocationState): Boolean {
        return full(state.frameState)
    }

    fun full(ring: ZeldaItem = ZeldaItem.None): Boolean {
        val damageThresholdToStillBeFull = when (ring) {
            // you can have half damage and still be full
            ZeldaItem.BlueRing -> 0.5
            ZeldaItem.RedRing -> 0.25
            else -> 0.0
        }
        val containers = heartContainers()
        val full = heartContainersFull()
        val damage = damageDecimal()
        // if you have the red ring, it will be 0.875
        // got into weird state where it was stuck at 0.25 and couldnt shoot
        return (containers == full && (damage == 0.0 || damage < damageThresholdToStillBeFull)).also {
            d { " isFull=$it full cont $containers full $full damage $damage full $it thresh: $damageThresholdToStillBeFull ring: $ring"}
        }
    }

    override fun toString(): String {
        return info()
    }

    private fun info(): String =
        if (full()) "F_${damageDecimal()}" else "${heartContainers() - heartContainersFull() + damageDecimal()}"

    fun makeHeartsFull(): Int {
        val containers = heartContainers() - 1
        val twoFull = "${containers}${containers}"
        return twoFull.toInt(16)
    }

    // empty: FD, 7E, FC (yes it is indeed full), FB, FA, F9, F4(2 full hearts), F3(1 heart)
    // half: 7D, 7C, 7B, 7A, 79, 74 (1.5 hearts), 73, only 0.5 hearts
    //C8, C7, C6(6 full hearts), C1(2 full hearts), c0 (1 heart full or 0.5)
    fun heartContainers(): Int =
        // will it always be +1 I think
        inventory.hearts.firstNibble() + 1

    private fun heartContainersFull(): Int =
        inventory.hearts.secondNibble() + 1

    private fun Int.firstNibble(): Int {
        if (this < 0) return 0
        val full = this.toString(16).first().toString()
        return Integer.parseInt(full, 16)
    }

    private fun Int.secondNibble(): Int {
        if (this < 0) return 0
        val full = this.toString(16).last().toString()
        return Integer.parseInt(full, 16)
    }
}