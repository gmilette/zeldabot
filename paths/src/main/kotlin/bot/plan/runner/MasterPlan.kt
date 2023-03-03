package bot.plan.runner

import bot.plan.action.Action
import bot.plan.action.EndAction
import bot.plan.action.MoveTo
import bot.state.MapLocationState
import util.d
import util.i

class MasterPlan(val segments: List<PlanSegment>) {
    private var justRemoved: PlanStep = PlanStep(PlanSegment("", "", emptyList()), EndAction())

    private val giant = segments.flatMap { seg -> seg.plan.map { PlanStep(seg, it) } }.toMutableList().also {
        d { " created plan with ${it.size} actions moves $numMoves" }
    }

    // estimated time to complete

    private val numMoves: Int
        get() = if (giant.isNullOrEmpty()) 0 else giant.count {
            true
//            it.action is MoveTo
        }

    fun log() {
        val first = giant.firstOrNull()

        first?.inSegment?.apply {
            d { "*** ${phase}: ${name}: ${first.action.name}" }
        }
    }

    override fun toString(): String {
        val first = giant.firstOrNull()

        val text = first?.inSegment?.let {
            "**** Phase: ${it.phase}: seg: ${it.name}: action:${first.action.name}"
        } ?: ""

        return text
    }

    fun toStringAll(): String {
        val first = giant.firstOrNull()

        var text = ""
        var i = 0
        for (step in giant) {
            text += "$i ${step.action.name} \n"
            i++
        }

        return text
    }

    fun current(): Action =
        giant.first().action

    fun next(): Action =
        if (giant.isEmpty()) {
            EmptyAction()
        } else {
            giant[0].action
        }

    fun nextAfter(): Action =
        if (giant.size <= 1) {
            EmptyAction()
        } else {
            giant[1].action
        }

    fun pop(): Action {
        justRemoved = giant.removeFirst()
        return justRemoved.action.also {
            i { "--> switch to ${it.name}" }
        }
    }

    fun toStringCurrentPlanPhase(): String =
        justRemoved.inSegment.toStringShort()
}

class EmptyAction : Action {
    override fun complete(state: MapLocationState): Boolean {
        return true
    }

    override val name: String
        get() = "empty"
}

data class PlanStep(val inSegment: PlanSegment, val action: Action)


data class PlanSegment(
    val phase: String, val name: String, val plan:
    List<Action>
) {
    override fun toString(): String {
        return plan.fold("") { R, t -> "$R $t " }
    }

    fun toStringShort(): String =
        "ph: $phase seg: $name" // #act: ${plan.size}
}
