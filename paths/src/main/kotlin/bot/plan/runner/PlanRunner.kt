package bot.plan.runner

import bot.DirectoryConstants
import bot.ZeldaBot
import bot.plan.action.Action
import bot.plan.action.DoNothing
import bot.plan.action.moveHistoryAttackAction
import bot.state.*
import bot.state.map.destination.ZeldaItem
import nintaco.api.API
import util.ZRandom
import util.d
import util.w
import java.io.File

typealias PlanMaker = () -> MasterPlan

class PlanRunner(private val makePlan: PlanMaker,
                 private val api: API,
                 private val experiment: String = "default"
) {
    var action: Action? = null
        private set
    lateinit var runLog: RunActionLog
    lateinit var masterPlan: MasterPlan
    lateinit var startPath: String

    private val experiments = Experiments(makePlan)

    private fun getExp(name: String): Experiment {
        return experiments.experiments[name] ?: experiments.evaluation[name] ?: experiments.default
    }

    //    private val target = "afterLev4"
//    private val target = "level7"
    private val target: Experiment
        get() = getExp(experiment)
//        get() = experiments.evaluation["level2Bomb6wws"] ?: experiments.current

    private var runCt = 0
    private var runSetupCt = 0

    private var levelExperiment: Experiment? = null

    private var maxFramesPerTrial = 0
    private var batchDone = false

    // room_<lvl>_<loc>[_<sword>[_<ring>]][,<lvl>_<loc>...] - a spec with no sword/ring
    // inherits them from the first, so room_8_62_m_b,8_94,7_24 is all magic sword + blue ring
    private val roomsToTrial: List<Experiment> by lazy {
        val specs = experiment.removePrefix("room_").split(",").map { it.trim().split("_") }
        val firstSword = specs.first().getOrElse(2) { "d" }
        val firstRing = specs.first().getOrElse(3) { "g" }
        specs.map { spec ->
            Experiments.roomTrial(
                level = spec[0].toInt(),
                mapLoc = spec[1].toInt(),
                name = "room_${spec[0]}_${spec[1]}",
                sword = swordFor(spec.getOrElse(2) { firstSword }),
                ring = ringFor(spec.getOrElse(3) { firstRing }),
                hearts = ZeldaBot.startHearts,
                shield = ZeldaBot.startShield,
                maxFramesPerTrial = ZeldaBot.maxTrialFrames ?: Experiments.DEFAULT_TRIAL_FRAME_BUDGET
            )
        }
    }

    private fun runFrom() {
        val exp = experiment
        d { " run from $exp"}
        if (exp.startsWith("room_")) {
            // rotate rather than run each room to exhaustion, so drift over a long batch
            // lands on every room equally instead of only the last one
            runIt(load = true, ex = roomsToTrial[runCt % roomsToTrial.size])
        } else if (exp.contains("run")) {
            runHere()
        } else if (exp.contains("_") || exp.contains(",")) {
            val split = exp.split("_", ",")
            val level = split.first().toInt()
            val mapLoc = split[1].toInt()
            // w = white, m = magic, d="none"
            val s = split.getOrElse(2, { "d" })
            // b = blue, r=red, g=none
            val ring = split.getOrElse(3, { "g" })
            levelExperiment = Experiment(
                name = exp,
                startSave = "",
                { MasterPlan(emptyList()) },
                addEquipment = false,
                sword = swordFor(s),
                ring = ringFor(ring),
                keys = 4,
                bombs = 4,
                rupees = 250,
                potion = true,
                boomerang = ZeldaItem.MagicalBoomerang,
                magicArrowAndBow = true
            )
            runLoc(mapLoc, level)
        } else {
            runIt(exp)
        }
    }

    private fun swordFor(s: String) = when (s) {
        "w" -> ZeldaItem.WhiteSword
        "m" -> ZeldaItem.MagicSword
        else -> ZeldaItem.WoodenSword
    }

    private fun ringFor(s: String) = when (s) {
        "b" -> ZeldaItem.BlueRing
        "r" -> ZeldaItem.RedRing
        else -> ZeldaItem.None
    }

    init {
        runFrom()
//        if (runCt % 10 == 0) {
//            experiments.experimentIncrement++
//        }
//        val runIt: Experiment =
//        runIt("level2rhino")
//        runIt("level25h")
//        runIt("all")
//        runIt("level3plan")
//        runLoc(true,91, 3) // lev 3 sword guys
    //
//        val loc: MapLoc = 64+16+1+16

//        runLoc(true,loc, 4) // near start
        // pancake
//        runLoc(true,18, 4) // dragon
//        runLoc(true,120, 0) // near start
//        runLoc(true,10, 0) // near start
//        runLoc(true,26, 0) // near start

//        val runIt: Experiment = getExp("allBoom")
//        runIt(ex = runIt)
//        run(name = "level1drag")
//        run(name = "level2Boom")
//        run(name = "level6start")
//        run(name = "level8")
//        run(name = "level1L") // with ladder
//        run(name = "level1drag")
//        run(name = "level1Ladder") // with ladder
//          run(name = "level1")
//        run(name = "level1dodge")
//        run(name = "level2dodge")
//        run(name = "overworlddodge")
//        run(name = "level1dodgeb")

//        run(name = "level3")
//        run(name = "level5") // with ladder
//        runLoc(true, 120, 6)
//        run(name = "afterLev4")
//        runIt("level2w")
//         run(name = "all")
//        runLoc(true,121, 0) // near start
//        runLoc(true,26, 0) // near start

//            runLoc(true,91, 0)
//        run(name = "level7"
//        run(name = "go to level 9")
//        run(name = "level2rhinoAfter")
//        run(name = "gannon")
//        run(name = "level9") // with ladder
//        run(name = "ladder_heart")
//        runLoc(true,5, 0)
//        run(name = "level1")
//        run(name = "level2w")
//        runLoc(true,62, 2) // sand
//        runLoc(true,94, 2) // before boomerang
//        runLoc(true,35, 1) // before bow
//        runLoc(true,35+16+16, 1)
//        runLoc(true,69, 1) // dragon
//        runLoc(true,35+16+16, 1)
//        runLoc(true,87, 5)
//        runLoc(true,48, 4)
//        runLoc(true,50, 4) // push ladder
//        runLoc(true,16, 4) // push drag
//        runLoc(true,24, 7)
//        runLoc(true,76, 3)
//        runLoc(true,91, 3) //sword guy
//        runLoc(true,107, 3) // right stair
//        runLoc(true,5, 5)
//        runLoc(true,6, 5)
//        runLoc(true,63, 8)
//        runLoc(true,104, 0) // near start
//        runLoc(true,45, 0) // forest statue
//        runLoc(true,61, 0) // forest statue
//        runLoc(true,52, 0) // forest statue
//        runLoc(true,36, 0) // forest statue
//        runLoc(true,99-16, 0) // forest statue
//        runLoc(true,69+16+16, 0) // going to 13
    }

    private fun rerun() {
        runFrom()
//        runIt(load = true, ex = target)
    }

    private fun runHere() {
        val plan = makePlan()
        masterPlan = plan
        val start = masterPlan.findStartHere() ?: return
        val (level, mapLoc) = start.mapCoordinates
        levelExperiment = start.experiment
        val root = "mapstate/mapstate_${level}_${mapLoc}.save"

        d { " run runHere $mapLoc lev $level for map $root" }
        startPath = root
        action = masterPlan.skipToStartAt()
        runLog = RunActionLog("here_mapstate_${level}_${mapLoc}", target, save = false)
    }


    private fun runLoc(mapLoc: Int, level: Int = 0) {
        val plan = makePlan()
//        val ex = experiments.ex(name)
//        d { "  run experiment ${ex.name}"}
//        ZeldaBot.addEquipment = ex.addEquipment
        masterPlan = plan
//        val root = "../Nintaco_bin_2020-05-01/states/mapstate_${level}_${mapLoc}.save"

        val root = "mapstate/mapstate_${level}_${mapLoc}.save"

        d { " run loc $mapLoc lev $level for map $root"}
        startPath = root
        action = withDefaultAction(masterPlan.skipToLocation(mapLoc, level))
        runLog = RunActionLog("mapstate_${level}_${mapLoc}", target, save = false)
    }

    private fun runIt(ex: String) {
        runIt(true, getExp(ex))
    }

    private fun runIt(load: Boolean = false, ex: Experiment) {
        d { "  run experiment ${ex.name} load=$load"}

//        val ex = experiments.getExp()
//        ZeldaBot.addEquipment = ex.addEquipment
        masterPlan = ex.plan()
        startPath = ex.startSave
        masterPlan.reset()
        action = withDefaultAction(masterPlan.skipToStart())
        d { " START AT ${action?.name}"}
        levelExperiment = ex
        maxFramesPerTrial = ex.maxFramesPerTrial
        runLog = RunActionLog(
            ex.name, ex,
            save = DirectoryConstants.enableInfo,
            label = ZeldaBot.runLabel ?: "",
            runId = ZeldaBot.runId,
            trial = runCt + 1
        )
        if (load) {
            d { "reset" }
            Thread( {
                api.reset()
            }).start()
            Thread.sleep(100)
            d { "reload" }
            val root = DirectoryConstants.states
            api.loadState("$root/${startPath}")
        }
        runCt++
        runSetupCt = 0
        ZRandom.startTrial(runCt)
    }

    fun runSetup(manipulator: StateManipulator) {
        d { " run setup "}
        // api.loadState is asynchronous and can land after these writes, restoring the
        // save state's own inventory over them. Never latch on a single confirmation:
        // keep checking for the whole window and rewrite only when something is wrong,
        // so a late load is caught but consumed bombs are not silently restored.
        if (runSetupCt > SETUP_FRAME_LIMIT) {
            return
        }
//        api.setSpeed(400)
        runSetupCt++
        val ex = levelExperiment ?: this.target

        if (runSetupCt > ALWAYS_APPLY_FRAMES) {
            if (loadoutMatches(manipulator, ex)) return
            w {"WARNING: loadout drifted at frame $runSetupCt, reapplying" +
                    " (wanted ${ex.hearts}h/${ex.sword}/${ex.ring}/shield=${ex.shield}," +
                    " have ${manipulator.heartContainers()}h/sword=${manipulator.swordId()}" +
                    "/ring=${manipulator.ringId()}/shield=${manipulator.hasMagicShield()})" }
        }
        d { " set sword to ${ex.sword} hearts to ${ex.hearts}"}
        manipulator.setSword(ex.sword)
        d { "set ring to ${ex.ring}" }
        manipulator.setRing(ex.ring)
        ex.hearts?.let {
            manipulator.setHearts(it)
        }
        manipulator.clearRupee()
        manipulator.setBombs(ex.bombs)
        if (ex.shield) {
            manipulator.setMagicShield()
        }
        if (ex.setTriforce) {
            manipulator.setTriforceAll()
        }
        if (ex.wand) {
            manipulator.setWandNoBook()
        }
        if (ex.rupees > 0) {
            manipulator.setRupees(ex.rupees)
        }
        if (ex.candle) {
            manipulator.setRedCandle()
        }
        if (ex.arrowAndBow) {
            manipulator.setArrow()
        }
        if (ex.magicArrowAndBow) {
            manipulator.setMagicArrow()
        }
        if (ex.magicKey) {
            manipulator.setMagicKey()
        }
        //
        if (ex.ladderAndRaft) {
            manipulator.setLadderAndRaft(ex.ladderAndRaft)
        }
        if (ex.whistle) {
            manipulator.setHaveWhistle()
        }
//        manipulator.setLetter()
//        manipulator.setLadderAndRaft(true)
        manipulator.setBoomerang(ex.boomerang)
        manipulator.setKeys(ex.keys)
        if (ex.potion) {
            manipulator.setMaxPotion()
        }
        if (ex.bait) {
            manipulator.setBait()
        }

        if (runSetupCt >= SETUP_FRAME_LIMIT) {
            w { "WARNING: loadout never took after $SETUP_FRAME_LIMIT frames -" +
                    " wanted ${ex.hearts}h/${ex.sword}/${ex.ring}/shield=${ex.shield}," +
                    " have ${manipulator.heartContainers()}h/sword=${manipulator.swordId()}" +
                    "/ring=${manipulator.ringId()}/shield=${manipulator.hasMagicShield()}"}
        }
    }

    // only the permanent equipment: bombs, keys and rupees are legitimately spent
    private fun loadoutMatches(manipulator: StateManipulator, ex: Experiment): Boolean {
        ex.hearts?.let { if (manipulator.heartContainers() != it) return false }
        if (manipulator.swordId() != swordId(ex.sword)) return false
        if (manipulator.ringId() != ringId(ex.ring)) return false
        if (ex.shield && !manipulator.hasMagicShield()) return false
        return true
    }

    private fun swordId(item: ZeldaItem) = when (item) {
        ZeldaItem.WoodenSword -> 1
        ZeldaItem.WhiteSword -> 2
        ZeldaItem.MagicSword -> 3
        else -> 0
    }

    private fun ringId(item: ZeldaItem) = when (item) {
        ZeldaItem.BlueRing -> 1
        ZeldaItem.RedRing -> 2
        else -> 0
    }

    private fun withDefaultAction(action: Action) = moveHistoryAttackAction(action)

    fun next(state: MapLocationState): GamePad {
        val action = action ?: return GamePad.None
        runLog?.frameCompleted(state)

        if (maxFramesPerTrial > 0 && (runLog?.elapsedFrames ?: 0) > maxFramesPerTrial) {
            d { " trial timed out after $maxFramesPerTrial frames" }
            runLog?.advance(action, state, masterPlan)
            endTrial(state, "timeout")
            return GamePad.None
        }

        if (action.complete(state) || state.frameState.isDead) {
            runLog?.advance(action, state, masterPlan)
            advance(state)
        }

        return try {
            action.nextStep(state)
        } catch (e: Exception) {
            DoNothing().nextStep(state)
        }
    }

    /**
     * advance to the next step
     */
    private fun advance(state: MapLocationState) {
        if (masterPlan.complete || state.frameState.isDead) {
            d { " complete "}
            endTrial(state, if (state.frameState.isDead) "dead" else "complete")
        } else {
            d { " complete action ${action?.javaClass?.name ?: ""}"}
            completedAction(state, action)
            action = withDefaultAction(masterPlan.pop())
            action?.reset()
        }
    }

    private fun endTrial(state: MapLocationState, result: String) {
        if (batchDone) return
        runLog.logFinalComplete(state, masterPlan, result)

        val roomCount = if (experiment.startsWith("room_")) roomsToTrial.size else 1
        val limit = ZeldaBot.trials?.let { it * roomCount }
        if (limit != null && runCt >= limit) {
            batchDone = true
            action = null
            ZeldaBot.doAct = false
            val where = runLog.outputFileName
            println("=== batch done: $runCt trials of '$experiment'" +
                    " label='${ZeldaBot.runLabel ?: ""}' last=$where ===")
            d { " batch done after $runCt trials" }
            return
        } else {
            println("=== completed: $runCt trials of '$experiment'" +
                    " label='${ZeldaBot.runLabel ?: ""}' ===")
        }
        rerun()
    }

    private fun completedAction(state: MapLocationState, action: Action?) {
//        val stateName = "${state.frameState.level}_${state.movedTo}"
        val stateName = "${state.frameState.level}_${state.frameState.mapLoc}"
        val root = DirectoryConstants.states
        val filePath = "mapstate/mapstate_${stateName}.save"
        // this path is for nintaco
        val stateFileName = "${root}$filePath"
        // this path is for checking file existence
        val stateFile = "${DirectoryConstants.statesFromExecuting}$filePath"
        if (DirectoryConstants.enableEmulatorSavedState) {
            if (File(stateFile).exists()) {
                d { "Already saved $stateFileName" }
            } else {
                d { "Saved a state to $stateFileName" }
                api.saveState(stateFileName)
                api.saveScreenshot()
            }
        }
    }

    fun afterThis() = masterPlan.next()

    fun afterAfterThis() = masterPlan.nextAfter()

    fun target(): FramePoint {
        return action?.target() ?: FramePoint(1,1)
    }

    fun targets(): List<FramePoint> {
        return action?.targets() ?: emptyList()
    }

    fun path(): List<FramePoint> {
        d { " path for action ${action?.name ?: ""}"}
        return action?.path() ?: emptyList()
    }

    override fun toString(): String {
        return "*** ${action?.name ?: ""}: Plan: $masterPlan"
    }

    companion object {
        private const val ALWAYS_APPLY_FRAMES = 40
        private const val SETUP_FRAME_LIMIT = 60
    }
}
