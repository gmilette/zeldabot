package bot.plan.action.routeto

import bot.plan.action.Action
import bot.plan.action.AlwaysAttack
import bot.plan.action.RouteTo
import bot.plan.action.RouteTo.Param
import bot.plan.action.RouteTo.RouteParam
import bot.plan.action.boomerangActive
import bot.state.Agent
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import util.d
import kotlin.random.Random


sealed class PointMoveAction() {
    data class Block(val gamePad: GamePad): PointMoveAction()
    data object ContinueAttack: PointMoveAction()
    data object LongAttack: PointMoveAction()
    data object BoomerangAttack: PointMoveAction()
    data object Route: PointMoveAction()
    sealed class ForceAction(val gamePad: GamePad): PointMoveAction() {
        class ExitAction(gamePad: GamePad): ForceAction(gamePad)
        class LadderAction(gamePad: GamePad): ForceAction(gamePad)
        class RandomAction(gamePad: GamePad): ForceAction(gamePad)
        class FaceEnemyForAttack(gamePad: GamePad): ForceAction(gamePad)
    }
    object ShortAttack: PointMoveAction()
}

/**
 * execute routes one after another keeps state that is relevant
 * between routes to determine what next actions are, for example
 * the [boomerangCt]
 */
class RouteExecution(val params: Param = Param()) {
    companion object {
        /**
         * dont shoot the boomerang too much
         */
        private const val WAIT_BETWEEN_BOOMERANG = 20
        private const val WAIT_BETWEEN_NOT_BOOMERANG = 2
    }

    private val attack = AlwaysAttack()
    private val attackB = AlwaysAttack(useB = true)
    private var boomerangCt = 0

    var theAttack: AlwaysAttack = attack

    fun route(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam = RouteParam(),
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList(),
        routeTo: RouteTo,
    ): GamePad {
        val preparation = RoutePreparation(params)
        preparation.prepare(state, to, param, attackableSpec)
        val determine = RouteToDetermineAction(preparation)

        val action = determine.nextAction(
            state,
            to,
            param,
            boomerangCt,
             theAttack.isAttacking(),
        )

        theAttack = if (determine.preparation.useB) {
            attackB
        } else {
            attack
        }

        d { " Route Action -> Type is $action attacking=${theAttack.isAttacking()}" }
        // keeps turning around while attacking

        return when (action) {
            is PointMoveAction.Block -> action.gamePad
            PointMoveAction.Route -> {
                attack.reset()
                attackB.reset()
//                routeTo.doRouteTo(state, to, param)
                val nextPoint = routeTo.makeNewRoute(
                    param,
                    state,
                    to,
                    preparation.avoid,
                    preparation.avoidProjectiles,
                    FramePoint()
                )
                routeTo.getActionFromRoute(nextPoint, state.link)
            }
            PointMoveAction.BoomerangAttack -> {
                boomerangCt = if (state.boomerangActive) {
                    // it's possible to get stuck doing the boomerang over and over never attacking
                    typically()
                } else {
                    WAIT_BETWEEN_NOT_BOOMERANG
                }
                attackB.nextStep(state)
            }
            PointMoveAction.LongAttack -> theAttack.nextStep(state) // before this was theAttack but only a makes sense
            PointMoveAction.ShortAttack -> theAttack.nextStep(state)
            PointMoveAction.ContinueAttack -> theAttack.nextStep(state)
            is PointMoveAction.ForceAction -> action.gamePad
        }
    }

    private fun typically(typical: Int = WAIT_BETWEEN_BOOMERANG, everySoOften: Int = typical * 2): Int =
        if (Random.nextInt(6) == 1) {
            everySoOften
        } else {
            typical
        }
}