import bot.plan.action.*
import bot.state.*
import bot.state.map.grid
import bot.state.oam.grabbyHands
import bot.state.oam.grabbyHands2
import util.d

class KillHandsInLevel7 : Action {
    private val safe = ActionSequence(InsideNavAbout(KillHandsLevel7Data.safe, 4, ignoreProjectiles = true), still)
    private val positionAttack = ActionSequence(
        InsideNavAbout(KillHandsLevel7Data.attackFrom, 0),
        GoIn(5, GamePad.MoveLeft, reset = true), AlwaysAttack()
    )
    private val attract = ActionSequence(InsideNavAbout(KillHandsLevel7Data.attractFrom, 0, ignoreProjectiles = true), GoIn(5, GamePad.MoveLeft, reset = true), still)

    private var lastAction = ""
    private var lastTarget = FramePoint(0, 0)

    private val criteria = DeadForAWhile {
        lastAction != "SAFE" && it.clearedWithMinIgnoreLoot(0) // leave no enemies
    }

    // need to wait to make sure they are killed maybe
    override fun complete(state: MapLocationState): Boolean = criteria(state)

    // was droppedId == 1
    private val MapLocationState.handActive: Boolean
        get() = this.frameState.enemies.any { (it.tile == grabbyHands || it.tile == grabbyHands2)  && it.state == EnemyState.Alive }

    override fun target(): FramePoint {
        return super.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {"KillHandsInLevel7 has hands ${state.handActive}"}
        criteria.nextStep(state)

        state.frameState.logAliveEnemies()

        return when {
            !state.frameState.canUseSword -> safe.nextStep(state).also {
                d {" --> SAFE"}
                lastAction = "SAFE"
                lastTarget = KillHandsLevel7Data.safe
            }
            state.handActive -> positionAttack.nextStep(state).also {
                d {" --> ATTACK"}
                lastAction = "ATTACK"
                criteria.seenEnemy()
                lastTarget = KillHandsLevel7Data.attackFrom
            }
            !state.handActive -> attract.nextStep(state).also {
                d {" --> ATTRACT"}
                lastAction = "ATTRACT"
                lastTarget = KillHandsLevel7Data.attractFrom
            }
            else -> GamePad.randomDirection(state.link).also {
                d {" --> RANDOM"}
                lastTarget = FramePoint()
            }
        }
    }

    override val name: String
        get() = "killHandsInLevel7 $lastAction ${criteria.frameCount}"
}

object KillHandsLevel7Data {
    val safe = FramePoint(3.grid, 6.grid)
    val attackFrom = FramePoint(3.grid, 6.grid)
    val attractFrom = FramePoint(2.grid, 6.grid) //-1?
}