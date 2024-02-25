package bot.plan.action

import bot.state.*
import bot.state.map.*
import bot.state.oam.swordDir
import util.Geom
import util.d

object AttackActionDecider {
    // how close to get to enemies before engaging the dodge
    private const val dodgeBuffer = 3
    var DEBUG = true

    private val longExtra = MapConstants.swordGridPlusOne
    private val shortExtra = MapConstants.swordGrid

    fun attackPoints(point: FramePoint) =
        point.upPoints() + point.rightPoints() + point.leftPoint() + point.downPoint() + point.cornersIn()

    // don't swing if there is projectile near by!
    fun FramePoint.upPoints(): List<FramePoint> =
        FramePointBuilder.hasL(
            listOf(
                // up points
                // for now
//            x - MapConstants.halfGrid to (y + MapConstants.oneGridPoint5),
                // this -1 should allow link to chase
                x to (y + longExtra) - 2,
                x to (y + longExtra) - 1,
                x to (y + longExtra),
                // to do add all the points in between
                // i don't think so
                x + shortExtra to (y + longExtra),
                x + shortExtra to (y + longExtra) - 1,
                x + shortExtra to (y + longExtra) - 2,
            )
        )

    fun FramePoint.rightPoints(): List<FramePoint> =
        FramePointBuilder.hasL(
            listOf(
                // up points
                // for now
//            x - shortExtra to (y + longExtra),
                // this -1 should allow link to chase
                (x - longExtra) to y,
                (x - longExtra + 1) to y,
                (x - longExtra + 2) to y,
                (x - longExtra) to (y + shortExtra),
                (x - longExtra + 1) to (y + shortExtra),
                (x - longExtra + 2) to (y + shortExtra),
            )
        )

    fun FramePoint.leftPoint(): List<FramePoint> =
        FramePointBuilder.hasL(
            listOf(
                // up points
                // for now
//            x - shortExtra to (y + longExtra),
                // this -1 should allow link to chase
                (x + longExtra) to y,
                (x + longExtra - 1) to y,
                (x + longExtra - 2) to y,
                (x + longExtra) to y + shortExtra,
                (x + longExtra - 1) to y + shortExtra,
                (x + longExtra - 2) to y + shortExtra,
            )
        )

    fun FramePoint.downPoint(): List<FramePoint> =
        FramePointBuilder.hasL(
            listOf(
                // i think these should be -1.5
//            x to (y - longExtra) - 2,
//            x to (y - longExtra) - 1,
                x to (y - longExtra),
                x + shortExtra to (y - longExtra),
//            x + shortExtra to (y - MapConstants.oneGridPoint5) - 1,
//            x + shortExtra to (y - MapConstants.oneGridPoint5) - 2,
            )
        )

    fun FramePoint.isInUpPointPosition(): Boolean =
        this in upPoints()

    fun getInFrontOfGrids(state: MapLocationState): Boolean {
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
        val enemies = state.aliveEnemies.map { it.point }.filter { it.isInGrid(link, dodgeBuffer) }
        if (enemies.isEmpty()) return GamePad.None

        // but some directions you cannot move in! depending on what the previous
        // movement was
        val dirTo = directionToDir(state.previousLocation, link)
        val validDirs = validDir(dirTo, state.previousLocation)

        val preferredDirection = Direction.values().filter { validDirs.contains(it) }.maxBy { dir ->
            val moved = dir.pointModifier()(link)
            val sum = enemies.sumOf { it.distToSquare(moved) }
            d { " dodge $dir -> $sum enemies = ${enemies.size}" }
            sum
        }
        d { " dodge $preferredDirection from ${state.previousMove}" }
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

    // if link is facing the right direction to hit this target
    fun shouldAttack(
        state: MapLocationState,
        target: FramePoint
    ): Boolean {
        // untested
        val dir = state.frameState.link.dir
        val swords = swordRectangles(state.link)
        val enemy = target.toRect()
        return swords[dir]?.intersect(enemy) ?: false
    }

    fun inRangeOf(state: MapLocationState): GamePad {
        return inRangeOf(
            state.frameState.link.dir,
            state.link,
            state.aliveEnemies.map { it.point },
            false
        )
    }

    fun inStrikingRange(from: FramePoint, enemies: List<FramePoint>): Boolean {
        val swords = swordRectangles(from)

        val enemiesClose = enemies.filter { from.distTo(it) < MapConstants.twoGrid }.map { it.toRect() }
        for (enemy in enemiesClose) {
            if (swords.any { it.value.intersect(enemy) }) {
                return true
            }
        }

        return false
    }

    fun inStrikingRange(from: FramePoint, enemy: FramePoint): Boolean {
        val swords = swordRectangles(from)
        val enemyRect = enemy.toRect()
        return swords.any { it.value.intersect(enemyRect) }
    }

    /**
     * null, no attack
     * gamepad dir, move, or gamePad.None not in range, cant attack
     * gamepad
     */
    fun inRangeOf(
        from: Direction,
        link: FramePoint,
        enemies: List<FramePoint>,
        useB: Boolean
    ): GamePad {
        val swords = swordRectangles(link)

        d { " link $link from $from" }
        for (enemy in enemies) {
            d { " enemy $enemy" }
        }
        d { "**check intersect**" }

        val enemiesClose = enemies.filter { link.distTo(it) < MapConstants.twoGrid }.map { it.toRect() }
        if (DEBUG) {
            for (enemy in enemiesClose.sortedBy { it.topLeft.distTo(link) }) {
                d { "enemy: $enemy" }
                for (sword in swords) {
                    if (sword.value.intersect(enemy)) {
                        d { "  ${sword.key}: Intersects $enemy" }
                    } else {
                        d { "  ${sword.key}: Intersects No $enemy" }
                    }
                }
            }
        }

        val linkRect = link.toRectPlus(-2) // make bigger to make sure there is contact
        val intersectWithLink = enemiesClose.any { it.intersect(linkRect) }
        if (intersectWithLink) {
            d { " intersects with link $linkRect"}
        }

        return if (intersectWithLink || enemiesClose.any {
                it.intersect(
                    swords.getOrDefault(
                        from,
                        Geom.Rectangle(FramePoint(), FramePoint())
                    )
                )
            }) {
            // attack it
            GamePad.aOrB(useB)
        } else {
            // check other directions
            val otherDirs = swords.filter { it.key != from }
            val attackMove = enemiesClose.firstNotNullOfOrNull { enemy ->
                val attackDir = otherDirs.firstNotNullOfOrNull {
                    val dir = it.key
                    if (enemy.intersect(it.value)) {
                        dir
                    } else {
                        null
                    }
                }
                attackDir
            }?.toGamePad() ?: GamePad.None
            attackMove
        }
    }

    fun swordRectangles(link: FramePoint): Map<Direction, Geom.Rectangle> {
//        val nearSize = MapConstants.halfGrid
//        val farSize = MapConstants.oneGridPoint5
        val swordSizeLessThanGrid = 6 // experiment with this number
        val nearSize = MapConstants.oneGrid - swordSizeLessThanGrid
        val farSize = MapConstants.twoGrid - swordSizeLessThanGrid
        val leftAttack = Geom.Rectangle(
            link.justDownFourth.withX(link.x - nearSize), link.justDownThreeFourth
        )
        val rightAttack = Geom.Rectangle(
            link.justDownFourth.justRightEnd,
            link.justDownThreeFourth.justRightEnd.withX(link.x + farSize)
        )
        val upAttack = Geom.Rectangle(
            link.justRightFourth.withY(link.y - nearSize), link.justRightThreeFourth
        )
        val downAttack = Geom.Rectangle(
            link.justRightFourth.justLeftDown,
            link.justRightThreeFourth.justLeftDown.withY(link.y + farSize)
        )

        val swords = mapOf(
            Direction.Left to leftAttack,
            Direction.Right to rightAttack,
            Direction.Up to upAttack,
            Direction.Down to downAttack
        )
        return swords
    }
}