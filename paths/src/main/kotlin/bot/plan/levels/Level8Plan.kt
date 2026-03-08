package bot.plan.levels

import bot.plan.InLocations
import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.plan.action.goNoPush
import bot.plan.action.makeCenterPush
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.map.destination.ZeldaItem
import bot.state.map.grid
import bot.state.map.level.LevelSpecBuilder
import bot.state.map.level.LevelStartMapLoc

object Level8Plan {
    private object Loc {
        val triforceHeartAbove = FramePoint(3.grid, 6.grid)
        val triforceHeart = FramePoint(2.grid, 8.grid)
        val keySpot = FramePoint(8.grid, 5.grid)
    }
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
            goTo(Loc.keySpot)
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
            goTo(Loc.triforceHeartAbove)
            goTo(Loc.triforceHeart)

            uponlym
            getTri
        }
}