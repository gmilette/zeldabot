package bot.plan.runner

import bot.DirectoryConstants
import bot.plan.action.Action
import bot.state.GamePad
import bot.state.MapCoordinates
import bot.state.MapLoc
import bot.state.MapLocationState
import bot.state.map.MapCell
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.google.gson.Gson
import util.ZRandom
import util.d
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * file outputs:
 *  perStep output: per run
 *  all experiments output: one row per run, accumulate
 */
class RunActionLog(private val fileNameRoot: String,
                   private val experiment: Experiment,
                   private val save: Boolean = true,
                   private val label: String = "",
                   private val runId: String = "",
                   private val trial: Int = 0
) {
    val started = System.currentTimeMillis()
    var startedStep = System.currentTimeMillis()
    var framesForStep = 0
    private var totalFrames = 0

    /**
     * need these so we can limit any trials to an absolute number of frames
     */
    private var runStartFrame = NO_FRAME
    private var lastFrame = NO_FRAME

    val elapsedFrames: Int
        get() = if (runStartFrame == NO_FRAME) 0 else lastFrame - runStartFrame

    private var settled = false
    private var previousDamagedFlag = false
    var heartsAtStart = 0.0
        private set

    val hits = DataCount()
    val damage = DataCount()
    val heal = DataCount()
    val bombsUsed = DataCount()
    val keysUsed = DataCount()
    val keysGot = DataCount()
    val rupeesSpent = DataCount()
    val rupeesGained = DataCount()
    val damaged = DataCount()
    val damagedEvents = DataCount()

    private val dataCounts = listOf(hits, damage, heal, bombsUsed, keysGot, keysUsed,
        rupeesSpent, rupeesGained, damaged, damagedEvents)

    private var directionCt = mutableMapOf<GamePad, DataCount>()

    init {
        for (gamePad in GamePad.entries) {
            directionCt[gamePad] = DataCount()
        }
    }

    private val experimentRoot = DirectoryConstants.outDir("zexperiment")

    private val labelPrefix = if (label.isBlank()) "" else "${label}_"
    val outputFileName = "${labelPrefix}${fileNameRoot}_${System.currentTimeMillis()}"
    val outputFile = "$experimentRoot${outputFileName}.csv"
    val outputFileAll = "${experimentRoot}experiments.jsonl"

    data class StepCompleted(
        val level: Int,
        val mapLoc: MapLoc,
        val seg: String,
        val name: String,
        val action: String,
        val hearts: Double,
        val time: Long,
        val totalTime: Long,
        val bombsUsed: Int,
        val frame: Int,
        val numFrames: Int = 0,
        val hits: Int = 0,
        val damage: Double = 0.0,
        val damaged: Int = 0,
        val damagedEvents: Int = 0,
        val heal: Double = 0.0,
        val keys: Int = 0,
        val rupees: Int,
        val potionFills: Int = 0,
        val numBombs: Int = 0
    )

    val completedStep = mutableListOf<StepCompleted>()

    fun frameCompleted(state: MapLocationState) {
        directionCt[state.previousGamePad]?.inc()

        framesForStep++
        totalFrames++

        val frame = state.frameState.currentFrame
        if (runStartFrame == NO_FRAME) {
            runStartFrame = frame
        }
        lastFrame = frame

        if (!settled) {
            if (state.frameState.gameMode != NORMAL_PLAY || totalFrames < SETTLE_FRAMES) {
                return
            }
            settled = true
            totalFrames = 0
            framesForStep = 0
            heartsAtStart = state.frameState.inventory.heartCalc.lifeInHearts()
            previousDamagedFlag = state.frameState.link.damaged
            runStartFrame = frame
            return
        }

        setDamage(state)
        setHearts(state)
        setBombs(state)
        setKeys(state)
        setRupees(state)
    }

    private fun setDamage(state: MapLocationState) {
        val isDamaged = state.frameState.link.damaged
        if (isDamaged) {
            damaged.inc()
            if (!previousDamagedFlag) {
                damagedEvents.inc()
            }
        }
        previousDamagedFlag = isDamaged
    }

    private fun setHearts(state: MapLocationState) {
        val currentHeart = state.frameState.inventory.heartCalc.lifeInHearts()
        val previousHeart = state.previousHeart
        val damage = previousHeart - currentHeart
        if (damage != 0.0) {
            if (damage > 0) {
                hits.inc()
                this.damage.add(damage)
            } else {
                heal.add(-damage)
            }
        }
    }

    private fun setBombs(state: MapLocationState) {
        val numBombs = state.frameState.inventory.numBombs
        val previousNumBombs = state.previousNumBombs
        if (numBombs < previousNumBombs) {
            bombsUsed.inc()
        }
    }

    private fun setKeys(state: MapLocationState) {
        val num = state.frameState.inventory.numKeys
        val previous = state.previousNumKeys
        if (num < previous) {
            keysUsed.inc()
        } else if (num > previous) {
            keysGot.inc()
        }
    }

    private fun setRupees(state: MapLocationState) {
        val num = state.frameState.inventory.numRupees
        val previous = state.previousNumRupees
        val diff = abs(num - previous)
        if (num < previous) {
            rupeesSpent.add(diff)
        } else if (num > previous) {
            rupeesGained.add(diff)
        }
    }

    private var rowsWritten = 0

    private fun logCompletedStep() {
        if (!save || rowsWritten >= completedStep.size) return

        val csvWriter = CsvWriter()
        if (rowsWritten == 0) {
            csvWriter.open(outputFile, false) {
                writeRow("index", "level", "mapLoc", "seg", "name", "time", "totalTime",
                    "frame", "numFrames", "action", "hearts", "bombsUsed",
                    "hits", "damage", "damaged", "damagedEvents", "heal", "keys", "rupees",
                    "potion", "bombs")
            }
        }
        csvWriter.open(outputFile, true) {
            for (index in rowsWritten until completedStep.size) {
                completedStep[index].apply {
                    writeRow(index, level, mapLoc, seg, name, time, totalTime,
                        frame, numFrames, action, hearts, bombsUsed,
                        hits, damage, damaged, damagedEvents, heal, keys, rupees,
                        potionFills, numBombs)
                }
            }
        }
        rowsWritten = completedStep.size
    }

    fun logFinalComplete(state: MapLocationState, masterPlan: MasterPlan, forcedResult: String? = null) {
        if (!save) return

        logCompletedStep()

        val heartsAtEnd = state.frameState.inventory.heartCalc.lifeInHearts()
        // a trial can end before the settle window, leaving no measured baseline. Use the
        // current hearts so netHeartsLost reads 0 rather than the whole heart bar.
        val heartsAtStart = if (settled) heartsAtStart else heartsAtEnd
        val summary = TrialSummary(
            date = now(),
            label = label,
            runId = runId,
            config = experiment.signature + (ZRandom.seed?.let { "/seed$it" } ?: ""),
            gitSha = gitSha(),
            experiment = fileNameRoot,
            file = outputFileName,
            trial = trial,
            start = MapCoordinates(experiment.level, experiment.startMapLoc).id,
            result = forcedResult ?: when {
                masterPlan.complete -> "complete"
                state.frameState.isDead -> "dead"
                else -> "other"
            },
            percent = masterPlan.percentDoneInt,
            end = MapCoordinates(state.frameState.level, state.currentMapCell.mapLoc).id,
            elapsedFrames = elapsedFrames,
            totalFrames = totalFrames,
            heartsStart = heartsAtStart,
            heartsEnd = heartsAtEnd,
            netHeartsLost = heartsAtStart - heartsAtEnd,
            damagedEvents = damagedEvents.totalInt,
            damagedFrames = damaged.totalInt,
            bombsUsed = bombsUsed.totalInt,
            rupees = state.frameState.inventory.numRupees,
            rawHits = hits.totalInt,
            rawDamage = damage.total,
            rawHeal = heal.total,
            sword = experiment.sword.name,
            ring = experiment.ring.name,
            bombs = experiment.bombs,
            boom = experiment.boomerang.name,
            shield = experiment.shield
        )
        File(outputFileAll).appendText(summaryGson.toJson(summary) + "\n")
    }

    fun advance(action: Action, state: MapLocationState, masterPlan: MasterPlan) {
        d { "*** advance time" }
        completedStep.add(calculateStep(action.name, masterPlan.toStringCurrentSeg(), state, framesForStep))
        logCompletedStep()
        startedStep = System.currentTimeMillis()
        framesForStep = 0
        for (dataCount in dataCounts) {
            dataCount.actionDone()
        }
        for (dataCount in directionCt.values) {
            dataCount.actionDone()
        }
    }

    private val potionReplace = "UsePotion or "

    private fun calculateStep(
        name: String,
        seg: String,
        state: MapLocationState,
        frameCt: Int
    ): StepCompleted {
        val cellName = state.getCell().mapData.name
        val frame = state.frameState.currentFrame
        return StepCompleted(
            level = state.frameState.level,
            mapLoc = state.frameState.mapLoc,
            seg = seg.take(8),
            name = cellName.take(8),
            action = name.replace("\"", "").replace(potionReplace, "").trim(),
            hearts = state.frameState.inventory.heartCalc.lifeInHearts(),
            time = (System.currentTimeMillis() - startedStep) / 1000,
            totalTime = (System.currentTimeMillis() - started) / 1000,
            bombsUsed = bombsUsed.perStepInt,
            frame = frame,
            numFrames = frameCt,
            hits = hits.perStepInt,
            damage = damage.perStep,
            damaged = damaged.perStepInt,
            damagedEvents = damagedEvents.perStepInt,
            heal = heal.perStep,
            keys = state.frameState.numKeys,
            rupees = state.frameState.numRupees,
            potionFills = state.frameState.inventory.numPotions,
            numBombs = state.frameState.inventory.numBombs
        )
    }

    private fun MapLocationState.getCell(): MapCell = if (frameState.isOverworld) {
        hyrule.getMapCell(frameState.mapLoc)
    } else {
        hyrule.levelMap.cellOrEmpty(frameState.level, frameState.mapLoc)
    }

    companion object {
        // JsonFile's shared instance pretty-prints, which would break one-object-per-line
        private val summaryGson = Gson()

        private const val NO_FRAME = -1
        private const val NORMAL_PLAY = 5
        private const val SETTLE_FRAMES = 30
    }
}

data class DataCount(
    var total: Double = 0.0,
    var perStep: Double = 0.0) {

    val totalInt get() = total.toInt()
    val perStepInt get() = perStep.toInt()

    fun inc() = add(1.0)

    fun add(amount: Int) = add(amount.toDouble())

    fun add(amount: Double) {
        perStep += amount
        total += amount
    }

    fun actionDone() {
        perStep = 0.0
    }
}

fun now(): String {
    val time = Calendar.getInstance().time
    val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm")
    val date = formatter.format(time)
    return date
}

private val gitShaValue: String by lazy {
    try {
        var dir: File? = File(".").absoluteFile
        var gitDir: File? = null
        while (dir != null && gitDir == null) {
            val candidate = File(dir, ".git")
            if (candidate.isDirectory) gitDir = candidate
            dir = dir.parentFile
        }
        gitDir ?: return@lazy ""
        val head = File(gitDir, "HEAD").readText().trim()
        val sha = if (head.startsWith("ref:")) {
            File(gitDir, head.removePrefix("ref:").trim()).readText().trim()
        } else {
            head
        }
        sha.take(8)
    } catch (e: Exception) {
        ""
    }
}

fun gitSha(): String = gitShaValue
