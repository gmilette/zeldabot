package bot.plan.levels

import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.map.MapConstants
import bot.state.map.andAHalf
import bot.state.map.destination.ZeldaItem
import bot.state.map.grid
import bot.state.map.level.LevelStartMapLoc

object Level2Plan {
    object Loc {
        val heartMid = FramePoint(128, 88) //boss heart
        val keyMid = FramePoint(128, 88)
        val keyMidDown = FramePoint(128, 81)
        val bombItemRight = FramePoint(208, 43)
        //        val triforce = FramePoint(120, 88) // get the middle of the triangle at the top
//        val triforce = FramePoint(128, 88) // get the middle of the triangle at the top
        val triforce = FramePoint(8.grid - MapConstants.halfGrid, 5.grid.andAHalf) // could add 1/4 here // get the middle of the triangle at the top
    }

    val level2: PlanBuilder.() -> Unit
        get() = {
            phase(Phases.lev(2))
            lev(2)
            startAt(LevelStartMapLoc.lev(2))
            seg("gather 3 keys", ZeldaItem.Key)
            right
            kill
            goTo(Loc.keyMid)
            loot // maybe try to get loot
            up // nothing here
            seg("gather key 2")
            left
            kill
            left
            goTo(Loc.keyMid)
            loot
            right
            right // grid room
            seg("sprint up from grid")
            up
//                seg("go get blue boomerang")
            upm
            seg("gather key 3", ZeldaItem.Key)
            kill
            loot // key 2
            seg("Get to Dodongo", ZeldaItem.Triforce)
            up
            // skip getting key from squishy guy
//            .kill
//            .goTo(Loc.keyMid)
            upNoBlock // the squishy guy appears like a projectile so do not block
            kill
            seg("bomb room")
            // no key I think
//            goTo(Loc.keyMid)
            up
            kill // blocked before going // allow bombs
            goTo(Loc.bombItemRight)
            up
            seg(Phases.Segment.lev2Boss)
            switchToBomb
            killLevel2Rhino
            seg("get the triforce")
            wait(200) // there might be a bomb appearing that I want
            goTo(Loc.heartMid)
            loot // in case there is a bomb
            leftonlym
            goIn(GamePad.MoveLeft, 20)
            getTri
        }

    val levelPlan2Boomerang: PlanBuilder.() -> Unit
        get() = {
            phase(Phases.reenterLevel2)
            lev(2)
            startAt(LevelStartMapLoc.lev(2))
            seg("go to boomerang", ZeldaItem.Boomerang)
            up
            right
            up
            up
            right
            seg("get boomerang")
            kill
            goAbout(Loc.keyMid, 1, 1, true, ignoreProjectiles = true)
            seg("depart", ZeldaItem.None)
            left
            kill // door is locked until all killed
            down
            down
            left
            seg("Go to exit")
            down
            seg("move out")
            goTo(FramePoint(7.grid + MapConstants.halfGrid, 8.grid))
            goInConsume(GamePad.MoveDown, MapConstants.threeGrid)
            // try inside nav instead
//            downTo(60)
//            goInConsume(GamePad.MoveDown,90)
            startAt(60)
        }

    val levelPlan2Harvest2: PlanBuilder.() -> Unit
        get() = {
//        phase(Phases.level2Harvest)
            lev(2)
            startAt(LevelStartMapLoc.lev(2))
            seg("Wander level 2", ZeldaItem.Rupee)
            right
            upk
            leftk
            leftk
            rightk
            rightk
            upk
            upk
            upk
            upk
            seg("escape", ZeldaItem.None)
            downk
            downk
            downk
            downk
            seg("depart")
            leftk
            seg("move out")
            goTo(FramePoint(7.grid + MapConstants.halfGrid, 8.grid))
            goInConsume(GamePad.MoveDown, MapConstants.threeGrid)
            startAt(60)
        }
}