package bot.plan.runner

import bot.plan.action.Action
import bot.state.Addresses
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
    private var totalDamage = 0
    private var stepHits = 0
    private var stepDamage = 0

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

    // bombs used
    // time
    // damage taken
    data class StepCompleted(
        val action: String,
        val time: Long,
        val totalTime: Long,
        val numBombs: Int,
        val numFrames: Int = 0,
        val hits: Int = 0,
        val damage: Int = 0
    )

    val completedStep = mutableListOf<StepCompleted>()

    fun frameCompleted(state: MapLocationState) {
        directionCt[state.previousGamePad]?.inc()

        framesForStep++
        totalFrames++
        d { " previous heart ${state.previousHeart} current heart ${state.frameState.inventory.hearts}"}

        if (state.previousHeart > 0 && (state.previousHeart > state.frameState.inventory.hearts)) {
            totalHits++
            val damage = state.previousHeart - state.frameState.inventory.hearts
            totalDamage += damage
            stepHits++
            stepDamage += damage
        }
    }

    fun logCompleted() {
        completedStep.forEachIndexed { index, stepCompleted ->
            stepCompleted.apply {
                d { "$index, $time, $totalTime, $action, $numBombs, $hits, $damage" }
            }
        }
        if (SAVE) {
            val csvWriter2 = CsvWriter()
            csvWriter2.open(outputFile, false) {
                writeRow("index", "time", "totalTime", "action", "numBombs", "hits", "damage")
                completedStep.forEachIndexed { index, stepCompleted ->
                    stepCompleted.apply {
                        writeRow(index, time, totalTime, numFrames, action, numBombs, hits, damage)
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
                totalDamage
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
                writeRow("date", "action", "file", "totalTime", "totalFrames", "totalHits", "totalDamage")
            }
        }
    }

    fun advance(action: Action, state: MapLocationState) {
        d { "*** advance time" }
        logCompleted()
        completedStep.add(calculateStep(action.name, state, framesForStep, stepHits, stepDamage))
        startedStep = System.currentTimeMillis()
        framesForStep = 0
        stepDamage = 0
        stepHits = 0
        for (dataCount in directionCt.values) {
            dataCount.actionDone()
        }
    }

    private fun calculateStep(
        name: String,
        state: MapLocationState,
        frameCt: Int,
        hits: Int,
        damage: Int
    ): StepCompleted {
        val time = (System.currentTimeMillis() - startedStep) / 1000
        val totalTime = (System.currentTimeMillis() - started) / 1000
        return StepCompleted(
            name.replace("\"n", "N"),
            time,
            totalTime,
            state.frameState.inventory.numBombs,
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