package bot.state

import bot.*
import bot.plan.action.PreviousMove
import bot.state.map.Hyrule
import bot.state.map.MapConstants
import nintaco.api.API
import sequence.ZeldaItem
import util.d

class FrameStateUpdater(
    private val api: API,
    private val hyrule: Hyrule
) {
    fun getLinkX() = api.readCPU(Addresses.linkX)
    fun getLinkY() = api.readCPU(Addresses.linkY)
    fun getLink() = FramePoint(getLinkX(), getLinkY())
    var state: MapLocationState = MapLocationState(hyrule = hyrule)

    fun setLadderAndRaft(enable: Boolean) {
        api.writeCPU(Addresses.hasLadder, enable.intTrue)
        api.writeCPU(Addresses.hasRaft, enable.intTrue)
    }

    fun setBait() {
        d { " !! deactivate clock "}
        api.writeCPU(Addresses.hasFood, 1)
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
        val yAdjustFactor = 61
        val previous = state.frameState
        state.lastGamePad = currentGamePad
        //        https://datacrystal.romhacking.net/wiki/The_Legend_of_Zelda:RAM_map
        val linkX = api.readCPU(Addresses.linkX)
        val linkY = api.readCPU(Addresses.linkY) - yAdjustFactor
        val linkPoint = FramePoint(linkX, linkY)
        val linkDir = mapDir(Addresses.linkDir)
        val linkDir2 = Addresses.moveDir
        d { " Link direction $linkDir2"}
        val link = Agent(0, linkPoint, linkDir)
        val tenth = api.readCPU(Addresses.tenthEnemyCount)

        val killedEnemyCount = api.readCPU(Addresses.Ram.killedEnemyCount)
//        val screenOptions = api.readRAM(Addresses.screenOptions)
        // works
        // turns into 1 is candle used
        val candleUsed = api.readCPU(Addresses.Ram.candleUsed)

        val enemyData = mutableListOf<AgentData>()
        val enemies = mutableListOf<Agent>()
        for (i in Addresses.ememiesX.indices) {
            val x = api.readCPU(Addresses.ememiesX[i])
            val y = api.readCPU(Addresses.ememiesY[i]) - yAdjustFactor
            val pt = FramePoint(x, y)
            val dir = mapDir(api.readCPU(Addresses.ememyDir[i]))

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
//        val enemyDirs = Addresses.ememyDir.map { mapDir(api.readCPU(it)) }
//        val enemyDirs = emptyList<Dir>()
//        val droppedItems = Addresses.dropItemType.map { api.readCPU(it) }
//        val droppedEnemyItems = Addresses.dropEnemyItem.map { api.readCPU(it) }
//        val countdowns = Addresses.enemyCountdowns.map { api.readCPU(it) }
        val dungeonItem = api.readCPU(Addresses.dungeonFloorItem)
//        val dungeonTypeOfItem = api.readCPU(Addresses.dungeonTypeOfItem)
//        d {" INFO: $dungeonItem $dungeonTypeOfItem"}
//        for (droppedItem in droppedItems) {
//            d { " dropped $droppedItem"}
//        }
//        for (enemyItem in droppedEnemyItems) {
//            d { " droppedenemyItem $enemyItem"}
//        }
        // 142 no key [moves around]
        // 109 key dropped [I wonder if this is a location or something
        // 255 got it

        // dungeon item is 93 after key
        // turns to 255

        //15 big coin
        //24 coin
        //33 clock

        val swordUseCountdown = api.readCPU(Addresses.swordUseCountdown)

        val subY = api.readCPU(Addresses.ZeroPage.subY)
        val subX = api.readCPU(Addresses.ZeroPage.subX)
        val mapLoc = api.readCPU(Addresses.ZeroPage.mapLoc)
        val subPoint = FramePoint(subY, subX)

        val gameMode = api.readCPU(Addresses.gameMode)
        val level = api.readCPU(Addresses.level)

        state.currentMapCell = if (level == MapConstants.overworld) {
            hyrule.getMapCell(mapLoc)
        } else {
            val a = state.hyrule.levelMap.cellOrEmpty(level, mapLoc)
            d { "GGG cell is $a lev $level map $mapLoc" }
            state.hyrule.levelMap.cellOrEmpty(level, mapLoc)
        }

        state.enemyReasoner.add(enemyPoint)

        val previousNow = state.previousMove
        state.previousMove = PreviousMove(
            previous = state.previousMove.copy(previous = null), // TODO: previousNow.copy()),
            from = state.previousLocation,
            // assumes the
            to = state.previousLocation.adjustBy(state.lastGamePad),
            actual = link.point,
            move = state.lastGamePad,
            triedToMove = state.lastGamePad.isMoveAction())
        // reset to prevent infinite memory being allocated
        previousNow.previous = null

        val selectedItem = api.readCPU(Addresses.selectedItem)
        // inventory
        val numBombs = api.readCPU(Addresses.numBombs)
        val numRupees = api.readCPU(Addresses.numRupees)
        val numKeys = api.readCPU(Addresses.numKeys)
        val items = mutableSetOf<ZeldaItem>()
        when (api.readCPU(Addresses.hasSword)) {
            1 -> items.add(ZeldaItem.WoodenSword)
            2 -> items.add(ZeldaItem.WhiteSword)
            3 -> items.add(ZeldaItem.MagicSword)
            else -> {}
        }
        val candleStatus = api.readCPU(Addresses.hasCandle)
        when (candleStatus) {
            1 -> items.add(ZeldaItem.RedCandle)
            2 -> items.add(ZeldaItem.BlueCandle)
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
        val inventory = Inventory(selectedItem, numBombs, numKeys, numRupees, items)
        val hasDungeonItem = dungeonItem == 255

        // EXPERIMENTS
        // it works
        val clockActivated = api.readCpuB(Addresses.clockActivated)
        if (clockActivated) {
            d { "!!! clock activated !!!"}
        }
        // not sure
        val enemiesKilledAlt = api.readCPU(Addresses.enemiesKilledAlt)
        val enemiesKilledWithoutTakingDamage = api.readCPU(Addresses.enemiesKilledWithoutTakingDamage)
//        d { " !!! enemiesKilledAlt $enemiesKilledAlt enemiesKilledWithoutTakingDamage $enemiesKilledWithoutTakingDamage"}

        // END EXPERIMENTS

        //$08=North, $04=South, $01=East, $02=West
        //could try to remember how link last moved and that will be the
        // direction

//        state.enemyReasoner.log()
        //enemies
        val frame = FrameState(gameMode, reasonerEnemies, killedEnemyCount, link, subPoint,
            mapLoc, inventory, hasDungeonItem, tenth, level, clockActivated, swordUseCountdown)

        this.state.previousLocation = link.point

        state.framesOnScreen++
        state.frameState = frame
    }

    // $00=False, $01=True
    private fun API.readCpuB(address: Int): Boolean =
        api.readCPU(address) != 0

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