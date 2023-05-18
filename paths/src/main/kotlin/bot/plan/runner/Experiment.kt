package bot.plan.runner

data class Experiment(
    val name: String,
    val startSave: String,
    val plan: MasterPlan
)

object MakeExperiment {
    fun go(masterPlan: MasterPlan) {
        val lev2 = masterPlan.getPlanPhase("Destroy level 2")

        val lev2Run = Experiment("rhino_kill", "sss.save", lev2)

    }

}
// add a final action to re-run, collect data

