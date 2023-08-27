package bot.plan

import KillHandsInLevel7
import bot.state.GamePad
import bot.plan.action.*
import bot.plan.runner.MasterPlan
import bot.plan.runner.PlanSegment
import bot.state.*
import bot.state.map.*
import bot.state.map.level.LevelMapCellsLookup
import bot.state.map.level.LevelStartMapLoc
import sequence.AnalysisPlanBuilder
import sequence.DestType
import sequence.EntryType
import sequence.ZeldaItem
import util.d

class PlanBuilder(
    private val mapData: MapCells,
    private val levelData: LevelMapCellsLookup,
    private val optimizer: AnalysisPlanBuilder.MasterPlanOptimizer,
    private var phase: String = ""
) {
    private var segment: String = ""
    private var plan = mutableListOf<Action>()
    private var lastMapLoc = 0
    private var level: Int = OVERWORLD_LEVEL
    private var segments = mutableListOf<PlanSegment>()

    private var rememberLoc: MapLoc = 0

    companion object {
        private const val OVERWORLD_LEVEL = 0
    }

    operator fun invoke(block: PlanBuilder.() -> Unit): MasterPlan {
        this.block()
        return this.build()
    }

    private fun makeSegment() {
        if (plan.isNotEmpty()) {
            segments.add(PlanSegment(phase, segment, plan.toList()))
            plan = mutableListOf()
        }
    }

    fun routeTo(loc: MapLoc, name: String = "", opp: Boolean = false): PlanBuilder {
        //d { " path from $lastMapLoc to $loc"}
        val path = optimizer.findPath(lastMapLoc, loc) ?: return this
        for (mapCell in optimizer.correct(path.vertexList)) {
            if (mapCell.mapLoc != lastMapLoc) {
                if (opp) {
                    add(mapCell.mapLoc, opportunityKillOrMove(mapCell))
                } else {
                    add(mapCell.mapLoc, MoveTo(lastMapLoc, mapCell))
                }
//                d { " routeTo $name $last to ${mapCell.mapLoc}"}
//                    add(mapCell.mapLoc, opportunityKillOrMove(mapCell))
            }
        }
        return this
    }

    fun startAt(loc: MapLoc): PlanBuilder {
        lastMapLoc = loc
        return this
    }

    fun include(other: MasterPlan): PlanBuilder {
        makeSegment()
        segments.addAll(other.segments)
        return this
    }

    fun add(other: PlanBuilder.() -> Unit) {
        other()
    }

    fun includeLevelPlan(other: MasterPlan, exitDirection: Direction = Direction.Down): PlanBuilder {
        makeSegment()
        remember
        segments.addAll(other.segments)
        // exit the level so dont accidently go back in
        // have to wait for the triforce animation
        goIn(GamePad.None, 500)
        goIn(exitDirection.toGamePad(), 20)
        recall
        return this
    }

    fun phase(name: String): PlanBuilder {
        makeSegment()
        phase = name
        return this
    }

    fun String.seg() {
        seg(this)
    }

    fun seg(name: String): PlanBuilder {
        makeSegment()
        segment = name
        return this
    }
    private val remember: PlanBuilder
        get() {
            rememberLoc = lastMapLoc
            return this
        }
    private val recall: PlanBuilder
        get() {
            lastMapLoc = rememberLoc
            rememberLoc = 0
            return this
        }
    val end: PlanBuilder
        get() {
            makeSegment()
            plan.add(EndAction())
            return this
        }
    val up: PlanBuilder
        get() {
            add(lastMapLoc.up)
            return this
        }
    fun upTo(nextLoc: MapLoc): PlanBuilder {
        add(nextLoc, MoveTo(lastMapLoc, mapCell(nextLoc), Direction.Up))
        return this
    }
    val loot: PlanBuilder
        get() {
            add(lastMapLoc, GetLoot())
            return this
        }
    val lootInside: PlanBuilder
        get() {
            add(lastMapLoc, GetLoot(true))
            return this
        }
    val pickupDeadItem: PlanBuilder
        get() {
            add(lastMapLoc, PickupDroppedDungeonItemAndKill())
            return this
        }
    val wait: PlanBuilder
        get() {
            add(lastMapLoc, Wait(2500))
            return this
        }
    fun wait(time: Int): PlanBuilder {
        add(lastMapLoc, Wait(time))
        return this
    }
    val killHandsInLevel7: PlanBuilder
        get() {
            add(lastMapLoc, KillHandsInLevel7())
            return this
        }
    val killArrowSpider: PlanBuilder
        get() {
            switchToArrow()
            add(lastMapLoc, KillArrowSpider())
            return this
        }
    val killAllInCenter: PlanBuilder
        get() {
            // need to redo this so that link moves back to the position
            add(lastMapLoc, KillAll(considerEnemiesInCenter = true))
            goTo(FramePoint(3.grid, 8.grid))
//            goTo(FramePoint(7.grid, 7.grid)) // second to bottom
            goTo(FramePoint(8.grid, 8.grid)) // bottom center
            // shoot with arrows?
            add(lastMapLoc, KillInCenter())
            return this
        }
    val killb: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(useBombs = true, waitAfterAttack = false))
            return this
        }
    val killLevel2Rhino: PlanBuilder
        get() {
            add(lastMapLoc, KillRhino())
//            add(lastMapLoc, KillAll(useBombs = true, waitAfterAttack = true))
            return this
        }
    val switchToBomb: Unit
        get() {
            switchToBomb()
        }
    val killG: PlanBuilder
        get() {
            add(lastMapLoc, KillGannon())
            return this
        }
    fun killUntil(leftDead: Int): PlanBuilder {
        add(lastMapLoc, KillAll(
            numberLeftToBeDead = leftDead
        ))
        return this
    }
    val killAndLoot: PlanBuilder
        get() {
            add(lastMapLoc, KillAll())
            add(lastMapLoc, GetLoot())
            return this
        }
    val kill: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false))
            return this
        }
    val killUntil2: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false, numberLeftToBeDead = 2))
            return this
        }
    val killLev4Dragon: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false, targetOnly = listOf(dragon4Head)))
            return this
        }
    val killLev1Dragon: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false, targetOnly = listOf(dragonHead, dragonNeck)))
            return this
        }
    val startHere: PlanBuilder
        get() {
            add(lastMapLoc, StartHereAction())
            return this
        }
    val upm: PlanBuilder
        get() {
            // don't try to fight
            val nextLoc = lastMapLoc.up
            add(nextLoc, MoveTo(lastMapLoc, mapCell(nextLoc)))
            return this
        }
    val down: PlanBuilder
        get() {
            add(lastMapLoc.down)
            return this
        }
    val downm: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.down
            add(nextLoc, MoveTo(lastMapLoc, mapCell(nextLoc)))
            return this
        }
    val inLevel: PlanBuilder
        get() {
            level = 1
            return this
        }
    fun lev(levelIn: Int): PlanBuilder {
        level = levelIn
        return this
    }
    val inOverworld: PlanBuilder
        get() {
            level = OVERWORLD_LEVEL
            return this
        }
    val left: PlanBuilder
        get() {
            add(lastMapLoc.left)
            return this
        }
    val leftm: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.left
            add(nextLoc, MoveTo(lastMapLoc, mapCell(nextLoc)))
            return this
        }
    val right: Unit
        get() {
            add(lastMapLoc.right)
        }
    val rightm: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.right
            add(nextLoc, MoveTo(lastMapLoc, mapCell(nextLoc)))
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
        // go to stairs, maybe not always ignore projectiles
        add(lastMapLoc, InsideNav(stairsTarget, ignoreProjectiles = true))
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
                is EntryType.Walk -> {
                    // need special parameter for walk in
                    goInGetCenterItem(point, itemLocPoint, this.type.entry.requireLetter)
                }
                EntryType.WalkIn -> goTo(point)
                EntryType.Bomb -> bombThenGoIn(point, itemLocPoint)
                is EntryType.Fire -> burnFromGo(point, this.type.entry.from, itemLocPoint)
                is EntryType.Push -> pushDownGetItem(point, itemLocPoint)
                is EntryType.Statue -> pushDownGetItem(point, itemLocPoint, position)
                is EntryType.WhistleWalk -> whistleThenGo(point)
            }
        }
    }

    /**
     * for when the push block is part of a middle stair
     *   *
     *  * *
     *   *
     */
    fun pushThenGoTo(toB: FramePoint, toT: FramePoint = InLocations.middleStair): PlanBuilder {
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

    fun pushWait(toB: FramePoint): PlanBuilder {
        // if it fails need to retry
        // position
        add(lastMapLoc, InsideNavAbout(FramePoint(toB.x - MapConstants.twoGrid, toB.y - MapConstants.twoGrid), 4))
        add(lastMapLoc, InsideNavAbout(toB, 2))
        add(lastMapLoc, GoDirection(GamePad.MoveDown, 100))
        add(lastMapLoc, Wait(300))
        // escape it
        add(lastMapLoc, GoDirection(GamePad.MoveUp, 20))
        return this
    }
    fun go(to: FramePoint): PlanBuilder {
        add(lastMapLoc, InsideNav(to))
        return this
    }

    private fun showLetterIfRequired(): PlanBuilder {
        add(lastMapLoc, ShowLetterConditionally())
        return this
    }

    fun goTo(to: FramePoint, makePassable: FramePoint? = null): PlanBuilder {
        // was 4,2
        // made false so link can get into a stair
        goAbout(to, 2, 1, false, makePassable = makePassable)
        return this
    }

    fun goToOrMapChanges(to: FramePoint, makePassable: FramePoint? = null): PlanBuilder {
        add(lastMapLoc, CompleteIfMapChanges(InsideNavAbout(to, 2, 1, negVertical = 0, makePassable = makePassable)))
        return this
    }

    private fun bombThenGoIn(entrance: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem): PlanBuilder {
        bomb(entrance.copy(y = entrance.y + 8))
//        go(entrance)
        add(lastMapLoc, ReBombIfNecessary(InsideNav(entrance)))

        goIn(GamePad.MoveUp, 5)

        goGetItem(itemLoc)

        return this
    }

    // do whistle then move
    private fun whistleThenGo(entrance: FramePoint): PlanBuilder {
        goIn(GamePad.MoveUp, 20)
        useWhistle()
        // wait until whistle sounds
        // no need to wait long
        goIn(GamePad.None, 100)
        go(entrance)
        goIn(GamePad.MoveUp, 5)
        return this
    }

    private fun useWhistle() {
        switchToWhistle()
        goIn(GamePad.None, 100)
        plan.add(UseItem())
    }

    private fun goInGetCenterItem(to: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem, showLetter: Boolean = false): PlanBuilder {
        goTo(to)
        // move in the door
        goIn(GamePad.MoveUp, 5)
        if (showLetter) {
            d { " LETTER REQUIRED "}
            showLetterIfRequired()
        }
        goGetItem(itemLoc)
        return this
    }

    private fun goGetItem(itemLoc: FramePoint = InLocations.Overworld.centerItem) {
        if (itemLoc != Objective.ItemLoc.None.point) {
            // walking down stairs, plus a few steps in
            // too much for bait
            goInConsume(GamePad.MoveUp, 15)

            goShop(itemLoc)
            if (itemLoc != Objective.ItemLoc.Enter.point) {
                exitShop()
                goIn(GamePad.MoveDown, 5)
            }
        }
    }

    private fun switchToItem(item: Int) {
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

    private fun switchToCandle() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.candle))
    }

    private fun switchToBomb() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.bomb))
//        switchToItem(Inventory.Selected.bomb)
//        goIn(GamePad.None, 15)
    }

    fun switchToArrow() {
        switchToItem(Inventory.Selected.arrow)
    }

    fun switchToLetter() {
        switchToItem(Inventory.Selected.letter)
//        plan.add(SwitchToItemConditionally(Inventory.Selected.whistle))
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
    fun burnFromGo(to: FramePoint, from: Direction, itemLoc: FramePoint = InLocations.Overworld.centerItem): PlanBuilder {
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
//        goTo(to, to) // that should be passable now
        goToOrMapChanges(to, to) // that should be passable now
        // maybe just keep trying to get to a location in center of the push
        goIn(opposite.toGamePad(), 16)
        goGetItem(itemLoc)
        return this
    }

    private fun pushDownGetItem(to: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem, position: Boolean = false):
            PlanBuilder {
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

//        goTo(to)
        goToOrMapChanges(to)

        // do a few random movements in case link narrowly missed the entrance
        repeat(10) {
            goIn(GamePad.randomDirection(), 1)
        }

        // add 16 to it and that's there you need to go I think
        // but you don't need to nav here unless you didn't go in the other way
//        goTo(to.justLeftBottom)
        goGetItem(itemLoc)
        return this
    }

    fun goAbout(to: FramePoint, horizontal: Int, vertical: Int = 1, negVertical: Boolean = false, ignoreProjectiles: Boolean = false, makePassable: FramePoint? = null): PlanBuilder {
        add(lastMapLoc, InsideNavAbout(to, horizontal, vertical, negVertical = if (negVertical) 1 else 0,
            ignoreProjectiles = ignoreProjectiles, makePassable = makePassable))
        return this
    }

    private fun goShop(to: FramePoint): PlanBuilder {
        add(lastMapLoc, InsideNavAbout(to, 4, 2, 1,
            shop = true))
        return this
    }

    private fun exitShop(): PlanBuilder {
        add(lastMapLoc, ExitShop())
        add(lastMapLoc, GoIn(10, GamePad.MoveDown))
        return this
    }

    fun lootKill(): PlanBuilder {
        add(lastMapLoc, lootAndKill)
        return this
    }

    fun goIn(dir: GamePad = GamePad.MoveUp, num: Int): PlanBuilder {
        add(lastMapLoc, GoIn(num, dir))
        return this
    }

    fun goInConsume(dir: GamePad = GamePad.MoveUp, num: Int): PlanBuilder {
        add(lastMapLoc, GoInConsume(num, dir))
        return this
    }

    fun goToAtPoint(loc: MapLoc, point: FramePoint) {
        routeTo(loc)
        goTo(point)
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

    fun bomb(target: FramePoint, switch: Boolean = true): PlanBuilder {
        if (switch) {
            switchToBomb()
        }
        // wait a little longer
        goIn(GamePad.None, 10)
        add(lastMapLoc, Bomb(target))
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
//        add(nextLoc, opportunityKillOrMove(mapCell(nextLoc)))
        add(nextLoc, MoveTo(nextLoc, mapCell(nextLoc)))
    }

    private fun add(loc: MapLoc, action: Action): PlanBuilder {
        lastMapLoc = loc
        plan.add(action)
        return this
    }
}