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
        d { " KILL ALL step ${state.currentMapCell.mapLoc} count $frameCount wait $waitAfterAllKilled" }

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
                // just wait a little in case a loot is going to appear
                waitAfterAllKilled = 5
                val firstEnemyOrNull = aliveEnemies.firstOrNull()
                if (firstEnemyOrNull == null) {
                    // added for the dragon, doesn't really work well
                    d { "No enemies!!" }
                    return routeTo.routeTo(
                        state, listOf(FramePoint(8.grid, 6.grid)),
                        RouteTo.RouteParam()
                    )
                }
                firstEnemyOrNull.let { firstEnemy ->
                    target = firstEnemy.point
                    val link = state.frameState.link

                    // possibly remove some attack points in front of the enemy
                    val targetsToAttack = when {
                        (firstEnemy.state == EnemyState.Loot) -> target.lootTargets
                        (firstEnemy.canAttackFront) ->
                            AttackActionDecider.attackPointsNoCorner(target)

                        else -> AttackActionDecider.attackPoints(target, not = firstEnemy.dir)
                    }

                    d { "Plan: attack: ${firstEnemy.point} target is $target targets $targetsToAttack" }

                    if (link.point in targetsToAttack) {
                        d { " !On Target " }
                    }

                    val killRouting = false
//                    if (killRouting) {
//                        val pad = routeToBest(state, aliveEnemies)
//                        // TODO: this probably doesn't work
//                        if (pad == GamePad.B && (firstAttackBomb || useBombs)) {
//                            numPressB = 3
//                            d { "USE BOMB! it=$pad first $firstAttackBomb $useBombs" }
//                            numPressB++
//                            if (numPressB > 3) {
//                                firstAttackBomb = false
//                            }
//                            GamePad.B
//                        }
//                        return pad
//                    }

                    // could route to all targets
                    routeTo.routeTo(
                        state, targetsToAttack,
                        RouteTo.RouteParam(
                            useB = firstAttackBomb || useBombs,
                            allowRangedAttack = !firstAttackBomb,
                            allowBlock = allowBlock,
                            breadthFirst = killRouting,
                            rParam = RouteTo.RoutingParamCommon(
                                mapNearest = true,
                                finishWithinStrikingRange = true
                            ),
                        ),
                        attackableSpec = aliveEnemies
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

    private fun routeToBest(state: MapLocationState, aliveEnemies: List<Agent>): GamePad {
        return routeTo.routeToBest(
            state,
            RouteTo.RouteParam(
                useB = firstAttackBomb || useBombs,
                allowRangedAttack = !firstAttackBomb,
                allowBlock = allowBlock,
                rParam = RouteTo.RoutingParamCommon(
                    mapNearest = true,
                    finishWithinStrikingRange = true
                ),
            ),
            attackableSpec = aliveEnemies
        )
    }

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