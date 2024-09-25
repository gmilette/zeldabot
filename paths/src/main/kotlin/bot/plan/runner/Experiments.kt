package bot.plan.runner

import bot.plan.Phases
import bot.plan.action.Action
import bot.plan.action.KillAll
import bot.plan.action.StartAtAction
import bot.plan.action.dodge
import bot.state.map.destination.ZeldaItem

class Experiments(private val masterPlan: PlanMaker) {
    val default = Experiment("all", "start_nothing.save", masterPlan, sword = ZeldaItem.WoodenSword, addEquipment = false)

//    var current: Experiment = default
//        get() = evaluation.getOrElse(0) { default }

//    var current: Experiment = default
////        get() = experiments["level2rhino"] ?: default
//      get() = experiments["level3plan"] ?: evaluation["level3plan"] ?: default

    var experimentIncrement = 0

    val experiments: Map<String, Experiment>

    val evaluation: Map<String, Experiment>

//    fun getExp(): Experiment {
//        val one = Experiment("level1", "level1_start_no_ladder.save",
//            { masterPlan().getPlanPhase("Destroy level 1") },
//            addEquipment = false,
//            sword = ZeldaItem.WoodenSword)
//        return one.copy(name = "lev1white5", hearts = 5, bombs = 4, sword = ZeldaItem.WhiteSword)
//    }
//
    init {
        val one = Experiment("level1", "level1_start_no_ladder.save",
            { masterPlan().getPlanPhase("Destroy level 1") },
            addEquipment = false,
            sword = ZeldaItem.WoodenSword)

        val twoBomb = Experiment("level2Boom", "level2_boom_5h.save",
            plan = { masterPlanWith(StartAtAction(79), KillAll.make()) }
        )
        val two = Experiment("level2", "level2.save",
            { masterPlan().getPlanPhase("Destroy level 2") },
            addEquipment = false,
            sword = ZeldaItem.WoodenSword)

        val three = Experiment("level3", "level3.save",
            { masterPlan().getPlanPhase(Phases.lev(3)) },
            addEquipment = false,
            sword = ZeldaItem.WhiteSword)

        val four = Experiment("level4", "level4.save",
            { masterPlan().getPlanPhase(Phases.lev(4)) },
            addEquipment = false,
            sword = ZeldaItem.WhiteSword,
            hearts = 8,
            boomerang = ZeldaItem.Boomerang,
            ring = ZeldaItem.BlueRing,
            shield = true
            )

    val five = Experiment("level5", "level5_start_in.save",
        { masterPlan().getPlanPhase(Phases.lev(5)) },
        addEquipment = false,
        sword = ZeldaItem.WhiteSword,
        hearts = 14, //9,
        boomerang = ZeldaItem.Boomerang,
        potion = true,
        ring = ZeldaItem.BlueRing,
        shield = true
    )

    val six = Experiment("level6", "level6_start_in.save",
        { masterPlan().getPlanPhase(Phases.lev(6)) },
        addEquipment = false,
        sword = ZeldaItem.MagicSword,
        hearts = 13,
        boomerang = ZeldaItem.Boomerang,
        ring = ZeldaItem.BlueRing,
        shield = true,
        potion = true
    )

    val seven = Experiment("level7", "level7_start.save",
        { masterPlan().getPlanPhase(Phases.lev(7)) },
        addEquipment = false,
        sword = ZeldaItem.MagicSword,
        hearts = 14,
        boomerang = ZeldaItem.Boomerang,
        ring = ZeldaItem.BlueRing,
        shield = true,
        potion = true
    )

    val eight = Experiment("level8", "level8_start.save",
        { masterPlan().getPlanPhase(Phases.lev(8)) },
        addEquipment = false,
        sword = ZeldaItem.MagicSword,
        hearts = 15,
        boomerang = ZeldaItem.Boomerang,
        ring = ZeldaItem.BlueRing,
        shield = true,
        potion = true
    )

    val nine = Experiment("level9", "level9_start.save",
        { masterPlan().getPlanPhase(Phases.lev(8)) },
        addEquipment = false,
        sword = ZeldaItem.WhiteSword,
        hearts = 9,
        boomerang = ZeldaItem.Boomerang,
        ring = ZeldaItem.BlueRing,
        shield = true
    )

    evaluation = listOf(
            six.copy(name = "level6plan", bombs = 4, keys = 4),
            five.copy(name = "level5plan", bombs = 4, keys = 2),
            four.copy(name = "level4plan", hearts = 8, bombs = 4, keys = 2),
            three.copy(name = "level3plan", hearts = 7, bombs = 4, boomerang = ZeldaItem.Boomerang, shield = true, ring = ZeldaItem.BlueRing),
            // go straight to level1
            // should have 0 bombs though
            two.copy(name = "level2b", hearts = 3, bombs = 0, sword = ZeldaItem.WoodenSword, boomerang = ZeldaItem.Boomerang),
            two.copy(name = "level2", hearts = 3, bombs = 0, sword = ZeldaItem.WoodenSword),
            two.copy(name = "level25h", hearts = 5, bombs = 0, sword = ZeldaItem.WoodenSword),
            twoBomb.copy(name = "level2Bomb4", hearts = 4, bombs = 0, sword = ZeldaItem.WoodenSword),
            twoBomb.copy(name = "level2Bomb5w", hearts = 5, bombs = 0, sword = ZeldaItem.WhiteSword),
            twoBomb.copy(name = "level2Bomb6ws", hearts = 6, bombs = 0, sword = ZeldaItem.WhiteSword, shield = true),
            twoBomb.copy(name = "level2Bomb6wws", hearts = 5, bombs = 0, sword = ZeldaItem.WoodenSword, shield = true),
            twoBomb.copy(name = "level2Bomb5", hearts = 5, bombs = 0, sword = ZeldaItem.WoodenSword),
            twoBomb.copy(name = "level2Bomb", hearts = 3, bombs = 0, sword = ZeldaItem.WoodenSword),
            one.copy(name = "level1plan", hearts = 3, bombs = 1),
            one.copy(name = "level1", hearts = 3, bombs = 1),
            // get two bomb hearts and white sword
            one.copy(name = "lev1white5", hearts = 5, bombs = 4, sword = ZeldaItem.WhiteSword),
            // beat level two, then grad bomb hearts
            one.copy(name = "lev1white6", hearts = 6, bombs = 4, sword = ZeldaItem.WhiteSword),
            // two bomb hearts (no level2), candle, magic shield, forestheart
            one.copy(name = "level1sh6", hearts = 6, bombs = 1, sword = ZeldaItem.WhiteSword, shield = true),
            // controls
            one.copy(name = "level1Full", hearts = 16, bombs = 1),
            one.copy(name = "level1WhiteFull", hearts = 16, bombs = 1, sword = ZeldaItem.WhiteSword),
        ).associateBy { it.name }

        val over = Experiment("allBoom", "start_nothing.save", masterPlan, sword = ZeldaItem.WoodenSword, boomerang = ZeldaItem.Boomerang)

        experiments = listOf(
            over,
            Experiment("all", "start_nothing.save", masterPlan, sword = ZeldaItem.MagicSword, addEquipment = false),
            Experiment("overworlddodge", "overworlddodge.save", { masterPlanWith(dodge) }, sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level2dodge", "level2_dodge_shield.save", { masterPlanWith(dodge) }, sword = ZeldaItem.MagicSword, addEquipment = true),
//            Experiment("level2r", "level2.save", masterPlan.getPlanAfter(Phases.reenterLevel2), sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level2dodgeNoShield", "level2_boom_dead.save", { masterPlanWith(dodge) }, sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level1dodgeb", "level_1_boomerang.save", { masterPlanWith(dodge) }, sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level1dodge", "level1_skele.save", { masterPlanWith(dodge) }, sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level6start", "level6_start.save", { masterPlan().getPlanAfter(Phases.level6) }, sword = ZeldaItem.MagicSword, addEquipment = true),
//            Experiment("level6end", "level6_done.save", masterPlan.getPlanAfter(Phases.afterLevel6), sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level2rhino", "lev2_14_boss.save", { masterPlan().getPlanPhase("Destroy level 2", Phases.Segment.lev2Boss) }, sword = ZeldaItem.WoodenSword, addEquipment = true, bombs = 8),
//            Experiment("level2rhinoAfter", "lev2_14_boss.save", masterPlan.getPlanAfter("Destroy level 2", Phases.Segment.lev2Boss)),
//            Experiment("level2Boom", "level2_boom_5h.save", masterPlanWith(KillAll.make()), sword = ZeldaItem.WhiteSword),
            Experiment("level2w", "level2.save", { masterPlan().getPlanPhase("Destroy level 2", null) }, sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level5", "level5_start2.save", { masterPlan().getPlanAfter(Phases.level5) }, sword = ZeldaItem.MagicSword),
//            Experiment("level2", "level2.save", masterPlan.getPlanPhase("Destroy level 2", null), sword = ZeldaItem.MagicSword),
//            Experiment("level1Ladder", "lev1_ladder.save", masterPlan.getPlanPhase("Destroy level 1", "grab key from zig"), addEquipment = false),
//            Experiment("level1L", "lev1_start.save", masterPlan.getPlanPhase("Destroy level 1", null), addEquipment = false),
            Experiment("level1", "level1_start_no_ladder.save", { masterPlan().getPlanPhase("Destroy level 1", null) }, addEquipment = false),
            Experiment("level1drag", "lev1_dragon.save", { masterPlan().getPlanAfter("Destroy level 1", "destroy dragon") }),
//            Experiment("afterLev4", "level4_beat.save", masterPlan.getPlanAfter(Phases.grabHearts), addEquipment = true),
//            Experiment("afterForest30", "forest_30.save", masterPlan.getPlanPhase(Phases.forest30, null)),
            Experiment("level7", "level7_strart.save", { masterPlan().getPlanAfter(Phases.level7) }, sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("go to level 9", "mapstate_0_5.save", masterPlan, sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("go to level 9", "mapstate_0_5.save", masterPlan, sword = ZeldaItem.MagicSword, addEquipment = true),
            Experiment("level3", "level3.save", { masterPlan().getPlanAfter(Phases.level3) }, sword = ZeldaItem.MagicSword, addEquipment = true),
//            Experiment("level3After", "level3After.save", masterPlan.getPlanAfter(Phases.level3After)),
            Experiment("ladder_heart", "ladder_heard.save", { masterPlan().getPlanAfter(Phases.ladderHeart) }),
            Experiment("level8", "level8_start.save", { masterPlan().getPlanPhase(Phases.level8, segment = null) }, sword = ZeldaItem.MagicSword, addEquipment = true),
//            Experiment("level8", "level8_start.save", masterPlan.getPlanPhase(Phases.level8, null), sword = ZeldaItem.WoodenSword, addEquipment = true),
            Experiment("level9", "level9_start.save", { masterPlan().getPlanAfter(Phases.level9) }, sword = ZeldaItem.MagicSword, addEquipment = true),
//            Experiment("level1bat", "lev1_bat.save", masterPlanWith(KillAll()), addEquipment = false),
//            Experiment("gannon", "level9_gannon.save", masterPlan.getPlanAfter(Phases.level9, "seg kill gannon"), addEquipment = false),
        ).associateBy { it.name }
    }

    private fun masterPlanWith(vararg action: Action): MasterPlan {
        val segment = PlanSegment("phase", "set", action.toList())
        return MasterPlan(listOf(segment))
    }
}

