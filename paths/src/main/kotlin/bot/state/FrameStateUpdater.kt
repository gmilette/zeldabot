package bot.state

import bot.plan.action.PreviousMove
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.map.MapConstants
import bot.state.map.horizontal
import nintaco.api.API
import bot.state.map.destination.ZeldaItem
import util.d
import util.e
import kotlin.math.max

class FrameStateUpdater(
    private val api: API,
    private val hyrule: Hyrule
) {
    val yAdjustFactor = 61

    private fun getLinkX() = api.readCPU(Addresses.linkX)
    private fun getLinkY() = api.readCPU(Addresses.linkY)
    fun getLink() = FramePoint(getLinkX(), getLinkY())
    var state: MapLocationState = MapLocationState(hyrule = hyrule)

    fun reset() {
        d { "RESET State" }
        state = MapLocationState(hyrule)
    }

    fun fillTriforce() {
        api.writeCPU(Addresses.triforce, 255)
    }

    fun fillHearts() {
        api.writeCPU(Addresses.heartContainers, 0xCC) // 13 hearts all full
        api.writeCPU(Addresses.heartContainersHalf, 0xFF) // make the half heart full too
    }

    fun setRing(item: ZeldaItem) {
        val ringId = when (item) {
            ZeldaItem.BlueRing -> 1
            ZeldaItem.RedRing -> 2
            else -> 0
        }
        // doesnt change visual but affects damage
        api.writeCPU(Addresses.hasRing, ringId)
    }

    fun setLadderAndRaft(enable: Boolean) {
        api.writeCPU(Addresses.hasLadder, enable.intTrue)
        api.writeCPU(Addresses.hasRaft, enable.intTrue)
    }

    fun setBait() {
        api.writeCPU(Addresses.hasFood, 1)
    }

    fun setLetter() {
        api.writeCPU(Addresses.hasLetter, 1)
    }

    fun setArrow() {
        api.writeCPU(Addresses.hasBow, 1)
        // silver arrow of course, let's be luxurious
        api.writeCPU(Addresses.hasArrow, 2)
    }

    fun deactivateClock() {
        api.writeCPU(Addresses.clockActivated, 0)
    }

    fun setRedCandle() {
        api.writeCPU(Addresses.hasCandle, 2)
    }

    fun setHaveWhistle() {
        api.writeCPU(Addresses.hasWhistle, 1)
    }

    fun addKey() {
        val current = state.frameState.inventory.numKeys
        api.writeCPU(Addresses.numKeys, current + 1)
    }

    fun addRupee() {
        val current = state.frameState.inventory.numRupees
        val plus100 = max(252, current + 100)
        api.writeCPU(Addresses.numRupees, plus100)
    }

    fun setSword(item: ZeldaItem) {
        when (item) {
            ZeldaItem.WoodenSword -> api.writeCPU(Addresses.hasSword, 1)
            ZeldaItem.WhiteSword -> api.writeCPU(Addresses.hasSword, 2)
            ZeldaItem.MagicSword -> api.writeCPU(Addresses.hasSword, 3)
            else -> {
                api.writeCPU(Addresses.hasSword, 0)
            }
        }
    }

    fun updateFrame(currentFrame: Int, currentGamePad: GamePad) {
        val previous = state.frameState

        if (currentFrame > 10) {
            this.state.previousLocation = previous.link.point
            this.state.previousHeart = previous.life
            this.state.previousDamageNumber = previous.damageNumber
            this.state.previousNumBombs = previous.numBombs
        }

        state.lastGamePad = currentGamePad
        //        https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:RAM_map

        val linkX = api.readCPU(Addresses.linkX)
        val linkY = api.readCPU(Addresses.linkY) - yAdjustFactor
        val linkPoint = FramePoint(linkX, linkY)
//        val calculatedDir = previous.link.point.directionToDir(linkPoint)

        // works
        // turns into 1 is candle used
//        val candleUsed = api.readCPU(Addresses.Ram.candleUsed)
        val mapLoc = api.readCPU(Addresses.ZeroPage.mapLoc)
        val level = api.readCPU(Addresses.level)

        state.currentMapCell = if (level == MapConstants.overworld) {
            hyrule.getMapCell(mapLoc)
        } else {
            state.hyrule.levelMap.cellOrEmpty(level, mapLoc)
        }

        val oam = OamStateReasoner(api)
        val theEnemies = oam.agents()
        val linkDir = oam.direction
        val ladderMem = api.readCPU(Addresses.ladderDeployed) != 0
        // check ladder memory first
        d { "ladder mem $ladderMem ${api.readCPU(Addresses.ladderDeployed)}" }
        val ladder = if (ladderMem) oam.ladderSprite else null
        val damagedTile = if (oam.damaged) LinkDirection.damagedAttribute.last() else 0
        val link = Agent(0, linkPoint, linkDir, tile = damagedTile)
        // has to persist between states
        if (ladder != null) {
            d { "ladder was ${state.ladderStateHorizontal} prev ${state.previousMove.dir.horizontal}" }
        }
        state.ladderStateHorizontal = when {
            ladder == null -> null
            state.ladderStateHorizontal == null ->
                state.previousMove.dir.horizontal

            else -> state.ladderStateHorizontal
        }
        // and if it is horizontal, then make above and below not passable
        if (ladder != null) {
            d { " ladder at ${ladder.point} directionHorizontal: ${state.ladderStateHorizontal}" }
            d { " prev ${state.previousMove.dir.horizontal}" }
        }

        val previousNow = state.previousMove
        state.previousMove = PreviousMove(
            previous = state.previousMove.copy(previous = null), // TODO: previousNow.copy()),
            from = state.previousLocation,
            to = state.previousLocation.adjustBy(state.lastGamePad),
            actual = linkPoint,
            move = state.lastGamePad,
            triedToMove = state.lastGamePad.isMoveAction()
        )
        // reset to prevent infinite memory being allocated
        previousNow.previous = null

        val frame = FrameState(api, theEnemies, level, mapLoc, link, ladder)

        state.framesOnScreen++
        state.frameState = frame
    }
}

private val Boolean.intTrue: Int
    get() = if (this) 1 else 0