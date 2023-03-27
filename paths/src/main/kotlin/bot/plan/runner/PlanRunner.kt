package bot.plan.runner

import bot.GamePad
import bot.plan.action.Action
import bot.plan.action.DoNothing
import bot.plan.action.moveHistoryAttackAction
import bot.state.FramePoint
import bot.state.MapLocationState
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import util.d

class PlanRunner(val masterPlan: MasterPlan) {
    var action: Action

    val started = System.currentTimeMillis()
    var startedStep = System.currentTimeMillis()

    val outputFile = "1_wood_sword_${(Math.random()*10000).toInt()}.csv"

    data class StepCompleted(val action: Action, val time: Long, val totalTime: Long)
    val completedStep = mutableListOf<StepCompleted>()

    init {
        action = masterPlan.skipToStart()
    }

    private fun withDefaultAction(action: Action) = moveHistoryAttackAction(action)

    // currently does this
    fun next(state: MapLocationState): GamePad {
        // update plan
        // if actions are
        if (action.complete(state)) {
            advance()
        }

        return try {
            action.nextStep(state)
        } catch (e: Exception) {
            DoNothing().nextStep(state)
        }
    }

    private fun logCompleted() {
        completedStep.forEachIndexed { index, stepCompleted ->
            d { "$index, ${stepCompleted.time}, ${stepCompleted.totalTime}, ${stepCompleted.action.name}" }
        }

//        val csvWriter2 = CsvWriter()
//        csvWriter2.open(outputFile, false) {
//            completedStep.forEachIndexed { index, stepCompleted ->
//                writeRow(index, stepCompleted.time, stepCompleted.totalTime, stepCompleted.action.name)
//            }
//        }
    }

    /**
     * advance to the next step
     */
    fun advance() {
        val time = (System.currentTimeMillis() - startedStep) / 1000
        val totalTime = (System.currentTimeMillis() - started) / 1000
        d { "*** advance time $time"}
        logCompleted()
        completedStep.add(StepCompleted(action, time, totalTime))
        action = withDefaultAction(masterPlan.pop())
        action.reset()
        startedStep = System.currentTimeMillis()
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