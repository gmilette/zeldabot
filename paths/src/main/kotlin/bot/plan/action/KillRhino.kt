package bot.plan.action

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.grid
import util.d

// assume switched to arrow
class KillRhino(waitFrames: Int) : Action {
//    private val attract = ActionSequence(InsideNavAbout(KillHandsLevel7Data.attractFrom, 0, ignoreProjectiles = true), GoIn(5, GamePad.MoveLeft, reset = true), still)
    val attackBomb = AlwaysAttack(useB = true)
    val wait = GoIn(waitFrames, GamePad.None)
    val attackSword = AlwaysAttack()

    val eatingBombTile = setOf(
        // attrib 03, and 43
        0xFE,
        // attrib 43
        0xF0, 0xee,
        // attrib 43
        0xF2
    )

    val rhinoHeadLeftUp = 0xFA
    val rhinoHeadLeftDown = 0xF6
    val head = setOf(rhinoHead, rhinoHeadLeftUp, rhinoHeadLeftDown)

    private val one = -1
    // add the blow up directions too
    private val dirs = mapOf(
        0xFC to mapOf(one to Direction.Up),
        rhinoHeadLeftUp to mapOf(one to Direction.Up),
//        0x34 to Direction.Down,
        0xF4 to mapOf(one to Direction.Down),
        rhinoHeadLeftDown to mapOf(one to Direction.Down),
        0xE0 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
        0xE2 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
        0xDE to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
        0xDC to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    )

    private fun findDir(tile: Int, attrib: Int): Direction? {
        val tiles = dirs[tile]?: return null
        return tiles[one] ?: tiles[attrib]
    }

    // stopped moving
    val closeMouth = 0xEA
    val openMouth = 0xe2

    // general purpose algorithm always does optimal thing
    // position to be able to bomb into the target spot (adjacent squares)
    // change direction to face it
    // bomb
    // wait for eat, or count blinks
    // if count blinks, attack
    // if eat, fast time, bomb
    // if moved, RESET
    // wait for eat, or count blinks
    // if rhino moves RESET
    // if eat, wait, until death
    // otherwise attack with sword until dead

    // experiment
    // position
    // bomb
    // wait X
    // bomb or sword

    private fun MapLocationState.isEatingBomb() =
        frameState.enemies.firstOrNull { eatingBombTile.contains(it.tile) } != null

    private fun MapLocationState.rhinoDir(): Direction? =
        frameState.enemies.firstNotNullOfOrNull{ findDir(it.tile, it.attribute) }

    private val criteria = DeadForAWhile(limit = 200) {
        it.clearedWithMinIgnoreLoot(0)
    }

    override fun complete(state: MapLocationState): Boolean = criteria(state)

    override fun target(): FramePoint {
        return FramePoint()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        val isEatingBomb = state.isEatingBomb()
        val dir = state.rhinoDir()
        d { "Kill Rhino eating $isEatingBomb dir = $dir" }
        criteria.nextStep(state)

        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack

        return GamePad.None
    }

    override val name: String
        get() = "KillArrowSpider ${criteria.frameCount}"
}
class KillAll2(
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
    private val targetOnly: List<Int> = listOf()
) :
    Action {
    private val routeTo = RouteTo(RouteTo.Param(dodgeEnemies = true))
    private val criteria = KillAllCompleteCriteria()

    private var sameEnemyCount = 0

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
                // have to release for longer than 1
                d { "Press A last time" }
                return GamePad.None
            }

            pressACount > 4 -> {
                pressACount--
                d { "Press A" }
                return if (useBombs) GamePad.B else GamePad.A
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
            var aliveEnemies = state.frameState.enemiesClosestToLink()
            aliveEnemies = aliveEnemies.filter { state.currentMapCell.passable.get(it.point) }
            // need special handling, cant route into center
            if (targetOnly.isNotEmpty()) {
                d { " target only $targetOnly" }
                aliveEnemies = aliveEnemies.filter { targetOnly.contains(it.tile) }
            }
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
                            pressACount = 6
                            // for the rhino
                            if (waitAfterAttack) {
                                waitAfterPressing = 60
                            }
                            if (useBombs) GamePad.B else GamePad.A
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
                            d { "Plan: target changed was $previousTarget now ${target}"}
                            // can't tell if the target has changed
                            // handle replanning when just close, this might be fine
                            routeTo.routeTo(state, listOf(target), false)
                        }
                    }
                } ?: GamePad.None
            }
        }
    }
}