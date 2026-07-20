package bot.plan.zstar.route

import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.plan.zstar.NeighborFinder
import bot.plan.zstar.route.BreadthFirstSearch.Companion.SAFE_GOAL
import bot.state.FramePoint
import bot.state.map.Direction
import util.d

class GoalFunction(
    private var ableToLongAttack: Boolean = false,
    private var ableToAttack: Boolean = true,
    private val neighborFinder: NeighborFinder,
    private val longDecider: AttackLongActionDecider = AttackLongActionDecider,
    private val shortDecider: AttackActionDecider = AttackActionDecider
) {
    /**
     * search out from current location
     * and find all goals
     * then we have
     */
    fun isGoal(point: FramePoint, targets: List<FramePoint>, initial: Boolean = false): Boolean {
        d { " goal from $point}"}
        val longAttack = ableToLongAttack && longDecider.targetInLongRange(neighborFinder.passable, point, targets)
        val attackAction by lazy { shortDecider.inRangeOf(point.direction ?: Direction.None, point, targets, false) }
        // could add overlapping attack.
//        d { " attackAction $attackAction"}
        val attack = ableToAttack && attackAction.isAttack
        val safe = isSafe(point)

        // when you are long attacking, just try to stay in a safe area the safer the better

        // if you dont find a safe route



        // need two levels of safety
        // intersection!
        // close (1/2 more grid)

        // if you are trying to shoot, stay 1.5 grid away
        // if you need to attack, you need to get within 1.5 grid
        // when travelling, stay 1/2 grid away if possible

        // TODO: need to make this more exact when comparing to the grid of hte targets
//        val tooClose = targets.any { it.distTo(point) <= MapConstants.twoGrid }

        // this seems ok
//        val tooClose by lazy { targets.any { it.minDistToRect(point) <= MapConstants.oneGridPoint5 } }
        val tooClose by lazy { false }
//        val tooCloseForAttack by lazy { targets.any { it.minDistToRect(point) <= MapConstants.halfGrid } }
        val tooCloseForAttack by lazy { false }

        // pick the route that minimizes time in unsafe territory

        // should return a goal type of LONG_ATTACK, vs SHORT_ATTACK
        // and the algorithm should prefer LONG_ATTACK range goals
        return when {
            !initial && SAFE_GOAL && safe -> true // dont want this actually, i want to move not just sit
            longAttack && !tooClose && safe -> true // could be B or A
            attack && !tooCloseForAttack && safe -> true
            // need some better logic for getting loot than exact target
            // we don't want to use the other routing for loot getting because
            // that will put link in danger, could use it for now I guess
//            point in lootTargets -> true // this is for getting items not enemies

            // otherwise, link is probably unsafe, and link should therefor get to a safe spot
            // and attack again
            // then again maybe it link is on top of an enemy he should attack anyway
            else -> false
        }.also {
            if (it) {
                d { "FOUND GOAL!"}
            }
        }
    }

    private fun isSafe(point: FramePoint): Boolean {
        return neighborFinder.isSafe(point)
    }

    enum class GoalType(val rank: Int) {
        UNSAFE_NO_ACTION(0),
        UNSAFE_BUT_CAN_BLOCK(1),
        UNSAFE_BUT_CAN_ATTACK(2),
        ATTACK(3),
        ATTACK_FROM_SAFE_DISTANCE(10)
    }

    private fun processGoals(targets: List<FramePoint>, point: FramePoint) {
        // which are safe locations?
        // which are more than 1/2 grid away or more
        // closest
        // is there an acceptable path to reach them that is also safe
//        val longAttack = ableToLongAttack && AttackLongActionDecider.targetInLongRange(targets)
    }
}