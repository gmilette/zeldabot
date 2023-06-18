package bot.plan.action

import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapConstants
import bot.state.map.pointModifier
import bot.state.map.toGamePad
import util.d

object AttackActionDecider {
    // how close to get to enemies before engaging the dodge
    private const val dodgeBuffer = 3

    fun shouldAttack(state: MapLocationState) =
        shouldAttack(
            state.frameState.link.dir,
            state.link,
            state.aliveEnemies.map { it.point }).also {
//            state.aliveEnemies.forEach {
//                d { " enemy: $it" }
//            }
        }

    fun shouldDodgeDepending(state: MapLocationState): GamePad {
        // if it is ball projectile, going diagonal dodge all directions
        // if it is normal one-direction monster and that monster is facing link and in front of, retreat
        // if it is blockable one-direction projectile, and not facing it, face it (like an arrow)
          // if you have magic shield, you can block more
        // fast monster: attack at 2+ out
        return GamePad.None
    }

    fun shouldDodge(state: MapLocationState): GamePad {
        val link = state.link
        // if it is a small projectile, then we only need to avoid a half grid
        val enemies = state.aliveEnemies.map { it.point } .filter { it.isInGrid(link, dodgeBuffer) }
        if (enemies.isEmpty()) return GamePad.None

        // but some directions you cannot move in! depending on what the previous
        // movement was
        val dirTo = directionToDir(state.previousLocation, link)
        val validDirs = validDir(dirTo, state.previousLocation)

        val preferredDirection = Direction.values().filter { validDirs.contains(it) }.maxBy { dir ->
            val moved = dir.pointModifier()(link)
            val sum = enemies.sumOf { it.distToSquare(moved) }
            d { " dodge $dir -> $sum enemies = ${enemies.size}"}
            sum
        }
        d { " dodge $preferredDirection from ${state.previousMove}"}
        return preferredDirection.toGamePad()
    }

    private fun validDir(dir: Direction, from: FramePoint): List<Direction> {
        val all = Direction.values().toList()

        return when (dir) {
            Direction.Left,
            Direction.Right -> {
                if (from.onHighwayY) {
                    all
                } else {
                    Direction.horizontal
                }
            }
            Direction.Up,
            Direction.Down -> {
                if (from.onHighwayX) {
                    all
                } else {
                    Direction.vertical
                }
            }
            Direction.None -> all
        }
    }

    private fun directionToDir(from: FramePoint, to: FramePoint): Direction {
        return when {
            from.x == to.x -> {
                if (from.y < to.y) Direction.Down else Direction.Up
            }

            from.y == to.y -> {
                if (from.x < to.x) Direction.Right else Direction.Left
            }

            else -> Direction.Left
        }
    }

    fun shouldAttack(from: Direction, link: FramePoint, enemiesClose: List<FramePoint>, dist: Int = MapConstants.oneGrid): Boolean {
        val attackDirectionGrid = from.pointModifier(dist - 1)(link) // -1 otherwise the sword is just out of reach

        d { "should attack dir = $from link = $link dirGrid = $attackDirectionGrid numEnemies ${enemiesClose.size}" }

        // if it is on top of link ALWAYS attack, if it is in the direction link is facing, also attack
        return enemiesClose.any { link.isInGrid(it) || attackDirectionGrid.isInGrid(it) }
    }
}