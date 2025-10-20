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

val level7: PlanBuilder.() -> Unit
    get() = {
        // all boomerang
        phase(Phases.lev(7))
        lev(7)
        startAt(LevelStartMapLoc.lev(7))
        seg("Enter", ZeldaItem.RedCandle)
        upm
        bombUp
        switchToBoomerang
        upm
        seg("past water")
        kill //2
        upk
        leftm
        up
        seg("bait spot")
        goIn(GamePad.MoveUp, 20)
        switchToBait()
        goIn(GamePad.None, 100)
        goIn(GamePad.MoveUp, 20) // move more in
        goTo(FramePoint(8.grid, 7.grid))
        useItem()
        switchToBoomerang
        upm
        rightm
        bombRight
        seg("red candle")
        // can't kill the guy inside
        // so skip
        killAllInCenter
        +makeCenterPush(88, makeUp(26))
        seg("move right", ZeldaItem.Triforce)
        bombRight
        rightm
        seg("kill whistle")
        goIn(GamePad.MoveRight, 20) // more in a bit before whistlin'
        switchToWhistle()
        goIn(GamePad.None, 50)
        goIn(GamePad.MoveRight, 20) // move more in
        useItem()
        wait(300)
        kill
        seg("move on")
        upk
        cheatBombs
        bombRight
        seg("Kill hands")
//        switchToWand
        goIn(GamePad.MoveRight, 10)
        killHandsInLevel7
        goTo(FramePoint(2.grid, 8.grid))
        addNext(
            41, makePush(
                InLocations.Push.right,
                makeUp(41),
                LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.corner
            )
        )
        seg("near dragon")
        // just wait until switching
        goIn(GamePad.None, 100)
        seg("dragon")
        bombRight
        startHereAtLoaded()
        killLev1Dragon // aim for the head
        goTo(InLocations.Level7.triforceHeart)
        rightonlym
        getTri
    }
