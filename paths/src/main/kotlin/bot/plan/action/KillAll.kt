package bot.plan.action

import bot.state.*
import bot.state.map.MapConstants
import bot.state.map.grid
import bot.state.oam.Monsters
import util.LogFile
import util.d

class KillAll(
    /**
     * keep going after the current enemy for at least 60 frames before switching to another
     */
    private val sameEnemyFor: Int = 60,
    private val useBombs: Boolean = false,
    private val waitAfterAttack: Boolean = false,
    private val numberLeftToBeDead: Int = 0,
    // do not try to kill the enemies in the center
    private val considerEnemiesInCenter: Boolean = false,
    /**
     * how long to wait after all enemies dead to assume all is dead
     */
    private var needLongWait: Boolean = false,
    /**
     * only target these tiles
     */
    private val targetOnly: List<Int> = listOf(),
    /**
     * only target these tiles
     */
    private val ignoreProjectiles: List<Int> = listOf(),
    /**
     * if true, ignore all enemies (useful for level dragon fighting, but makes link suicidal)
     * consider projectiles though
     */
//    ignoreEnemies: Boolean = false,
    private var firstAttackBomb: Boolean = false,
    private var allowBlock: Boolean = true,
//    ignoreProjectilesRoute: Boolean = false,
    whatToAvoid: RouteTo.WhatToAvoid = RouteTo.WhatToAvoid.All
) : Action {
    companion object {
        fun make() = KillAll()
    }

    private val killAll: LogFile = LogFile("KillAll")

    private val routeTo = RouteTo(RouteTo.Param(
        whatToAvoid = whatToAvoid)
    )
    private val criteria = KillAllCompleteCriteria()

    private var previousAttack = false
    private var pressACount = 0
    private var numPressB = 0
    private var target: FramePoint = FramePoint(0, 0)

    private var frameCount = 0
    private var waitAfterPressing = 0

    // just be sure everything is dead and not just slow to move
    private var waitAfterAllKilled = 0

    override fun reset() {
        super.reset()
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun target(): FramePoint {
        return target
    }

    override val name: String
        get() = "KILL ALL $waitAfterAllKilled ${if (numberLeftToBeDead > 0) "until $numberLeftToBeDead" else ""}"

    private fun killedAllEnemies(state: MapLocationState): Boolean {
        return state.clearedWithMinIgnoreLoot(numberLeftToBeDead + centerEnemies(state))
    }

    private fun centerEnemies(state: MapLocationState): Int =
        if (considerEnemiesInCenter) state.numEnemiesAliveInCenter() else 0

    private fun killedAllEnemiesIgnoreLoot(state: MapLocationState): Boolean {
        return state.clearedWithMinIgnoreLoot(numberLeftToBeDead)
    }

    override fun complete(state: MapLocationState): Boolean =
//        criteria.complete(state)
        (waitAfterAllKilled <= 0 && frameCount > 33 && killedAllEnemies(state)).also {
            d { " kill all complete $it ${state.numEnemies} or ${numberLeftToBeDead}" }
            d { "result $it ${state.clearedWithMin(numberLeftToBeDead)} ct $frameCount wait $waitAfterAllKilled" }
            state.frameState.enemies.filter { it.state == EnemyState.Alive }.forEach {
                d { "enemy $it dist ${it.point.distTo(state.link)}" }
            }
        }

    override fun nextStep(state: MapLocationState): GamePad {
        val numEnemiesInCenter = state.numEnemiesAliveInCenter()
        // dont have to wait on any levels that have boomerangs
        // which gets confused with ghosts
        // if you are throwing boomerangs, this isnt going to work
        if (false && state.frameState.seenBoomerang) {
            needLongWait = false
        } else {
            // once set to true, do not change it back
            // only the wizzrobes
            if (!needLongWait && !considerEnemiesInCenter && state.frameState.level in Monsters.levelsWithWizzrobes) {
                needLongWait = state.longWait.isNotEmpty()
                if (needLongWait) {
                    d { " set long waited "}
                } else {
                    d { " no long wait "}
                }
            }
        }
//        needLongWait = false
        d { " KILL ALL step ${state.currentMapCell.mapLoc} count $frameCount wait $waitAfterAllKilled center: $numEnemiesInCenter needLong $needLongWait" }

        for (enemy in state.frameState.enemies.filter { it.state != EnemyState.Dead }) {
            d { " enemy: $enemy" }
        }
        criteria.update(state)

        frameCount++
        when {
            // reset on the last count
            pressACount == 1 -> {
                pressACount = 0
                firstAttackBomb = false // works!
                // have to release for longer than 1
                d { "Press A last time" }
                return GamePad.None
            }
            //4
            pressACount > 3 -> {
                pressACount--
                d { "Press A" }
                return if (useBombs || firstAttackBomb) GamePad.B else GamePad.A
            }

            // release for a few steps
            pressACount > 1 -> {
                pressACount--
                return if (useBombs) GamePad.ReleaseB else GamePad.ReleaseA
            }

            // only for boss
            pressACount == 0 && waitAfterPressing > 0 -> {
                d { "Press A WAIT" }
                waitAfterPressing--
                return GamePad.None
            }
        }

        return if (killedAllEnemies(state)) {
            d { " no enemies all killed ${numberLeftToBeDead}" }
            waitAfterAllKilled--
            return GamePad.None // just wait
        } else {
            // first kill the enemies not in center
            // but if there is a heart prefer that!
            var aliveEnemies = state.frameState.heartsClosestToLink().ifEmpty {
                state.frameState.enemiesClosestToLink()
            }.toMutableList()
            // if you have clock enabled, the ghost can get stuck on a location that is not passable
            // we should try to route to it still with the nearest
//            aliveEnemies = aliveEnemies.filter { state.currentMapCell.passable.get(it.point) }.toMutableList()
            if (considerEnemiesInCenter) {
                // all enemies
                if (numEnemiesInCenter != aliveEnemies.size) {
                    val centers = state.enemiesAliveInCenter()
                    for (agent in centers) {
                        aliveEnemies.remove(agent)
                    }
                } else {
                    d { "Attack center enemies" }
                }
            }
            // NEW
//            aliveEnemies = aliveEnemies.filter { !it.damaged }
            // need special handling, cant route into center
            if (targetOnly.isNotEmpty()) {
                d { " target only $targetOnly" }
                aliveEnemies = aliveEnemies.filter { targetOnly.contains(it.tile) }.toMutableList()
            }

            // for rhino, adjust target location
            // action

            aliveEnemies.forEach {
                d { "alive enemy $it dist ${it.point.distTo(state.frameState.link.point)}" }
            }

            if (killedAllEnemies(state)) {
                waitAfterAllKilled--
                return GamePad.None // just wait
            } else {
                // 110 too low for bats
//                waitAfterAllKilled = if (needLongWait) 250 else 50
                // need 250 for ghosts only
                waitAfterAllKilled = if (needLongWait) 250 else 50
                val firstEnemyOrNull = aliveEnemies.firstOrNull()
                if (firstEnemyOrNull == null) {
                    // added for the dragon, doesn't really work well
                    d { "No enemies!!" }
                    return routeTo.routeTo(
                        state, listOf(FramePoint(8.grid, 6.grid)),
                        RouteTo.RouteParam(forceNew = true)
                    )
                }
                // no enemies? do dodge
                // handle the null?? need to test
                firstEnemyOrNull.let { firstEnemy ->
                    val previousTarget = target
                    target = firstEnemy.point
                    val link = state.frameState.link
                    val dist = firstEnemy.point.distTo(link.point)
                    // force a new route if this has changed targets
                    val forceNew = previousTarget.oneStr != target.oneStr
                    d { "Plan: target changed was $previousTarget now ${target} forceNew = $forceNew" }

                    // possibly remove some attack points in front of the enemy
                    val targetsToAttack = when {
                        (firstEnemy.state == EnemyState.Loot) -> target.lootTargets
                        (firstEnemy.canAttackFront) ->
                            AttackActionDecider.attackPointsNoCorner(target)

                        else -> AttackActionDecider.attackPoints(target, not = firstEnemy.dir)
                    }

                    if (link.point in targetsToAttack) {
                        d { " !On Target " }
                    }

                    // could route to all targets
                    routeTo.routeTo(
                        state, targetsToAttack,
                        RouteTo.RouteParam(
                            useB = firstAttackBomb,
                            forceNew = forceNew,
                            allowBlock = allowBlock,
                            rParam = RouteTo.RoutingParamCommon(
                                attackTarget = target,
                                mapNearest = true,
                                finishWithinStrikingRange = true
                            ),
                        )
                    ).also {
                        if (it == GamePad.B && firstAttackBomb) {
                            d {"USE BOMB!" }
                            numPressB++
                            if (numPressB > 3) {
                                firstAttackBomb = false
                            }
                        }
                    }
                } ?: GamePad.None
            }
        }
    }
}

class KillAllCompleteCriteria {
    private var count = 0
    private var waitAfterAllKilled = 0

    fun update(state: MapLocationState) {
        count++
        if (state.hasEnemies) {
            waitAfterAllKilled = 110
        } else {
            waitAfterAllKilled--
        }
    }

    fun complete(state: MapLocationState): Boolean =
        (waitAfterAllKilled <= 0 && count > 33 && state.cleared).also {
            d { " kill all complete $it" }
            d { " kill all status ${state.frameState.enemies}" }
            state.frameState.enemies.forEach {
                d { "loot $it dist ${it.point.distTo(state.link)}" }
            }
        }
}

class AttackOnce(useB: Boolean = false, private val freq: Int = 5) :
    Action {
    private var frames = 0
    private val gameAction = if (useB) GamePad.B else GamePad.A

    override fun nextStep(state: MapLocationState): GamePad {
        // just always do it
        val move = if (frames < 0) {
            GamePad.None
        } else {
            when {
                frames % 10 < freq -> gameAction
                else -> GamePad.None
            }
        }
        frames++
        return move
    }

    override fun complete(state: MapLocationState): Boolean =
        frames >= 10

}

class AlwaysAttack(useB: Boolean = false, private val freq: Int = 5, private val otherwiseRandom: Boolean = false) :
    Action {
    private var frames = attackLength
    val gameAction = if (useB) GamePad.B else GamePad.A

    companion object {
        private const val attackLength = 10
    }

    fun attackWaiting(): Boolean =
        frames < 0

    override fun nextStep(state: MapLocationState): GamePad {
        // just always do it
        val move = if (frames < 0) {
            d { "*wait for frames $attackLength" }
            GamePad.None
        } else {
            when {
                frames % attackLength < freq -> {
                    d { "* do attack now $attackLength" }
                    gameAction
                }
                else -> {
                    d { "* do attack now wait $attackLength $otherwiseRandom" }
                    if (otherwiseRandom) GamePad.randomDirection(state.link) else GamePad.None
                }
            }
        }
        frames++
        return move
    }

    /**
     * link will attack for 10 frames. If frames start at 10, then continue attacking until 10 frames pass
     * (doesn't seem to hurt things
     */
    fun isAttacking(): Boolean =
        frames < (attackLength * 2) && frames != attackLength

    override fun reset() {
        frames = attackLength
    }

    override fun complete(state: MapLocationState): Boolean =
        false
}

class KillInCenterO : Action {
    private var frames = 0

    // need to wait to make sure they are killed maybe
    override fun complete(state: MapLocationState): Boolean =
        state.numEnemiesAliveInCenter() == 0

    override fun nextStep(state: MapLocationState): GamePad {
        val move = if (frames < 10) {
            GamePad.MoveUp
        } else {
            if (frames % 10 < 5) {
                GamePad.A
            } else {
                GamePad.None
            }
        }
        frames++
        return move
    }

    override val name: String
        get() = "KillInCenter"
}

class KillInCenter : Action {
    object KillInCenterLocations {
        // should be the middle of the attack area
        val position = FramePoint(3.grid, 8.grid)

        //        val attackFrom = FramePoint(8.grid, 8.grid)
        val attackFrom = FramePoint(8.grid, 8.grid)
    }

    // more debugging
    private val positionShootActions = mutableListOf(
        InsideNavAbout(
            KillInCenterLocations.attackFrom,
            1,
            vertical = 2
        ),
        GoIn(10, GamePad.MoveUp, true)
//        AlwaysAttack()
    )

    init {
        repeat(times = 5) {
            positionShootActions.add(GoIn(10, GamePad.MoveUp, true))
            positionShootActions.add(GoIn(3, GamePad.A, true))
            positionShootActions.add(GoIn(3, GamePad.None, true))
        }
    }


    private val positionShoot = OrderedActionSequence(positionShootActions, restartWhenDone = true)

    override fun complete(state: MapLocationState): Boolean =
        state.numEnemiesAliveInCenter() == 0

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d { "KillInCenter" }
        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack

        return positionShoot.nextStep(state)
    }

    override val name: String
        get() = "KillInCenter ${positionShoot.stepName} ${positionShoot.name}"
}

class DeadForAWhile(
    private val limit: Int = 450,
    val reset: Boolean = false,
    val completeCriteria: (MapLocationState) -> Boolean
) {
    var frameCount = 0

    operator fun invoke(state: MapLocationState): Boolean {
        return completeCriteria(state) && frameCount > limit
    }

    fun nextStep(state: MapLocationState) {
        d { "DEAD for a while $frameCount" }
        if (completeCriteria(state)) {
            frameCount++
        } else {
            if (reset) {
                frameCount = 0
            }
        }
    }

    fun seenEnemy() {
        frameCount = 0
    }
}
