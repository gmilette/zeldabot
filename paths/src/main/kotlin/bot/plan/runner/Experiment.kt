package bot.plan.runner

import bot.plan.Phases
import bot.plan.action.Action
import bot.plan.action.KillAll
import sequence.ZeldaItem

class Experiments(masterPlan: MasterPlan) {
    companion object {
        // to rerun
//        const val current = "level1bat"
        const val current = "level2Boom"
//        const val current = "level2rhino"
    }
    private val experiments: Map<String, Experiment>

    private val default = Experiment("all", "start_nothing.save", masterPlan, sword = ZeldaItem.MagicSword)

    init {
        experiments = listOf(
            Experiment("all", "start_nothing.save", masterPlan, sword = ZeldaItem.MagicSword, addEquipment = false),
            Experiment("level2rhino", "lev2_14_boss.save", masterPlan.getPlanPhase("Destroy level 2", Phases.Segment.lev2Boss), sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level2Boom", "level2_boom_5h.save", masterPlanWith(KillAll()), sword = ZeldaItem.WhiteSword),
            Experiment("level2w", "level2.save", masterPlan.getPlanPhase("Destroy level 2", null), sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level5start", "level2.save", masterPlan.getPlanPhase("Destroy level 5", null), sword = ZeldaItem.WoodenSword),
            Experiment("level2", "level2.save", masterPlan.getPlanPhase("Destroy level 2", null), sword = ZeldaItem.MagicSword),
            Experiment("level1L", "lev1_start.save", masterPlan.getPlanPhase("Destroy level 1", null), addEquipment = false),
            Experiment("level1", "level1_start_no_ladder.save", masterPlan.getPlanPhase("Destroy level 1", null), addEquipment = false),
            Experiment("level1drag", "lev1_dragon.save", masterPlan.getPlanPhase("Destroy level 1", "destroy dragon")),
            Experiment("afterLev4", "level4_beat.save", masterPlan.getPlanAfter(Phases.grabHearts)),
            Experiment("afterForest30", "forest_30.save", masterPlan.getPlanPhase(Phases.forest30, null)),
            Experiment("level7", "level6_done.save", masterPlan.getPlanAfter(Phases.level7), sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level3", "level3.save", masterPlan.getPlanAfter(Phases.level3), sword = ZeldaItem.MagicSword, addEquipment = true),
            //Experiment("level8", "level8_start.save", masterPlan.getPlanPhase(Phases.level8, segment = null), sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level8", "level8_start.save", masterPlan.getPlanPhase(Phases.level8, null), sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level9", "level9_start.save", masterPlan.getPlanAfter(Phases.level9), sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level1bat", "lev1_bat.save", masterPlanWith(KillAll()), addEquipment = false),
        ).associateBy { it.name }
    }

    private fun masterPlanWith(action: Action): MasterPlan {
        val segment = PlanSegment("phase", "set", listOf(action))
        return MasterPlan(listOf(segment))
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
)

