package bot.plan.runner

import bot.plan.action.Action
import bot.plan.action.EndAction
import bot.plan.action.StartHereAction
import bot.state.MapLoc
import bot.state.MapLocationState
import bot.state.map.Objective
import util.d
import util.i

// master plan
// - plan phase (main thing doing, get to lev 1, gather stuff for lev 3, just
// a name
//  - plan segment (sub route
//  -- plan objective (per screen)

class MasterPlan(val segments: List<PlanSegment>) {
    private var justRemoved: PlanStep = PlanStep(PlanSegment("", "", emptyList()), EndAction())

    private var initialPlanSize: Int = 0

    private val giant = segments.flatMap { seg -> seg.plan.map { PlanStep(seg, it) } }.toMutableList().also {
        initialPlanSize = it.size
        d { " created plan with $initialPlanSize actions moves $numMoves" }
    }

    val actionsLeft: Int
        get() = initialPlanSize - giant.size

    val percentDoneInt: Int
        get() = (percentDone * 100).toInt()

    val percentDone: Float
        get() = actionsLeft.toFloat() / initialPlanSize.toFloat()

    val complete: Boolean
        get() = giant.isEmpty()

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

    fun getPlanPhase(phaseName: String, segment: String? = null): MasterPlan =
        MasterPlan(segments.filter { segment == null || it.name == segment }.filter { it.phase == phaseName })

    fun getPlanAfter(phaseName: String, seg: String = ""): MasterPlan {
        val index = segments.indexOfFirst { it.phase == phaseName && (seg.isEmpty() || it.name == seg) }
        return MasterPlan(segments.subList(index, segments.size))
    }

    fun skipToLocation(mapLoc: MapLoc, level: Int): Action {
        var keepGoing = true
        var current = pop()
        while(keepGoing && giant.isNotEmpty()) {
            val first = giant.first().action
            d { " SEARCH one ${first.actionLoc}"}
//            if (first.actionLoc == mapLoc && first.toLevel == level) {
             if (first.actionLoc == mapLoc && first.levelLoc == level) {
                d { " search plan move ${first.actionLoc} " } // lev ${first.toLevel}"}
                // stop
                keepGoing = false
                 current = pop()
            } else {
                current = pop()
            }
        }
        d { " done search plan "}
        return current
    }

    /**
     * return the next action
     */
    fun skipToStart(): Action {
        var ct = 0
        return if (hasStartHere) {
            var action = pop()
            while (action !is StartHereAction) {
                action = pop()
                d { " skip ${action.name}" }
                ct++
            }
            (action as? StartHereAction)?.restoreSaveSlot()
            d { " advanced $ct steps" }
            action
        } else {
            pop()
        }
    }

    private val hasStartHere: Boolean
        get() = giant.any { it.action is StartHereAction }

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

    fun toStringCurrentPlanPhase(): String =
        justRemoved.inSegment.toStringShort()

    fun toStringCurrentPhase(): String =
        justRemoved.inSegment.phase

    fun toStringCurrentSeg(): String =
        justRemoved.inSegment.name

    fun currentSeg(): PlanSegment =
        justRemoved.inSegment

    override fun toString(): String {
        val first = giant.firstOrNull()

        val text = first?.inSegment?.let {
            "**** Phase: ${it.phase}: seg: ${it.name}: action:${first.action.name}"
        } ?: ""

        return text
    }

    fun toStringAll(): String {
        var text = ""
        for ((i, step) in giant.withIndex()) {
            text += "$i ${step.action.name} \n"
        }

        return text
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
    val phase: String,
    val name: String,
    val plan: List<Action>,
    val objective: Objective = Objective.empty
) {
    override fun toString(): String {
        return plan.fold("") { R, t -> "$R $t " }
    }

    fun toStringShort(): String =
        "ph: $phase seg: $name"
}
