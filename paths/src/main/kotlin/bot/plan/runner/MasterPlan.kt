package bot.plan.runner

import bot.plan.action.Action
import bot.state.MapLocationState
import util.d
import util.i

class MasterPlan(val segments: List<PlanSegment>) {
    private val giant = segments.flatMap { seg -> seg.plan.map { PlanStep(seg, it) } }.toMutableList().also {
        d { " created plan with ${it.size} actions" }
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

    fun pop(): Action =
        giant.removeFirst().action.also {
            i { "--> switch to ${it.name}" }
        }
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
}
