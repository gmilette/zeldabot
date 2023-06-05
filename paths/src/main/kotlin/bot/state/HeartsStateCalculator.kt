package bot.state

import util.d

/**
 * it turns out that this calculation of how much life link has isn't a single calculation.
 * I also want to know when lint has taken damage and how much
 */
class HeartsStateCalculator(private val inventory: Inventory) {
    fun calc() {
        d { " heart info "}
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
        // 20 = 3 heart containers, 1 filled
        // 2 (heart containers = h + 1)
        // (hearts filled = l + 1, but it could be empty depending on the half heart bit
        // need to read this, maybe only the first 2 hex digits to determine if there is a half heart
        val life = lifeInHearts()
        val damage = damageInHearts()
        d {" life $life damage $damage damageNumber = ${damageNumber()}"}

    }

//    fun life(): Float = 1 + fullHearts() + (if (halfHeart()) 0.5f else 0f)

    // this is just a decreasing number
    fun damageNumber(): Int {
        val damage = inventory.damage
        val whole = damage.secondNibble()
        val fraction = damage.firstNibble()
        // will be a decreasing number, it's not completely equal when comparing values
        // the max is F, but then D, B, 8, 7, 5, 3, 1
        val reversed = "${whole.padIfLessThan()}${fraction.padIfLessThan()}"
        d { " damage number $reversed"}
        return Integer.parseInt(reversed, 16)
    }

    private fun Int.padIfLessThan(): String =
        if (this < 10) "0$this" else this.toString()

    fun lifeInHearts(): Double =
        heartContainers().toDouble() - damageInHearts()

    fun damageInHearts(): Double {
        // nibble 2 is number of full hearts
        // nibble 1 contains lesser damage
        val ringType = inventory.inventoryItems.hasRing
        val containers = heartContainers()

        val half = inventory.damage
        d { " half hex = ${half.toString(16)}"}

        val mult = when (ringType) {
            1 -> 4
            2 -> 2
            else -> 7
        }

        val whole = half.secondNibble()
        val fraction = half.firstNibble()
        d { " whole = $whole fraction = $fraction"}

        val halfHeartShowing = fraction <= 7
        val decimal = when (ringType) {
            1 -> when (fraction) {
                11 -> 0.75
                7 -> 0.5
                3 -> 0.25
                else -> 0.0
            }
            2 -> when (fraction) {
                13 -> 0.875
                11 -> 0.75
                9 -> 0.625
                7 -> 0.5
                3 -> 0.25
                1 -> 0.125
                else -> 0.0
            }
            else -> when (fraction) {
                7 -> 0.5
                else -> 0
            }
        }

        val wholeDamage = 15 - whole
        val lifeTotalDecimal = wholeDamage.toDouble() + decimal.toDouble()
        d { " life: $lifeTotalDecimal"}
        return lifeTotalDecimal

//        val max = 255
//        val rawDamage = (max - half)
//        val damage = rawDamage * mult
////         val damage = half * mult
//        val lifeLeft = containers - damage
//        d { " damage = $damage lifeLeft = $lifeLeft out of $containers half = $half mult = $mult raw = $rawDamage"}
//        return lifeLeft
    }

    // empty: FD, 7E, FC (yes it is indeed full), FB, FA, F9, F4(2 full hearts), F3(1 heart)
    // half: 7D, 7C, 7B, 7A, 79, 74 (1.5 hearts), 73, only 0.5 hearts
//    fun halfHeart(): Boolean {
//        val h = api.readCPU(Addresses.heartContainersHalf)
//        d { " half = $h"}
////        return h > (0x0072) && h < 0x007F
//        return h in 1..127
//    }

    //C8, C7, C6(6 full hearts), C1(2 full hearts), c0 (1 heart full or 0.5)
    fun fullHearts(): Int {
        val heartContainers = inventory.hearts
        val full = heartContainers.toString(16).last().toString()
        val number = Integer.parseInt(full, 16)
        return number.toInt()
    }

    fun heartContainers(): Int =
        inventory.hearts.firstNibble()

    private fun Int.firstNibble(): Int {
        val full = this.toString(16).first().toString()
        return Integer.parseInt(full, 16)
    }

    private fun Int.secondNibble(): Int {
        val full = this.toString(16).last().toString()
        return Integer.parseInt(full, 16)
    }
}