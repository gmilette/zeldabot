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

        val enemyX = Addresses.ememiesX.map { api.readCPU(it) }
        val enemyY = Addresses.ememiesY.map { api.readCPU(it) }
        val enemyPoint = enemyX.zip(enemyY).map { FramePoint(it.first, it.second) }

        val subY = api.readCPU(Addresses.ZeroPage.subY)
        val subX = api.readCPU(Addresses.ZeroPage.subX)
        val mapLoc = api.readCPU(Addresses.ZeroPage.mapLoc)
        val subPoint = FramePoint(subY, subX)

        // start unknown
        // if moved -> Alive
        // if have not moved for X frames then dead (need another way) -> Dead

        val enemyStates = enemyPoint.map { EnemyState.Alive }.toMutableList()
        if (true || state.enemyState.isEmpty()) {
            val enemyStatesStart = enemyPoint.map { EnemyState.Alive }
            state.enemyState = enemyStatesStart
        } else {
            // this is tricky, ignore this algorithm
            for (i in 0..enemyPoint.size) {
                val prevPoint = previous.enemies[i]
                val prevState = previous.ememyState[i]

                val current = enemyPoint[i]
                val changedLoc = prevPoint.x != current.x || prevPoint.y !=
                        current.y

                enemyStates.add(when {
                    // it just moved!
                    // if it was alive it will stay alive
                    changedLoc && prevState == EnemyState.Unknown -> EnemyState
                        .Alive
//                !changedLoc && prevState == EnemyState.Alive -> EnemyState
//                .Dead
                    else -> EnemyState.Unknown
                })
            }
        }

        val frame = FrameState(linkPoint, enemyPoint, enemyStates, subPoint,
            mapLoc)

        state.frameState = frame

//        d { "$currentFrame: $frame" }
    }
}