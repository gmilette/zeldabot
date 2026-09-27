package bot.plan.runner

import bot.state.map.destination.ZeldaItem

data class Experiment(
    val name: String = "",
    val startSave: String = "",
    val plan: PlanMaker = { MasterPlan() },
    val sword: ZeldaItem = ZeldaItem.MagicSword,
    val hearts: Int? = null,
    val ring: ZeldaItem = ZeldaItem.None,
    val wand: Boolean = false,
    val bombs: Int = 0,
    val shield: Boolean = false,
    val setTriforce: Boolean = false,
    val keys: Int = 0,
    val potion: Boolean = false,
    val boomerang: ZeldaItem = ZeldaItem.None,
    val rupees: Int = 0,
    val arrowAndBow: Boolean = false,
    val magicArrowAndBow: Boolean = false,
    val candle: Boolean = false,
    val ladderAndRaft: Boolean = false,
    val whistle: Boolean = false,
    val bait: Boolean = false,
    val magicKey: Boolean = false,
    val addEquipment: Boolean = false,
    val startAt: Int = 52,
    val level: Int = -1,
    val startMapLoc: Int = -1,
    val maxFramesPerTrial: Int = 0,
    val nameFull: String = "${name}_s${sword.name.first()}_h${hearts}_r${ring.name.first()}_b${bombs}"
) {
    val signature: String
        get() = listOfNotNull(
            when (sword) {
                ZeldaItem.MagicSword -> "m"
                ZeldaItem.WhiteSword -> "w"
                ZeldaItem.WoodenSword -> "d"
                else -> "no-sword"
            },
            when (ring) {
                ZeldaItem.BlueRing -> "b"
                ZeldaItem.RedRing -> "r"
                else -> "g"
            },
            hearts?.let { "${it}h" },
            "shield".takeIf { shield },
            "b$bombs".takeIf { bombs > 0 },
            "k$keys".takeIf { keys > 0 },
            "r$rupees".takeIf { rupees > 0 },
            "potion".takeIf { potion },
            "marrow".takeIf { magicArrowAndBow } ?: "arrow".takeIf { arrowAndBow },
            when (boomerang) {
                ZeldaItem.MagicalBoomerang -> "mboom"
                ZeldaItem.Boomerang -> "boom"
                else -> null
            },
            "wand".takeIf { wand },
            "candle".takeIf { candle },
            "ladder".takeIf { ladderAndRaft },
            "max$maxFramesPerTrial".takeIf { maxFramesPerTrial > 0 }
        ).joinToString("/")
}
