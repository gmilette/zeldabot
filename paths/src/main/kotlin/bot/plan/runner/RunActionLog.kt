package bot.plan.runner

import bot.plan.action.Action
import bot.state.GamePad
import bot.state.MapLocationState
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import util.d
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RunActionLog(private val fileNameRoot: String) {

    private val SAVE = true
    val started = System.currentTimeMillis()
    var startedStep = System.currentTimeMillis()
    var framesForStep = 0
    private var totalFrames = 0

    private var totalHits = 0
    private var totalDamage = 0.0
    private var stepHits = 0
    private var stepDamage = 0.0
    private val bombsUsed = DataCount()

    private val dataCounts = listOf(bombsUsed)

    private var directionCt = mutableMapOf<GamePad, DataCount>()

    init {
        for (gamePad in GamePad.values()) {
            directionCt[gamePad] = DataCount()
        }
    }

    private val experimentRoot = "../../zexperiment/"

    val outputFileName = "${fileNameRoot}_${System.currentTimeMillis()}"
    val outputFile = "$experimentRoot${outputFileName}.csv"
    val outputFileAll = "${experimentRoot}experiments.csv"
    val heartLog = "${experimentRoot}heartLog_${System.currentTimeMillis()}.txt"

    // bombs used
    // time
    // damage taken
    data class StepCompleted(
        val action: String,
        val time: Long,
        val totalTime: Long,
        val bombsUsed: Int,
        val numFrames: Int = 0,
        val hits: Int = 0,
        val damage: Double = 0.0
    )

    val completedStep = mutableListOf<StepCompleted>()

    fun frameCompleted(state: MapLocationState) {
        directionCt[state.previousGamePad]?.inc()

        framesForStep++
        totalFrames++
        
        setHearts(state)
        setBombs(state)
    }
    
    private fun setHearts(state: MapLocationState) {
        // always decreases, but isn't always exactly accurate for some reason
        val currentHeart = state.frameState.inventory.heartCalc.lifeInHearts()
        val previousHeart = state.previousHeart
        d { " previous heart $previousHeart current heart $currentHeart"}
        // should just check if
        val currentDamage = state.frameState.damageNumber
        val previousDamage = state.previousDamageNumber

        if (previousDamage > 0 && (previousDamage > currentDamage)) {
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
            totalHits++
            val damage = previousHeart - currentHeart
            totalDamage += damage
            stepHits++
            stepDamage += damage
        }
    }
    
    private fun setBombs(state: MapLocationState) {
        val numBombs = state.frameState.inventory.numBombs
        val previousNumBombs = state.previousNumBombs
        if (numBombs < previousNumBombs) {
            bombsUsed.inc()
        }
    }

    private fun logCompletedStep() {
        completedStep.forEachIndexed { index, stepCompleted ->
            stepCompleted.apply {
                d { "$index, $time, $totalTime, $action, $bombsUsed, $hits, $damage" }
            }
        }
        if (SAVE) {
            val csvWriter2 = CsvWriter()
            csvWriter2.open(outputFile, false) {
                writeRow("index", "time", "totalTime", "action", "bombsUsed", "hits", "damage")
                completedStep.forEachIndexed { index, stepCompleted ->
                    stepCompleted.apply {
                        writeRow(index, time, totalTime, numFrames, action, bombsUsed, hits, damage)
                    }
                }
            }
        }
    }

    fun logFinalComplete(state: MapLocationState) {
        writeFinalHeader()
        val stepCompleted = calculateStep(fileNameRoot, state, totalFrames, totalHits, totalDamage)
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
                bombsUsed.total,
                state.frameState.gameMode
            )
        }
    }

    private fun now(): String {
        val time = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm")
        val date = formatter.format(time)
        return date
    }

    private fun writeFinalHeader() {
        val csvWriter2 = CsvWriter()
        if (!File(outputFileAll).exists()) {
            csvWriter2.open(outputFileAll, true) {
                writeRow("date", "action", "file", "totalTime", "totalFrames", "totalHits", "totalDamage", "bombsUsed", "gamemode")
            }
        }
    }

    fun advance(action: Action, state: MapLocationState) {
        d { "*** advance time" }
        logCompletedStep()
        completedStep.add(calculateStep(action.name, state, framesForStep, stepHits, stepDamage))
        startedStep = System.currentTimeMillis()
        framesForStep = 0
        stepDamage = 0.0
        stepHits = 0
        for (dataCount in dataCounts) {
            dataCount.actionDone()
        }
        for (dataCount in directionCt.values) {
            dataCount.actionDone()
        }
    }

    private fun calculateStep(
        name: String,
        state: MapLocationState,
        frameCt: Int,
        hits: Int,
        damage: Double
    ): StepCompleted {
        val time = (System.currentTimeMillis() - startedStep) / 1000
        val totalTime = (System.currentTimeMillis() - started) / 1000
        return StepCompleted(
            name.replace("\"", ""),
            time,
            totalTime,
            bombsUsed.perStep,
            frameCt,
            hits = hits,
            damage = damage
        )
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

    fun actionDone() {
        perStep = 0
    }
}