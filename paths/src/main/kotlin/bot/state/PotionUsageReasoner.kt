package bot.state

import util.d

enum class DiversionState {
    noNeed,
    needRupees,
    getPotion,
    usePotion
}

class PotionUsageReasoner {
    var diversionNeed: DiversionState = DiversionState.noNeed

    fun shouldUsePotion(state: FrameState) {
        val almostDead = state.inventory.heartCalc.lifeInHearts() <= 1.25f
        val haveEnough = state.inventory.heartCalc.heartContainers() >= 8
        val havePotion = state.inventory.hasPotion
        if (haveEnough && almostDead && havePotion) {
            d { "!!need potion!!"}
            diversionNeed = DiversionState.usePotion
        } else {
            goGet(state)
        }
    }

    fun usePotion() {
        // remember menu selection
        // open menu
        // select potion
        // use potion
        // restore menu selection
    }

    fun goGet(state: FrameState) {
        val reason = when {
            state.isLevel -> "can't get potion from level"
            state.inventory.hasFullPotion -> "already have potion"
            !state.inventory.hasLetter -> "need letter"
            else -> ""
        }

        if (reason.isNotEmpty()) {
            d { " dont get potion because $reason" }
            diversionNeed = DiversionState.noNeed
        } else {
            return
        }

        // might need to go get $$ for other reasons

        val rupeesNeeded = when {
            state.inventory.hasPotion -> 48
            else -> 68
        }

        diversionNeed = if (state.inventory.numRupees < rupeesNeeded) {
            d { " need to gather some rupees! "}
            // go towards level 2, until have enough rupees
            DiversionState.needRupees
        } else {
            // go towards nearest potion location, and buy it!
            DiversionState.getPotion
        }
    }
}