package bot.plan.runner

import bot.plan.action.Action
import bot.state.MapLocationState
import util.d

class RunActionLog {

    val started = System.currentTimeMillis()
    var startedStep = System.currentTimeMillis()

    val outputFile = "1_wood_sword_${(Math.random()*10000).toInt()}.csv"

    // bombs used
    // time
    // damage taken
    data class StepCompleted(val action: Action, val time: Long, val totalTime: Long, val numBombs: Int)
    val completedStep = mutableListOf<StepCompleted>()

    private fun logCompleted() {
        completedStep.forEachIndexed { index, stepCompleted ->
            stepCompleted.apply {
                d { "$index, $time, $totalTime, ${action.name} $numBombs" }
            }
        }

//        val csvWriter2 = CsvWriter()
//        csvWriter2.open(outputFile, false) {
//            completedStep.forEachIndexed { index, stepCompleted ->
//                writeRow(index, stepCompleted.time, stepCompleted.totalTime, stepCompleted.action.name)
//            }
//        }
    }

    fun advance(action: Action, state: MapLocationState) {
        val time = (System.currentTimeMillis() - startedStep) / 1000
        val totalTime = (System.currentTimeMillis() - started) / 1000
        d { "*** advance time $time"}
        logCompleted()
        completedStep.add(StepCompleted(action, time, totalTime, state.frameState.inventory.numBombs))
        startedStep = System.currentTimeMillis()
    }
}