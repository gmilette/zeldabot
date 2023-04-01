package bot.plan

import KillHandsInLevel7
import bot.GamePad
import bot.plan.action.*
import bot.plan.runner.MasterPlan
import bot.plan.runner.PlanSegment
import bot.state.*
import bot.state.map.*
import bot.state.map.level.LevelMapCellsLookup
import sequence.AnalysisPlanBuilder
import sequence.DestType
import sequence.EntryType
import sequence.ZeldaItem
import util.d

class LocationSequenceBuilder(
    private val mapData: MapCells,
    private val levelData: LevelMapCellsLookup,
    private val optimizer: AnalysisPlanBuilder.MasterPlanOptimizer,
    private var phase: String = ""
) {
    private var segment: String = ""
    private var plan = mutableListOf<Action>()
    private var lastMapLoc = 0
    private val builder = this
    private val OVERWORLD_LEVEL = 0
    private var level: Int = OVERWORLD_LEVEL
    private var segments = mutableListOf<PlanSegment>()

    private var rememberLoc: MapLoc = 0

    operator fun invoke(block: LocationSequenceBuilder.() -> Unit): MasterPlan {
        this.block()
        return this.build()
    }

    private fun makeSegment() {
        if (plan.isNotEmpty()) {
            segments.add(PlanSegment(phase, segment, plan.toList()))
            plan = mutableListOf()
        }
    }

    fun routeTo(loc: MapLoc, name: String = "", opp: Boolean = false): LocationSequenceBuilder {
        d { " path from $lastMapLoc to $loc"}
        val path = optimizer.findPath(lastMapLoc, loc) ?: return this
        var last = lastMapLoc
        for (mapCell in optimizer.correct(path.vertexList)) {
            if (mapCell.mapLoc != lastMapLoc) {  //  && mapCell.mapLoc != loc
                if (opp) {
                    add(mapCell.mapLoc, opportunityKillOrMove(mapCell))
                } else {
                    add(mapCell.mapLoc, MoveTo(mapCell))
                }
                d { " routeTo $name $last to ${mapCell.mapLoc}"}
//                    add(mapCell.mapLoc, opportunityKillOrMove(mapCell))
                last = mapCell.mapLoc
            }
        }
        return this
    }

    fun startAt(loc: MapLoc): LocationSequenceBuilder {
        lastMapLoc = loc
        return this
    }

    fun include(other: MasterPlan): LocationSequenceBuilder {
        makeSegment()
        segments.addAll(other.segments)
        return this
    }

    fun add(other: LocationSequenceBuilder.() -> Unit) {
        other()
    }

    fun includeLevelPlan(other: MasterPlan): LocationSequenceBuilder {
        makeSegment()
        remember
        segments.addAll(other.segments)
        recall
        return this
    }

    fun phase(name: String): LocationSequenceBuilder {
        makeSegment()
        phase = name
        return this
    }

    fun String.seg() {
        seg(this)
    }

    fun seg(name: String): LocationSequenceBuilder {
        makeSegment()
        segment = name
        return this
    }
    val remember: LocationSequenceBuilder
        get() {
            rememberLoc = lastMapLoc
            return this
        }
    val recall: LocationSequenceBuilder
        get() {
            lastMapLoc = rememberLoc
            rememberLoc = 0
            return this
        }
    val end: LocationSequenceBuilder
        get() {
            makeSegment()
            plan.add(EndAction())
            return this
        }
    val up: LocationSequenceBuilder
        get() {
            add(lastMapLoc.up)
            return this
        }
    val upp: LocationSequenceBuilder
        get() {
            addp(lastMapLoc.up)
            return this
        }
    fun upTo(loc: MapLoc): LocationSequenceBuilder {
        val nextLoc = loc
        add(nextLoc, Unstick(MoveTo(mapCell(nextLoc), Direction.Up)))
        return this
    }
    val loot: LocationSequenceBuilder
        get() {
            add(lastMapLoc, GetLoot())
            return this
        }
    val lootInside: LocationSequenceBuilder
        get() {
            add(lastMapLoc, GetLoot(true))
            return this
        }
    val pickupDeadItem: LocationSequenceBuilder
        get() {
            add(lastMapLoc, PickupDroppedDungeonItemAndKill())
            return this
        }
    val wait: LocationSequenceBuilder
        get() {
            add(lastMapLoc, Wait(2500))
            return this
        }
    fun wait(time: Int): LocationSequenceBuilder {
        add(lastMapLoc, Wait(time))
        return this
    }
    val killHandsInLevel7: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillHandsInLevel7())
            return this
        }
    val killArrowSpider: LocationSequenceBuilder
        get() {
            switchToArrow()
            add(lastMapLoc, KillArrowSpider())
            return this
        }
    val killAllInCenter: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillAll(considerEnemiesInCenter = true))
            goTo(FramePoint(3.grid, 8.grid))
//            goTo(FramePoint(7.grid, 7.grid)) // second to bottom
            goTo(FramePoint(8.grid, 8.grid)) // bottom center
            add(lastMapLoc, KillInCenter())
            return this
        }
    val killb: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillAll(useBombs = true, waitAfterAttack = false))
            return this
        }
    val killR: LocationSequenceBuilder
        get() {
            switchToBomb()
            add(lastMapLoc, KillAll(useBombs = true, waitAfterAttack = true))
            return this
        }
    val killG: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillGannon())
            return this
        }
    fun killUntil(leftDead: Int): LocationSequenceBuilder {
        add(lastMapLoc, KillAll(
            numberLeftToBeDead = leftDead
        ))
        return this
    }
    val killp: LocationSequenceBuilder
        get() {
            add(lastMapLoc, moveHistoryAttackAction(KillAll()))
            return this
        }
    val killAndLoot: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillAll())
            add(lastMapLoc, GetLoot())
            return this
        }
    val kill: LocationSequenceBuilder
        get() {
            add(lastMapLoc, moveHistoryAttackAction(KillAll()))
            return this
        }
//    fun kill(maxEnemies: Int): LocationSequenceBuilder {
//            add(lastMapLoc, KillAll())
//            return this
//        }
    val kill2: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillAll(numberLeftToBeDead = 2))
            return this
        }
    val kill1: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillAll(numberLeftToBeDead = 1))
            return this
        }
    val kill3: LocationSequenceBuilder
        get() {
            add(lastMapLoc, KillAll(numberLeftToBeDead = 3))
            return this
        }
    val startHere: Unit
        get() {
            add(lastMapLoc, StartHereAction())
        }
    val upLootThenMove: LocationSequenceBuilder
        get() {
            val nextLoc = lastMapLoc.up
            add(nextLoc, lootThenMove(mapCell(nextLoc)))
            return this
        }
    val upk: LocationSequenceBuilder
        get() {
            addKill(lastMapLoc.up)
            return this
        }
    val upm: LocationSequenceBuilder
        get() {
            // don't try to fight
            val nextLoc = lastMapLoc.up
            add(nextLoc, Unstick(MoveTo(mapCell(nextLoc))))
            return this
        }
    val upmp: LocationSequenceBuilder
        get() {
            // don't try to fight
            val nextLoc = lastMapLoc.up
            add(nextLoc, moveHistoryAttackAction(MoveTo(mapCell(nextLoc))))
            return this
        }
    val down: LocationSequenceBuilder
        get() {
            add(lastMapLoc.down)
            return this
        }
    val downp: LocationSequenceBuilder
        get() {
            val nextLoc = lastMapLoc.down
            add(nextLoc, moveHistoryAttackAction(MoveTo(mapCell(nextLoc))))
            return this
        }
    val downm: LocationSequenceBuilder
        get() {
            val nextLoc = lastMapLoc.down
            add(nextLoc, Unstick(MoveTo(mapCell(nextLoc))))
            return this
        }
    val inLevel: LocationSequenceBuilder
        get() {
            level = 1
            return this
        }
    fun lev(levelIn: Int): LocationSequenceBuilder {
        level = levelIn
        return this
    }
    val inOverworld: LocationSequenceBuilder
        get() {
            level = OVERWORLD_LEVEL
            return this
        }
    val left: LocationSequenceBuilder
        get() {
            add(lastMapLoc.left)
            return this
        }
    val leftLoot: LocationSequenceBuilder
        get() {
            val nextLoc = lastMapLoc.left
            add(nextLoc, GetLoot())
            return this
        }
    val leftm: LocationSequenceBuilder
        get() {
            val nextLoc = lastMapLoc.left
            add(nextLoc, Unstick(MoveTo(mapCell(nextLoc))))
            return this
        }
    val leftmp: LocationSequenceBuilder
        get() {
            val nextLoc = lastMapLoc.left
            add(nextLoc, moveHistoryAttackAction(MoveTo(mapCell(nextLoc))))
            return this
        }
    val rightKillForKey: LocationSequenceBuilder
        get() {
            val nextLoc = lastMapLoc.right
            // todo: can stop after get a key
            add(nextLoc, killThenLootThenMove(mapCell(nextLoc)))
            return this
        }
    val right: LocationSequenceBuilder
        get() {
            add(lastMapLoc.right)
            return this
        }
    val rightp: LocationSequenceBuilder
        get() {
            addp(lastMapLoc.right)
            return this
        }
    val rightk: LocationSequenceBuilder
        get() {
            lootKill()
            add(lastMapLoc.right)
            return this
        }
    val rightm: LocationSequenceBuilder
        get() {
            // don't try to fight
            val nextLoc = lastMapLoc.right
            add(nextLoc, Unstick(MoveTo(mapCell(nextLoc))))
            return this
        }

    // context
    fun pushInLevelMiddleStair(inMapLoc: MapLoc = 88, upTo: MapLoc = lastMapLoc, outLocation: FramePoint = InLocations.getItem) {
//        seg("push and go in upTo $upTo loc $outLocation")
        goTo(FramePoint(3.grid, 8.grid))
        pushThenGoTo(InLocations.diamondLeftBottomPush, InLocations.middleStair)
        startAt(inMapLoc)
        go(outLocation) //TODO no have to exit
//        add(lastMapLoc, GoDirection(GamePad.MoveUp, 100))
        upTo(upTo)
    }

    fun pushInLevelAnyBlock(inMapLoc: MapLoc = 88,
                            // target to push
                            pushTarget: FramePoint? = null,
                            // where the stairs are
                            stairsTarget: FramePoint,
                            // where to go back to
                            upTo: MapLoc = lastMapLoc,
                            // where to go inside
                            outLocation: FramePoint = InLocations.getItem,
                            directionFrom: Direction = Direction.Down,
                            position: FramePoint = FramePoint(3.grid, 8.grid), // for down
                            thenGo: GamePad = GamePad.None) {
        // position
        goTo(position)

        pushTarget?.let { pushTarget ->
            when (directionFrom) {
                Direction.Down -> {
                    // push from below
                    val belowPushTarget = pushTarget.downEnd
                    goTo(belowPushTarget)
                    // push
                    add(lastMapLoc, GoDirection(GamePad.MoveUp, 70))
                    add(lastMapLoc, GoDirection(GamePad.MoveDown, 10))
                    add(lastMapLoc, GoDirection(GamePad.MoveRight, 20))
                }

                Direction.Left -> {
                    val pushFrom = directionFrom.pointModifier(MapConstants.twoGrid)(pushTarget)
                    goTo(pushFrom)
                    // push
                    add(lastMapLoc, GoDirection(GamePad.MoveRight, 70))
                    add(lastMapLoc, GoDirection(GamePad.MoveLeft, 50))
                    add(lastMapLoc, GoDirection(GamePad.MoveUp, 20))
                }

                else -> {
                    throw IllegalArgumentException("can't do it")
                }
            }
        }
        // go to stairs
        add(lastMapLoc, InsideNav(stairsTarget))
        if (thenGo != GamePad.None) {
            goIn(thenGo, 15)
        }

        startAt(inMapLoc)
        go(outLocation)
        upTo(upTo)
    }

    fun obj(item: ZeldaItem, itemLoc: Objective.ItemLoc? = null) {
        // set the segment
        seg("get ${item.name}")
        captureObjective(mapData.findObjective(item), itemLoc)
    }

    fun goToAtPoint(loc: MapLoc, point: FramePoint) {
        routeTo(loc)
        goTo(point)
    }

    fun obj(dest: DestType, opp: Boolean = false, itemLoc: Objective.ItemLoc? = null, position: Boolean =
        false) {
        seg("get ${dest.javaClass.simpleName} for ${dest.name} at ${dest.entry.javaClass.simpleName}")
        captureObjective(mapData.findObjective(dest), itemLoc, opp, position)
    }

    private fun captureObjective(mapCell: MapCell, itemLocOverride: Objective.ItemLoc? = null, opp: Boolean = false, position: Boolean =
        false) {
        // depending on the entrance type, do different actions
        routeTo(mapCell.mapLoc, opp = opp)
        with (mapCell.mapData.objective) {
            // if entry
            val itemLocPoint =
                when {
                    this.type is DestType.Level -> Objective.ItemLoc.Enter
                    itemLocOverride != null -> itemLocOverride
                    else -> itemLoc
                }.point
//                itemLocOverride?.point ?: itemLoc.point
            when (this.type.entry) {
                // for levels you dont need to get an item
                // if item loc is n
                EntryType.Walk -> goInGetCenterItem(point, itemLocPoint)
                EntryType.WalkIn -> goTo(point)
                EntryType.Bomb -> bombThenGoIn(point, itemLocPoint)
                is EntryType.Fire -> burnFromGo(point, this.type.entry.from, itemLocPoint)
                is EntryType.Push -> pushDownGetItem(point, itemLocPoint)
                is EntryType.Statue -> pushDownGetItem(point, itemLocPoint, position)
                is EntryType.WhistleWalk -> whistleThenGo(point, itemLocPoint)
                // other types not supported yet
                else -> {}
            }
        }
    }

    /**
     * for when the push block is part of a middle stair
     *   *
     *  * *
     *   *
     */
    fun pushThenGoTo(toB: FramePoint, toT: FramePoint = InLocations.middleStair): LocationSequenceBuilder {
        add(lastMapLoc, InsideNavAbout(toB, 4))
        add(lastMapLoc, GoDirection(GamePad.MoveUp, 70))
        add(lastMapLoc, GoDirection(GamePad.MoveRight, 70))
        add(lastMapLoc, InsideNav(toT))
        return this
    }

    fun pushJust(target: FramePoint) {
        // position
        goTo(FramePoint(3.grid, 8.grid))
        add(lastMapLoc, InsideNavAbout(target.justLeftDown, 4))
        add(lastMapLoc, GoDirection(GamePad.MoveUp, 70))
        add(lastMapLoc, GoDirection(GamePad.MoveRight, 20))
    }

    fun pushWait(toB: FramePoint): LocationSequenceBuilder {
        // if it fails need to retry
        add(lastMapLoc, InsideNavAbout(toB, 4))
        add(lastMapLoc, GoDirection(GamePad.MoveDown, 100))
        add(lastMapLoc, Wait(300))
        return this
    }
    fun go(to: FramePoint): LocationSequenceBuilder {
        add(lastMapLoc, InsideNav(to))
        return this
    }

    fun goTo(to: FramePoint): LocationSequenceBuilder {
        goAbout(to, 4, 2, true)
        return this
    }

    fun bombThenGoIn(entrance: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem): LocationSequenceBuilder {
        bomb(entrance.copy(y = entrance.y + 8))
        go(entrance)
        goIn(GamePad.MoveUp, 5)

        goGetItem(itemLoc)

        return this
    }

    fun whistleThenGo(entrance: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem): LocationSequenceBuilder {
        goIn(GamePad.MoveUp, 20)
        useWhistle()
        // wait until whistle sounds
        // no need to wait long
        goIn(GamePad.None, 100)
        go(entrance)
        goIn(GamePad.MoveUp, 5)
        goGetItem(itemLoc)
        return this
    }

    fun useWhistle() {
        switchToWhistle()
        goIn(GamePad.None, 100)
        plan.add(UseItem())
    }

    fun testSwitch() {
        goIn(GamePad.Start, 25)
        goIn(GamePad.None, 100)
        plan.add(SwitchToItem(Inventory.Selected.bomb))
        goIn(GamePad.None, 10)
        goIn(GamePad.Start, 25)
        goIn(GamePad.None, 10)
    }

    fun goInGetCenterItem(to: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem): LocationSequenceBuilder {
        goTo(to)
        goIn(GamePad.MoveUp, 5)
        goGetItem(itemLoc)
        return this
    }

    private fun goGetItem(itemLoc: FramePoint = InLocations.Overworld.centerItem) {
        if (itemLoc != Objective.ItemLoc.None.point) {
            goShop(itemLoc)
            if (itemLoc != Objective.ItemLoc.Enter.point) {
                exitShop()
                goIn(GamePad.MoveDown, 5)
            }
        }
    }

    fun switchToItem(item: Int) {
        // todo: skip this if the current inventory has it
        toggleMenu()
        plan.add(SwitchToItem(item))
        goIn(GamePad.None, 100)
        toggleMenu()
    }

    private fun toggleMenu() {
        goIn(GamePad.Start, 2) // enough to get the UI to react
        goIn(GamePad.None, 30) // then more waiting
    }

    fun switchToCandle() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.candle))
    }

    fun switchToBomb() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.bomb))
//        switchToItem(Inventory.Selected.bomb)
        // extra wait??
//        goIn(GamePad.None, 15)
    }

    fun switchToArrow() {
        switchToItem(Inventory.Selected.arrow)
    }

    fun switchToWhistle() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.whistle))
    }

    fun switchToBait() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.bait))
    }

    fun useItem() {
        plan.add(UseItem())
    }

    //
    // L ... X
    // burn from left
    // turn opposite
    fun burnFromGo(to: FramePoint, from: Direction, itemLoc: FramePoint = InLocations.Overworld.centerItem): LocationSequenceBuilder {
        // switch to candle
        switchToCandle()
        val burnFrom = from.pointModifier(MapConstants.twoGrid)(to)
        val opposite = from.opposite()
//        switchToCandle()
        goTo(burnFrom)
        // turn in proper direction
        d { " burn from $burnFrom to $to op $opposite"}
        goIn(opposite.toGamePad(), 4)
//        val faceBurn = opposite.pointModifier(4)(burnFrom)
//        goTo(faceBurn)

        // execute burn
        plan.add(UseItem())
        // do not walk into the fire
        // todo, evade instead
        goIn(GamePad.None, 75)
//        val nextTo = from.pointModifier(6)(to.down2)
        // modified the map for this
        goTo(to)
        // maybe just keep trying to get to a location in center of the push
        goIn(opposite.toGamePad(), 16)
        goGetItem(itemLoc)
        return this
    }

    fun pushDownGetItem(to: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem, position: Boolean = false):
            LocationSequenceBuilder {
        if (position) {
            goTo(to.upOneGrid.justRightEnd)
        }
        val pushSpot = to.upOneGrid
        // doesn't work, need to use push spot
        goTo(pushSpot)
        // maybe just keep trying to get to a location in center of the push
        // needed to make this much lower for the pushing of magic sword
        goIn(GamePad.MoveDown, 20)
        // wait for it to move
        goIn(GamePad.None, 75)

        goTo(to)
        // add 16 to it and that's there you need to go I think
        // but you don't need to nav here unless you didn't go in the other way
//        goTo(to.justLeftBottom)
        goGetItem(itemLoc)
        return this
    }

    fun goAbout(to: FramePoint, horizontal: Int, vertical: Int = 1, negVertical: Boolean = false): LocationSequenceBuilder {
        add(lastMapLoc, InsideNavAbout(to, horizontal, vertical, if (negVertical) 1 else 0))
        return this
    }

    fun goShop(to: FramePoint): LocationSequenceBuilder {
        add(lastMapLoc, InsideNavAbout(to, 4, 2, 1,
            shop = true))
        return this
    }

    fun exitShop(): LocationSequenceBuilder {
        add(lastMapLoc, ExitShop())
        return this
    }

    fun lootKill(): LocationSequenceBuilder {
        add(lastMapLoc, lootAndKill)
        return this
    }

    fun goIn(dir: GamePad = GamePad.MoveUp, num: Int): LocationSequenceBuilder {
        add(lastMapLoc, GoIn(num, dir))
        return this
    }

    fun getSecret(): LocationSequenceBuilder {
        val secretMoneyLocation: FramePoint = FramePoint(100, 100)
        add(lastMapLoc, InsideNav(secretMoneyLocation))
        return this
    }

    fun rescuePrincess() {
        goIn(GamePad.MoveUp, 15)
        goIn(GamePad.A, 10)
        goIn(GamePad.None, 10)
        goIn(GamePad.MoveUp, 150)
    }

    fun peaceReturnsToHyrule() {
        goIn(GamePad.None, 1200)
    }

    fun bomb(target: FramePoint, switch: Boolean = true): LocationSequenceBuilder {
        // TODO: only do this conditionally somehow
        if (switch) {
            switchToBomb()
        }
        // wait a little longer
        goIn(GamePad.None, 10)
        add(lastMapLoc, Unstick(Bomb(target)))
        return this
    }

    fun build(): MasterPlan {
        makeSegment()
        return MasterPlan(segments.toList())
    }

    private fun mapCell(nextLoc: MapLoc): MapCell =
        if (level == OVERWORLD_LEVEL) {
            mapData.cell(nextLoc)
        } else {
            levelData.cell(level, nextLoc)
        }

    private fun add(nextLoc: MapLoc) {
        add(nextLoc, opportunityKillOrMove(mapCell(nextLoc)))
    }

    private fun addp(nextLoc: MapLoc) {
        add(nextLoc, moveHistoryAttackAction(opportunityKillOrMove(mapCell(nextLoc))))
//        add(nextLoc, UnstickP(opportunityKillOrMove(mapCell(nextLoc)),
//            unstickAction = GamePad.A,
//            howLong = 1000))
    }

    private fun addKill(nextLoc: MapLoc) {
        add(nextLoc, killThenLootThenMove(mapCell(nextLoc)))
    }

    private fun add(loc: MapLoc, action: Action): LocationSequenceBuilder {
        lastMapLoc = loc
        plan.add(action)
        return this
    }
}