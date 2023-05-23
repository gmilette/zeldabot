package bot.plan.runner

import sequence.ZeldaItem

class Experiments(masterPlan: MasterPlan) {
    private val experiments: Map<String, Experiment> = mutableMapOf()

    val lev5s: Experiment
    val lev2w: Experiment
    val lev2: Experiment
    val lev1: Experiment
    val all: Experiment

    init {
        all = Experiment("all", "start_nothing.save", masterPlan, sword = ZeldaItem.MagicSword)
        lev2w = Experiment("level2w", "level2.save", masterPlan.getPlanPhase("Destroy level 2"), sword = ZeldaItem.WoodenSword)
        lev5s = Experiment("level5start", "level2.save", masterPlan.getPlanPhase("Destroy level 5"), sword = ZeldaItem.WoodenSword)
        lev2 = Experiment("level2", "level2.save", masterPlan.getPlanPhase("Destroy level 2"), sword = ZeldaItem.MagicSword)
        lev1 = Experiment("level1", "lev1_start.save", masterPlan.getPlanPhase("Destroy level 1"))
    }

}

data class Experiment(
    val name: String,
    val startSave: String,
    val plan: MasterPlan,
    val sword: ZeldaItem = ZeldaItem.MagicSword
)

object MakeExperiment {
    fun go(masterPlan: MasterPlan) {


    }

}
// add a final action to re-run, collect data

