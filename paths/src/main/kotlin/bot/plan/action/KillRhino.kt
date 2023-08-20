package bot.plan.action

import bot.state.*
import bot.state.map.*
import util.LogFile
import util.d

data class RhinoStrategyParameters(
    val waitFrames: Int = 100,
    // how many grids ahead of the rhino to target
    val targetGridsAhead: Int = 2,
    // how many grids above or below to target
    val targetGridAboveBelow: Int = 0,
    // maybe a strategy to target any within the range specified by the above parameters
    val targetAllWithin: Boolean = false
) {
    fun getTargetGrid(from: FramePoint, direction: Direction?): FramePoint? {
        if (direction == null) return null
        // ahead
        val adjusted = direction.pointModifier(MapConstants.oneGrid * targetGridsAhead)
        val adjustAboveBelow = direction.opposite().pointModifier(MapConstants.oneGrid * targetGridAboveBelow)
//        return if (from.isInLevelMap) adjusted(from) else null
        return adjusted(from)
    }

    fun getOneInFront(from: FramePoint, direction: Direction?): FramePoint? {
        if (direction == null || direction.upOrLeft ) return null
        val adjusted = direction.pointModifier(MapConstants.oneGrid + 2)
        return adjusted(from)
    }

    fun getOneInFrontClose(from: FramePoint, direction: Direction?): FramePoint? {
        if (direction == null) return null
        val adjusted = if (direction == Direction.Down || direction == Direction.Right) {
            direction.pointModifier(MapConstants.oneGridPoint5)
        } else {
            direction.pointModifier(MapConstants.halfGrid)
        }
        return adjusted(from)
    }

}

fun FramePoint.attackPoints(): List<FramePoint> {
    return listOf(this.upTwoGrid, this.downTwoGrid, this.leftTwoGrid, this.rightTwoGrid).filter { it.isInLevelMap }
}

// assume switched to arrow
class KillRhino(private val params: RhinoStrategyParameters = RhinoStrategyParameters()) : Action {
    private val rhinoLog: LogFile = LogFile("Rhino")

    // 40 two bomb death
    private val ATTACK_DEATH_TIMING = 20
    private val TWO_BOMB_DEATH_TIMING = 30
    private val waitBetweenAttacks = ATTACK_DEATH_TIMING
    private val waitBetweenSwordAttacks = 9
    private var keepAttacking = 0

    private val navToTarget = NavToTarget(ATTACK_DEATH_TIMING, ::targetSelect, ::weaponSelect, ::inFront)

    private var prevKnownPoint: FramePoint? = null
    private var prevKnownPoints: List<FramePoint> = emptyList()
    private var prevKnownPointInFront: FramePoint? = null

    private val routeTo = RouteTo(RouteTo.Param(ignoreProjectiles = true, dodgeEnemies = true))

    private var waitCt = 0

    private var targets = listOf<FramePoint>()
    private var target: FramePoint = FramePoint()
    private var strategy: String = ""


    override fun targets(): List<FramePoint> = targets

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun complete(state: MapLocationState): Boolean = criteria(state)

    private val stateTracker = RhinoStateTracker()

    private val moveBuffer = MoveBuffer(6)

    private fun weaponSelect(state: MapLocationState): Boolean =
        bombHead(stateTracker.rhinoState)

    private fun inFront(state: MapLocationState): FramePoint? =
        state.rhino()?.let {
            val target = if (stateTracker.rhinoState is Stopped) {
                // it's too close for the two bomb timing
                // attack target
                d { " attack stopped rhino at ${it.point}"}
                null
            } else {
                params.getOneInFront(it.point, state.rhinoDir())
            }
            // not on the head, in front of it
//            val target =
            d { " rhino location ${it.point} $target dir: ${state.rhinoDir()}"}
            if (target != null) {
                prevKnownPointInFront = target
            }
            target
        } ?: prevKnownPointInFront

    private fun targetSelect(state: MapLocationState): FramePoint? =
        state.rhino()?.let {
            // if doing two bomb timing
            val target = if (stateTracker.rhinoState is Stopped) {
                // it's too close for the two bomb timing
                // attack target
                d { " attack stopped rhino at ${it.point}"}
                // just attack it
                it.point
//                params.getOneInFrontClose(it.point, state.rhinoDir())
            } else {
                d { " attack moving rhino"}
                params.getTargetGrid(it.point, state.rhinoDir())
            }
            d { " rhino location ${it.point} $target dir: ${state.rhinoDir()}"}
            if (target != null) {
                prevKnownPoint = target
            }
            target
        } ?: prevKnownPoint //known point can go null for a time

    val lastPoints: MoveBuffer = MoveBuffer(4)
    val prev: PrevBuffer<Boolean> = PrevBuffer(4)

    // if switched between two tiles (or maybe just not moving?)
    private fun isStationary(point: FramePoint?): Boolean {
        // assume not
        if (point == null) return false
        if (!lastPoints.isFull) return false
        lastPoints.add(point)
        return lastPoints.allSame()
    }

    private fun isBlinking(exists: Boolean): Boolean {
        // pattern is this
        // but the rhino normally goes in and out at chunks of time
        prev.add(exists)
        if (!prev.isFull) return false
        val numTrue = prev.buffer.count { it }
        return numTrue == 3
    }

    inner class RhinoStateTracker {
        var rhinoState: RhinoState = ZeroState()

        fun update(eating: Boolean, moved: Boolean) {
            rhinoState = rhinoState.transition(eating, moved)
        }
    }

    private fun updateState(state: MapLocationState) {
        val isEatingBomb = state.isEatingBomb() // reliable
        val dir = state.rhinoDir()
        val where = state.rhino()

        // reliable for movement
        // look at 3 because it stays at same point for more than 1
        where?.let {
            moveBuffer.add(it.point)
        }
        val moved = !moveBuffer.isFull || !moveBuffer.allSame()

        stateTracker.update(isEatingBomb, moved)
        d { "Kill Rhino state ${stateTracker.rhinoState.name} moved $moved eating $isEatingBomb dir = $dir where = ${where?.point ?: ""}"}
//        d { " move buffer $moveBuffer"}
//        rhinoLog.write(
//            where?.tile?.toString(16) ?: "noti",
//            if (isEatingBomb) "eat" else "noe",
//            dir?.toString() ?: "nodir",
//            where?.point?.oneStr ?: "non_pts",
//            if (moved) "moved" else "nomove",
//            if (isStationary(where?.point)) "stationary" else "notstation",
//            if (isBlinking(where != null)) "blink" else "nobli",
//            stateTracker.rhinoState.javaClass.simpleName
//        )
    }

    private var rDir: Direction? = null

    override fun nextStep(state: MapLocationState): GamePad {
        val previousTarget = target
        rDir = state.rhinoDir()

        updateState(state)
        criteria.nextStep(state)
        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack
        d { "ACTION" }

        val possibleRhino = state.rhino()
        d { " rhino at $possibleRhino"}

        // pick a spot near the rhino
        val dodgeTargets = dodgePoints(state)
        target = dodgeTargets.firstOrNull() ?: FramePoint(8.grid, 8.grid)

        val useB = weaponSelect(state)
        var attackRhino = false
        targets = when {
            possibleRhino == null -> {
                d { " targets -> dodge (no rhino)"}
                strategy = "dodgeN"
                dodgeTargets
            }
            !useB && (stateTracker.rhinoState is Stopped || stateTracker.rhinoState is Eating) -> {
                d { " targets -> corners of rhino ${possibleRhino.point}"}
                attackRhino = true
                strategy = "attack"
                target = possibleRhino.point
                possibleRhino.point.cornersInLevel()
            }
            false && useB -> {
                val targ = params.getTargetGrid(possibleRhino.point, state.rhinoDir())
                target = targ ?: FramePoint(8.grid, 4.grid)

                val targCorners = targ?.cornersInLevel()

                if (targCorners.isNullOrEmpty()) {
                    d { "targets -> bomb position none, dodge targets dir=${state.rhinoDir()}"}
                    strategy = "dodgeT"
                    dodgeTargets
                } else {
                    d { "targets -> bomb position $target rh ${possibleRhino.point}"}
                    strategy = "target"
                    targCorners.forEach {
                        d { "$it dist to rhino ${it.distTo(possibleRhino.point)} in ${possibleRhino.point.isInGrid(it)} "}
                    }
                    targCorners.filter { it.distTo(possibleRhino.point) >= 32}
                }
            }
            else -> {
                d { "targets -> else dodge"}
                strategy = "dodgeD"
                dodgeTargets
            }
        }
        d { "targets -> $targets" }

        val forceNew = previousTarget != target && previousTarget.distTo(target) > 3
        if (forceNew) {
            d { "forcenew, was $previousTarget no $target"}
        }
        val validTarget = possibleRhino?.let {
            target.distTo(it.point) <= MapConstants.twoGrid
        } ?: false
        val should = AttackActionDecider.shouldAttack(state.frameState.link.dir, state.link, listOf(target))
        val attack = validTarget && should

        d { " useB $useB attack $validTarget should $should state ${stateTracker.rhinoState}" }

        // dodge or still
        waitCt--
        keepAttacking--
        val action = when {
            waitCt > 0 && keepAttacking <= 0 -> {
                if (attack && stateTracker.rhinoState is Stopped || stateTracker.rhinoState is Eating) {
                    // if could attack just wait, but we could be in danger
                    d { " **do nothing wait to attack again" }
                    GamePad.None
                } else {
                    d { " **route to targets while waiting for attack" }
                    routeTo.routeTo(state, targets,
                        RouteTo.RouteParam(forceNew = forceNew, useB = useB)
                    )
                }
            }
            attack -> {
                d { " **do attack"}
                if (useB) {
                    if (keepAttacking <= 0) keepAttacking = 3
                    waitCt = waitBetweenAttacks
                    GamePad.B
                } else {
                    if (keepAttacking <= 0) keepAttacking = 3
                    waitCt = waitBetweenSwordAttacks
                    GamePad.A
                }
            }
            // can't attack yet, use normal targets
            else -> {
                // is the target the rhino? or near rhino
                val attackTarget = if (attackRhino) {
                    possibleRhino?.point
                } else null

                d { "**attack target " }
                // should keep attacking I think why doesn't it?
                if (stateTracker.rhinoState is Eating) {
                    // hack
                    if (keepAttacking <= 0) keepAttacking = 3
                    waitCt = waitBetweenAttacks
                    GamePad.B
                } else {
                    routeTo.routeTo(
                        state, targets,
                        RouteTo.RouteParam(forceNew = forceNew, useB = useB, attackTarget = attackTarget)
                    )
                }
            }
        }
        return action
    }

    private fun dodgePoints(state: MapLocationState): List<FramePoint> {
        var avoidTargets = listOf<FramePoint>()
        val reference = FramePoint(8.grid, 5.grid)
        val rhinoAt = state.rhino()?.point ?: reference
        // try to route near current target or near rhino
        val targ = params.getTargetGrid(rhinoAt, state.rhinoDir()) ?: reference


        var possibleUnfiltered = listOf(
            targ.downTwoGrid,
            targ.upTwoGrid,
            targ.leftTwoGrid,
            targ.rightTwoGrid,
        )
        var possible = possibleUnfiltered.filter { it.isInLevelMap }.filter { it != rhinoAt }
        d { "possible now $possibleUnfiltered after filter $possible"}
        if (targ.isInLevelMap && targ != rhinoAt && targ.distTo(rhinoAt) > 4) {
            d { "its just target" }
            possible = listOf(targ)
        }
        if (possible.isEmpty()) {
            d { " route to reference location "}
            possible = listOf(
                reference,
                reference.downOneGrid,
                reference.upOneGrid,
                reference.leftOneGrid,
                reference.rightOneGrid).filter { it.isInLevelMap }
        }
        if (possible.isNotEmpty()) {
            d { " sort ${possible.size}"}
            // maybe closest to link too?
//            avoidTargets = listOf(possible.maxBy { (it.distTo(rhinoAt) - state.link.distTo(it)).also {dist ->
//                d { " dist to $it is $dist "}
//            } })
            avoidTargets = listOf(possible.maxBy { (it.distTo(rhinoAt)).also {dist ->
                d { " dist to $it is $dist "}
            } })
        }

        d { "rhino $rhinoAt target grid $targ targs ${possible} avoid ${avoidTargets}"}

        return avoidTargets
    }

    override val name: String
        get() = "KillRhino ${stateTracker.rhinoState.name} ${criteria.frameCount} ${rDir} w ${waitCt} s $strategy" // ${actions.stepName}
}

class NavToTarget(
    val waitBetweenAttacks: Int = 60,
    val targetSelector: (MapLocationState) -> FramePoint?,
    val weaponSelectorUseB: (MapLocationState) -> Boolean,
    val oneInFront: (MapLocationState) -> FramePoint?) : Action {
    private val routeTo = RouteTo(RouteTo.Param(ignoreProjectiles = true, dodgeEnemies = true))

    private var waitCt = 0

    private var targets = listOf<FramePoint>()
    private var target = FramePoint()

    override fun targets(): List<FramePoint> = targets

    override fun complete(state: MapLocationState): Boolean = false

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun nextStep(state: MapLocationState): GamePad {
        val previousTarget = target

        target = targetSelector(state) ?: return GamePad.None

        val isRhino = target == state.rhino()?.point
        if (isRhino) {
            d { " targeting -> Rhino $target rhino ${state.rhino()?.point}"}
        } else {
            d { " targeting -> Not Rhino $target rhino ${state.rhino()?.point}"}
        }
//        targets = target.about()
        val waitingAfterAttack = waitCt > 0
        // if it is waiting, then move back to the target
        // if enemy is stopped I have to attack it!
        if (waitingAfterAttack) {
            d { " wait after wait count: $waitCt"}
            targets = adjustTarget(state, target)
//            // walk away from the target
//            val rhinoAt = state.rhino()?.point ?: FramePoint(8.grid, 8.grid)
//            val possible = listOf(
//                target.downTwoGrid,
//                target.upTwoGrid,
//                target.leftTwoGrid,
//                target.rightTwoGrid,
//                rhinoAt.downTwoGrid,
//                rhinoAt.upTwoGrid,
//                rhinoAt.leftTwoGrid,
//                rhinoAt.rightTwoGrid
//            ).filter { it.isInLevelMap }
//
//            if (possible.isEmpty()) {
//                targets = listOf()
//            } else {
//                targets = if (isRhino) {
//                    d { " avoid rhino"}
//                    listOf(possible.maxBy { it.distTo(target) })
//                } else {
//                    // keep same target but avoid the rhino?
//                    d { " not targetting rhino keep same target"}
//                    // but if that is out of level, dodge
//                    listOf(target)
//                }
//                d { " move away from target to $target \n possible $possible selected target $targets"}
//            }
        } else {
            // depends on what kind of target this is
            // always attack the corners
            d { " target corners"}
            targets = target.cornersInLevel()
            val reference = FramePoint(8.grid, 8.grid)
            val rhinoAt = state.rhino()?.point ?: FramePoint(8.grid, 8.grid)
            var possible = listOf(
                target.downTwoGrid,
                target.upTwoGrid,
                target.leftTwoGrid,
                target.rightTwoGrid,
                rhinoAt.downTwoGrid,
                rhinoAt.upTwoGrid,
                rhinoAt.leftTwoGrid,
                rhinoAt.rightTwoGrid,
                ).filter { it.isInLevelMap }
            if (possible.isEmpty()) {
                d { " route to reference location "}
                possible = listOf(
                    reference.downTwoGrid,
                    reference.upTwoGrid,
                    reference.leftTwoGrid,
                    reference.rightTwoGrid).filter { it.isInLevelMap }
            }
            if (targets.isEmpty() && possible.isNotEmpty()) {
                targets = listOf(possible.maxBy { it.distTo(target).also {dist ->
                    d { " dist to $it is $dist "}
                } })
                d { " no way to get to target location go to $targets possible $possible"}
            }
        }
        val useB = weaponSelectorUseB(state)

        val forceNew = previousTarget != target

        // if waiting, then this should avoid instead of keep moving towards target
        // do my own attack check here
        val attack = AttackActionDecider.shouldAttack(state.frameState.link.dir, state.link, targets)

        d { " move to spot attack: $attack $targets with ${useB.weapon} force ${if (forceNew) "new" else ""}" }
        // if the target is off map, don't route just do nothing

        return if (target.isInLevelMap) {
            if (attack && waitCt <= 0) {
                waitCt = waitBetweenAttacks
                GamePad.None
                if (weaponSelectorUseB(state)) {
                    GamePad.B
                } else {
                    GamePad.A
                }
            } else {
                waitCt--
                // add unmovable spot in front of rhino
//                val avoid = oneInFront(state)
//                d {"avoid: ${avoid ?: "none"}"}
//                val avoidList = if (avoid == null) emptyList() else listOf(avoid)
                val avoidList = emptyList<FramePoint>()

                var attackTarget: FramePoint? = null
                // is the target the rhino? or near rhino
                if (isRhino) {
                    attackTarget = target
                }

                // avoid all enemies
                // why force new, only if the target is different
                routeTo.routeTo(state, targets,
                    RouteTo.RouteParam(forceNew = forceNew, enemyAvoid = avoidList, useB = useB, attackTarget = attackTarget)
                ).also {
                    d { " Move to $it" }
                }
            }
        } else {
            waitCt--
            // should route somewhere, maybe expand points
            // why force new, only if the target is different
//            routeTo.routeTo(state, target.corners(),
//                RouteTo.RouteParam(forceNew = forceNew, enemyAvoid = emptyList())
//            ).also {
//                d { " Move to $it" }
//            }
            val avoidList = emptyList<FramePoint>()

            var attackTarget: FramePoint? = null
            // is the target the rhino? or near rhino
            if (isRhino) {
                attackTarget = target
            }

            // why force new, only if the target is different
            routeTo.routeTo(state, targets,
                RouteTo.RouteParam(forceNew = forceNew, enemyAvoid = avoidList, useB = useB, attackTarget = attackTarget)
            ).also {
                d { " Move to $it" }
            }
        }
//        return routeTo.routeTo(state, targets, forceNew)
    }

    private fun adjustTarget(state: MapLocationState, currentTarget: FramePoint): List<FramePoint> {
        var avoidTargets = listOf(currentTarget)
        val reference = FramePoint(8.grid, 8.grid)
        val rhinoAt = state.rhino()?.point ?: FramePoint(8.grid, 8.grid)
        // try to route near current target or near rhino
        var possible = listOf(
            currentTarget.downTwoGrid,
            currentTarget.upTwoGrid,
            currentTarget.leftTwoGrid,
            currentTarget.rightTwoGrid,
            rhinoAt.downTwoGrid,
            rhinoAt.upTwoGrid,
            rhinoAt.leftTwoGrid,
            rhinoAt.rightTwoGrid,
        ).filter { it.isInLevelMap }
        if (possible.isEmpty()) {
            d { " route to reference location "}
            possible = listOf(
                reference.downTwoGrid,
                reference.upTwoGrid,
                reference.leftTwoGrid,
                reference.rightTwoGrid).filter { it.isInLevelMap }
        }
        if (possible.isNotEmpty()) {
            avoidTargets = listOf(possible.maxBy { it.distTo(target).also {dist ->
                d { " dist to $it is $dist "}
            } })
            d { " no way to get to target location go to $targets possible $possible"}
        }

        return avoidTargets
    }

    override fun target() = target

    override val name: String
        get() = "Nav to Target ${target.oneStr}"
}


// rhino states



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

// link action
//Zero/One: BombTarget, Bomb wait Bomb
// then OneState or Stopped
//Eating: ()
////TwoState: Sword
//Stopped, TwoState: HeadTarget -> Sword
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
// rhino logging
interface RhinoState {
    fun transition(eating: Boolean, moving: Boolean): RhinoState = this

    val name: String
        get() = this.javaClass.simpleName
}

class Default: RhinoState {
    // new transition state that is for when the rhino is gone
    // you can go from one state to death
    override fun transition(eating: Boolean, moving: Boolean): RhinoState = this
}

class ZeroState : RhinoState {
    override fun transition(eating: Boolean, moving: Boolean): RhinoState {
        return when {
            eating -> Eating()
            !moving -> Stopped(this)
            else -> this
        }
    }
}

class Eating(private val then: RhinoState = OneState()) : RhinoState {
    override fun transition(eating: Boolean, moving: Boolean): RhinoState {
        return when {
            !eating -> OneState()
            moving -> OneState()
            else -> this
        }
    }

    override val name: String
        get() = "${this.javaClass.simpleName} then ${then.name}"
}


// attack with sword
class Stopped(private val was: RhinoState): RhinoState {
    override fun transition(eating: Boolean, moving: Boolean): RhinoState {
        return when {
            eating -> was.transition(eating = true, moving = moving) //??
            moving -> was
            else -> this
        }
    }

    override val name: String
        get() = "${this.javaClass.simpleName} was ${was.name}"
}

class OneState : RhinoState {
    override fun transition(eating: Boolean, moving: Boolean): RhinoState {
        return when {
            eating -> TwoState()
            !moving -> Stopped(this)
            else -> this
        }
    }
//
//    // dead!
//    override fun eat() = TwoState()
//
//    // rarer, escaped the first bombing, and link has well-placed bomb
//    override fun stopped(): RhinoState = Stopped(this)
}

// wait for rhino to die, sword to help it along, don't sword if eating
class TwoState : RhinoState {
    override fun transition(eating: Boolean, moving: Boolean): RhinoState {
        return when {
            eating -> TwoState()
            // shouldn't move in two state
            moving -> OneState()
            else -> this
        }
    }
}

// add min wait time between bomb1 and bomb2
// Zero: Bomb, (bomb)
// Eating: Bomb (after wait), but do not bomb more than twice
// One: Bomb (Bomb)
// Two: Sword (Head)
// Stopped: Sword (Head)

fun bombHead(state: RhinoState) =
    weaponFor(state) == GamePad.B

fun weaponFor(state: RhinoState): GamePad =
    when (state) {
        is Eating,
        is OneState,
        is ZeroState -> GamePad.B
        is TwoState -> GamePad.None //if (isEating) GamePad.None else GamePad.A
        is Stopped -> GamePad.A
        else -> GamePad.None
    }


private fun MapLocationState.isEatingBomb() =
    frameState.enemies.firstOrNull { eatingBombTile.contains(it.tile) } != null

private fun MapLocationState.rhinoDir(): Direction? =
    frameState.enemies.firstNotNullOfOrNull{ findDir(it.y, it.tile, it.attribute) }

private fun MapLocationState.rhino(): Agent? =
    // pick the left most head
    frameState.enemies.filter { it.y != 187 && head.contains(it.tile) }.minByOrNull { it.x }

private val criteria = DeadForAWhile(limit = 100, reset = true) {
    it.clearedWithMinIgnoreLoot(0).also {result ->
        if (result) {
            d { " DONE!!! num alive: ${it.frameState.enemies.filter { it.state == EnemyState.Alive }.size} "}
            it.frameState.logAliveEnemies()
        }
    }
}

private val eatingHead = 0xF2
private val eatingHeadDown = 0xF8
private val eatingUp = 0xFE

private val eatingBombTile = setOf(
    eatingHeadDown, // attrib 43 // going down
    // attrib 03, and 43
    eatingUp,
    // attrib 43
    0xF0, 0xee,
    // attrib 43
    eatingHead // yes
)

private val rhinoHeadLeftUp = 0xFA // foot up
private val rhinoHeadLeftUp2 = 0xFC // foot down
private val rhinoHeadLeftDown = 0xF6 // foot up
// not that
private val rhinoHeadLeftDown2 = 0xF4 // foot down
//val rhinoHead = (0xE2).toInt() // mouth open
//val rhinoHead2 = (0xE0).toInt()
// rhinoHeadMouthClosed

// these are the mouth parts
// rhinoHeadMouthOpen // 0xE2
// rhinoHeadMouthClosed = 0xEA

// either mouth is fine
// for up down, pick the most left
private val head = setOf(
    rhinoHeadHeadWithMouthOpen, rhinoHeadHeadWithMouthClosed,
    rhinoHeadLeftUp, rhinoHeadLeftUp2,
    rhinoHeadLeftDown, rhinoHeadLeftDown2,
    eatingHead,
    eatingHeadDown,
    eatingUp)
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
    //rhinoHeadHeadWithMouthClosed with attrib 03 is right
    0xE0 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    0xE2 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    // necessary?
    // no, just use one
    0xEA to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    0xDE to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    0xDC to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
)

private fun findDir(y: Int, tile: Int, attrib: Int): Direction? {
    if (y == 187) return null
    val tiles = dirs[tile]?: return null
    return tiles[one] ?: tiles[attrib]
}
