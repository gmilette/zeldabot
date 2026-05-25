package bot.plan.action

import bot.state.*
import bot.state.map.MapConstants
import bot.state.map.grid
import bot.state.oam.Monsters
import bot.state.oam.sun1
import bot.state.oam.sun2
import util.LogFile
import util.d
import util.ifTrue

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
    private val ignoreUntilOnly: Set<Int> = emptySet(),
    private val lookForBombs: Boolean = false,
//    ignoreProjectilesRoute: Boolean = false,
    whatToAvoid: RouteTo.WhatToAvoid = RouteTo.WhatToAvoid.All
) : Action {
    companion object {
        fun make() = KillAll()
    }

    private val killAll: LogFile = LogFile("KillAll", devType = true)

    private val routeTo = RouteTo(RouteTo.Param(
        whatToAvoid = whatToAvoid)
    )

    private var previousAttack = false
    private var pressACount = 0
    private var numPressB = 0
    private var target: FramePoint = FramePoint(0, 0)
    private var forceBPress = 0

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
        get() = "KILL ALL $waitAfterAllKilled ${if (numberLeftToBeDead > 0) "until $numberLeftToBeDead" else ""} ${ignoreUntilOnly.size} ${this.lookForBombs.ifTrue("*Bombs")} "

    private fun killedAllEnemies(state: MapLocationState): Boolean {
        // unkillable should be zora and sun even though you can kill zora
        val numBubbles = state.frameState.enemies.filter { it.tile == sun2 || it.tile == sun1 }.size
        val allDeadByCount by lazy { state.frameState.enemiesLeftCalculator.allEnemiesDead(numberLeftToBeDead + numBubbles + centerEnemies(state)) }
        val allDead = state.frameState.enemiesLeftCalculator.allDead || allDeadByCount
        if (allDead) {
            d { " ALL DEAD!"}
        }
        return allDead
//        return state.clearedWithMinIgnoreLoot(numberLeftToBeDead + centerEnemies(state))
    }

    private fun centerEnemies(state: MapLocationState): Int =
        if (considerEnemiesInCenter) state.numEnemiesAliveInCenter() else 0

    override fun complete(state: MapLocationState): Boolean =
        (waitAfterAllKilled <= 0 && frameCount > 33 && killedAllEnemies(state)).also {
            val killedAll = killedAllEnemies(state)
            d { " kill all complete $it ${state.numEnemies} or ${numberLeftToBeDead} cen ${centerEnemies(state)} killedAll=$killedAll $frameCount $waitAfterAllKilled" }
//            d { "result $it ${state.clearedWithMin(numberLeftToBeDead)} ct $frameCount wait $waitAfterAllKilled" }
//            state.frameState.enemies.filter { it.state == EnemyState.Alive }.forEach {
//                d { "enemy $it dist ${it.point.distTo(state.link)}" }
//            }
        }

    override fun nextStep(state: MapLocationState): GamePad {
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
        d { " KILL ALL step ${state.currentMapCell.mapLoc} count $frameCount wait $waitAfterAllKilled needLong $needLongWait" }

        for (enemy in state.frameState.enemies.filter { it.state != EnemyState.Dead }) {
            d { " enemy: $enemy" }
        }

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
            GamePad.None // just wait
        } else {
            val enemyFilter = KillAllTargetFilters(state, ignoreUntilOnly, targetOnly, considerEnemiesInCenter)
            val aliveEnemies = enemyFilter.filter(lookForBombs)

            if (killedAllEnemies(state)) {
                waitAfterAllKilled--
                GamePad.None // just wait
            } else {
                // just wait a little
                waitAfterAllKilled = 5
                val firstEnemyOrNull = aliveEnemies.firstOrNull()
                if (firstEnemyOrNull == null) {
                    // added for the dragon, doesn't really work well
                    d { "No enemies!!" }
                    return routeTo.routeTo(
                        state, listOf(FramePoint(8.grid, 6.grid)),
                        RouteTo.RouteParam(forceNew = true)
                    )
                }
                firstEnemyOrNull.let { firstEnemy ->
                    val previousTarget = target
                    target = firstEnemy.point
                    val link = state.frameState.link
                    // force a new route if this has changed targets
                    val forceNew = previousTarget.oneStr != target.oneStr
                    d { "Plan: attack: ${firstEnemy.point} target changed was $previousTarget now $target forceNew = $forceNew" }

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
//                    routeTo.routeToBest(
                        state, targetsToAttack,
                        RouteTo.RouteParam(
                            useB = firstAttackBomb || useBombs,
                            allowRangedAttack = !firstAttackBomb,
                            forceNew = forceNew,
                            allowBlock = allowBlock,
                            rParam = RouteTo.RoutingParamCommon(
                                attackTarget = target,
                                mapNearest = true,
                                finishWithinStrikingRange = true
                            ),
                        ),
                        attackableSpec = if (enemyFilter.attackOnlySpecified) aliveEnemies else emptyList()
                    ).let {
                        if (numPressB > 0) {
                            numPressB--
                            GamePad.B
                        } else if (it == GamePad.B && (firstAttackBomb || useBombs)) {
                            numPressB = 3
                            d { "USE BOMB! it=$it first $firstAttackBomb $useBombs" }
                            numPressB++
                            if (numPressB > 3) {
                                firstAttackBomb = false
                            }
                            GamePad.B
                        } else {
                            d { "move numbB=$numPressB" }
                            it
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