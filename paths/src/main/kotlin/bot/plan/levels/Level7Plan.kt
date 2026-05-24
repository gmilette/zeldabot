package bot.plan.levels

import bot.plan.InLocations
import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.plan.action.makeCenterPush
import bot.plan.action.makePush
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.map.destination.ZeldaItem
import bot.state.map.grid
import bot.state.map.level.LevelSpecBuilder
import bot.state.map.level.LevelStartMapLoc

object Level7Plan {
    private object Loc {
        val pushRight = FramePoint(12.grid, 5.grid)
        val triforceHeart = FramePoint(8.grid, 5.grid)
    }
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
            leftm // dont attack dogo
            up
            useBait()
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
            digdoggerWhistle(GamePad.MoveRight)
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
            killLev1Dragon // aim for the head
            goTo(Loc.triforceHeart)
            rightonlym
            getTri
        }

    private fun PlanBuilder.useBait() {
        add {
            seg("bait spot")
            goIn(GamePad.MoveUp, 20)
            switchToBait()
            goIn(GamePad.None, 100)
            goIn(GamePad.MoveUp, 20) // move more in
            goTo(FramePoint(8.grid, 7.grid))
            useItem()
        }
    }
}