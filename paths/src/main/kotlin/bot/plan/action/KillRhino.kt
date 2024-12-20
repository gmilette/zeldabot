package bot.plan.action

import bot.state.*
import bot.state.map.*
import bot.state.oam.*
import util.Geom
import util.LogFile
import util.d

// that way if rhine becomes undead link will attack again
//val KillRhinoThenLeft = DecisionAction(KillRhino(), MoveTo())

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
//        val grids = if (direction.upOrLeft) MapConstants.oneGrid else MapConstants.twoGrid
        //val grids = MapConstants.oneGrid // to trigger two bomb combo
//        val grids = 12 // good for two bomb
        val grids = 20 //  MapConstants.oneGridPoint5 // got for the one bomb drop
        val adjusted = direction.pointModifier(grids * targetGridsAhead)
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

// Kill until the big heart appears of heart container goes up (if link happens to be on top and it never appears)
val killRhinoCollectSeeHeart: Action
    get() = CompleteIfSeeTileOrHeartChange(wrapped = KillRhino())

// assume switched to arrow
class KillRhino(private val params: RhinoStrategyParameters = RhinoStrategyParameters()) : Action {
    private val rhinoLog: LogFile = LogFile("Rhino")

    // 40 two bomb death
    private val ATTACK_DEATH_TIMING = 20
    private val TWO_BOMB_DEATH_TIMING = 30
    private val waitBetweenAttacks = 10 //  ATTACK_DEATH_TIMING
    private val waitBetweenSwordAttacks = 9
    private var keepAttacking = 0

    private var prevKnownPoint: FramePoint? = null
    private var prevKnownPoints: List<FramePoint> = emptyList()
    private var prevKnownPointInFront: FramePoint? = null

    private val routeTo = RouteTo(params = RouteTo.Param(whatToAvoid = RouteTo.WhatToAvoid.JustEnemies))

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

    private fun shouldAttack(): Boolean =
        weaponFor(stateTracker.rhinoState) != GamePad.None

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

    private var ct = 0

    private fun updateState(state: MapLocationState) {
        val isEatingBomb = state.isEatingBomb() // reliable
        val dir = state.rhinoDir()
        val where = state.rhino()

        // reliable for movement
        // look at 3 because it stays at same point for more than 1
        where?.let {
            moveBuffer.add(it.point)
        }
        ct++
        val moved = ct < 50 || !moveBuffer.isFull || !moveBuffer.allSame()

        stateTracker.update(isEatingBomb, moved)
        d { "Kill Rhino state ${stateTracker.rhinoState.name} moved $moved eating $isEatingBomb dir = $dir where = ${where?.point ?: ""}"}
//        d { " move buffer $moveBuffer"}
        rhinoLog.write(
            where?.tile?.toString(16) ?: "noti",
            if (isEatingBomb) "eat" else "noe",
            dir?.toString() ?: "nodir",
            where?.point?.oneStr ?: "non_pts",
            if (moved) "moved" else "nomove",
            if (isStationary(where?.point)) "stationary" else "notstation",
            if (isBlinking(where != null)) "blink" else "nobli",
            stateTracker.rhinoState.javaClass.simpleName
        )
    }

    private var rDir: Direction? = null
    private var linkDir: Direction? = null
    override fun nextStep(state: MapLocationState): GamePad {
        val previousTarget = target
        rDir = state.rhinoDir()

        updateState(state)
        criteria.nextStep(state)
        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack
        d { "ACTION" }

        val possibleRhino = state.rhino()

        // pick a spot near the rhino
        val dodgeTargets = dodgePoints(state)
        target = dodgeTargets.firstOrNull() ?: FramePoint(8.grid, 8.grid)

        d { " Rhino is  dir = $rDir at ${possibleRhino?.point}" }

        var targ: FramePoint? = null
        val useB = weaponSelect(state)
        val shouldAttack = shouldAttack()
        var attackRhinoWithSword = false
        val isStoppedOrEating by lazy {
            (stateTracker.rhinoState is Stopped || stateTracker.rhinoState is Eating)
        }
        val isDangerous = stateTracker.rhinoState is OneState || stateTracker.rhinoState is ZeroState

        // if not moving or eating no need to dodge
//        val inFrontOfRhino = emptyList<FramePoint>()
        val inFrontOfRhino = if (isDangerous && rDir != null) {
            possibleRhino?.let {
//                val attackpt: FramePoint = params.getTargetGrid(it.point, state.rhinoDir()) ?: FramePoint()
                rDir?.let { ir ->
                    val mo2 = ir.pointModifier(MapConstants.twoGrid)
                    val mo15 = ir.pointModifier(MapConstants.oneGridPoint5)
                    val mo = ir.pointModifier(MapConstants.oneGrid)
                    // added this as a hack fix
                    val moHalf = ir.pointModifier(MapConstants.halfGrid)
                    listOf(mo(it.point), moHalf(it.point), mo2(it.point), mo15(it.point))
                } ?: emptyList()
            } ?: emptyList()
        } else {
            emptyList()
        }
//        val inFrontRect = inFrontOfRhino.map { it.toRect() }
        val inFrontRect = emptyList<Geom.Rectangle>()

        d { " rhino inf=${inFrontOfRhino.size} state=${stateTracker.rhinoState.name} at ${possibleRhino?.point} useB=$useB stoppedOrEat=$isStoppedOrEating"}
        targets = when {
            possibleRhino == null -> {
                d { " targets -> dodge (no rhino)"}
                strategy = "dodgeN"
                dodgeTargets
            }
            shouldAttack && !useB && isStoppedOrEating -> {
                d { " targets -> attack head ${possibleRhino.point}"}
                attackRhinoWithSword = true
                strategy = "attack"
                target = possibleRhino.point
                AttackActionDecider.attackPointsNoCorner(target).filter { it.isInLevelMap }.ifEmpty { dodgeTargets }
//                possibleRhino.point.cornersInLevel()
            }
            shouldAttack && useB -> {
                targ = if (isStoppedOrEating) {
                    possibleRhino.point
                } else {
                  params.getTargetGrid(possibleRhino.point, state.rhinoDir())
//                    possibleRhino.point // just always attack rhino
                }
                val rh = possibleRhino.point.toRect()

                val targCornersBomb = targ?.let { t ->
                    AttackActionDecider.attackPoints(t, not = rDir ?: Direction.None).filter { it.isInLevelMap }.filter { !rh.pointInside(it) }.filter { target ->
                        inFrontRect.any { !it.pointInside(target) }
                    }
                }
                val targCornersRhino = AttackActionDecider.attackPoints(possibleRhino.point)
                val targCorners = if (isStoppedOrEating) {
                    targCornersRhino
                } else {
                    targCornersBomb
                }
                d { "*ATTACK rhino ${possibleRhino.point} attack targ $targ"}
                if (targCorners != null) {
                    for (p in targCorners) {
                        d { "*ATTACK $p inside=${inFrontRect.any { !it.pointInside(target) }}"}
                    }
                }
                d { " front "}
                for (framePoint in inFrontOfRhino) {
                    d { "*front $framePoint"}
                }

                if (targCorners.isNullOrEmpty()) {
                    d { "targets -> bomb position none, dodge targets dir=${state.rhinoDir()}"}
                    strategy = "dodgeT"
                    dodgeTargets
                } else {
                    d { "targets -> bomb position $target rh ${possibleRhino.point}"}
                    strategy = "target"
                    targCorners
                }
            }
            else -> {
                d { "targets -> else dodge"}
                strategy = "dodgeD"
                dodgeTargets
            }
        }
        d { "targets -> ($strategy) $targets" }

        val allowAttack = shouldAttack && (waitCt <= 0)
        val action = routeTo.routeTo(
            state, targets,
            RouteTo.RouteParam(forceNew = true,
                useB = useB,
//                allowAttack = attackRhinoWithSword,
                allowAttack = shouldAttack && (waitCt <= 0),
                // no need to shoot swords or whatever at it
                allowRangedAttack = false,
                rParam = RouteTo.RoutingParamCommon(
                    forceHighCost = inFrontOfRhino,
                    // can get stuck
//                    findNearestSafeIfCurrentlyNotSafe = false
                ),
                ),
            targ?.let { listOf(targ) }?.map { Agent(point = it,tile = rhinoHead2) } ?: emptyList()
         ).also {
            d {" Rhino action --> $it $waitCt $useB allow=$allowAttack"}
        }

        if (action == GamePad.B && waitCt <= 0) {
            waitCt = waitBetweenAttacks
        }
        waitCt--

        return action
    }

    private fun dodgePoints(state: MapLocationState): List<FramePoint> {
        var avoidTargets = listOf<FramePoint>()
        val reference = FramePoint(8.grid, 5.grid)
        val rhinoAt = state.rhino()?.point ?: reference
        // try to route near current target or near rhino
        val targ = params.getTargetGrid(rhinoAt, state.rhinoDir()) ?: reference

        val possibleUnfiltered = listOf(
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
        get() = "KillRhino ${stateTracker.rhinoState.name} ${criteria.frameCount} O:${linkDir?.opposite() == rDir} w ${waitCt} s $strategy" // ${actions.stepName}
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

fun MapLocationState.rhinoDir(): Direction? =
    frameState.enemies.firstNotNullOfOrNull{ findDir(it.y, it.tile, it.attribute) }

fun MapLocationState.rhino(): Agent? =
    // pick the left most head
    frameState.enemies.filter { it.y != 187 && head.contains(it.tile) }.minByOrNull { it.x }

private val criteria = DeadForAWhile(limit = 100, reset = true) {
    it.clearedWithMinIgnoreLoot(0).also {result ->
        if (result) {
            d { " $result DONE!!! num alive: ${it.frameState.enemies.filter { it.state == EnemyState.Alive }.size} "}
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
// need these
val rhinoHead = (0xE2).toInt() // mouth open
val rhinoHead2 = (0xE0).toInt()
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
    rhinoHeadMouthClosed.first, // added this so that targetting is correct
    eatingHead,
    eatingHeadDown,
    eatingUp,
    rhinoHead,
    rhinoHead2
    )

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
    // eatingHeadDown
    0xf8 to mapOf(one to Direction.Down),
    //rhinoHeadHeadWithMouthClosed with attrib 03 is right
    0xE0 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    0xE2 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    // necessary?
    // no, just use one
    // 0xEA is reused
    0xEA to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    0xE8 to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // HEAD
    // these are the tail
    0xDE to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
    0xDC to mapOf (0x03 to Direction.Right, 0x43 to Direction.Left), // 03
)

private fun findDir(y: Int, tile: Int, attrib: Int): Direction? {
    if (y == 187) return null
    d { " findDir $y ${tile.toString(16)} ${attrib.toString(16)} ${dirs[tile]}"}
    val tiles = dirs[tile]?: return null
    return tiles[one] ?: tiles[attrib]
}

class CompleteIfSeeTileOrHeartChange(private val tileToLookFor: Int = bigHeart, wrapped: Action) : WrappedAction(wrapped) {
    private var initial: Int? = null

    private fun hasTile(state: MapLocationState): Boolean =
        state.frameState.enemies.any { it.tile == tileToLookFor } || ((initial != null) && (initial != hearts(state)))

    private fun hearts(state: MapLocationState): Int = state.frameState.inventory.heartCalc.heartContainers()

    override fun reset() {
        initial = null
    }

    override fun complete(state: MapLocationState): Boolean =
        hasTile(state) || wrapped.complete(state)

    override fun nextStep(state: MapLocationState): GamePad {
        d { " CompleteIfSeeTileOrHeartChange initial $initial current ${hasTile(state)}" }
        state.frameState.logEnemies()
        if (initial == null) {
            initial = hearts(state)
        }
        return wrapped.nextStep(state)
    }

    override val name: String
        get() = "Until see tile $tileToLookFor do ${wrapped.name}"
}
