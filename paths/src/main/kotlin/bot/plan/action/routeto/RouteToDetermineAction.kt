package bot.plan.action.routeto

import bot.plan.action.AttackActionBlockDecider
import bot.plan.action.AttackActionDecider
import bot.plan.action.AttackLongActionDecider
import bot.plan.action.LadderActionDecider
import bot.plan.action.RouteTo
import bot.plan.action.RouteTo.WhatToAvoid
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.toGamePad
import util.d
import kotlin.random.Random

class RouteToDetermineAction(val preparation: RoutePreparation) {
    private val ladderDecider = LadderActionDecider()

    fun nextAction(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteTo.RouteParam = RouteTo.RouteParam(),
        boomerangCt: Int,
        isAttacking: Boolean,
    ): PointMoveAction {
//        if (to.isEmpty()) {
//            w { " no where to go " }
//            return PointMoveAction.ForceAction.RandomAction(NavUtil.randomDir(state.link))
//        }

        val linkDir = state.frameState.link.dir
        val link = state.link

        val attackablePoints by lazy { preparation.attackable.points }
        val blockReflex: GamePad? = if (param.allowBlock && preparation.params.whatToAvoid != WhatToAvoid.JustEnemies) {
            AttackActionBlockDecider.blockReflex(state)
        } else {
            null
        }
        val shouldLongAttack by lazy { param.allowRangedAttack && AttackLongActionDecider.shouldShootSword(state, attackablePoints) }
        val shouldLongBoomerang by lazy { param.allowRangedAttack && boomerangCt <= 0 && AttackLongActionDecider.shouldBoomerang(state, preparation.boomerangable) }
        val inRangeOf by lazy { AttackActionDecider.inRangeOf(linkDir, link, attackablePoints, param.useB, faceEnemy = true) }
        val shouldShortAttack by lazy { inRangeOf.isAttack }
        val shouldFace by lazy { inRangeOf.isDirection }
        val ladderAction by lazy { ladderDecider.doLadderAction(state) }
        val exitOffScreenAction by lazy { exitOfScreen(state.frameState.link.point, to) }

        val canAttack = preparation.canAttack
        val attackPossible = preparation.attackPossible
//        val canAttack = param.allowAttack && !state.frameState.linkDoingAnAttack() && (param.useB || state.frameState.canUseSword)

        val considerAttacks = RouteTo.allowAttack && attackPossible
        d { " Determine route action attacking=$isAttacking consider=$considerAttacks "}

        return when {
            // don't get stuck attacking emptu space when the clock is activated
            (state.frameState.clockActivated && Random.nextInt(10) == 1) -> {
                d { " Route Action -> Force Route Clock" }
                PointMoveAction.Route
            }
            // get stuck in level1 trying to block the boomerang
            blockReflex != null -> {
                d { " Route Action -> Block Reflex! $blockReflex" }
                PointMoveAction.Block(blockReflex)
            }
            // this makes it so that link will keep trying to attack, instead of turning around during it
            isAttacking -> {
                d { " Route Action -> Keep Attacking" }
                PointMoveAction.ContinueAttack
            }
            // idea: if sword is flying, do not route in to short attack
            // instead evade until the sword is not flying
            // then after that do normal routing
            considerAttacks && canAttack && shouldLongAttack -> {
                d { " Route Action -> LongAttack" }
                PointMoveAction.LongAttack
            }
            // prefer short attack over boomeranging
            considerAttacks && canAttack && shouldShortAttack -> {
                d { " Route Action -> Attack" }
                PointMoveAction.ShortAttack
            }
            considerAttacks && preparation.canLongAttack && shouldLongBoomerang -> {
                d { " Route Action -> LongAttack Boomerang" }
                PointMoveAction.BoomerangAttack
            }
            considerAttacks && canAttack && shouldFace -> {
                d { " Route Action -> Face $inRangeOf" }
                PointMoveAction.ForceAction.FaceEnemyForAttack(inRangeOf)
            }
            // put this lower in the list so that link might
            // attack even if on ladder
            ladderAction != Direction.None -> {
                PointMoveAction.ForceAction.LadderAction(ladderAction.toGamePad())
            }
            exitOffScreenAction != GamePad.None -> {
                d { " Route Action -> Exit" }
                PointMoveAction.ForceAction.ExitAction(exitOffScreenAction)
            }
            else -> {
                d { " Route Action -> Route" }
                PointMoveAction.Route
            }
        }
    }

    private fun exitOfScreen(linkPt: FramePoint, to: List<FramePoint>): GamePad {
        if (to.isEmpty()) return GamePad.None
        // why this? let's go without it and see if it's ok
        // it gets stuck almost about to exit some levels
        // i'm not sure if this fixes it
        val closest = to.minBy { it.distTo(linkPt) }
        return if (linkPt.distTo(closest) <= 1) {
            d { " CLOSE!! $closest" }
            if (closest.y <= 1) {
                d { " CLOSE!! up" }
                GamePad.MoveUp
            } else if (closest.x <= 1) {
                d { " CLOSE!! left" }
                GamePad.MoveLeft
            } else if (closest.x >= MapConstants.MAX_X - 2) {
                d { " CLOSE!! right" }
                GamePad.MoveRight
            } else if (closest.y >= MapConstants.MAX_Y - 2) {
                d { " CLOSE!! down" }
                GamePad.MoveDown
            } else {
                GamePad.None
            }
        } else {
            GamePad.None
        }
    }
}