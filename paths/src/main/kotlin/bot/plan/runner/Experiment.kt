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
    val keys: Int = 0,
    val potion: Boolean = false,
    val boomerang: ZeldaItem = ZeldaItem.None,
    val rupees: Int = 0,
    val arrowAndBow: Boolean = true,
    val candle: Boolean = false,
    val ladderAndRaft: Boolean = false,
    val whistle: Boolean = false,
    val magicKey: Boolean = false,
    val addEquipment: Boolean = false,
    val startAt: Int = 52,
    val nameFull: String = "${name}_s${sword.name.first()}_h${hearts}_r${ring.name.first()}_b${bombs}"
)
