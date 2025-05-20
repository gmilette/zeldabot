package bot.plan.action

import bot.state.*
import bot.state.map.*
import bot.state.oam.*
import util.Geom
import util.d

object AttackActionDecider {
    // how close to get to enemies before engaging the dodge
    private const val dodgeBuffer = 3
    var DEBUG = false

    private val longExtra = MapConstants.swordGridPlusOne
    // if link is half way into a grid, can he swing and hit the target
    private val shortExtra = MapConstants.swordGrid

    fun attackPoints(point: FramePoint) =
        point.upPoints() + point.rightPoints() + point.leftPoint() + point.downPoint() + point.cornersIn()

    fun attackPointsNoCorner(point: FramePoint) =
        point.upPoints() + point.rightPoints() + point.leftPoint() + point.downPoint()

    fun attackPoints(point: FramePoint, not: Direction): List<FramePoint> {
        return when (not) {
            Direction.Up -> point.upPoints() + point.rightPoints() + point.leftPoint() // + point.downPoint()
            Direction.Down -> point.rightPoints() + point.leftPoint() + point.downPoint()
            Direction.Left -> point.upPoints() + point.leftPoint() + point.downPoint()
            Direction.Right -> point.upPoints() + point.rightPoints() + point.downPoint()
            else -> attackPointsNoCorner(point)
        }
    }


    fun FramePoint.upPoints(): List<FramePoint> =
        FramePointBuilder.hasL(
            listOf(
                // up points
                // for now
//            x - MapConstants.halfGrid to (y + MapConstants.oneGridPoint5),
                // this -1 should allow link to chase
                x to (y + longExtra) - 3,
                x to (y + longExtra) - 2,
                x to (y + longExtra) - 1,
                x to (y + longExtra),
                // to do add all the points in between
                // i don't think so
                x + shortExtra to (y + longExtra),
                x + shortExtra to (y + longExtra) - 1,
                x + shortExtra to (y + longExtra) - 2,
                x + shortExtra to (y + longExtra) - 3,
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
                (x - longExtra + 3) to y,
                (x - longExtra) to (y + shortExtra),
                (x - longExtra + 1) to (y + shortExtra),
                (x - longExtra + 2) to (y + shortExtra),
                (x - longExtra + 3) to (y + shortExtra),
            )
        )

    fun FramePoint.leftPoint(): List<FramePoint> =
        FramePointBuilder.hasL(
            listOf(
                // up points
                // for now
                // this -1 should allow link to chase
                (x + longExtra) to y,
                (x + longExtra - 1) to y,
                (x + longExtra - 2) to y,
                (x + longExtra - 3) to y,
                (x + longExtra) to y + shortExtra,
                (x + longExtra - 1) to y + shortExtra,
                (x + longExtra - 2) to y + shortExtra,
                (x + longExtra - 3) to y + shortExtra,
            )
        )

    fun FramePoint.downPoint(): List<FramePoint> =
        FramePointBuilder.hasL(
            listOf(
                x to (y - longExtra) - 3,
                x to (y - longExtra) - 2,
                x to (y - longExtra) - 1,
                x to (y - longExtra),
                x + shortExtra to (y - longExtra),
                x + shortExtra to (y - longExtra) - 1,
                x + shortExtra to (y - longExtra) - 2,
                x + shortExtra to (y - longExtra) - 3,
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
        val all = Direction.entries

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

    fun inRangeOf(state: MapLocationState, targets: List<FramePoint>, useB: Boolean = false): GamePad {
        return inRangeOf(
            state.frameState.link.dir,
            state.link,
            targets,
            useB
        )
    }

    /**
     * it's annoying to watch link attack the spin guys, ignore those
     */
    fun aliveEnemiesCanAttack(state: MapLocationState): List<Agent> {
        val oppositeFrom by lazy { state.frameState.link.dir.opposite() }

        var enemies = state.aliveEnemies.toMutableList()
        return if (state.frameState.isLevel) {
            if (state.frameState.level == 1 && state.frameState.mapLoc == 53) {
                enemies.filter { it.tile in EnemyGroup.dragon1}
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
                // for sword guys, absolutely don't attach
                // for ghosts, it's ok to attack in front, as long as you are not DIRECTLY in front
                // problem: ghosts and swords use the same tile, making them indistinguishable
                val haveWizzRobe = (state.frameState.level in Monsters.levelsWithNotSword)
                if (enemies.any { !it.canAttackFront(state.frameState.level) }) {
                    for (dont in enemies.filter { !it.canAttackFront && it.dir == oppositeFrom }) {
                        d { "SWORD FRONT $haveWizzRobe DONT CHECK ${dont.point} can't attack from ${dont.dir} link facing ${state.frameState.link.dir}"}
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
        } else {
            if (state.currentMapCell.mapData.attributes.contains(MapCellAttribute.NoAttack)) {
                // don't attack anything
                emptyList()
            } else {
                enemies.filter { it.tile !in EnemyGroup.enemiesToNotAttackInOverworld }
            }
//        }.also {
//            d { " attackable opposite from $oppositeFrom" }
//            for (agent in it) {
//                d { " attackable: $agent"}
//            }
        }
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
        val smallTarget = useB // for bombing

        val enemiesClose = enemies
            .filter { link.distTo(it) < MapConstants.sixGrid }
            .sortedBy { it.distTo(link) }
            .map { if (smallTarget) it.toCenteredRect() else it.toRect16() }

        logDebugInfo(enemiesClose, swords, enemies, link, from, smallTarget)

        val linkRect = link.toRectPlus(-2) // make bigger to make sure there is contact
        val linkRectExact = link.toRectPlus(0) // make bigger to make sure there is contact

        // i donno, it seems like it isn't good for fighting sword guys
        if (enemiesClose.any { it == linkRectExact || (it.topLeft == linkRectExact.topLeft) }) {
            d { " kill the pancake!" }
            return GamePad.aOrB(useB)
        }

        val intersectResult = handleIntersectWithLink(enemiesClose, linkRect, swords, from, useB)
        if (intersectResult != null) {
            return intersectResult
        }

        if (enemiesClose.any {
                it.intersect(
                    swords.getOrDefault(
                        from,
                        Geom.Rectangle(FramePoint(), FramePoint())
                    )
                )
            }) {
            // attack it
            return GamePad.aOrB(useB)
        }

        // it's problematic when there is a corner
        // because it faces up, but can't go up, it has to persist in the cornering
        val FACE_ENEMY = true
        val attackMove = if (FACE_ENEMY) {
            // check other directions
            val otherDirs = swords.filter { it.key != from}
            enemiesClose.firstNotNullOfOrNull { enemy ->
                val attackDir = otherDirs.firstNotNullOfOrNull {
                    val dir = it.key
                    if (enemy.intersect(it.value)) dir else null
                }
                attackDir
            }?.toGamePad() ?: GamePad.None
        } else {
            GamePad.None
        }

        d {"Attack move $attackMove" }
        return attackMove
    }

    private fun handleIntersectWithLink(
        enemiesClose: List<Geom.Rectangle>,
        linkRect: Geom.Rectangle,
        swords: Map<Direction, Geom.Rectangle>,
        from: Direction,
        useB: Boolean
    ): GamePad? {
        // need?
        val intersectWithLink = enemiesClose.firstOrNull { it.intersect(linkRect) } ?: return null

        d { " intersects with link $linkRect enemy that intersects: $intersectWithLink" }
        val dirToAttack = intersectWithLink.distTo(swords[from] ?: Geom.Rectangle())
        d { " sword is ${swords[from]} $dirToAttack"}

        // if link is in a pancake then no swords will intersect
        if (intersectWithLink.intersect(swords[from] ?: Geom.Rectangle())) {
            d { " intersects with link's sword. Attack!" }
            return GamePad.aOrB(useB)
        } else {
            for (enemy in enemiesClose) {
                d { " enemy close to not evade $enemy" }
            }
            d { " doesnt intersect, evade" }
            return null // try null, maybe link will move towards enemy then
            // otherwise he just sits there, doesn't actually move
//            return GamePad.None
        }
    }

    private fun logDebugInfo(enemiesClose: List<Geom.Rectangle>, swords: Map<Direction, Geom.Rectangle>,
                             enemies: List<FramePoint>, link: FramePoint, from: Direction, smallTarget: Boolean) {
        if (DEBUG) {
            d { " link $link from direction $from" }
            for (enemy in enemies) {
                d { " enemy $enemy dist=${enemy.distTo(link)}" }
            }
            d { "**check intersect** near small=$smallTarget numClose=${enemiesClose.size}" }

            for (enemy in enemiesClose) {
                d { "enemy: $enemy" }
                for (sword in swords) {
                    if (sword.value.intersect(enemy)) {
                        d { "  ${sword.key}: Intersects sword ${sword.value}" }
                    } else {
                        d { "  ${sword.key}: Intersects No ${sword.value}" }
                    }
                }
            }
        }
    }

    fun facing(dir: Direction, other: Direction) = dir.opposite() == other

    fun swordRectangles(link: FramePoint): Map<Direction, Geom.Rectangle> {
//        val nearSize = MapConstants.halfGrid
//        val farSize = MapConstants.oneGridPoint5
        // experiment with this number. 2 seems too short
//        val swordSizeLessThanGrid = 6
        // 4 I still saw it miss, standing next to an enemy, so i changed it back to 6 so the
        // sword length would be 16 - 6 = 10, I just tried 8 to see if that is better
        // based on visuals, 8 is too small, go 6
        // could go 1 or 2 more, but let's try 6 for now
        val swordSizeLessThanGrid = 6 // // sword length
        val nearSize = MapConstants.oneGrid - swordSizeLessThanGrid
        val farSize = MapConstants.twoGrid - swordSizeLessThanGrid
        val leftAttack = Geom.Rectangle(
            link.justDown6.withX(link.x - nearSize), link.justDownLast6
        )
        val rightAttack = Geom.Rectangle(
            link.justDown6.justRightEnd,
            link.justDownLast6.justRightEnd.withX(link.x + farSize)
        )
        val upAttack = Geom.Rectangle(
            link.justRight6.withY(link.y - nearSize), link.justRightLast6
        )
        val downAttack = Geom.Rectangle(
            link.justRight6.justLeftDown,
            link.justRightLast6.justLeftDown.withY(link.y + farSize)
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