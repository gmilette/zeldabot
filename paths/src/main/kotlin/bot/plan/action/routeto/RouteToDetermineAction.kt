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
import util.ZRandom

class RouteToDetermineAction(val preparation: RoutePreparation) {
    private val ladderDecider = LadderActionDecider()
    private val DEBUG = false

    fun nextAttackAction(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteTo.RouteParam = RouteTo.RouteParam(),
        boomerangCt: Int,
        isAttacking: Boolean,
        faceEnemy: Boolean, // note only for nextAttackAction right now, I dont know how to compute it
        linkDir: Direction = state.frameState.link.dir,
        link: FramePoint = state.link
    ): PointMoveAction {
        val attackPossible = preparation.attackPossible
        val considerAttacks = RouteTo.allowAttack && attackPossible
        d(DEBUG) { " Determine route action consider=$considerAttacks "}

        if (!considerAttacks) {
            d(DEBUG) { " Route Action -> Route (consider)" }
            return PointMoveAction.Route
        }

        if (isAttacking) {
            return PointMoveAction.ContinueAttack
        }

        val canAttack = preparation.canAttack
        val shouldLongBoomerang by lazy { preparation.canLongAttack && param.allowRangedAttack && boomerangCt <= 0 && AttackLongActionDecider.shouldBoomerang(state, preparation.boomerangable, link, linkDir) }
        val attackablePoints by lazy { preparation.attackable(linkDir).points }
        val shouldLongAttack by lazy { canAttack && param.allowRangedAttack && AttackLongActionDecider.shouldShootSword(state, attackablePoints, link, linkDir) }
        val inRangeOf by lazy {
            AttackActionDecider.inRangeOf(linkDir, link, attackablePoints, param.useB, faceEnemy = faceEnemy)
        }
        val shouldShortAttack by lazy { canAttack && inRangeOf.isAttack }
        val shouldFace by lazy { canAttack && inRangeOf.isDirection }

        return when {
            shouldLongAttack -> {
                d(DEBUG) { " Route Action -> Long" }
                PointMoveAction.LongAttack
            }
            shouldShortAttack -> {
                d(DEBUG) { " Route Action -> Short" }
                PointMoveAction.ShortAttack
            }
            shouldLongBoomerang -> {
                d(DEBUG) { " Route Action -> Boomerang" }
                PointMoveAction.BoomerangAttack
            }
            shouldFace -> {
                d(DEBUG) { " Route Action -> Face $inRangeOf" }
                PointMoveAction.ForceAction.FaceEnemyForAttack(inRangeOf)
            }
            else -> {
                d(DEBUG) { " Route Action -> Route" }
                PointMoveAction.Route
            }
        }
    }

    fun nextAction(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteTo.RouteParam = RouteTo.RouteParam(),
        boomerangCt: Int,
        isAttacking: Boolean,
        linkDir: Direction = state.frameState.link.dir,
        link: FramePoint = state.link
    ): PointMoveAction {
//        if (to.isEmpty()) {
//            w { " no where to go " }
//            return PointMoveAction.ForceAction.RandomAction(NavUtil.randomDir(state.link))
//        }

        val attackablePoints by lazy { preparation.attackable(linkDir).points }
        val blockReflex: GamePad? = if (param.allowBlock && preparation.params.whatToAvoid != WhatToAvoid.JustEnemies) {
            AttackActionBlockDecider.blockReflex(state)
        } else {
            null
        }

        // check if link intersects any enemies or dangerzones..?
        val isSafe = state.currentMapCell.zstar.neighborFinder
        val canAttack = preparation.canAttack
        val shouldLongBoomerang by lazy { preparation.canLongAttack && param.allowRangedAttack && boomerangCt <= 0 && AttackLongActionDecider.shouldBoomerang(state, preparation.boomerangable, link, linkDir) }
        val shouldLongAttack by lazy { canAttack && param.allowRangedAttack && AttackLongActionDecider.shouldShootSword(state, attackablePoints, link, linkDir) }
        val inRangeOf by lazy { AttackActionDecider.inRangeOf(linkDir, link, attackablePoints, param.useB, faceEnemy = true) }
        val shouldShortAttack by lazy { canAttack && inRangeOf.isAttack }
        val shouldFace by lazy { canAttack && inRangeOf.isDirection }

        val ladderAction by lazy { ladderDecider.doLadderAction(state) }
        val exitOffScreenAction by lazy { exitOfScreen(state.frameState.link.point, to) }

        val attackPossible = preparation.attackPossible
//        val canAttack = param.allowAttack && !state.frameState.linkDoingAnAttack() && (param.useB || state.frameState.canUseSword)

        val considerAttacks = RouteTo.allowAttack && attackPossible
        d(DEBUG) { " Determine route action attacking=$isAttacking consider=$considerAttacks "}

        return when {
            // don't get stuck attacking emptu space when the clock is activated
            (state.frameState.clockActivated && ZRandom.nextInt(10) == 1) -> {
                d(DEBUG) { " Route Action -> Force Route Clock" }
                PointMoveAction.Route
            }
            // get stuck in level1 trying to block the boomerang
            blockReflex != null -> {
                d(DEBUG) { " Route Action -> Block Reflex! $blockReflex" }
                PointMoveAction.Block(blockReflex)
            }
            // this makes it so that link will keep trying to attack, instead of turning around during it
            isAttacking -> {
                d(DEBUG) { " Route Action -> Keep Attacking" }
                PointMoveAction.ContinueAttack
            }
            // idea: if sword is flying, do not route in to short attack
            // instead evade until the sword is not flying
            // then after that do normal routing
            considerAttacks && shouldLongAttack -> {
                d(DEBUG) { " Route Action -> LongAttack" }
                PointMoveAction.LongAttack
            }
            // prefer short attack over boomeranging
            considerAttacks && shouldShortAttack -> {
                d(DEBUG) { " Route Action -> Attack" }
                PointMoveAction.ShortAttack
            }
            considerAttacks && shouldLongBoomerang -> {
                d(DEBUG) { " Route Action -> LongAttack Boomerang" }
                PointMoveAction.BoomerangAttack
            }
            considerAttacks && shouldFace -> {
                d(DEBUG) { " Route Action -> Face $inRangeOf" }
                PointMoveAction.ForceAction.FaceEnemyForAttack(inRangeOf)
            }
            // put this lower in the list so that link might
            // attack even if on ladder
            ladderAction != Direction.None -> {
                PointMoveAction.ForceAction.LadderAction(ladderAction.toGamePad())
            }
            exitOffScreenAction != GamePad.None -> {
                d(DEBUG) { " Route Action -> Exit" }
                PointMoveAction.ForceAction.ExitAction(exitOffScreenAction)
            }
            else -> {
                d(DEBUG) { " Route Action -> Route" }
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
            d(DEBUG) { " CLOSE!! $closest" }
            if (closest.y <= 1) {
                d(DEBUG) { " CLOSE!! up" }
                GamePad.MoveUp
            } else if (closest.x <= 1) {
                d(DEBUG) { " CLOSE!! left" }
                GamePad.MoveLeft
            } else if (closest.x >= MapConstants.MAX_X - 2) {
                d(DEBUG) { " CLOSE!! right" }
                GamePad.MoveRight
            } else if (closest.y >= MapConstants.MAX_Y - 2) {
                d(DEBUG) { " CLOSE!! down" }
                GamePad.MoveDown
            } else {
                GamePad.None
            }
        } else {
            GamePad.None
        }
    }
}