package bot.plan.action

import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.Direction
import bot.state.map.toGamePad
import util.d


class AlwaysAttack(useB: Boolean = false, private val freq: Int = 5, private val otherwiseRandom: Boolean = false) :
    Action {
    private var frames = attackLength
    val gameAction = if (useB) GamePad.B else GamePad.A

    companion object {
        private const val attackLength = 10
    }

    fun attackWaiting(): Boolean =
        frames < 0

    override fun nextStep(state: MapLocationState): GamePad {
        // just always do it
        val move = if (frames < 0) {
            d { "*x* wait for frames $attackLength frames = $frames" }
            GamePad.None
        } else {
            when {
                frames % attackLength < freq -> {
                    d { "*x* do attack now mod ${frames % attackLength} $attackLength frames = $frames" }
                    gameAction
                }
                else -> {
                    d { "*x* do attack now wait $attackLength $otherwiseRandom frames = $frames" }
                    if (otherwiseRandom) {
                        val dir = state.bestDirection()
                        if (dir == Direction.None) {
                            GamePad.randomDirection(state.link)
                        } else {
                            dir.toGamePad()
                        }
                    } else {
                        GamePad.None
                    }
                }
            }
        }
        frames++
        return move
    }

    /**
     * link will attack for 10 frames. If frames start at 10, then continue attacking until 10 frames pass
     * (doesn't seem to hurt things
     * Incorrect, the attack actually lasts 15 frames
     */
    fun isAttacking(): Boolean =
        frames < (attackLength * 2) && frames != attackLength

    override fun reset() {
        frames = attackLength
    }

    override fun complete(state: MapLocationState): Boolean =
        false
}


/**
 * attacks as soon as it can, but if there is a projectile sword the projectile
 * will not fire so the states are
 * Can Attack
 * Can Attack Or Shoot
 * Attacking
 * Not attacking
 */
class AlwaysAttackWhenCan(useB: Boolean = false, private val otherwiseRandom: Boolean = false) :
    Action {
    private var framesSinceAttacked = 0
    val gameAction = if (useB) GamePad.B else GamePad.A

    override fun nextStep(state: MapLocationState): GamePad {
        return if (state.frameState.linkDoingAnAttack()) {
            framesSinceAttacked++
            if (otherwiseRandom) {
                val dir = state.bestDirection()
                if (dir == Direction.None) {
                    GamePad.randomDirection(state.link)
                } else {
                    dir.toGamePad()
                }
            } else {
                GamePad.None
            }
        } else {
            if (framesSinceAttacked > 0) {
                d { " attacked with $framesSinceAttacked" }
            }
            framesSinceAttacked = 1
            gameAction
        }
    }

    fun isAttacking(state: MapLocationState): Boolean =
        state.frameState.linkDoingAnAttack()

    override fun complete(state: MapLocationState): Boolean =
        false

    override fun reset() {
        framesSinceAttacked = 0
    }
}