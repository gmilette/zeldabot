package bot.plan.runner

import bot.plan.Phases
import sequence.ZeldaItem

class Experiments(masterPlan: MasterPlan) {
    companion object {
        const val current = "level2rhino"
    }

    private val experiments: Map<String, Experiment>

    private val default = Experiment("all", "start_nothing.save", masterPlan, sword = ZeldaItem.MagicSword)

    init {
        experiments = listOf(
            Experiment("all", "start_nothing.save", masterPlan, sword = ZeldaItem.MagicSword, addEquipment = false),
            Experiment("level2rhino", "lev2_14_boss.save", masterPlan.getPlanPhase("Destroy level 2", Phases.Segment.lev2Boss), sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level2w", "level2.save", masterPlan.getPlanPhase("Destroy level 2", null), sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level5start", "level2.save", masterPlan.getPlanPhase("Destroy level 5", null), sword = ZeldaItem.WoodenSword),
            Experiment("level2", "level2.save", masterPlan.getPlanPhase("Destroy level 2", null), sword = ZeldaItem.MagicSword),
            Experiment("level1", "lev1_start.save", masterPlan.getPlanPhase("Destroy level 1", null)),
            Experiment("afterLev4", "level4_beat.save", masterPlan.getPlanAfter(Phases.grabHearts)),
            Experiment("afterForest30", "forest_30.save", masterPlan.getPlanPhase(Phases.forest30, null)),
            Experiment("level7", "level6_done.save", masterPlan.getPlanAfter(Phases.level7), sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level9", "level6_start.save", masterPlan.getPlanAfter(Phases.level9), sword = ZeldaItem.MagicSword, addEquipment = true),
        ).associateBy { it.name }
    }

    fun ex(name: String) = experiments[name] ?: default
}

data class Experiment(
    val name: String,
    val startSave: String,
    val plan: MasterPlan,
    val sword: ZeldaItem = ZeldaItem.MagicSword,
    val addEquipment: Boolean = false,
    val startAt: Int = 52
    // startAt
)

object MakeExperiment {
    fun go(masterPlan: MasterPlan) {


    }

}
// add a final action to re-run, collect data

