package bot.plan.runner

import bot.GamePad
import bot.plan.action.Action
import bot.state.FramePoint
import bot.state.MapLocationState
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import util.d

class PlanRunner(val masterPlan: MasterPlan) {
    var action = masterPlan.pop()

    val started = System.currentTimeMillis()
    var startedStep = System.currentTimeMillis()

    val outputFile = "1_wood_sword_${(Math.random()*10000).toInt()}.csv"

    data class StepCompleted(val action: Action, val time: Long, val totalTime: Long)
    val completedStep = mutableListOf<StepCompleted>()

    // currently does this
    fun next(state: MapLocationState): GamePad {
        // update plan
        // if actions are
        if (action.complete(state)) {
            val time = (System.currentTimeMillis() - startedStep) / 1000
            val totalTime = (System.currentTimeMillis() - started) / 1000
            d { "*** advance time $time"}
            logCompleted()
            completedStep.add(StepCompleted(action, time, totalTime))
            advance()
            startedStep = System.currentTimeMillis()
        }

        return action.nextStep(state)
    }

    private fun logCompleted() {
        completedStep.forEachIndexed { index, stepCompleted ->
            d { "$index, ${stepCompleted.time}, ${stepCompleted.totalTime}, ${stepCompleted.action.name}" }
        }

        val csvWriter2 = CsvWriter()
        csvWriter2.open(outputFile, false) {
            completedStep.forEachIndexed { index, stepCompleted ->
                writeRow(index, stepCompleted.time, stepCompleted.totalTime, stepCompleted.action.name)
            }
        }
    }

    private fun advance() {
        action = masterPlan.pop()
        action.reset()
    }

    fun afterThis() = masterPlan.next()

    fun afterAfterThis() = masterPlan.nextAfter()

    fun target(): FramePoint {
        return action.target()
    }

    fun path(): List<FramePoint> {
        return listOf()
    }


    override fun toString(): String {
        return "*** ${action.name}: Plan: $masterPlan"
    }
}