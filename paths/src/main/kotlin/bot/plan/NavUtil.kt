package bot.plan

import bot.*
import bot.state.*
import util.d
import kotlin.math.abs
import kotlin.random.Random
import java.util.TreeMap

class MoveTowardsUtil() {
    private var directions: MutableList<GamePad> = mutableListOf()

    fun moveTowards(link: FramePoint, target: FramePoint, previousMove: PreviousMove): GamePad {
        d { " dir $directions $link to ${previousMove.to} didnt ${previousMove
            .didntMove} actual ${previousMove.actual} near " +
                "${previousMove.movedNear}"}
        return when {
            directions.isEmpty() -> {
                makeDirections(link, target)
                directions.first()
            }
            // the previous link didn't even try to move
            !previousMove.triedToMove -> directions.first()
            // next best
            previousMove.didntMove -> {
                // remove
                val removed = directions.removeFirst()
                d { " ** removed $removed"}
                if (directions.isEmpty()) {
                    makeDirections(link, target)
                }
                directions.first()
            }
            // keep going!
            previousMove.movedNear -> {
                d { " keep going "}
                directions.first()
            }
            // new path, probably got pushed far away
            else -> {
                makeDirections(link, target)
                directions.first()
            }
         }
    }

    data class CostDir(val cost: Int, val dist: Int, val dir: GamePad)

    private fun makeDirections(link: FramePoint, target: FramePoint) {
        val moveLeftVal = link.x - target.x
        val moveRightVal = target.x - link.x
        val moveUpValue = link.y - target.y
        val moveDownValue = target.y - link.y

        val distToGoal = link.distTo(target)
        val distLeft = link.left.distTo(target)
        val distR = link.right.distTo(target)
        val distU = link.up.distTo(target)
        val distD = link.down.distTo(target)

        var all = mutableListOf<CostDir>()
        if (moveLeftVal >= 0 && distLeft < distToGoal) all.add(CostDir(moveLeftVal,
            distLeft, GamePad
            .MoveLeft))
        if (moveRightVal >= 0 && distR < distToGoal) all.add(CostDir
            (moveRightVal, distR, GamePad.MoveRight))
        if (moveUpValue >= 0 && distU < distToGoal) all.add(CostDir
            (moveUpValue, distU, GamePad.MoveUp))
        if (moveDownValue >= 0 && distD < distToGoal) all.add(CostDir
            (moveDownValue, distD, GamePad.MoveDown))
        all = all.sortedBy { -it.cost }.toMutableList()

        // only keep positive

        // todo: now only keep if get closer to the goal

            // which every is positive
        // the positive values should be first

//        val all = mutableListOf<CostDir>(
//            CostDir(moveLeftVal, GamePad.MoveLeft),
//            CostDir(moveRightVal, GamePad.MoveRight),
//            CostDir(moveUpValue, GamePad.MoveUp),
//            CostDir(moveDownValue, GamePad.MoveDown),
//        ).sortedBy { -it.cost }

        directions = all.map { it.dir }.toMutableList()

        d {"Can move: ${all} ${directions}"}
    }
}

data class PreviousMove(
    val from: FramePoint = FramePoint(0, 0), val to:
    FramePoint = FramePoint(0, 0),
    val actual: FramePoint = FramePoint(0, 0),
    val triedToMove: Boolean = true,
    // calculated
    val skipped: Boolean = from.distTo(actual) > 1,
    val distOff: Int = from.distTo(actual)
) {
    val didntMove: Boolean = from.x == actual.x && from.y == actual.y
    val movedNear: Boolean = from.distTo(actual) < 3
}

object NavUtil {

    fun moveTowardsRoute(mapCell: MapCell, link: FramePoint, target:
    FramePoint
    ): GamePad {

//        val next = mapCell.path(link, target).firstOrNull() ?:
//            return ZeldaBot.GamePad.MoveRight
//
//        return directionTo(link, next)
        return GamePad.MoveLeft
    }

    fun directionToAvoidingObstacleM(mapCell: MapCell, from: FramePoint, to:
    FramePoint): GamePad {
        return manhattanPathFinder(mapCell, from, to) ?: randomDir()
    }

    fun randomDir(): GamePad {
        val dir = Random.nextInt(4)
        return when (dir) {
            0 -> GamePad.MoveLeft
            1 -> GamePad.MoveRight
            2 -> GamePad.MoveUp
            else -> GamePad.MoveDown
        }
    }

    fun manhattanPathFinder(mapCell: MapCell, from: FramePoint, to:
        FramePoint): GamePad? {
        val path = mutableListOf<FramePoint>()
        val directions = mutableListOf<GamePad>()
        var current = from
        var dist = from.distTo(to)
        val limit = 1000
        var tries = 0
        while (dist > 5 && tries < limit) {
            tries++
            dist = current.distTo(to)
            val dir = directionToAvoidingObstacleZZ(mapCell, current, to)
            d { "go $dir $dist" }
            current = when {
                dir == GamePad.MoveUp && !current.isTop -> current.up
                dir == GamePad.MoveDown && !current.isBottom -> current.down
                dir == GamePad.MoveLeft && !current.isLeft -> current.left
                dir == GamePad.MoveRight && !current.isRight -> current
                    .right
                else -> current.up
            }
            directions.add(dir)
            path.add(current)
        }

        return if (tries > limit - 10 || directions.isEmpty()) {
            d { " random dir "}
            null
        } else {
            return directions.first()
        }
    }

    /// !!!! develop this one
    fun directionToAvoidingObstacleZ(mapCell: MapCell, from: FramePoint, to:
        FramePoint): GamePad {
        val canGoUpOrDown = mapCell.passable.get(from.up) ||
                mapCell.passable.get(from.down)

        val canGoRorL = mapCell.passable.get(from.left) ||
                mapCell.passable.get(from.right)

        val closerToY = (abs(from.x - to.x) > abs(from.y - to.y))
        val closeToX = abs(from.x - to.x) < 16
        val closeToY = abs(from.y - to.y) < 16
        d { " action: $closerToY upd $canGoUpOrDown rl $canGoRorL " }
        return when {
            //            dist < 18 -> {
//                whenClose()
//            }
            closerToY && canGoRorL -> {
                if (from.x < to.x) GamePad.MoveRight else GamePad.MoveLeft
            }
            canGoUpOrDown -> { // closer to y
                // if within range
                if (from.y < to.y) GamePad.MoveDown else GamePad.MoveUp
            }
            else -> {
                d { " default action " }
                GamePad.None
//                when {
//                    mapCell.passable.get(from.down) -> ZeldaBot.GamePad.MoveDown
//                    mapCell.passable.get(from.up) -> ZeldaBot.GamePad.MoveUp
//                    mapCell.passable.get(from.right) -> ZeldaBot.GamePad
//                        .MoveRight
//                    else -> ZeldaBot.GamePad.MoveLeft
//                }
            }
        }
    }

    fun directionToAvoidingObstacle(mapCell: MapCell, from: FramePoint, to: FramePoint):
            GamePad {
        return directionToAvoidingObstacleR(mapCell, from, listOf(to))
//        return directionToAvoidingObstacleM(mapCell, from, to)
    }

    fun directionToAvoidingObstacle(mapCell: MapCell, from: FramePoint, to: List<FramePoint>):
            GamePad {
        return directionToAvoidingObstacleR(mapCell, from, to)
//        return directionToAvoidingObstacleM(mapCell, from, to)
    }

    fun directionToAvoidingObstacleR(mapCell: MapCell, from: FramePoint, to:
        List<FramePoint>):
            GamePad {
        val route = mapCell.gstar.route(from, to)
        d { " route size ${route.size} "}
        if (route.size < 2) return randomDir()

        val nextPoint = mapCell.gstar.route(from, to)[1]
        d { " next point $nextPoint"}
        return if (nextPoint == null) {
            randomDir()
        } else {
            directionTo(from, nextPoint)
        }
    }

    // test case
    // map 103
    // link: FramePoint(x=176, y=128)

    fun directionToAvoidingObstacleZZ(mapCell: MapCell, from: FramePoint, to:
    FramePoint):
            GamePad {

        val canGoRight = mapCell.passable.get(from.rightEnd)
        val canGoLeft = mapCell.passable.get(from.left)
//        val canGoUp = mapCell.passable.get(from.upEnd)
        val canGoUp = mapCell.passable.get(from.up)
        val canGoDown = mapCell.passable.get(from.downEnd)
        d { " action: up $canGoUp d $canGoDown r $canGoRight l $canGoLeft" }

        return when {
            (from.y > to.y) && canGoUp -> GamePad.MoveUp
            (from.x > to.x) && canGoLeft -> GamePad.MoveLeft
            canGoDown -> GamePad.MoveDown
            canGoRight -> GamePad.MoveRight
            else -> randomDir()
        }

    }

    fun directionTo(from: FramePoint, to: FramePoint): GamePad {
        return when {
            from.x == to.x -> {
                if (from.y < to.y) GamePad.MoveDown else GamePad.MoveUp
            }
            from.y == to.y -> {
                if (from.x < to.x) GamePad.MoveRight else GamePad.MoveLeft
            }
            else -> {
                d { " default direction to " }
                GamePad.MoveLeft
            }
        }
    }

    fun directionToDir(from: FramePoint, to: FramePoint): Direction {
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


    fun moveTowards(link: FramePoint, target: FramePoint): GamePad {
        val dist = abs(target.x - link.x) + abs(target
            .y - link.y)
        d { " go find $target from $link distance: $dist"}
        return when {
//            dist < 18 -> {
//                whenClose()
//            }
            (link.x.closeTo(target.x, 5)) -> {
                when {
                    (link.y > target.y) -> GamePad.MoveUp
                    else -> GamePad.MoveDown
                }
            }
            else -> when {
                (link.x.closeTo(target.x, 5)) -> GamePad
                    .MoveLeft
                else -> GamePad.MoveRight
            }
        }
    }
}

fun Int.closeTo(other: Int, tolerance: Int) =
    this > (other - tolerance) && this < other + tolerance


