package bot.plan

import KillHandsInLevel7
import bot.plan.action.*
import bot.plan.runner.Experiment
import bot.plan.runner.MasterPlan
import bot.plan.runner.PlanSegment
import bot.state.*
import bot.state.map.*
import bot.state.map.destination.Dest
import bot.state.map.destination.DestType
import bot.state.map.destination.EntryType
import bot.state.map.destination.ZeldaItem
import bot.state.map.level.LevelMapCellsLookup
import bot.state.oam.*
import util.d

class PlanBuilder(
    private val mapData: MapCells,
    private val levelData: LevelMapCellsLookup,
    private val optimizer: OverworldRouter,
    private var phase: String = ""
) {
    private var objective: Objective = Objective.empty
    private var segment: String = ""
    private var plan = mutableListOf<Action>()
    var lastMapLoc = 0
        private set
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
            segments.add(PlanSegment(phase, segment, plan.toList(), objective))
            plan = mutableListOf()
        }
    }

    fun whiteSwordDodgeRoutine() {
        val moveUp = makeUp(lastMapLoc.down.up)
        val moveDown = makeDown(lastMapLoc.down)
        add(lastMapLoc, whiteSwordManeuverSneakManeuver(moveDown, moveUp))
    }

    fun  routeTo(loc: MapLoc): PlanBuilder {
        //d { " path from $lastMapLoc to $loc"}
        val path = optimizer.findPath(lastMapLoc, loc) ?: return this
        var previousCell: MapCell? = null
        for (mapCell in optimizer.correct(path.vertexList)) {
            if (mapCell.mapLoc != lastMapLoc) {
                previousCell?.let { currentCell ->
                    if (currentCell.mapData.attributes.contains(MapCellAttribute.HasBombEnemies)) {
                        if (currentCell.mapData.attributes.contains(MapCellAttribute.SlowForEnemiesToAppear)) {
                            add(currentCell.mapLoc, completeIfHaveFullBombs(completeIfHaveEnemies(Wait(200))))
                        } else {
                            // give level a chance to start
                            add(currentCell.mapLoc, completeIfHaveFullBombs(completeIfHaveEnemies(Wait(20))))
                        }
                        val killAction = completeIfHaveFullBombs(
                            lootAndKill(
                                KillAll(
                                    numberLeftToBeDead = 0,
                                    lookForBombs = true,
                                    needLongWait = false,
                                )
                            )
                        )
                        add(currentCell.mapLoc, killAction)
                    }
                }
                previousCell = mapCell
                val move = MoveTo(lastMapLoc, mapCell, level)
                add(mapCell.mapLoc, lootAndMove(move))
            }
        }
        return this
    }

    fun objective(item: ZeldaItem) {
        setObjective(Objective(type = DestType.Item(item)))
    }

    fun startAt(loc: MapLoc): PlanBuilder {
        // could add level too
        add(loc, StartAtAction())
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

    infix fun Int.using(levelActions: PlanBuilder.() -> Unit) {
        val level = this
        add {
            includeLevelObjPlan(level, levelActions)
            inOverworld
        }
    }

    private fun PlanBuilder.includeLevelObjPlan(level: Int, levelActions: PlanBuilder.() -> Unit, exitDirection: Direction = Direction.Down, consume: Boolean = true) {
        add {
//            phase("Destroy level $level")
            obj(Dest.level(level))
            makeSegment()
            remember
            levelActions()
//            segments.addAll(other.segments)
            // exit the level so dont accidently go back in
            // have to wait for the triforce animation
            // useless
//        goIn(GamePad.None, 500)
            if (consume && (level != 7 && level != 8)) {
                // i found link going back and forth
                goInConsumeCompleteIfMapChanges(
                    exitDirection.toGamePad(), 20)
            }
            recall
        }
    }

    fun includeLevelPlan(other: MasterPlan, exitDirection: Direction = Direction.Down, consume: Boolean = true): PlanBuilder {
        makeSegment()
        remember
        segments.addAll(other.segments)
        // exit the level so dont accidently go back in
        // have to wait for the triforce animation
        // useless
//        goIn(GamePad.None, 500)
        if (consume) {
            goInConsume(exitDirection.toGamePad(), 20)
        }
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

    operator fun Action.unaryPlus() {
        add(lastMapLoc, this)
    }

    fun addNext(nextLoc: MapLoc, action: Action) {
        add(nextLoc, action)
    }

    private fun setObjective(objective: Objective) {
        this.objective = objective
    }

    fun seg(name: String, item: ZeldaItem? = null): PlanBuilder {
        item?.let {
            objective(item)
        }
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
        add(nextLoc, MoveTo(lastMapLoc, mapCell(nextLoc), level, Direction.Up))
        return this
    }

    fun makeUp(nextLoc: MapLoc): MoveTo =
        MoveTo(lastMapLoc, mapCell(nextLoc), level, Direction.Up)

    fun makeDown(nextLoc: MapLoc): MoveTo =
        MoveTo(lastMapLoc, mapCell(nextLoc), level, Direction.Down)

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
            wait(20) // it looked like in level 6 link just skipped to moving
            add(lastMapLoc, killSpider())
            return this
        }
    val killAllInCenter: PlanBuilder
        get() {
            // need to redo this so that link moves back to the position
            add(lastMapLoc, lootAndKill(KillAll(considerEnemiesInCenter = true)))
            goTo(FramePoint(3.grid, 8.grid))
//            goTo(FramePoint(7.grid, 7.grid)) // second to bottom
            goTo(FramePoint(8.grid, 8.grid)) // bottom center
            // shoot with arrows?
            switchToArrow()
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
            add(lastMapLoc, killRhinoCollectSeeHeart)
//            add(lastMapLoc, KillRhino())
//            add(lastMapLoc, KillAll(useBombs = true, waitAfterAttack = true))
            return this
        }
    val switchToBomb: Unit
        get() {
            switchToBomb()
        }
    val switchToBoomerang: Unit
        get() {
            switchToBoomerang()
        }
    val switchToWand: Unit
        get() {
            switchToWand()
        }
    val killG: PlanBuilder
        get() {
//            add(lastMapLoc, gannonKill)
//            add(lastMapLoc, KillGannon())
            add(lastMapLoc, killGannonFromCorner())
            return this
        }
    fun killUntil(leftDead: Int): PlanBuilder {
        add(lastMapLoc, lootAndKill(KillAll(
            numberLeftToBeDead = leftDead
        )))
        return this
    }
    fun killUntilGetBomb(leftDead: Int): PlanBuilder {
        add(lastMapLoc, lootAndKill(completeIfHaveBombs(KillAll(
            numberLeftToBeDead = leftDead,
            lookForBombs = true
//            targetOnly = EnemyGroup.enemiesWhoMightHaveBombs
        ))))
        return this
    }
    val killUntilGetKey: PlanBuilder
        get() {
            add(lastMapLoc, CompleteIfGetItem(KillAll()))
            add(lastMapLoc, Wait(20)) // try to prevent leaving the key there
            add(lastMapLoc, GetLoot())
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
            add(lastMapLoc, lootAndKill(KillAll(needLongWait = false)))
            return this
        }
    val killCenterMonster: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false, ignoreUntilOnly = circleMonsterCenters))
            return this
        }
    val killUntilBombsLikely: PlanBuilder
        get() {
            add(lastMapLoc, killUntilBombsLikely())
            return this
        }
    val killUntilGetBomb: PlanBuilder
        get() {
            killUntilGetBomb(0)
            return this
        }
    val killLongWait: PlanBuilder
        get() {
            add(lastMapLoc, lootAndKill(KillAll(needLongWait = true)))
            return this
        }
    val killFirstAttackBomb: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false, firstAttackBomb = true))
            return this
        }
    val killWithBombs: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false, useBombs = true))
            return this
        }
    val starKill: PlanBuilder
        get() {
            add(
                lastMapLoc,
                lootAndKill(
                    KillAll(
                        needLongWait = false,
                        firstAttackBomb = true,
                        allowBlock = false,
                        // ignore just projectiles I think
//                        ignoreEnemies = true,
//                        ignoreProjectilesRoute = true
                        whatToAvoid = RouteTo.WhatToAvoid.All, // was enemies..
                    )
                )
            )
            return this
        }

    val killWithBombsUntil4: PlanBuilder
        get() {
            add(lastMapLoc, lootAndKill(KillAll(needLongWait = false, useBombs = true, numberLeftToBeDead = 4)))
            return this
        }

    val killUntil2: PlanBuilder
        get() {
            add(lastMapLoc, lootAndKill(KillAll(needLongWait = false, numberLeftToBeDead = 2)))
            return this
        }
    val killLev4Dragon: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false,
                whatToAvoid = RouteTo.WhatToAvoid.JustEnemies,
                targetOnly = listOf(dragon4Head, dragonHead, dragonHead2)))
            return this
        }
    val killLev1Dragon: PlanBuilder
        get() {
            add(lastMapLoc, KillAll(needLongWait = false,
                targetOnly = EnemyGroup.dragon1.toList(),
                //targetOnly = listOf(dragonHead), //, dragonNeckTile
                whatToAvoid = RouteTo.WhatToAvoid.JustEnemies))// .JustProjectiles))
            return this
        }
    val startHere: PlanBuilder
        get() {
            add(lastMapLoc, StartHereAction())
            return this
        }
    val startHereAt: PlanBuilder
        get() {
            add(lastMapLoc, StartHereByLocationAction(MapCoordinates(level, lastMapLoc)))
            return this
        }
    fun startHereAt(experiment: Experiment): PlanBuilder {
        add(lastMapLoc, StartHereByLocationAction(MapCoordinates(level, lastMapLoc), experiment))
        return this
    }
    val upm: PlanBuilder
        get() {
            // don't try to fight, but get loot if there
            addm(lastMapLoc.up)
            return this
        }
    val upNoLoot: Unit
        get() {
            val nextLoc = lastMapLoc.up
            add(nextLoc, moveTo(nextLoc))
        }
    val upNoBlock: PlanBuilder
        get() {
            // don't try to fight
            val nextLoc = lastMapLoc.up
            add(nextLoc, moveTo(nextLoc, false))
            return this
        }

    val down: PlanBuilder
        get() {
            add(lastMapLoc.down)
            return this
        }
    fun downTo(to: MapLoc): PlanBuilder {
        add(to, lootAndMove(moveTo(to)))
        return this
    }
    val cheatBombs: PlanBuilder
        get() {
            add(lastMapLoc, CheatBombs)
            return this
        }
    val enoughForRing: PlanBuilder
        get() {
            add(lastMapLoc, EnoughForRing)
            return this
        }
    val enoughForPotion: PlanBuilder
        get() {
            add(lastMapLoc, EnoughForPotion)
            return this
        }
    val haveLetter: PlanBuilder
        get() {
            add(lastMapLoc, HaveLetter)
            return this
        }
    val cheatAddKey: PlanBuilder
        get() {
            add(lastMapLoc, AddKey)
            return this
        }
    val enoughForArchery: PlanBuilder
        get() {
            add(lastMapLoc, EnoughForArchery)
            return this
        }
    val enoughForBait: PlanBuilder
        get() {
            add(lastMapLoc, EnoughForBait)
            return this
        }
    val enoughForArrow: PlanBuilder
        get() {
            add(lastMapLoc, EnoughForArrow)
            return this
        }
    val downm: PlanBuilder
        get() {
            addm(lastMapLoc.down)
            return this
        }
    val downIgnoreProjectiles: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.down
            add(nextLoc, moveTo(nextLoc, ignoreProjectiles = true))
            return this
        }
    val inLevel: PlanBuilder
        get() {
            lev(1)
            return this
        }
    fun lev(levelIn: Int): PlanBuilder {
        level = levelIn
        if (level == 9) {
            setObjective(Objective(type = DestType.Princess))
        } else {
            setObjective(Objective(type = DestType.Triforce(levelIn)))
        }
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
    val level6TriggerDoorThenUp: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.up
            add(nextLoc, level6TriggerDoorTrapThenDo(moveTo(nextLoc)))
            return this
        }
    val level3TriggerDoorThen: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.left
            add(nextLoc, level3TriggerDoorTrapThenDo(moveTo(nextLoc, ignoreProjectiles = true)))
            return this
        }
    val level3BombThenRight: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.right
            add(nextLoc, level3TriggerBombThenDo(moveTo(nextLoc)))
            return this
        }
    val leftm: PlanBuilder
        get() {
            addm(lastMapLoc.left)
            return this
        }
    val right: Unit
        get() {
            add(lastMapLoc.right)
        }
    val rightNoLoot: Unit
        get() {
            val nextLoc = lastMapLoc.right
            add(nextLoc, moveTo(nextLoc))
        }
    val rightIfNeedBombs: Unit
        get() {
            val nextLoc = lastMapLoc.right
            add(nextLoc, lootAndMove(completeIfHaveBombs(moveTo(nextLoc))))
        }
    val leftIfNeedBombs: Unit
        get() {
            val nextLoc = lastMapLoc.left
            add(nextLoc, lootAndMove(completeIfHaveBombs(moveTo(nextLoc))))
        }
    val rightk: Unit
        get() {
            addK(lastMapLoc.right)
        }
    val leftk: Unit
        get() {
            addK(lastMapLoc.left)
        }
    val downk: Unit
        get() {
            addK(lastMapLoc.down)
        }
    val upk: Unit
        get() {
            addK(lastMapLoc.up)
        }
    val uptk: Unit
        get() {
            addTk(lastMapLoc.up)
        }
    val getTri: Unit
        get() {
            add(lastMapLoc, getTriforce())
        }
    val rightonlym: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.right
            add(nextLoc, moveTo(nextLoc))
            return this
        }
    val leftonlym: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.left
            add(nextLoc, moveTo(nextLoc))
            return this
        }
    val uponlym: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.up
            add(nextLoc, moveTo(nextLoc))
            return this
        }
    val rightm: PlanBuilder
        get() {
            addm(lastMapLoc.right)
//            val nextLoc = lastMapLoc.right
//            add(nextLoc, moveTo(nextLoc))
            return this
        }
    val rightNoP: PlanBuilder
        get() {
            val nextLoc = lastMapLoc.right
            add(nextLoc, MoveTo(lastMapLoc, mapCell(nextLoc), toLevel = level, ignoreProjectiles = true))
            return this
        }

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

        // dynamic direction?

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

    fun obj(dest: DestType, itemLoc: Objective.ItemLoc? = null, position: Boolean =
        false, action: Boolean = true) {
//        seg("get ${dest.javaClass.simpleName} for ${dest.name} at ${dest.entry.javaClass.simpleName}")
        seg("get ${dest.name}")
        captureObjective(mapData.findObjective(dest), itemLoc, position, action)
    }

    private fun captureObjective(mapCell: MapCell,
                                 itemLocOverride: Objective.ItemLoc? = null,
                                 position: Boolean = false,
                                 action: Boolean = true) {
        val objective = mapCell.mapData.objective
        if (objective.type !is DestType.None) {
            setObjective(objective)
        }
        routeTo(mapCell.mapLoc)
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
                is EntryType.WalkUp -> {
                    goInConsume(GamePad.MoveUp, 20)
                    goIn(GamePad.MoveUp, 50)
                }
                // for levels you dont need to get an item
                // if item loc is n
                is EntryType.Walk -> {
                    // need special parameter for walk in
                    goInGetCenterItem(point, itemLocPoint, this.type.entry.requireLetter)
                }
                EntryType.WalkInLadder -> goToNotMonitored(point)
                EntryType.WalkIn -> goTo(point)
                is EntryType.Bomb -> if (action) bombThenGoIn(point, itemLocPoint, this.type.entry.requireLetter) else goInGetCenterItem(point, itemLocPoint)
                is EntryType.Fire -> burnFromGo(point, this.type.entry.from, itemLocPoint, this.type.entry.requireLetter)
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
    fun pushThenGoTo(toB: FramePoint, toT: FramePoint = InLocations.middleStair, ignoreProjectiles: Boolean = false): PlanBuilder {
//        pushThenGoToDynamic(toB, toT, ignoreProjectiles)
        add(lastMapLoc, InsideNavAbout(toB, 4, ignoreProjectiles = ignoreProjectiles))
        add(lastMapLoc, GoDirection(GamePad.MoveUp, 70))
        add(lastMapLoc, GoDirection(GamePad.MoveRight, 70))
        add(lastMapLoc, InsideNav(toT, ignoreProjectiles = ignoreProjectiles))
        return this
    }

    fun pushThenGoToDynamic(blockIn: FramePoint, toT: FramePoint = InLocations.middleStair, ignoreProjectiles: Boolean = false): PlanBuilder {
//        // just for compat ability
        val block = blockIn.upOneGrid
        val pushFromUp = Direction.Up.pointModifier(MapConstants.oneGrid)(block)
        val pushFromDown = Direction.Down.pointModifier(MapConstants.oneGrid)(block)
        add(lastMapLoc, InsideNavAbout(pushFromUp, 4, ignoreProjectiles = ignoreProjectiles, orPoints = listOf(pushFromDown)))
        // go there
        add(lastMapLoc, GoToward(block, 70))
        add(lastMapLoc, GoDirection(GamePad.MoveRight, 70))
        add(lastMapLoc, InsideNav(toT, ignoreProjectiles = ignoreProjectiles))
        add(lastMapLoc, GoDirection(GamePad.MoveRight, 4))
        add(lastMapLoc, GoDirection(GamePad.MoveLeft, 4))
        return this
    }

    fun pushJust(target: FramePoint) {
        // position
        goTo(FramePoint(3.grid, 8.grid))
        add(lastMapLoc, InsideNavAbout(target.justLeftDown, 4))
        add(lastMapLoc, GoDirection(GamePad.MoveUp, 70))
        add(lastMapLoc, GoDirection(GamePad.MoveRight, 20))
    }

    fun pushActionThenGoRight(push: InLocations.Push): PlanBuilder {
        val right = lastMapLoc.right
        add(right, makePushActionThen(push, right, moveTo(right)))
        return this
    }

    fun pushActionThenGoUp(push: InLocations.Push): PlanBuilder {
        val up = lastMapLoc.up
        add(up, makePushActionThen(push, up, moveTo(up)))
        return this
    }

//    fun pushActionThenGoCorner(toB: FramePoint): PlanBuilder {
//        add(lastMapLoc.right, makePushActionThen(toB, moveTo(lastMapLoc.right)))
//        return this
//    }

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

    fun makeGo(to: FramePoint) = InsideNav(to)

    private fun showLetterIfRequired(): PlanBuilder {
        add(lastMapLoc, ShowLetterConditionally())
        return this
    }

    fun goTo(to: FramePoint, makePassable: FramePoint? = null, ignoreProjectiles: Boolean = false, monitorEnabled: Boolean = true): PlanBuilder {
        // need to add this elsewhere probably
        add(lastMapLoc, StartAtAction(0, -1))
        goAbout(to, 2, 1, false, makePassable = makePassable, ignoreProjectiles = ignoreProjectiles, monitorEnabled = monitorEnabled)
        return this
    }

    fun goToNotMonitored(to: FramePoint): PlanBuilder {
        add(lastMapLoc, StartAtAction(0, -1))
        add(lastMapLoc, InsideNavAbout(to, 4, 2, 1, setMonitorEnabled = false))
        return this
    }

    private fun goToOrMapChanges(to: FramePoint, makePassable: FramePoint? = null): PlanBuilder {
        add(lastMapLoc, CompleteIfMapChanges(InsideNavAbout(to, 2, 1, negVertical = 0, makePassable = makePassable)))
        return this
    }

    private fun bombThenGoIn(entrance: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem, showLetter: Boolean = false): PlanBuilder {
        bomb(entrance.copy(y = entrance.y + 8))

        add(lastMapLoc, CompleteIfMapChanges(ReBombIfNecessary(InsideNav(entrance, makePassable = entrance))))

        goIn(GamePad.MoveUp, 5)

        goGetItem(itemLoc, letterRequired = showLetter)

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
        // it's possible the shop will be printing something and that
        // can take some time, either wait for it to be done
        goTo(to, makePassable = to, monitorEnabled = false)
        // move in the door
        goInConsume(GamePad.MoveUp, 5)
        if (showLetter) {
            showLetterIfRequired()
        }
        goGetItem(itemLoc, showLetter)
        return this
    }

    private fun goGetItem(itemLoc: FramePoint = InLocations.Overworld.centerItem, letterRequired: Boolean = false) {
        if (itemLoc != Objective.ItemLoc.None.point && itemLoc != Objective.ItemLoc.Enter.point) {
            // walking down stairs, plus a few steps in
            // too much for bait
            // triggers escape
            goInConsume(GamePad.MoveUp, 17)

            waitUntilCloudIsGone()
            // wait until all words are done being displayed...

            // avoid accidently picking the center item
            if (itemLoc != InLocations.Overworld.centerItem) {
                goShop(itemLoc.downTwoGrid)
            }
            if (letterRequired) {
                goPotionDecision()
            } else {
                goShop(itemLoc)
            }
            if (itemLoc != Objective.ItemLoc.Enter.point) {
                exitShop()
                // help exit after exiting
                goInConsume(GamePad.MoveDown, MapConstants.halfGrid)
            }
        }
    }

    private fun switchToItem(item: Int) {
        // prevent accidental go back
//        startAt(0)
        toggleMenu()
        plan.add(SwitchToItem { item } )
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

    private fun switchToBoomerang() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.boomerang))
    }

    private fun switchToWand() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.wand))
    }

    fun switchToArrowConditionally() {
        plan.add(SwitchToItemConditionally(Inventory.Selected.arrow))
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
    private fun burnFromGo(to: FramePoint, from: Direction, itemLoc: FramePoint = InLocations.Overworld.centerItem, requireLetter: Boolean = false): PlanBuilder {
        // switch to candle
        switchToCandle()
        val burnFrom = from.pointModifier(MapConstants.twoGrid)(to)
        val opposite = from.opposite()

        val directionExit: Direction = mapCell(lastMapLoc).exits.keys.firstNotNullOf {
            if (mapCell(lastMapLoc).exits[it]?.isNotEmpty() == true) it else null
        }
        val mod = directionExit.mapLocModifier()
        val exitLoc = mod(lastMapLoc)
        plan.add(makePositionBurn(burnFrom, opposite.toGamePad(), to, exitLoc))

//        goIn(opposite.toGamePad(), 16)
        goGetItem(itemLoc, requireLetter)
        return this
    }

    private fun makePositionBurn(burnFrom: FramePoint, direction: GamePad, to: FramePoint, exitLoc: MapLoc, retry: Boolean = true): Action =
        OrderedActionSequence(listOf(
            InsideNav(burnFrom, tag = "burn position"),
            GoIn(MapConstants.fourthGrid, direction, desiredDirection = direction.toDirection(), reset = true),
            GoIn(2, GamePad.B, reset = true),
            GoIn(75, GamePad.None, reset = true),
            CompleteIfMapChanges(WaitUntilFireIsGone()),
            if (retry) {
                TimeoutThen(
                    action = CompleteIfMapChanges(InsideNav(to, makePassable = to, ignoreProjectiles = false, tag = "go in")),
                    contingency = exitReturnAction(burnFrom, direction, to, exitLoc)
                )
            } else {
                CompleteIfMapChanges(
                    Timeout(
                        action = InsideNav(to, makePassable = to, ignoreProjectiles = false, tag = "go in"),
                        // brown 100 secret didn't work, link just walked away
                        frameLimit = 1000
                    )
                )
            },
        ), restartWhenDone = true, shouldComplete = true, tag = "burn") // fine if this restarts, it will end once user exits

    private fun exitReturnAction(burnFrom: FramePoint, direction: GamePad, to: FramePoint, exitLoc: MapLoc): Action =
        OrderedActionSequence(listOf(
            MoveTo(fromLoc = lastMapLoc, next = mapCell(exitLoc), toLevel = 0),
            MoveTo(fromLoc = exitLoc, next = mapCell(lastMapLoc), toLevel = 0),
            makePositionBurn(burnFrom, direction, to, exitLoc, false)
        ), restartWhenDone = false, shouldComplete = true, tag = "exit then return") // fine if this restarts, it will end once user exits

    private fun PlanBuilder.pushDownGetItem(to: FramePoint, itemLoc: FramePoint = InLocations.Overworld.centerItem, position: Boolean = false):
            PlanBuilder {
        if (itemLoc == Objective.ItemLoc.None.point) {
            +makeStatuePushGo(statue = to)
        } else {
            +makeStatuePush(statue = to, itemLoc = itemLoc)
        }
        return this
    }

    fun goAbout(to: FramePoint, horizontal: Int, vertical: Int = 1, negVertical: Boolean = false, ignoreProjectiles: Boolean = false,
                makePassable: FramePoint? = null, monitorEnabled: Boolean = true): PlanBuilder {
        add(lastMapLoc, InsideNavAbout(to, horizontal, vertical, negVertical = if (negVertical) 1 else 0,
            ignoreProjectiles = ignoreProjectiles, makePassable = makePassable, setMonitorEnabled = monitorEnabled))
        return this
    }

    private fun goShop(to: FramePoint): PlanBuilder {
        add(lastMapLoc, InsideNavAbout(to, 4, 2, 1,
            shop = true))
        return this
    }

    private fun goPotionDecision(): PlanBuilder {
        add(lastMapLoc, eitherPotion())
        return this
    }

    private fun exitShop(): PlanBuilder {
        add(lastMapLoc, ExitShop())
        add(lastMapLoc, GoIn(10, GamePad.MoveDown))
        return this
    }

    fun goIn(dir: GamePad = GamePad.MoveUp, num: Int, monitor: Boolean = true): PlanBuilder {
        add(lastMapLoc, GoIn(num, dir, setMonitorEnabled = monitor))
        return this
    }

    /**
     * end when achieved desired direction
     */
    fun goInTowards(dir: GamePad = GamePad.MoveUp, num: Int): PlanBuilder {
        add(lastMapLoc, GoIn(num, dir, desiredDirection = dir.toDirection()))
        return this
    }

    fun usePotionIfNeed() {
        add(lastMapLoc, UsePotion())
    }

    fun goInConsume(dir: GamePad = GamePad.MoveUp, num: Int): PlanBuilder {
        add(lastMapLoc, GoInConsume(num, dir))
        return this
    }

    private fun goInConsumeCompleteIfMapChanges(dir: GamePad = GamePad.MoveUp, num: Int): PlanBuilder {
        add(lastMapLoc, CompleteIfMapChanges((GoInConsume(num, dir))))
        return this
    }

    private fun waitUntilCloudIsGone(): PlanBuilder {
        add(lastMapLoc, WaitUntilCloudIsGone())
        return this
    }

    fun goToAtPoint(loc: MapLoc, point: FramePoint) {
        routeTo(loc)
        goTo(point)
    }

    fun rescuePrincess() {
        goIn(GamePad.MoveUp, 4)
        goIn(GamePad.A, 4)
        goIn(GamePad.MoveLeft, 4)
        goIn(GamePad.MoveUp, 1)
        goIn(GamePad.A, 4)
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
        // ordered sequence, bomb and movement
        add(lastMapLoc, Bomb(target))

        return this
    }

    val bombUpNoBlock: PlanBuilder
        get() {
            doBomb(lastMapLoc.up, InLocations.topMiddleBombSpot, allowBlocking = false)
            return this
        }
    val bombUp: PlanBuilder
        get() {
            doBomb(lastMapLoc.up, InLocations.topMiddleBombSpot)
            return this
        }
    val bombLeft: PlanBuilder
        get() {
            doBomb(lastMapLoc.left, InLocations.bombLeft)
            return this
        }
    val bombRightExactly: PlanBuilder
        get() {
            doBomb(lastMapLoc.right, InLocations.bombRightExactly)
            return this
        }

    val bombRight: PlanBuilder
        get() {
            doBomb(lastMapLoc.right, InLocations.bombRight)
            return this
        }

    private fun doBomb(nextLoc: MapLoc, bombLoc: FramePoint, allowBlocking: Boolean = true): PlanBuilder {
        switchToBomb()
        // wait a little longer
        goIn(GamePad.None, 10)
        add(nextLoc, BombThenMove(bombLoc = bombLoc, moveTo = moveTo(nextLoc, allowBlocking = allowBlocking)))

        return this
    }



    private fun build(): MasterPlan {
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
        add(nextLoc, escapeCaveAndLootAndMove(moveTo(nextLoc)))
    }

    private fun addm(nextLoc: MapLoc) {
        add(nextLoc, lootAndMove(moveTo(nextLoc)))
    }

    // kill and loot and move?
    private fun addK(nextLoc: MapLoc) {
        add(nextLoc, killThenLootThenMove(moveTo(nextLoc)))
    }

    private fun addTk(nextLoc: MapLoc) {
        add(nextLoc, killThenMove(moveTo(nextLoc)))
    }

    private fun moveTo(next: Int, allowBlocking: Boolean = true, ignoreProjectiles: Boolean = false): MoveTo =
        MoveTo(lastMapLoc, mapCell(next), level, ignoreProjectiles = ignoreProjectiles, allowBlocking = allowBlocking)

    private fun add(loc: MapLoc, action: Action): PlanBuilder {
        lastMapLoc = loc
        plan.add(action)
        return this
    }
}