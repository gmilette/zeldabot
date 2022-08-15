package bot.state

import bot.*
import nintaco.api.API
import util.d

class FrameStateUpdater(
    private val api: API
) {
    var state: MapLocationState = MapLocationState()

    fun updateFrame(currentFrame: Int, currentGamePad: ZeldaBot.GamePad) {
        val previous = state.frameState
        state.lastGamePad = currentGamePad
        //        https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:RAM_map
        val linkX = api.readCPU(Addresses.linkX)
        val linkY = api.readCPU(Addresses.linkY)
        val linkPoint = FramePoint(linkX, linkY)
        val linkDir = mapDir(Addresses.linkDir)
        val link = Agent(linkPoint, linkDir)

        val enemies = mutableListOf<Agent>()
        for (i in Addresses.ememiesX.indices) {
            val x = api.readCPU(Addresses.ememiesX[i])
            val y = api.readCPU(Addresses.ememiesY[i])
            val pt = FramePoint(x, y)
            val dir = mapDir(api.readCPU(Addresses.ememyDir[i]))
            val dropped = api.readCPU(Addresses.dropItemType[i])
            val countDown = api.readCPU(Addresses.enemyCountdowns[i])
            val enemy = Agent(pt, dir, enemyState(i), dropped > 0, countDown)
            enemies.add(enemy)
        }

        val enemyX = Addresses.ememiesX.map { api.readCPU(it) }
        val enemyY = Addresses.ememiesY.map { api.readCPU(it) }
        val enemyPoint = enemyX.zip(enemyY).map { FramePoint(it.first, it.second) }
        val enemyDirs = Addresses.ememyDir.map { mapDir(api.readCPU(it)) }
//        val enemyDirs = emptyList<Dir>()
        val droppedItems = Addresses.dropItemType.map { api.readCPU(it) }
        val countdowns = Addresses.enemyCountdowns.map { api.readCPU(it) }

        val subY = api.readCPU(Addresses.ZeroPage.subY)
        val subX = api.readCPU(Addresses.ZeroPage.subX)
        val mapLoc = api.readCPU(Addresses.ZeroPage.mapLoc)
        val subPoint = FramePoint(subY, subX)

        state.enemyLocationHistory.add(enemyPoint)
        if (state.enemyLocationHistory.size > 25) {
            state.enemyLocationHistory.removeFirst()
        }

        // start unknown
        // if moved -> Alive
        // if have not moved for X frames then dead (need another way) -> Dead

        val enemyStates: List<EnemyState>
        if (state.enemyLocationHistory.isEmpty()) {
            enemyStates = enemyPoint.map { EnemyState.Alive }
        } else {
            enemyStates = mutableListOf()
            // this is tricky, ignore this algorithm
            for (i in enemyPoint.indices) {
                val current = enemyPoint[i]
                val first = state.enemyLocationHistory.first()[i]
                val same = state.enemyLocationHistory.all {
                    it[i].x == first.x && it[i].y == first.y
                }

                val determinedState = when {
                    same -> EnemyState.Dead
                    else -> EnemyState.Alive
                }

                enemyStates.add(determinedState)

//                d { "it is $determinedState current ${current} first ${first}"}
//                state.enemyLocationHistory.forEach {
//                    d { "loc ${it[i]}" }
//                }
            }
        }

        //$08=North, $04=South, $01=East, $02=West
        //could try to remember how link last moved and that will be the
        // direction
        val frame = FrameState(enemies, link, subPoint,
            mapLoc)

        state.frameState = frame

        d { "$currentFrame: $frame" }
    }

    private fun enemyState(index: Int): EnemyState {
        if (state.enemyLocationHistory.isEmpty()) {
            return EnemyState.Unknown
        }
        val first = state.enemyLocationHistory.first()[index]
        val same = state.enemyLocationHistory.all {
            it[index].x == first.x && it[index].y == first.y
        }

        return when {
            same -> EnemyState.Dead
            else -> EnemyState.Alive
        }
    }

    private fun mapDir(dir: Int): Dir {
        val read = api.readCPU(dir) shr 4
//        d { "read $read" }
        // not sure what this notation means but
        // this could be bits
        // $08=North, $04=South, $01=East, $02=West
        // I think i need to ignore the higher bits
        val f1 = getBit(read, 1)
        val f2 = getBit(read, 2)
        val f3 = getBit(read, 3)
        val f4 = getBit(read, 4)
//        d {"f1 $f1 f2 $f2 f3 $f3 f $f4" }
        return if (f3 == 1) {
            Dir.Right
        }
        else if (f1 == 1) {
            d { " dir is 2 " }
            Dir.Up
        }
        else if (read == 64) {
            Dir.Down
        }
        else if (read == 8) {
            Dir.Up
        }
        else {
            Dir.Unknown
        }
    }
}

fun getBit(value: Int, position: Int): Int {
    return (value shr position) and 1;
}