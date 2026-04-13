package bot.plan.zstar.route

import bot.plan.action.aliveEnemies
import bot.state.Agent
import bot.state.MapLocationState
import bot.state.map.Direction
import bot.state.map.MapCellAttribute
import bot.state.map.horizontal
import bot.state.map.opposite
import bot.state.map.vertical
import bot.state.oam.EnemyGroup
import bot.state.oam.Monsters
import bot.state.oam.circleMonsterCenters
import util.d

object AttackableDecider {
    private val DEBUG = false
    /**
     * it's annoying to watch link attack the spin guys, ignore those
     */
    fun aliveEnemiesCanAttack(state: MapLocationState): List<Agent> {
        val oppositeFrom by lazy { state.frameState.link.dir.opposite() }

        val enemies = state.aliveEnemies.toMutableList()
        return if (state.frameState.isLevel) {
            forLevel(state, enemies, oppositeFrom)
        } else {
            forOverworld(state, enemies)
        }
    }

    private fun forOverworld(
        state: MapLocationState,
        enemies: MutableList<Agent>
    ): List<Agent> = if (state.currentMapCell.mapData.attributes.contains(MapCellAttribute.NoAttack)) {
        // don't attack anything
        emptyList()
    } else {
        enemies.filter { it.tile !in EnemyGroup.enemiesToNotAttackInOverworld }
    }

    private fun forLevel(
        state: MapLocationState,
        enemies: MutableList<Agent>,
        oppositeFrom: Direction
    ): List<Agent> = if (state.frameState.level == 1 && state.frameState.mapLoc == 53) {
        enemies.filter { it.tile in EnemyGroup.dragon1 }
    } else {
        // doesnt really work
        if (state.frameState.level == 9) {
            if (state.frameState.mapLoc != 97 && state.frameState.mapLoc != 82) {
    //                        enemies = enemies.filter { it.tile !in circleMonsterCenters }
                if (enemies.any { it.tile !in circleMonsterCenters }) {
                    d { " ignore only have other monsters remove center" }
                    // disable until this works
                    enemies.removeIf { it.tile in circleMonsterCenters }
                } else {
                    d { " ignore only its just the center" }
                }
            }
            // it's not an enemy
    //                    if (state.frameState.mapLoc == 66) {
    //                        enemies.removeIf { it.tile in EnemyGroup.triforceTiles }
    //                    }
        }
        // nuance here
        // for sword guys, absolutely don't attack
        // for ghosts, it's ok to attack in front, as long as you are not DIRECTLY in front
        // problem: ghosts and swords use the same tile, making them indistinguishable
        val haveWizzRobe = (state.frameState.level in Monsters.levelsWithNotSword)
        if (DEBUG) {
            if (enemies.any { !it.canAttackFront(state.frameState.level) }) {
                for (dont in enemies.filter { !it.canAttackFront && it.dir == oppositeFrom }) {
                    d { "SWORD FRONT $haveWizzRobe DONT CHECK ${dont.point} can't attack from ${dont.dir} link facing ${state.frameState.link.dir}" }
                }
            }
        }
        // allow attacking as long as not directly in line with the wizzrobe!
        enemies.filter {
            it.canAttackFront(state.frameState.level) ||
                    (!haveWizzRobe && it.dir != oppositeFrom) ||
                    (haveWizzRobe && (it.dir.vertical && state.frameState.link.y != it.y)) ||
                    (haveWizzRobe && (it.dir.horizontal && state.frameState.link.x != it.x))
        }
    }
}