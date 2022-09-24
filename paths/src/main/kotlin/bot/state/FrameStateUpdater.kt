package bot.state

import bot.*
import bot.plan.PreviousMove
import nintaco.api.API
import sequence.ZeldaItem
import util.d

class FrameStateUpdater(
    private val api: API,
    private val hyrule: Hyrule
) {
    var state: MapLocationState = MapLocationState()

    fun updateFrame(currentFrame: Int, currentGamePad: GamePad) {
        val yAdjustFactor = 61
        val previous = state.frameState
        state.lastGamePad = currentGamePad
        //        https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:RAM_map
        val linkX = api.readCPU(Addresses.linkX)
        val linkY = api.readCPU(Addresses.linkY) - yAdjustFactor
        val linkPoint = FramePoint(linkX, linkY)
        val linkDir = mapDir(Addresses.linkDir)
        val link = Agent(linkPoint, linkDir)

        val enemies = mutableListOf<Agent>()
        for (i in Addresses.ememiesX.indices) {
            val x = api.readCPU(Addresses.ememiesX[i])
            val y = api.readCPU(Addresses.ememiesY[i]) - yAdjustFactor
            val pt = FramePoint(x, y)
            val dir = mapDir(api.readCPU(Addresses.ememyDir[i]))
            val dropped = api.readCPU(Addresses.dropItemType[i])
            val countDown = api.readCPU(Addresses.enemyCountdowns[i])
            val enemy = Agent(pt, dir, enemyState(i), dropped > 0, countDown)
            enemies.add(enemy)
        }

        val enemyX = Addresses.ememiesX.map { api.readCPU(it) }
        val enemyY = Addresses.ememiesY.map { api.readCPU(it) - yAdjustFactor}
        val enemyPoint = enemyX.zip(enemyY).map { FramePoint(it.first, it.second) }
        val enemyDirs = Addresses.ememyDir.map { mapDir(api.readCPU(it)) }
//        val enemyDirs = emptyList<Dir>()
        val droppedItems = Addresses.dropItemType.map { api.readCPU(it) }
        val countdowns = Addresses.enemyCountdowns.map { api.readCPU(it) }

        val subY = api.readCPU(Addresses.ZeroPage.subY)
        val subX = api.readCPU(Addresses.ZeroPage.subX)
        val mapLoc = api.readCPU(Addresses.ZeroPage.mapLoc)
        state.currentMapCell = hyrule.getMapCell(mapLoc)
        val subPoint = FramePoint(subY, subX)

        val gameMode = api.readCPU(Addresses.gameMode)

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

        state.previousMove = PreviousMove(
            from = state.previousLocation,
            to = state.previousLocation.adjustBy(state.lastGamePad),
            actual = link.point,
            triedToMove = state.lastGamePad.isMoveAction())

        val selectedItem = api.readCPU(Addresses.selectedItem)
        // inventory
        val numBombs = api.readCPU(Addresses.numBombs)
        val items = mutableSetOf<ZeldaItem>()
        when (api.readCPU(Addresses.hasSword)) {
            1 -> items.add(ZeldaItem.WoodenSword)
            2 -> items.add(ZeldaItem.WhiteSword)
            2 -> items.add(ZeldaItem.MagicSword)
            else -> {}
        }
        if (api.readCpuB(Addresses.hasBow)) items.add(ZeldaItem.Bow)
        if (api.readCpuB(Addresses.hasBook)) items.add(ZeldaItem.BookOfMagic)
        if (api.readCpuB(Addresses.hasBoomerang)) items.add(ZeldaItem.Boomerang)
        if (api.readCpuB(Addresses.hasFood)) items.add(ZeldaItem.Food)
        if (api.readCpuB(Addresses.hasBracelet)) items.add(ZeldaItem.PowerBracelet)
        if (api.readCpuB(Addresses.hasLadder)) items.add(ZeldaItem.Ladder)
        if (api.readCpuB(Addresses.hasLetter)) items.add(ZeldaItem.Letter)
        if (api.readCpuB(Addresses.hasMagicBoomerang)) items.add(ZeldaItem.MagicalBoomerang)
        when (api.readCPU(Addresses.hasArrow)) {
            1 -> items.add(ZeldaItem.Arrow)
            2 -> items.add(ZeldaItem.SilverArrow)
            else -> {}
        }
        when (api.readCPU(Addresses.hasPotion)) {
            1 -> items.add(ZeldaItem.Potion)
            2 -> items.add(ZeldaItem.SecondPotion)
            else -> {}
        }
        if (api.readCpuB(Addresses.hasPotion)) items.add(ZeldaItem.Potion)
        if (api.readCpuB(Addresses.hasRaft)) items.add(ZeldaItem.Raft)
        when (api.readCPU(Addresses.hasRing)) {
            1 -> items.add(ZeldaItem.BlueRing)
            2 -> items.add(ZeldaItem.RedRing)
            else -> {}
        } // todo
        if (api.readCpuB(Addresses.hasShield)) items.add(ZeldaItem.MagicShield)
        if (api.readCpuB(Addresses.hasWhistle)) items.add(ZeldaItem.Whistle)
        if (api.readCpuB(Addresses.hasRod)) items.add(ZeldaItem.Wand)
        val inventory = Inventory(selectedItem, numBombs, items)

        //$08=North, $04=South, $01=East, $02=West
        //could try to remember how link last moved and that will be the
        // direction
        val frame = FrameState(gameMode, enemies, link, subPoint,
            mapLoc, inventory)

        this.state.previousLocation = link.point

        state.framesOnScreen++
        state.frameState = frame

//        d { "$currentFrame: $frame" }
//        d { "MAP --> ${frame.mapLoc}" }
        if (gameMode != 5) {
            d { "gameMode --> ${gameMode}" }
        }
    }

    // $00=False, $01=True
    private fun API.readCpuB(address: Int): Boolean =
        api.readCPU(address) != 0

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