package bot.state

import bot.plan.action.PreviousMove
import bot.state.map.Hyrule
import bot.state.map.MapConstants
import bot.state.map.stats.MapStatsTracker
import bot.state.oam.LinkDirectionFinder
import bot.state.oam.OamStateReasoner
import nintaco.api.API
import util.d

class FrameStateUpdater(
    private val api: API,
    private val hyrule: Hyrule
) {
    private val mapStats = MapStatsTracker()
    private val yAdjustFactor = MapConstants.yAdjust

    private fun getLinkX() = api.readCPU(Addresses.linkX)
    private fun getLinkY() = api.readCPU(Addresses.linkY)
    fun getLink() = FramePoint(getLinkX(), getLinkY())
    var state: MapLocationState = MapLocationState(hyrule = hyrule)

    fun reset() {
        d { "RESET State" }
        state = MapLocationState(hyrule)
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

        val oam = OamStateReasoner(api, mapStats)
        val theEnemies = oam.agents()
        val theUncombined = oam.agentsUncombined()
        val linkDir = oam.direction
        val ladderMem = api.readCPU(Addresses.ladderDeployed) != 0
        // check ladder memory first
        val ladderSprite = oam.ladderSprite?.let { "ladder sprite "} ?: "no sprite"
        d { "ladder mem $ladderMem ${api.readCPU(Addresses.ladderDeployed)} $ladderSprite" }
        val ladder = if (ladderMem) oam.ladderSprite else null
        val damagedTile = if (oam.damaged) LinkDirectionFinder.damagedAttribute.last() else 0
        val link = Agent(0, linkPoint, linkDir, tile = damagedTile)

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

        // TODO: analyze direction
        // agents must be the same type (projectile or not) maybe same tile

        val mapCoordinates = MapCoordinates(level, mapLoc)
        mapStats.track(mapCoordinates, theEnemies)
        val seenBoomerang = mapStats.seenBoomerang
        val frame = FrameState(api, theEnemies, theUncombined, level, mapLoc, link, ladder, seenBoomerang)

        state.framesOnScreen++
        state.frameState = frame
    }
}

val Boolean.intTrue: Int
    get() = if (this) 1 else 0