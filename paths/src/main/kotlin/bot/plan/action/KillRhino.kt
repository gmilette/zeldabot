package bot.plan.action

import bot.state.*
import bot.state.map.*
import util.LogFile
import util.d

data class RhinoStrategyParameters(
    val waitFrames: Int = 100,
    // how many grids ahead of the rhino to target
    val targetGridsAhead: Int = 0,
    // how many grids above or below to target
    val targetGridAboveBelow: Int = 0,
    // maybe a strategy to target any within the range specified by the above parameters
    val targetAllWithin: Boolean = false
) {
    fun getTargetGrid(from: FramePoint, direction: Direction?): FramePoint? {
        if (direction == null) return null
        val adjusted = direction.pointModifier(MapConstants.oneGrid * targetGridsAhead)
        val adjustAboveBelow = direction.opposite().pointModifier(MapConstants.oneGrid * targetGridAboveBelow)
        return if (from.isInLevelMap) adjusted(from) else null
    }
}

class RhinoStateTracker() {

}

// rhino logging

// assume switched to arrow
class KillRhino(private val params: RhinoStrategyParameters = RhinoStrategyParameters()) : Action {
    private val rhinoLog: LogFile = LogFile("Rhino")

    private val navToTarget = NavToTarget { state ->
        state.rhino()?.let {
            d { " rhino location ${it.point}"}
            it.point
//            params.getTargetGrid(it.point, state.rhinoDir())
        } ?: prevKnownPoint //known point can go null for a time
    }

    private var prevKnownPoint: FramePoint? = null

    private val attackBomb = AttackOnce(useB = true)
    private val wait = GoIn(params.waitFrames, GamePad.None)
    private val attackBomb2 = AttackOnce(useB = true)
    private val attackSword = AttackOnce()

    // if moving
    //  NavToTarget
    // else
    //  Bomb, wait, Bomb, sword


    // if not in range, (nav)
    // if hits = 0, bomb
    // if detect eat, (hits for bomb 0)++ // only add once
    // if hits = 1 + near + #frames < X, wait
    // if done waiting + still in range, bomb
    // if bomb == 2, sword until dead


    private val actions =
        ActionSequence(
            navToTarget,
            attackBomb,
            wait,
            attackBomb2,
            attackSword
        )

    private val eatingBombTile = setOf(
        // attrib 03, and 43
        0xFE,
        // attrib 43
        0xF0, 0xee,
        // attrib 43
        0xF2
    )

    private val rhinoHeadLeftUp = 0xFA
    private val rhinoHeadLeftUp2 = 0xFC
    private val rhinoHeadLeftDown = 0xF6
    // not that
    private val rhinoHeadLeftDown2 = 0xF4
    private val head = setOf(rhinoHead, rhinoHeadLeftUp, rhinoHeadLeftDown)
    // dont use, just use dir
//    private val head = setOf(rhinoHead, rhinoHead2, rhinoHeadLeftUp, rhinoHeadLeftUp2, rhinoHeadLeftDown, rhinoHeadLeftDown2)

    private val one = -1
    // add the blow up directions too
    private val dirs = mapOf(
        rhinoHeadLeftUp2 to mapOf(one to Direction.Up),
        rhinoHeadLeftUp to mapOf(one to Direction.Up),
//        0x34 to Direction.Down,
        rhinoHeadLeftDown2 to mapOf(one to Direction.Down),
        rhinoHeadLeftDown to mapOf(one to Direction.Down),
        0xE0 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
        0xE2 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
        // necessary?
        // no, just use one
        0xDE to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
        0xDC to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    )

    private fun findDir(y: Int, tile: Int, attrib: Int): Direction? {
        if (y == 187) return null
        val tiles = dirs[tile]?: return null
        return tiles[one] ?: tiles[attrib]
    }

    val lastPoints: MoveBuffer = MoveBuffer(4)
    val prev: PrevBuffer<Boolean> = PrevBuffer(4)

    // if switched between two tiles (or maybe just not moving?)
    fun isStationary(point: FramePoint?): Boolean {
        // assume not
        if (point == null) return false
        if (!lastPoints.isFull) return false
        lastPoints.add(point)
        return lastPoints.allSame()
        // if there is no rhino assume moving
        // if last 4 movements are all the same
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
//        226,false,Right,158_128,nomove
        // Large block of null because the head isn't found?

        // vs
        // regular movement
//        226,false,Right,165_128,moved
//        226,false,Right,165_128,nomove
//        226,false,Right,166_128,moved
//        226,false,Right,166_128,nomove
//        226,false,Right,167_128,moved
//        226,false,Right,167_128,nomove
//        226,false,Right,168_128,moved
//        226,false,Right,168_128,nomove
    }

    fun isBlinking(exists: Boolean): Boolean {
        // pattern is this
        // but the rhino normally goes in and out at chunks of time
        prev.add(exists)
        if (!prev.isFull) return false
        val numTrue = prev.buffer.count { it }
        return numTrue == 3

//        226,false,Right
//        226,false,Right
//        226,false,Right
//        null,false,Right
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

    // Sword only if rhino is not moving


    // experiment
    // position
    // bomb
    // wait X
    // bomb or sword

    private fun MapLocationState.isEatingBomb() =
        frameState.enemies.firstOrNull { eatingBombTile.contains(it.tile) } != null

    private fun MapLocationState.rhinoDir(): Direction? =
        frameState.enemies.firstNotNullOfOrNull{ findDir(it.y, it.tile, it.attribute) }

//    private fun MapLocationState.rhino(): Agent? =
//        frameState.enemies.firstOrNull { head.contains(it.tile) }

    private fun MapLocationState.rhino(): Agent? =
        frameState.enemies.firstOrNull { head.contains(it.tile) && it.y != 187 }

    // maybe more than 200?
    private val criteria = DeadForAWhile(limit = 200) {
        it.clearedWithMinIgnoreLoot(0)
    }

    override fun complete(state: MapLocationState): Boolean = criteria(state)

    override fun target(): FramePoint {
        return actions.target()
    }

    private var prevPoint: FramePoint? = null

    override fun nextStep(state: MapLocationState): GamePad {
        val isEatingBomb = state.isEatingBomb() // reliable
        val dir = state.rhinoDir()
        val where = state.rhino()

        // reliable for movement
        val moved = where != null && prevPoint != null && prevPoint != where.point
        d { "Kill Rhino eating $isEatingBomb dir = $dir where = ${where?.point ?: ""}"}
        rhinoLog.write(
            where?.tile?.toString(16) ?: "noti",
            if (isEatingBomb) "eat" else "noe",
            dir?.toString() ?: "nodir",
            where?.point?.oneStr ?: "non_pts",
            if (moved) "moved" else "nomove",
            if (isStationary(where?.point)) "stationary" else "notstation",
            if (isBlinking(where != null)) "blink" else "nobli"
        )
        criteria.nextStep(state)
        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack
        d { "ACTION" }
        val action = actions.nextStep(state)
        if (where != null) {
            prevKnownPoint = where.point
        }
        prevPoint = where?.point
        return action
//        return GamePad.None
    }

    override val name: String
        get() = "KillRhino ${criteria.frameCount} ${actions.stepName}"
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