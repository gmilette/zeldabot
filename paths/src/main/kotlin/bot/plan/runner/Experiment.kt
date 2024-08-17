package bot.plan.runner

import bot.state.map.destination.ZeldaItem

data class Experiment(
    val name: String,
    val startSave: String,
    val plan: PlanMaker,
    val sword: ZeldaItem = ZeldaItem.MagicSword,
    val hearts: Int? = null,
    val ring: ZeldaItem = ZeldaItem.None,
    val bombs: Int = 0,
    val shield: Boolean = false,
    val keys: Int = 0,
    val potion: Boolean = false,
    val boomerang: ZeldaItem = ZeldaItem.None,
    val addEquipment: Boolean = false,
    val startAt: Int = 52,
    val nameFull: String = "${name}_s${sword.name.first()}_h${hearts}_r${ring.name.first()}_b${bombs}"
)
