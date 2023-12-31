package bot.plan.action

import bot.state.*
import bot.state.map.*
import bot.state.oam.swordDir
import util.d
import kotlin.random.Random

object AttackActionDecider {
    // how close to get to enemies before engaging the dodge
    private const val dodgeBuffer = 3
    var DEBUG = true

    // don't swing if there is projectile near by!

    fun shouldAttack(state: MapLocationState) =
        // since the enemies aren't moving when the clock is activated
        // need to introduce the chance for link to do some random movement instead
        // should be fine since link can't get damaged
        if (state.frameState.clockActivated && Random.nextInt(4) == 1) {
            false
//        } else if (!NearestSafestPoint.isMapSafe(state, state.link) ) {
//            // need test
//            false
        } else {
            !getInFrontOfGrids(state) &&
            shouldAttack(
                state.frameState.link.dir,
                state.link,
                state.aliveEnemies.map { it.point }).also {
                state.aliveEnemies.forEach {
                    d { " enemy: $it in grid ${getInFrontOfGrids(state)}" }
                }
            }
        }

    private fun getInFrontOfGrids(state: MapLocationState): Boolean {
        val linkDir = state.frameState.link.dir
        return state.frameState.enemies.any { agent: Agent ->
            swordDir.dirFront(agent)?.let { dir ->
                val pt = dir.pointModifier(MapConstants.oneGrid)(agent.point)
                // link is in danger zone and is facing the enemy
                (state.link.isInGrid(pt) && dir.opposite() == linkDir)
            } ?: false
        }
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

    // throw b randomly
    fun shouldThrowProjectile() {
        // if have a projectile
        // if boomerang enabled, throw it at loot
        // if full health
        // if lined up vertical or horiz, and facing
    }

    // this should be same but isn't
    fun shouldAttack(from: Direction, link: FramePoint, enemiesClose: List<FramePoint>, considerInLink: Boolean = true, dist: Int = MapConstants.halfGrid): Boolean {
        val isLeftUp = when (from) {
            Direction.Left,
            Direction.Up -> true

            Direction.Right,
            Direction.Down -> false

            else -> false
        }
        // I think the enemy can keep moving while link winds up to hit so -1 is not enough
        val enemyMovesWhileSwingingFactor = 1

        val attackDirectionGrid = if (isLeftUp) {
            from.pointModifier(MapConstants.oneGridPoint5 - enemyMovesWhileSwingingFactor)(link)
        } else {
            // i think it should be whole
            from.pointModifier(MapConstants.oneGrid - enemyMovesWhileSwingingFactor)(link)
        }

        if (DEBUG) {
            d { " grid size third = $isLeftUp grid $attackDirectionGrid from $link" }
            for (framePoint in enemiesClose) {
                val distToGrid = framePoint.distTo(attackDirectionGrid)
                val distToLink = framePoint.distTo(link)
                d { "   linkg: ${link.isInGrid(framePoint)} ($distToLink) $link" }
                d { "   grid: ${attackDirectionGrid.isInGrid(framePoint)} ($distToGrid) $attackDirectionGrid" }
            }
        }

        return enemiesClose.any {
            // this doesn't always work unless the monster is really close, or link is facing the correct
            // way,
            (considerInLink && (link.isInHalfGrid(it) || (it.isInHalfGrid(link)))) ||
                    (attackDirectionGrid.isInHalfFatGrid(it, wide = from.vertical))
        }.also {
            d { "should attack $it dir = $from link = $link dirGrid = $attackDirectionGrid numEnemies ${enemiesClose.size}" }
        }
    }

    // I should be able to use the refactored version but whatever
    fun shouldAttackC(from: Direction, link: FramePoint, enemiesClose: List<FramePoint>, considerInLink: Boolean = true, dist: Int = MapConstants.halfGrid): Boolean {
        val useThirdGrid = when (from) {
            Direction.Left,
            Direction.Up -> true // MapConstants.oneGridPoint5
            Direction.Right,
            Direction.Down -> false // MapConstants.twoGridPoint5 //.oneGridPoint5 // MapConstants.halfGrid
            else -> false
        }
        // I think the enemy can keep moving while link winds up to hit so -1 is not enough
        // not quite
        // this can
        val enemyMovesWhileSwingingFactor = 1
//        val attackDirectionGrid = from.pointModifier(MapConstants.oneGrid - enemyMovesWhileSwingingFactor)(link) // -1 otherwise the sword is just out of reach
//        val attackDirectionGrid2 = from.pointModifier(MapConstants.oneGridPoint5 - enemyMovesWhileSwingingFactor)(link) // -1 otherwise the sword is just out of reach
//        val attackDirectionGrid3 = from.pointModifier(MapConstants.twoGridPoint5 - enemyMovesWhileSwingingFactor)(link) // -1 otherwise the sword is just out of reach
        val attackDirectionGrid = from.pointModifier(MapConstants.halfGrid - enemyMovesWhileSwingingFactor)(link) // -1 otherwise the sword is just out of reach
        val attackDirectionGrid2 = from.pointModifier(MapConstants.oneGridPoint5 - enemyMovesWhileSwingingFactor)(link) // -1 otherwise the sword is just out of reach
        val attackDirectionGrid3 = from.pointModifier(MapConstants.halfGrid - enemyMovesWhileSwingingFactor)(link) // -1 otherwise the sword is just out of reach

        d { " grid size third = $useThirdGrid grid $attackDirectionGrid from $link"}
        for (framePoint in enemiesClose) {
            val distToGrid = framePoint.distTo(attackDirectionGrid)
            val distToLink = framePoint.distTo(link)

            d { "enemy: $framePoint useThird $useThirdGrid in grid ${attackDirectionGrid.isInGrid(framePoint)} ($distToGrid) in link ${link.isInGrid(framePoint)} ($distToLink)"}
            if (DEBUG) {
                val distToGrid2 = framePoint.distTo(attackDirectionGrid2)
                val distToGrid3 = framePoint.distTo(attackDirectionGrid3)
                d { "   linkg: ${link.isInGrid(framePoint)} ($distToLink) $link"}
                d { "   grid1: ${attackDirectionGrid.isInGrid(framePoint)} ($distToGrid) $attackDirectionGrid"}
                d { "   grid2: ${attackDirectionGrid2.isInGrid(framePoint)} ($distToGrid2) $attackDirectionGrid2"}
                d { "   grid3: ${attackDirectionGrid3.isInGrid(framePoint)} ($distToGrid3) $attackDirectionGrid3"}
            }
        }
        // if it is on top of link ALWAYS attack, if it is in the direction link is facing, also attack
        //|| it.isInGrid(attackDirectionGrid) || it.isInGrid(attackDirectionGrid2)
        //|| attackDirectionGrid2.isInGrid(it)
//        val isInOnePt5Grid =
        return enemiesClose.any {
            // this doesn't always work unless the monster is really close, or link is facing the correct
            // way,
            (considerInLink && (link.isInHalfGrid(it) || (it.isInHalfGrid(link)))) ||
                    (!useThirdGrid && attackDirectionGrid.isInHalfGrid(it)) ||
                    (useThirdGrid && attackDirectionGrid2.isInHalfGrid(it)) ||
                    (false && useThirdGrid && attackDirectionGrid3.isInGrid(it))
        }.also {
            d { "should attack $it dir = $from link = $link dirGrid = $attackDirectionGrid numEnemies ${enemiesClose.size}" }
        }
    }

    fun shouldAttackB(from: Direction, link: FramePoint, enemiesClose: List<FramePoint>, considerInLink: Boolean = true, dist: Int = MapConstants.halfGrid): Boolean {
        val useThirdGrid = when (from) {
            Direction.Left,
            Direction.Up -> true // MapConstants.oneGridPoint5
            Direction.Right,
            Direction.Down -> false // MapConstants.twoGridPoint5 //.oneGridPoint5 // MapConstants.halfGrid
            else -> false
        }
        val attackDirectionGrid = from.pointModifier(MapConstants.oneGrid - 1)(link) // -1 otherwise the sword is just out of reach
        val attackDirectionGrid2 = from.pointModifier(MapConstants.oneGridPoint5 - 1)(link) // -1 otherwise the sword is just out of reach
        val attackDirectionGrid3 = from.pointModifier(MapConstants.twoGridPoint5 - 1)(link) // -1 otherwise the sword is just out of reach

        d { " grid size third = $useThirdGrid grid $attackDirectionGrid from $link"}
        for (framePoint in enemiesClose) {
            val distToGrid = framePoint.distTo(attackDirectionGrid)
            val distToLink = framePoint.distTo(link)

            d { "enemy: $framePoint useThird $useThirdGrid in grid ${attackDirectionGrid.isInGrid(framePoint)} ($distToGrid) in link ${link.isInGrid(framePoint)} ($distToLink)"}
            if (DEBUG) {
                val distToGrid2 = framePoint.distTo(attackDirectionGrid2)
                val distToGrid3 = framePoint.distTo(attackDirectionGrid3)
                d { "   grid1: ${attackDirectionGrid.isInGrid(framePoint)} ($distToGrid) $attackDirectionGrid"}
                d { "   grid2: ${attackDirectionGrid2.isInGrid(framePoint)} ($distToGrid2) $attackDirectionGrid2"}
                d { "   grid3: ${attackDirectionGrid3.isInGrid(framePoint)} ($distToGrid3) $attackDirectionGrid3"}
            }
        }
        // if it is on top of link ALWAYS attack, if it is in the direction link is facing, also attack
        //|| it.isInGrid(attackDirectionGrid) || it.isInGrid(attackDirectionGrid2)
        //|| attackDirectionGrid2.isInGrid(it)
//        val isInOnePt5Grid =
        return enemiesClose.any { (considerInLink && (link.isInGrid(it) || (it.isInGrid(link)))) ||
                attackDirectionGrid.isInGrid(it) ||
                attackDirectionGrid2.isInGrid(it) ||
                (useThirdGrid && attackDirectionGrid3.isInGrid(it))
        }.also {
            d { "should attack $it dir = $from link = $link dirGrid = $attackDirectionGrid numEnemies ${enemiesClose.size}" }
        }
    }

}