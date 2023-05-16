package bot.state

import bot.plan.action.PreviousMove
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.map.MapConstants
import nintaco.api.API
import sequence.ZeldaItem
import util.d

class FrameStateUpdater(
    private val api: API,
    private val hyrule: Hyrule
) {
    val yAdjustFactor = 61

    private fun getLinkX() = api.readCPU(Addresses.linkX)
    private fun getLinkY() = api.readCPU(Addresses.linkY)
    fun getLink() = FramePoint(getLinkX(), getLinkY())
    var state: MapLocationState = MapLocationState(hyrule = hyrule)

    fun fillTriforce() {
        api.writeCPU(Addresses.triforce, 255)
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

        this.state.previousLocation = previous.link.point

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

        val previousNow = state.previousMove
        state.previousMove = PreviousMove(
            previous = state.previousMove.copy(previous = null), // TODO: previousNow.copy()),
            from = state.previousLocation,
            to = state.previousLocation.adjustBy(state.lastGamePad),
            actual = linkPoint,
            move = state.lastGamePad,
            triedToMove = state.lastGamePad.isMoveAction())
        // reset to prevent infinite memory being allocated
        previousNow.previous = null

        val oam = OamStateReasoner(api)
        val theEnemies = oam.agents()
        val linkDir = oam.direction
        val ladder = oam.ladderSprite
        val link = Agent(0, linkPoint, linkDir)
        if (ladder != null) {
            d { " ladder at ${ladder.point}"}
        }
        d { " link directon $linkDir"}
        val frame = FrameState(api, theEnemies, level, mapLoc, link, ladder)

        state.framesOnScreen++
        state.frameState = frame
    }

    private fun getEnemyInfoForReasoner() {
        val enemyData = mutableListOf<AgentData>()
        val enemies = mutableListOf<Agent>()
        for (i in Addresses.ememiesX.indices) {
            val x = api.readCPU(Addresses.ememiesX[i])
            val y = api.readCPU(Addresses.ememiesY[i]) - yAdjustFactor
            val pt = FramePoint(x, y)
            val dir = Direction.None

            val xStatus = api.readCPU(Addresses.enemyStatus[i])
            // we know if the xHP is 128 then the enemy must be dead
            // other values it could still be dead, or not
            val xHp = api.readCPU(Addresses.enemyHp[i])
            val xVel = api.readCPU(Addresses.velocity[i])
            // check out all these indicators of aliveness, and if you see it change
            // the guy is still alive, as soon as it doesn't change at all
            // it's dead
            val xAnimation = api.readCPU(Addresses.enemyAnimationOnOff[i])
            val xPresence = api.readCPU(Addresses.enemyPresence[i])

            // can get what sprites are active
            // and this will be an easy way to tell if there
            // are any enemies alive, if I can filter out things
            // that are not enemies
//            api.readOAM()

            // type 15: i think it's the clock
            //  dropped item type 24 i think it's a bomb
            val droppedId = api.readCPU(Addresses.dropItemType[i])
            val projectileState = ProjectileMapper.map(i, droppedId)
            val droppedEnemyItem = api.readCPU(Addresses.dropEnemyItem[i])
            val dropped = droppedItemMap(droppedId)
            // if this changes, it is an indication that the enemy was alive
            val countDown = api.readCPU(Addresses.enemyCountdowns[i])
            if (droppedId != 0) {
                d { " $i: dropped item type $droppedId $dropped" }
                // && droppedId != 24
            }
            // enemyData
            // enemyState

            // what is the state of this enemy:
            // loot
            // projectile
            // alive
            // killed
            // neverAlive

            val isLoot = droppedId != 2 && droppedId > 0
            val enemyStateCalc = state.enemyReasoner.enemyState(i)
            val wasAlive = state.enemyReasoner.wasAlive(i)
            // doesn't always reset to 0
//            val enemyState = if (xStatus == 0 && wasAlive(i)) EnemyState.Alive else EnemyState.Dead // a number of different dead states
            val enemy = Agent(i, pt, dir, enemyStateCalc, countDown, xHp, projectileState, droppedId)
            enemies.add(enemy)

            val data = AgentData(
                point = pt,
                dir = dir,
                countDown = countDown,
                status = xStatus,
                hp = xHp,
                velocity = xVel,
                animation = xAnimation,
                presence = xPresence,
                droppedId = droppedId,
                droppedEnemyItem = droppedEnemyItem,
                droppedItem = dropped,
                projectileState = projectileState
            )

//            d { "!! enemy $i: $data"}
            enemyData.add(data)
        }
        state.enemyReasoner.addData(enemyData)
        // since not using reasoner dont not do it
        val reasonerEnemies = mutableListOf<Agent>()
        for (i in Addresses.ememiesX.indices) {
            val stateCalc = state.enemyReasoner.makeState(i)
            //d { "!! enemy $i: $stateCalc"}
            val latest = state.enemyReasoner.latest(i)
            val enemy = Agent(i, latest.point, latest.dir,
                stateCalc, latest.countDown,
                latest.hp, latest.projectileState, droppedId = latest.droppedId)
            reasonerEnemies.add(enemy)
        }

        val enemyX = Addresses.ememiesX.map { api.readCPU(it) }
        val enemyY = Addresses.ememiesY.map { api.readCPU(it) - yAdjustFactor}
        val enemyPoint = enemyX.zip(enemyY).map { FramePoint(it.first, it.second) }
        state.enemyReasoner.add(enemyPoint)

        //        state.enemyReasoner.log()
        //enemies
//        val theEnemies = reasonerEnemies

//        val enemiesKilledAlt = api.readCPU(Addresses.enemiesKilledAlt)
//        val enemiesKilledWithoutTakingDamage = api.readCPU(Addresses.enemiesKilledWithoutTakingDamage)
//        d { " !!! enemiesKilledAlt $enemiesKilledAlt enemiesKilledWithoutTakingDamage $enemiesKilledWithoutTakingDamage"}
    }
}


fun droppedItemMap(dropped: Int): DroppedItem =
    DroppedItem.values().find { it.num == dropped } ?: DroppedItem.Unknown

// type 15: i think it's the clock
//  dropped item type 24 i think it's a bomb
enum class DroppedItem(val num: Int) {
//    Unknown(-1),
    Unknown(0),
    Bait(4),

//    Clock(24),
    BlueRupee(15),
    Rupee(24),

    Key(25),
    Compass(22),
    Map(23),

    Clock(33),
    Heart(34), //0x22, yep
    Fairy(35),
    Boomerang(29),

    // seen
    Missle1(128),
    // possibly, but it is also a bat
//    Missle2(16),
    Missle3(18),
    //https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:Notes
//    00=Bomb
//    01=Sword Wood
//    02=Sword Iron
//    03=Sword Master
//    04=Bait
//    05=Flute
//    06=Candle Blue
//    07=Candle Red
//    08=Glitch (looks like poop)
//    09=Nothing
//    0A=Bow
//    0B=Magic Key
//    0C=Raft
//    0D=Ladder
//    0E=Glitched
//    0F=Rupee Blue (x5)
//    10=Magic Rod
//    11=Magic Book
//    12=Ring Blue
//    13=Ring Red
//    14=Bracelet
//    15=Letter Blue
//    16=Compass
//    17=Letter Orange
//    18=Rupee Orange (x1)
//    19=Key
//    1A=Heart Container
//    1B=Triforce
//    1C=Big Shield
//    1D=Boomerang
//    1E=Boomerang Blue
//    1F=Potion Blue
//    20=Potion Red
//    21=Clock
//    22=Heart
//    23=Ferry
//    24=Glitched (Ferry)
//    28=Glitched (Remove bomb)
//    29=Glitched (Double bomb)
//    55=Arrow Normal??
//    FF=Glitched Items
}

val Boolean.intTrue: Int
    get() = if (this) 1 else 0