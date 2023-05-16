package bot.plan.runner

data class Experiment(
    val name: String,
    val startSave: String,
    val plan: MasterPlan
)

val rhino = Experiment("rhino_kill", "sss.save", MasterPlan(emptyList()))
// add a final action to re-run, collect data
