package bot.plan.runner

import bot.DirectoryConstants
import bot.plan.action.Action
import bot.plan.action.UsePotion
import bot.state.GamePad
import bot.state.MapLoc
import bot.state.MapLocationState
import bot.state.map.MapCell
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import util.d
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class RunActionLog(private val fileNameRoot: String,
                   private val experiment: Experiment,
                   private val save: Boolean = true
) {
    val started = System.currentTimeMillis()
    var startedStep = System.currentTimeMillis()
    var framesForStep = 0
    private var totalFrames = 0

    var totalHits = 0
    var totalDamage = 0.0
    var stepHits = 0
    var stepDamage = 0.0
    var stepHeal = 0.0
    var totalHeal = 0.0
    val bombsUsed = DataCount()
    val keysUsed = DataCount()
    val keysGot = DataCount(0, 0)
    val rupeesSpent = DataCount()
    val rupeesGained = DataCount(-1, -1)

    private val dataCounts = listOf(bombsUsed, keysGot, keysUsed, rupeesSpent, rupeesGained)

    private var directionCt = mutableMapOf<GamePad, DataCount>()

    init {
        for (gamePad in GamePad.entries) {
            directionCt[gamePad] = DataCount()
        }
    }

    private val experimentRoot = DirectoryConstants.outDir("zexperiment")

    val outputFileName = "${fileNameRoot}_${System.currentTimeMillis()}"
    val outputFile = "$experimentRoot${outputFileName}.csv"
    val outputFileAll = "${experimentRoot}experiments.csv"
    val heartLog = "${experimentRoot}heartLog_${System.currentTimeMillis()}.txt"

    // bombs used
    // time
    // damage taken
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
        val frames: Int,
        val numFrames: Int = 0,
        val hits: Int = 0,
        val damage: Double = 0.0,
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
        
        setHearts(state)
        setBombs(state)
        setKeys(state)
        setRupees(state)
    }

    private fun setHearts(state: MapLocationState) {
        // always decreases, but isn't always exactly accurate for some reason
        val currentHeart = state.frameState.inventory.heartCalc.lifeInHearts()
        val previousHeart = state.previousHeart
        d { " previous heart $previousHeart current heart $currentHeart" }
        // should just check if
        val currentDamage = state.frameState.damageNumber
        val previousDamage = state.previousDamageNumber
        val damage = previousHeart - currentHeart
        if (damage != 0.0) {
            val WRITE_HEART = false
            if (WRITE_HEART && save) {
                val csvWriter2 = CsvWriter()
                // damage, hearts, damageIn, life, life2, damage number
                csvWriter2.open(heartLog, true) {
                    writeRow(
                        state.frameState.inventory.damage.toString(16),
                        state.frameState.inventory.hearts.toString(16),
                        state.frameState.inventory.heartCalc.damageInHearts(),
                        state.frameState.inventory.heartCalc.lifeInHearts(),
                        state.frameState.inventory.heartCalc.lifeInHearts2(),
                        state.frameState.inventory.heartCalc.damageNumber()
                    )
                }
            }
            if (damage > 0) {
                totalHits++
                totalDamage += damage
                stepHits++
                stepDamage += damage
            } else {
                stepHeal += -damage
                totalHeal += -damage
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

    private fun logCompletedStep() {
        completedStep.forEachIndexed { index, stepCompleted ->
            stepCompleted.apply {
                d { "$index, $time, $totalTime, $action, $bombsUsed, $hits, $damage" }
            }
        }
        if (save) {
            val csvWriter2 = CsvWriter()
            csvWriter2.open(outputFile, false) {
                writeRow("index", "level", "mapLoc", "name", "time", "totalTime", "totalFrames", "numFrames", "action", "hearts", "bombsUsed", "hits", "damage", "heal", "keys", "rupees", "potion", "bombs")
                completedStep.forEachIndexed { index, stepCompleted ->
                    stepCompleted.apply {
                        writeRow(index, level, mapLoc, name, time, totalTime, frames, numFrames, action, hearts, bombsUsed, hits, damage, heal, keys, rupees, potionFills, numBombs)
                    }
                }
            }
        }
    }

    fun logFinalComplete(state: MapLocationState, masterPlan: MasterPlan) {
        // it's possible that link just
        val result = when {
            masterPlan.complete -> "complete"
            state.frameState.isDead -> "dead"
            else -> "other"
        }
        val percentDone = masterPlan.percentDoneInt
        val finalMapLoc = state.currentMapCell.mapLoc
        if (save) {
            writeFinalHeader()
            val stepCompleted = calculateStep(fileNameRoot, masterPlan.toStringCurrentSeg(), state, totalFrames, totalHits, totalDamage, totalHeal)
            val csvWriter2 = CsvWriter()
            csvWriter2.open(outputFileAll, true) {
                writeRow(
                    now(),
                    stepCompleted.action,
                    outputFileName,
                    stepCompleted.totalTime,
                    totalFrames,
                    totalHits,
                    totalDamage,
                    totalHeal,
                    bombsUsed.total,
                    state.frameState.inventory.numRupees,
                    state.frameState.gameMode,
                    percentDone,
                    finalMapLoc,
                    result,
                    experiment.sword,
                    experiment.ring,
                    experiment.hearts,
                    experiment.bombs,
                    experiment.boomerang,
                    experiment.shield,
                )
            }
        }
    }

    private fun writeFinalHeader() {
        val csvWriter2 = CsvWriter()
        if (!File(outputFileAll).exists() && save) {
            csvWriter2.open(outputFileAll, true) {
                writeRow("date", "action", "file", "totalTime", "totalFrames",
                    "totalHits", "totalDamage", "totalHeal", "bombsUsed", "rupees",
                    "gamemode", "percent", "mapLoc", "result",
                    "sword", "ring", "hearts", "bombs",
                    "boom", "shield")
            }
        }
    }

    fun advance(action: Action, state: MapLocationState, masterPlan: MasterPlan) {
        d { "*** advance time" }
        logCompletedStep()
        completedStep.add(calculateStep(action.name, masterPlan.toStringCurrentSeg(), state, framesForStep, stepHits, stepDamage, stepHeal))
        startedStep = System.currentTimeMillis()
        framesForStep = 0
        stepDamage = 0.0
        stepHits = 0
        stepHeal = 0.0
        for (dataCount in dataCounts) {
            dataCount.actionDone()
        }
        for (dataCount in directionCt.values) {
            dataCount.actionDone()
        }
    }

    private val potionOrReplace = "${UsePotion::javaClass.javaClass.simpleName} or "
    private val potionReplace = "UsePotion or "

    private fun calculateStep(
        name: String,
        seg: String,
        state: MapLocationState,
        frameCt: Int,
        hits: Int,
        damage: Double,
        heal: Double
    ): StepCompleted {
        val time = (System.currentTimeMillis() - startedStep) / 1000
        val totalTime = (System.currentTimeMillis() - started) / 1000
        val cellName = state.getCell().mapData.name
        return StepCompleted(
            level = state.frameState.level,
            mapLoc = state.frameState.mapLoc,
            seg = seg.take(8),
            name = cellName.take(8),
            action = name.replace("\"", "").replace(potionReplace, "").trim(),
            hearts = state.frameState.inventory.heartCalc.lifeInHearts(),
            time = time,
            totalTime = totalTime,
            bombsUsed = bombsUsed.perStep,
            frames = state.frameState.currentFrame,
            numFrames = frameCt,
            hits = hits,
            damage = damage,
            heal = heal,
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
}

data class DataCount(
    // maybe a name?
    var total: Int = 0,
    var perStep: Int = 0) {

    fun inc() {
        perStep++
        total++
    }

    fun add(amount: Int) {
        perStep += amount
        total += amount
    }

    fun actionDone() {
        perStep = 0
    }
}

fun now(): String {
    val time = Calendar.getInstance().time
    val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm")
    val date = formatter.format(time)
    return date
}

