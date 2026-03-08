package bot.plan.levels

import bot.plan.InLocations
import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.map.MapConstants
import bot.state.map.destination.ZeldaItem
import bot.state.map.grid
import bot.state.map.level.LevelStartMapLoc

object Level3Plan {
    private object Level3Locations {
        val keyMid = FramePoint(128, 88)
        val keyElbowSquishy = FramePoint(8.grid, 5.grid)
        val heartMid = FramePoint(128, 88)

        // requires then moving up / needed?
        val triforce = FramePoint(7.grid, 6.grid) // get the middle of the triangle at the top
    }

    val level3: PlanBuilder.() -> Unit
        get() = {
            phase(Phases.lev(3))
            lev(3)
            startAt(LevelStartMapLoc.lev(3))
            seg("grab key", ZeldaItem.Key)
            left
            goTo(Level3Locations.keyMid)
            loot
            seg("walk round corner", ZeldaItem.Raft)
            up // grab key it's easy
            kill
            loot
            up
            leftm
            // get past trap
            goIn(GamePad.MoveLeft, 30)
            seg("past the compass")
            killUntil2
            level3TriggerDoorThen // it's not great but ok
            goIn(GamePad.MoveLeft, 10)
            seg("fight swords")
            kill // don't attack half
            down  // downk, i think it as causing no completing
            seg("get raft")
            goIn(GamePad.MoveDown, 10)
            switchToBomb
            // drop clearing bomb
            goIn(GamePad.B, 6)
            goTo(InLocations.rightStair)
            startAt(15)
            go(InLocations.getItem)
            upTo(105)
            seg("get to back to center", ZeldaItem.Triforce)
            upm
            rightNoP
            seg("right no p")
            goTo(FramePoint(12.grid, 5.grid))
            goIn(GamePad.MoveRight, MapConstants.twoGrid)
            rightNoP
            seg("get to boss")
            upm // option to get key up, but skip
            seg("Keys from squishy")
            kill
            goTo(Level3Locations.keyElbowSquishy)
            // walk past trap ??
            rightm
            goIn(GamePad.MoveRight, MapConstants.threeGrid)
            seg("BOMB RIGHT")
            switchToBomb
            level3BombThenRight
            seg("kill boss")
            starKill
            go(Level3Locations.heartMid)
            uponlym
            getTri
        }
}