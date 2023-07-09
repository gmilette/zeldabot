package bot.plan.action

import bot.state.*
import bot.state.map.*
import util.LogFile
import util.d

data class RhinoStrategyParameters(
    val waitFrames: Int = 100,
    // how many grids ahead of the rhino to target
    val targetGridsAhead: Int = 1,
    // how many grids above or below to target
    val targetGridAboveBelow: Int = 0,
    // maybe a strategy to target any within the range specified by the above parameters
    val targetAllWithin: Boolean = false
) {
    fun getTargetGrid(from: FramePoint, direction: Direction?): FramePoint? {
        if (direction == null) return null
        val adjusted = direction.pointModifier(MapConstants.oneGrid * targetGridsAhead)
        val adjustAboveBelow = direction.opposite().pointModifier(MapConstants.oneGrid * targetGridAboveBelow)
//        return if (from.isInLevelMap) adjusted(from) else null
        return adjusted(from)
    }
}

// assume switched to arrow
class KillRhino(private val params: RhinoStrategyParameters = RhinoStrategyParameters()) : Action {
    private val rhinoLog: LogFile = LogFile("Rhino")

    // 40 two bomb death
    private val ATTACK_DEATH_TIMING = 20
    private val TWO_BOMB_DEATH_TIMING = 30

    private val navToTarget = NavToTarget(ATTACK_DEATH_TIMING, ::targetSelect, ::weaponSelect)

    private var prevKnownPoint: FramePoint? = null

    private fun weaponSelect(state: MapLocationState): Boolean =
        bombHead(stateTracker.rhinoState)

    private fun targetSelect(state: MapLocationState): FramePoint? =
        state.rhino()?.let {
            // if doing two bomb timing
            val target = if (stateTracker.rhinoState is Stopped) {
                // it's too close for the two bomb timing
                it.point
            } else {
                params.getTargetGrid(it.point, state.rhinoDir())
            }
            // not on the head, in front of it
//            val target =
            d { " rhino location ${it.point} $target dir: ${state.rhinoDir()}"}
            if (target != null) {
                prevKnownPoint = target
            }
            target
//            it.point
//            params.getTargetGrid(it.point, state.rhinoDir())
        } ?: prevKnownPoint //known point can go null for a time

    private val eatingBombTile = setOf(
        0xF8, // attrib 43 // going down
        // attrib 03, and 43
        0xFE,
        // attrib 43
        0xF0, 0xee,
        // attrib 43
        0xF2
    )

    private val rhinoHeadLeftUp = 0xFA // foot up
    private val rhinoHeadLeftUp2 = 0xFC // foot down
    private val rhinoHeadLeftDown = 0xF6 // foot up
    // not that
    private val rhinoHeadLeftDown2 = 0xF4 // foot down
    //val rhinoHead = (0xE2).toInt() // mouth open
    //val rhinoHead2 = (0xE0).toInt()
    // rhinoHeadMouthClosed
    // rhinoHeadMouthOpen // 0xE2
    // rhinoHeadMouthClosed = 0xEA

    // either mouth is fine
    // for up down, pick the most left
    private val head = setOf(rhinoHeadMouthOpen, rhinoHeadMouthClosed, rhinoHeadLeftUp, rhinoHeadLeftUp2, rhinoHeadLeftDown, rhinoHeadLeftDown2)
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

    private fun MapLocationState.isEatingBomb() =
        frameState.enemies.firstOrNull { eatingBombTile.contains(it.tile) } != null

    private fun MapLocationState.rhinoDir(): Direction? =
        frameState.enemies.firstNotNullOfOrNull{ findDir(it.y, it.tile, it.attribute) }

    private fun MapLocationState.rhino(): Agent? =
        // pick the left most head
        frameState.enemies.filter { it.y != 187 && head.contains(it.tile) }.minByOrNull { it.x }

    private val criteria = DeadForAWhile(limit = 100) {
        it.clearedWithMinIgnoreLoot(0)
    }

    override fun path(): List<FramePoint> = navToTarget.path()

    override fun complete(state: MapLocationState): Boolean = criteria(state)

    override fun target(): FramePoint = navToTarget.target()

    private var prevPoint: FramePoint? = null

    val stateTracker = RhinoStateTracker()

    inner class RhinoStateTracker() {
        var rhinoState: RhinoState = ZeroState()

        fun update(eating: Boolean, moved: Boolean) {
            rhinoState = rhinoState.transition(eating, moved)
        }
    }

    private val moveBuffer = MoveBuffer(6)

    override fun nextStep(state: MapLocationState): GamePad {
        val isEatingBomb = state.isEatingBomb() // reliable
        val dir = state.rhinoDir()
        val where = state.rhino()

        // reliable for movement
        // look at 3 because it stays at same point for more than 1
//        val moved = where != null && prevPoint != null && prevPoint != where.point
        where?.let {
            moveBuffer.add(it.point)
        }
        val moved = !moveBuffer.isFull || !moveBuffer.allSame()

        stateTracker.update(isEatingBomb, moved)
        d { "Kill Rhino state ${stateTracker.rhinoState.name} moved $moved eating $isEatingBomb dir = $dir where = ${where?.point ?: ""}"}
        d { " move buffer $moveBuffer"}
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
        criteria.nextStep(state)
        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack
        d { "ACTION" }
//        val action = actions.nextStep(state)
        val action = navToTarget.nextStep(state)
//        if (where != null) {
//            prevKnownPoint = where.point
//        }
        prevPoint = where?.point
        return action
//        return GamePad.None
    }

    override val name: String
        get() = "KillRhino ${stateTracker.rhinoState.name} ${criteria.frameCount} ${navToTarget.name} " // ${actions.stepName}
}

private val attackA = AlwaysAttack()
private val attackB = AlwaysAttack(useB = true)

class NavToTarget(
    val waitBetweenAttacks: Int = 60,
    val targetSelector: (MapLocationState) -> FramePoint?,
                  val weaponSelectorUseB: (MapLocationState) -> Boolean) : Action {
    private val routeTo = RouteTo(RouteTo.Param(ignoreProjectiles = false, dodgeEnemies = false))

    private var waitCt = 0

    private var targets = listOf<FramePoint>()
    private var target = FramePoint()

//    override fun complete(state: MapLocationState): Boolean =
//        targets.isNotEmpty() && AttackActionDecider.shouldAttack(
//            from = state.frameState.link.dir,
//            state.link,
//            targets).also { "completed NavToTarget $it"}

    override fun complete(state: MapLocationState): Boolean = false

    override fun path(): List<FramePoint> = routeTo.route?.path ?: emptyList()

    override fun nextStep(state: MapLocationState): GamePad {
        val previousTarget = target

        target = targetSelector(state) ?: return GamePad.None
//        targets = target.about()
        targets = listOf(target)
        val useB = weaponSelectorUseB(state)

        val forceNew = previousTarget != target
        val weaponName = if (useB) "Bomb" else "Sword"

        // do my own attack check here
        val attack = AttackActionDecider.shouldAttack(state.frameState.link.dir, state.link, targets)

        d { " move to spot attack: $attack $target with $weaponName force ${if (forceNew) "new" else ""}" }
        // if the target is off map, don't route just do nothing

        return if (target.isInLevelMap) {
            if (attack) {
                if (waitCt > 0) {
                    waitCt--
                    GamePad.None
                } else {
                    waitCt = waitBetweenAttacks
                    if (weaponSelectorUseB(state)) {
                        GamePad.B
//                        attackB.nextStep(state)
                    } else {
//                        attackA.nextStep(state)
                        GamePad.A
                    }
                }
            } else {
                attackA.reset()
                attackB.reset()
                // why force new, only if the target is different
                routeTo.routeTo(state, targets, forceNew = forceNew, useB = useB).also {
                    d { " Move to $it" }
                }
            }
        } else {
            GamePad.None
        }
//        return routeTo.routeTo(state, targets, forceNew)
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
    fun notEating(): RhinoState? = null

    fun eat(): RhinoState = this

    fun stopped(): RhinoState = this

    fun moved(): RhinoState = this

    fun transition(eating: Boolean, moving: Boolean): RhinoState = this

    val name: String
        get() = this.javaClass.simpleName
}

class Default: RhinoState {
    override fun notEating(): RhinoState = this

    override fun eat(): RhinoState = this

    // is stopped moving, but not eating
    override fun stopped(): RhinoState = this

    override fun moved(): RhinoState = this

    // new transition state that is for when the rhino is gone
    // you can go from one state to death
    override fun transition(eating: Boolean, moving: Boolean): RhinoState = this
}

class ZeroState : RhinoState {
    override fun eat() = Eating()

    // if dropped the bomb in the right spot
    override fun stopped(): RhinoState = Stopped(this)

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

    // if was eating, and link didn't bomb right spot
    override fun moved(): RhinoState = then

    override fun notEating() = then

    override val name: String
        get() = "${this.javaClass.simpleName} then ${then.name}"
}

// don't add more bombs
class EatingTwo() : RhinoState {
}

// attack with sword
class Stopped(private val was: RhinoState): RhinoState {
    override fun transition(eating: Boolean, moving: Boolean): RhinoState {
        return when {
            eating -> was.eat()
            moving -> was
            else -> this
        }
    }

    override fun moved(): RhinoState = was

    // whatever should happen after the state it was
    override fun eat() = was.eat()

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

    // dead!
    override fun eat() = TwoState()

    // rarer, escaped the first bombing, and link has well-placed bomb
    override fun stopped(): RhinoState = Stopped(this)
}

// wait for rhino to die, sword to help it along, don't sword if eating
class TwoState : RhinoState

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
