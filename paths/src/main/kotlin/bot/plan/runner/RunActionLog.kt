package bot.plan.runner

import bot.plan.action.Action
import bot.state.MapLocationState
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import util.d

class RunActionLog(private val fileNameRoot: String) {

    private val SAVE = true
    val started = System.currentTimeMillis()
    var startedStep = System.currentTimeMillis()

    val outputFile = "${fileNameRoot}_${System.currentTimeMillis()}.csv"
    val outputFileAll = "experiments.csv"

    // bombs used
    // time
    // damage taken
    data class StepCompleted(val action: String, val time: Long, val totalTime: Long, val numBombs: Int)
    val completedStep = mutableListOf<StepCompleted>()

    fun logCompleted() {
        completedStep.forEachIndexed { index, stepCompleted ->
            stepCompleted.apply {
                d { "$index, $time, $totalTime, ${action} $numBombs" }
            }
        }

        if (SAVE) {
            val csvWriter2 = CsvWriter()
            csvWriter2.open(outputFile, false) {
                completedStep.forEachIndexed { index, stepCompleted ->
                    writeRow(index, stepCompleted.time, stepCompleted.totalTime, stepCompleted.action)
                }
            }
        }
    }

    fun logFinalComplete(state: MapLocationState) {
        val stepCompleted = calculateStep(fileNameRoot, state)
        val csvWriter2 = CsvWriter()
        csvWriter2.open(outputFileAll, true) {
            writeRow(0, stepCompleted.time, stepCompleted.totalTime, stepCompleted.action)
        }
    }

    fun advance(action: Action, state: MapLocationState) {
        d { "*** advance time"}
        logCompleted()
        completedStep.add(calculateStep(action.name, state))
        startedStep = System.currentTimeMillis()
    }

    private fun calculateStep(name: String, state: MapLocationState): StepCompleted {
        val time = (System.currentTimeMillis() - startedStep) / 1000
        val totalTime = (System.currentTimeMillis() - started) / 1000
        return StepCompleted(name.replace("\"", ""), time, totalTime, state.frameState.inventory.numBombs)
    }
}