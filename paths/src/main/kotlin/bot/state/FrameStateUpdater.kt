package bot.state

import bot.plan.action.PreviousMove
import bot.plan.action.ProjectileDirectionCalculator
import bot.state.map.*
import bot.state.map.stats.MapStatsTracker
import bot.state.oam.LinkDirectionFinder
import bot.state.oam.OamStateReasoner
import nintaco.api.API
import org.apache.commons.math3.analysis.function.Add
import util.d
import kotlin.math.abs

class FrameStateUpdater(
    private val api: API,
    private val hyrule: Hyrule
) {
    private val mapStats = MapStatsTracker()
    private val yAdjustFactor = MapConstants.yAdjust

//    private fun getLinkX() = api.readCPU(Addresses.linkX)
//    private fun getLinkY() = api.readCPU(Addresses.linkY)
//    fun getLink() = FramePoint(getLinkX(), getLinkY())
    var state: MapLocationState = MapLocationState(hyrule = hyrule)

    fun reset() {
        d { "RESET State" }
        state = MapLocationState(hyrule)
    }

    fun updateFrame(currentFrame: Int, currentGamePad: GamePad, forcedLinkPoint: FramePoint? = null) {
        val previous = state.frameState

        // read the hearts by pixel
//        val screen = IntArray(256*256)
//        api.getPixels(screen)
        // plot it?

        if (currentFrame > 10) {
            this.state.previousLocation = previous.link.point
            this.state.previousHeart = previous.life
            this.state.previousDamageNumber = previous.damageNumber
            this.state.previousNumBombs = previous.numBombs
            this.state.previousNumKeys = previous.numKeys
            this.state.previousNumRupees = previous.numRupees
        }

        state.lastGamePad = currentGamePad
        //        https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:RAM_map

        val linkX = api.readCPU(Addresses.linkX)
        val linkY = api.readCPU(Addresses.linkY) - yAdjustFactor
        val linkPt = FramePoint(linkX, linkY)
        val linkPoint =
            forcedLinkPoint ?: FramePoint(linkX, linkY)
        d { " linkPoint: $linkPt $forcedLinkPoint"}

        // works
        // turns into 1 is candle used
//        val candleUsed = api.readCPU(Addresses.Ram.candleUsed)
        val mapLoc = api.readCPU(Addresses.ZeroPage.mapLoc)
        val level = api.readCPU(Addresses.level)

        val isOverworld = level == MapConstants.overworld
        state.currentMapCell = if (isOverworld) {
            hyrule.getMapCell(mapLoc)
        } else {
            state.hyrule.levelMap.cellOrEmpty(level, mapLoc)
        }

        // combining sprites messes up rhino analysis because when the rhino goes right,
        // everything gets combined to the back
        val isRhino = mapLoc == 14 && level == 2
        val isSpiderLevel8 = mapLoc == 30 && level == 8
        val isSpiderLevel6 = mapLoc == 28 && level == 6
        val isGannon = mapLoc == 66 && level == 9
        val combine = !isRhino && !isSpiderLevel8 && !isSpiderLevel6 && !isGannon
        d { "combine is $combine" }
        val oam = OamStateReasoner(isOverworld, api, mapStats, combine = combine, level, isGannon = isGannon)
        val dirLookup = DirectionByMemoryLookup(api)
        val theEnemies = oam.agents(dirLookup)

        val theUncombined = oam.agentsUncombined()
        val theRaw = oam.agentsRaw(dirLookup)
        val ladderMem = api.readCPU(Addresses.ladderDeployed) != 0
        // check ladder memory first
//        val ladderSprite = oam.ladderSprite?.let { "ladder sprite "} ?: "no sprite"
//        d { "ladder mem $ladderMem ${api.readCPU(Addresses.ladderDeployed)} $ladderSprite" }
        val ladder = if (ladderMem) oam.ladderSprite else null

        val linkDir2 = oam.direction
//        val linkTile = LinkDirectionFinder.damagedAttribute.last()
//        val damagedTile = if (oam.damaged) LinkDirectionFinder.damagedAttribute.last() else 0
        // lags behind one frame
        val linkDir = dirLookup.readLinkPointDir()
        // never changes
//        d { " link projectile sword >>>> ${api.readCPU(Addresses.linkSwordProjectile)}"}
        val link = Agent(0, linkPoint, linkDir, tile = 0)

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
        state.lastPoints.add(linkPoint)

        val seenBoomerang = mapStats.seenBoomerang
        val willSkip = SkipDetector.willSkip(api)

        d { " num enemies ${theEnemies.size}"}
        for (enemy in theEnemies) {
            d { "update enemy: $enemy" }
        }

        val frame = FrameState(api, currentFrame, theEnemies, theUncombined, theRaw, level, mapLoc, link, ladder, seenBoomerang, Inventory(api))

        if (!frame.isScrolling) {
            // don't track if the screen is scrolling
            val mapCoordinates = MapCoordinates(level, mapLoc)
            mapStats.track(mapCoordinates, theEnemies, frame)
        }
        state.framesOnScreen++
        state.frameState = frame

        // game mode 8, is the dead screen
        // 11 is cave
        d { " GAME MODE ${state.frameState.gameMode}"}
//        if (state.frameState.gameMode == 5) {
//            try {
////                percentBlack()
//            } catch (e: Exception) {
//
//            }
//        }
    }

    fun updateDecision(gamePad: GamePad) {
        if (!state.frameState.isScrolling) {
            // don't track if the screen is scrolling
            val mapCoordinates = MapCoordinates(state.frameState.level, state.frameState.mapLoc)
            val subPixel = api.readCPU(Addresses.subPixel)
            val subTile = api.readCPU(Addresses.subTile)
            val linkDir = api.readCPU(Addresses.linkDir)
            val skip = SkipDetector.getSkip(this.api)
            mapStats.trackDecision(state.link, gamePad, skip)
        }
    }


    val allPixels by lazy { IntArray(256 * 240) }

    fun isShowingSelectInventory(): Boolean {
        api.getPixels(allPixels)
        return allPixels.count { it == 0 } == 40
    }

    fun percentBlack() {
        api.getPixels(allPixels)
        val numBlack = allPixels.count { it == 0 }
        d { " num black $numBlack"}
        if (numBlack == 40) {
            // on the start screen
        }
//        allPixels.forEachIndexed { index, i ->
//            if (i < (256 * 10)) {
//                print("${i} ")
//                if (i % 256 == 0) {
//                    println()
//                }
//            }
//        }
    }
}

val Boolean.intTrue: Int
    get() = if (this) 1 else 0