package bot.plan.action

import bot.state.Agent
import bot.state.ItemDropPrediction
import bot.state.MapLocationState
import bot.state.distTo
import bot.state.oam.MonsterColor
import bot.state.oam.circleMonsterOutside
import util.d

class KillAllTargetFilters(private val state: MapLocationState,
                           private val ignoreUntilOnly: Set<Int> = emptySet(),
                           private val targetOnly: List<Int>,
                           private val considerEnemiesInCenter: Boolean = false,
) {
    private var aliveEnemies = state.frameState.heartsClosestToLink().ifEmpty {
        state.frameState.enemiesClosestToLink()
    }.toMutableList()

    fun filter(lookForBombs: Boolean): List<Agent> {
        removeDamaged()
        if (considerEnemiesInCenter) {
            considerEnemiesInCenter()
        }
        // either of these could filter the targets
        targetOnlyInUse()
        lookForBombs(lookForBombs)

        aliveEnemies.forEach {
            d { "alive enemy $it dist ${it.point.distTo(state.frameState.link.point)}" }
        }

        return aliveEnemies
    }

    fun considerEnemiesInCenter() {
        val numEnemiesInCenter = state.numEnemiesAliveInCenter()
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

    fun targetOnlyInUse() {
        // specially handling for level 8 spinning center guy
        val targetOnlyUse =
            if (ignoreUntilOnly.isNotEmpty() && aliveEnemies.any { !ignoreUntilOnly.contains(it.tile) }) {
                d { " ignore only $ignoreUntilOnly" }
                circleMonsterOutside.toList()
            } else {
                targetOnly
            }

        if (targetOnlyUse.isNotEmpty()) {
            d { " target only $targetOnlyUse" }
            aliveEnemies = aliveEnemies.filter { targetOnlyUse.contains(it.tile) }.toMutableList()
        }
    }

    fun lookForBombs(lookForBombs: Boolean = true) {
        if (state.frameState.isOverworld &&
            (lookForBombs && state.frameState.inventory.numBombs < 4)) {
            if (ItemDropPrediction().bombsLikely()) {
                d { " bombs likely "}
                val enemiesThatMightProduceBombs =
                    aliveEnemies.filter { it.color == MonsterColor.blue || it.color == MonsterColor.grey }
                if (enemiesThatMightProduceBombs.isNotEmpty()) {
                    d { " !! only target enemies that might produce bombs" }
                    aliveEnemies = enemiesThatMightProduceBombs.toMutableList()
                }
            } else {
                val enemiesThatWillNotProduceBombs =
                    aliveEnemies.filter { it.color == MonsterColor.red }
                if (enemiesThatWillNotProduceBombs.isNotEmpty()) {
                    d { " !! only target enemies that will not produce bombs" }
                    aliveEnemies = enemiesThatWillNotProduceBombs.toMutableList()
                }
            }
        }
    }

    fun removeDamaged() {
        aliveEnemies = aliveEnemies.filter { !it.damaged }.toMutableList()
    }
}