package bot.plan.action

import bot.state.*
import bot.state.map.grid
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
     */
    private val ignoreEnemies: Boolean = false,
    private val roundX: Boolean = false,
    private var firstAttackBomb: Boolean = false
) :
    Action {
    private val routeTo = RouteTo(RouteTo.Param(dodgeEnemies = true))
    private val criteria = KillAllCompleteCriteria()

    private var previousAttack = false
    private var pressACount = 0
    private var target: FramePoint = FramePoint(0, 0)

    private var frameCount = 0
    private var waitAfterPressing = 0

    // just be sure everything is dead and not just slow to move
    private var waitAfterAllKilled = 200

    override fun reset() {
        super.reset()
    }

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun target(): FramePoint {
        return target
    }

    override val name: String
        get() = "KILL ALL ${if (numberLeftToBeDead > 0) "until $numberLeftToBeDead" else ""}"

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
        needLongWait = state.longWait.isNotEmpty()
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
            d { " no enemies" }
            waitAfterAllKilled--
            return GamePad.None // just wait
        } else {
            // first kill the enemies not in center
            var aliveEnemies: MutableList<Agent> = state.frameState.enemiesClosestToLink().toMutableList()
            aliveEnemies = aliveEnemies.filter { state.currentMapCell.passable.get(it.point) }.toMutableList()
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
            if (ignoreProjectiles.isNotEmpty()) {
                d { " ignore projectiles "}
                aliveEnemies = aliveEnemies.filter { !ignoreProjectiles.contains(it.tile) }.toMutableList()
            }

            // for rhino, adjust target location
            // action

//            aliveEnemies.forEach {
//                d { "alive enemy $it dist ${it.point.distTo(state.frameState.link.point)}" }
//            }

            if (killedAllEnemies(state)) {
                waitAfterAllKilled--
                return GamePad.None // just wait
            } else {
                // 110 too low for bats
                waitAfterAllKilled = if (needLongWait) 250 else 50
                val firstEnemyOrNull = aliveEnemies.firstOrNull()
                if (firstEnemyOrNull == null) {
                    // added for the dragon, doesn't really work well
                    d { "No enemies!!" }
                    return routeTo.routeTo(state, listOf(FramePoint(8.grid, 6.grid)),
                        RouteTo.RouteParam(forceNew = true)
                    )
                }
                // no enemies? do dodge
                // handle the null?? need to test
                firstEnemyOrNull?.let { firstEnemy ->
                    val previousTarget = target
                    target = firstEnemy.point
                    val link = state.frameState.link
                    val dist = firstEnemy.point.distTo(link.point)
                    //d { " go find $firstEnemy from $link distance: $dist"}
                    when {
                        state.frameState.canUseSword && AttackActionDecider.shouldAttack(state) -> {
                            // is linked turned in the correct direction towards
                            // the enemy?
                            previousAttack = true
                            pressACount = 5
                            // for the rhino
                            if (waitAfterAttack) {
                                waitAfterPressing = 60
                            }
                            if (firstAttackBomb || useBombs) {
                                GamePad.B
                            } else GamePad.A
                        }

                        else -> {
                            // changed enemies
                            // why is this here?
                            // guess, don't keep switching target?
                            // I dont like this, it could lead to chasing the wrong target
                            // remove
//                            sameEnemyCount++
//                            val sameEnemyForTooLong = sameEnemyCount > sameEnemyFor
//                            if (sameEnemyForTooLong) {
//                                sameEnemyCount = 0
//                            }
//                            val forceNew = previousTarget != target && sameEnemyForTooLong

                            // force a new route if this has changed targets
                            val forceNew = previousTarget.oneStr != target.oneStr
                            d { "Plan: target changed was $previousTarget now ${target} forceNew = $forceNew"}
                            // can't tell if the target has changed
                            // handle replanning when just close, this might be fine

                            val targetsToAttack: List<FramePoint> = if (roundX) {
                                // for gradon
                                val mod = target.x % 8
                                listOf(FramePoint(target.x - mod, target.y))
                            } else {
                                listOf(target)
                            }

                            // could route to all targets
                            routeTo.routeTo(state, targetsToAttack,
                                RouteTo.RouteParam(forceNew = forceNew, attackTarget = target, ignoreEnemies = this.ignoreEnemies)
                            )


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

@Deprecated("unused")
class ClockActivatedKillAll : Action {
    private val routeTo = RouteTo()
    private val criteria = KillAllCompleteCriteria()

    private var target: FramePoint = FramePoint()

    private var enemies = mutableListOf<Agent>()

    private var pressedACt = 0

    override val name: String
        get() = "pickup dropped item and kill"

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun target(): FramePoint {
        return target
    }

    override fun complete(state: MapLocationState): Boolean =
    // needs another criteria like number killed is number seen or something
        // need to add another "is alive" criteria like some of the presence
        criteria.complete(state)

    // need to visit all the enemy locations until it is done
    override fun nextStep(state: MapLocationState): GamePad {
        criteria.update(state)

        if (enemies.isEmpty()) {
            enemies = state.frameState.enemies.toMutableList()
        }

        state.frameState.logEnemies()

        target = state.frameState.enemiesSorted.first().point

        // todo; debug
        return if (pressedACt > 0) {
            pressedACt--
            GamePad.A
        } else if (enemies.any { it.topCenter.distTo(state.frameState.link.topCenter) < 8 }) {
            // press A for a bit
            pressedACt = 3
            GamePad.A
        } else {
            routeTo.routeTo(state, enemies.map { it.topCenter })
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
    private var frames = 0
    private val gameAction = if (useB) GamePad.B else GamePad.A

    override fun nextStep(state: MapLocationState): GamePad {
        // just always do it
        val move = if (frames < 0) {
            GamePad.None
        } else {
            when {
                frames % 10 < freq -> gameAction
                else -> if (otherwiseRandom) GamePad.randomDirection(state.link) else GamePad.None
            }
        }
        frames++
        return move
    }

    override fun reset() {
        frames = 10
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
        val attackFrom = FramePoint(8.grid, 8.grid)
    }

    // more debugging
    private val positionShootActions = mutableListOf<Action>(
        InsideNavAbout(
            KillInCenterLocations.attackFrom,
            2,
            vertical = 2,
            negVertical = 2
        ),
        GoIn(2, GamePad.MoveUp),
        AlwaysAttack()
    )

    private val positionShoot = OrderedActionSequence(positionShootActions)

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
        get() = "KillInCenter"
}

class DeadForAWhile(private val limit: Int = 450, val reset: Boolean = false, val completeCriteria: (MapLocationState) -> Boolean) {
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
