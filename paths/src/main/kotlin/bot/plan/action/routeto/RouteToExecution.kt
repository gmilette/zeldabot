package bot.plan.action.routeto

import bot.plan.action.AlwaysAttackWhenCan
import bot.plan.action.RouteTo
import bot.plan.action.RouteTo.Param
import bot.plan.action.RouteTo.RouteParam
import bot.plan.action.boomerangActive
import bot.state.*
import bot.state.map.toGamePad
import util.ZRandom
import util.d


sealed class PointMoveAction {
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
         * typical wait time between shooting the boomerang
         */
        private const val WAIT_BETWEEN_BOOMERANG = 15
        private const val WAIT_BETWEEN_NOT_BOOMERANG = 2
    }

    private val attack = AlwaysAttackWhenCan()
    private val attackB = AlwaysAttackWhenCan(useB = true)
    var boomerangCt = 0
        private set

    var theAttack: AlwaysAttackWhenCan = attack
        private set

    fun getDetermine(
        state: MapLocationState,
        param: RouteParam,
        attackableSpec: List<Agent> = emptyList(),
    ): RouteToDetermineAction {
        val prep = RoutePreparation(params)
        prep.prepare(state, param, attackableSpec)
        return RouteToDetermineAction(prep)
    }

    fun route(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam = RouteParam(),
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList(),
        routeTo: RouteTo,
    ): GamePad {
        val preparation = RoutePreparation(params)
        preparation.prepare(state, param, attackableSpec)
        val determine = RouteToDetermineAction(preparation)

        theAttack = if (determine.preparation.useB) {
            attackB
        } else {
            attack
        }
        boomerangCt = (boomerangCt - 1).coerceAtLeast(0)

        val action = determine.nextAction(
            state,
            to,
            state.frameState.enemies,
            param,
            boomerangCt,
             theAttack.isAttacking(state),
        )

        d { " Route Action -> Type is $action attacking=${theAttack.isAttacking(state)}" }
        // keeps turning around while attacking

        return when (action) {
            is PointMoveAction.Block -> action.gamePad
            PointMoveAction.Route -> {
                attack.reset()
                attackB.reset()
                val nextPoint = routeTo.makeNewRoute(
                    param,
                    state,
                    to,
                    preparation.avoid,
                    preparation.avoidProjectiles,
                    FramePoint(),
                    attackableSpec
                )
                getActionFromRoute(nextPoint, state.link, state)
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

    fun getActionFromRoute(nextPoint: FramePoint, linkPt: FramePoint, state: MapLocationState): GamePad {
        return when {
            nextPoint.isZero && linkPt.x == 0 -> GamePad.MoveLeft
            nextPoint.isZero && linkPt.y == 0 -> GamePad.MoveUp
            // already in a good spot
            // this is the default action when link does not find a safe or unsafe route
            nextPoint.isZero -> theAttack.nextStep(state)
            else -> nextPoint.direction?.toGamePad() ?: linkPt.directionTo(nextPoint)
        }.also {
            //        writeFile(to, state, it)
            d { " link point $linkPt next point $nextPoint dir: $it ${if (nextPoint.direction != null) "HAS DIR ${nextPoint.direction}" else ""}" }
        }
    }

    private fun typically(typical: Int = WAIT_BETWEEN_BOOMERANG, everySoOften: Int = typical * 2): Int =
        if (ZRandom.nextInt(3) == 1) {
            // get unstuck by waiting much longer sometimes
            everySoOften
        } else {
            typical
        }
}