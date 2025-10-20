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

fun PlanBuilder.levelPlan9PhaseRedRing() {
    this.add {
        lev(9)
        startAt(LevelStartMapLoc.lev(9))
        // really dont want to accidently exit because link can't find way back to the entrance
        goInConsume(GamePad.MoveUp, 30)
        seg("get red ring", ZeldaItem.RedRing)
        cheatBombs // for now
        upm
        leftm
        bombUp
        switchToWand
        wait(100)
        kill
        switchToBoomerang
        addNext(
            20,
            makeCenterPush(
                LevelSpecBuilder.Companion.Nine.travel1,
                makeUp(20),
                out = InLocations.OutLocation.outLeft
            )
        )

        "spiral".seg()
        right // kill the pancakes, getting quite stuck
        rightm
        "go down to ring".seg()
        downm
        bombRight
        bombUp
        bombUp
        "ring spot".seg()
//        switchToWand // it doesnt wait and gets to pushing
        kill
        "ring spot push".seg()
        +makeCenterPush(LevelSpecBuilder.getItemLoc8, makeUp(lastMapLoc))
    }
}

fun PlanBuilder.levelPlan9PhaseSilverArrow() {
    add {
        seg("get silver arrow", ZeldaItem.SilverArrow)
        down // kill pancake
        cheatBombs // for now
        downm
        leftm
        upm
        "go in next room".seg()
        // avoid battling the circle monster
        goIn(GamePad.MoveUp, 8.grid, monitor = false)
        upm
        bombLeft
        GoIn(10, GamePad.MoveLeft)
        "kill travel 1".seg()
        kill
        // trigger trap first
        addNext(
            99, makePush(
                InLocations.Push.moveLeftOfTwo,
                makeUp(99),
                startAt = LevelSpecBuilder.Companion.Nine.travel2,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.corner
            )
        )
        "travel to arrow".seg()
        kill // kill the squishies looking for hearts
        left
        "past bats".seg()
        leftm  //bats
        "circle monster kill".seg()
        killCenterMonster
        "to in stair".seg()
        addNext(
            32, makeCenterPush(
                LevelSpecBuilder.Companion.Nine.travel3, makeUp(32),
                out = InLocations.OutLocation.outLeft
            )
        )
        bombUp
        "acquire arrow".seg()
//        switchToWand
        // don't need this i think
//        wait(100)
        kill
        "set the arrow".seg()
        // dont need it I think
//        goTo(FramePoint(13.grid, 5.grid), ignoreProjectiles = true) // other side
        +makeStairsItemPush(
            startAt = LevelSpecBuilder.Companion.Nine.silverArrow,
            makeUp(lastMapLoc)
        )

    }
}

fun PlanBuilder.levelPlan9PhaseGannon() {
    add {
        seg("return to center")
        objectivePrincess()
        downIgnoreProjectiles // there are only sun monsters
        kill
        "take stair back".seg()
        addNext(
            97, makeCenterPush(
                88, makeUp(97),
                out = InLocations.OutLocation.outRight
            )
        )
        // maybe it would be ok to use boomerang here
        upm
        "past first pancake".seg()
        upm
        "past second pancake".seg()
        upm
        "bomb left ok".seg()
        bombLeft
        kill
        "push to inbetween travel".seg()
        addNext(
            4, makePush(
                InLocations.Push.moveLeftOfTwo,
                makeUp(4),
                startAt = LevelSpecBuilder.Companion.Nine.travel4,
                out = InLocations.OutLocation.outRight,
                stairs = InLocations.StairsLocation.corner
            )
        )

        "get to final stair".seg() // save7
        bombLeft
        // not really needed but it would be nice
//        switchToBoomerang
        kill
        addNext(
            82, makeCenterPush(
                119, makeUp(82),
                out = InLocations.OutLocation.outLeft
            )
        )
        "doorstep of gannon".seg()
//        startHereAtLoaded()
        goInConsume(GamePad.MoveLeft, 10)
        enoughForArchery
        killCenterMonster
        killCenterMonster
        uptk
        "seg kill gannon".seg()
        goIn(GamePad.None, 20)
        switchToArrow()
        // cheap attack maneuver doesnt work well
        // fireball interrupts it
        goTo(InLocations.Level9.center)
//        goTo(InLocations.Level9.centerGannonAttackLeft)
//        goIn(GamePad.MoveUp, 2)
//        goIn(GamePad.A, 2)
//        goIn(GamePad.None, 2)
//        goIn(GamePad.A, 2)
        killG
        lootInside
        uponlym
        "seg get princess".seg()
        rescuePrincess()
        peaceReturnsToHyrule()
        // display some summary stats before ending
//            booya
        end
    }
}

val level9: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(9))
        lev(9)
        levelPlan9PhaseRedRing()
        levelPlan9PhaseSilverArrow()
        levelPlan9PhaseGannon()
        end
    }
