import bot.GamePad
import bot.plan.action.*
import bot.state.EnemyState
import bot.state.FramePoint
import bot.state.MapLocationState
import bot.state.map.grid
import util.d

class KillHandsInLevel7 : Action {
    private val safe = ActionSequence(InsideNavAbout(KillHandsLevel7Data.safe, 4), still)
    private val positionAttack = ActionSequence(
        InsideNavAbout(KillHandsLevel7Data.attackFrom, 0),
        GoIn(5, GamePad.MoveLeft, reset = true), AlwaysAttack()
    )
    private val attract = ActionSequence(InsideNavAbout(KillHandsLevel7Data.attractFrom, 0), GoIn(5, GamePad.MoveLeft, reset = true), still)

    private var lastAction = ""
    private var lastTarget = FramePoint(0, 0)

    private val criteria = DeadForAWhile {
        lastAction != "SAFE" && it.clearedWithMinIgnoreLoot(3)
    }

    // need to wait to make sure they are killed maybe
    override fun complete(state: MapLocationState): Boolean = criteria(state)

    private val MapLocationState.handActive: Boolean
        get() = this.frameState.enemies.any { it.droppedId == 1 && it.state == EnemyState.Alive }

    override fun target(): FramePoint {
        return super.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d {"KillHandsInLevel7 has hands ${state.handActive}"}
        criteria.nextStep(state)

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
            else -> GamePad.randomDirection().also {
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
    val attractFrom = FramePoint(2.grid-1, 6.grid)
}