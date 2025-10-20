package bot.plan.levels

import bot.plan.InLocations
import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.plan.action.*
import bot.state.*
import bot.state.map.*
import bot.state.map.destination.ZeldaItem
import bot.state.map.level.LevelSpecBuilder
import bot.state.map.level.LevelStartMapLoc

val level8: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(8))
        lev(8)
        startAt(LevelStartMapLoc.lev(8))
        seg("run past", ZeldaItem.BookOfMagic)
        goIn(GamePad.MoveUp, 10)
        switchToBomb
        left
        seg("bomb guy")
        killFirstAttackBomb
        // get the coin
        goTo(FramePoint(8.grid, 5.grid))
        left
        seg("go get book")
        kill
        +makeCenterPush(LevelSpecBuilder.getItemLoc8, makeUp(124))
        seg("get back to start", ZeldaItem.MagicKey)
        rightm
        rightm // at start
        seg("get to crossroads")
        up
        "bomb".seg()
        bombUpNoBlock
        kill
        goTo(InLocations.Level8.keySpot)
        upm
        upm // master battle
        // clear out a little before moving on
        killUntil(4)
        bombUp
//        cheatAddKey
        upNoBlock
        killArrowSpider // kill arrow guy
        loot // spider tends to generate loot sometimes
        rightm
        seg("get key")
        killAllInCenter
        +makeCenterPush(LevelSpecBuilder.getItemLoc8Key, makeUp(31))
        seg("get back to master battle", ZeldaItem.Triforce)
        left
        down
        down
        kill // master battle
        seg("take stair to end")
        right // master battle
        killUntil(4) // kill some, otherwise link just tries to run through them, which is not good
        // 31, 46, 62
        addNext(
            76, goNoPush(
                makeUp(76),
                startAt = LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.rightStairGrid
            )
        )

        switchToArrowConditionally()
        killWithBombsUntil4
        // give time to enter so that switching to bomb works
        goTo(FramePoint(11.grid, 2.grid))
        "bomb to get dragon".seg()
        bombUp
        "kill dragon".seg()
        killLev4Dragon // dragon
        "grab tri".seg()
        goTo(InLocations.Level8.triforceHeartAbove)
        goTo(InLocations.Level8.triforceHeart)

        uponlym
        getTri
    }
