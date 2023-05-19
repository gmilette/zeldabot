package bot.plan.runner

class Experiments(masterPlan: MasterPlan) {
    private val experiments: Map<String, Experiment> = mutableMapOf()

    val lev2: Experiment
    val lev1: Experiment

    init {
        lev2 = Experiment("level2", "level2.save", masterPlan.getPlanPhase("Destroy level 2"))
        lev1 = Experiment("level1", "lev1_start.save", masterPlan.getPlanPhase("Destroy level 1"))
    }

}

data class Experiment(
    val name: String,
    val startSave: String,
    val plan: MasterPlan
)

object MakeExperiment {
    fun go(masterPlan: MasterPlan) {


    }

}
// add a final action to re-run, collect data

