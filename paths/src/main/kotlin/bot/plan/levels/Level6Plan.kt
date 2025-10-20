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

val level6: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(6))
        lev(6)
        startAt(LevelStartMapLoc.lev(6))
        objective(ZeldaItem.Wand)
        seg("move to level 6")
        left
        seg("first ghost")
        up // capture any loot dropped, there is usually some bombs
        switchToBoomerang
        seg("squishies")
        up // todo grab key in center
        seg("kill bats")
        // have to kill them to go up
        kill // can't there too many fireballs, just move on
        // get key after kill, not needed because kill gets loot
//            goTo(FramePoint(8.grid, 5.grid))
        up
        seg("kill and push to continue")
        goIn(GamePad.MoveUp, 6.grid, monitor = false) // custom action avoid traps, just walk straight
        upm
        switchToBoomerang
        killLongWait
        pushActionThenGoUp(InLocations.Push.moveLeftOfTwo)
        bombRightExactly
        seg("go up to get wand")
        switchToBoomerang // so i can get the pancakes
        // dont need this key
//            kill
//            loot // another key,  this is hard though because of ladder
        up
        up
        seg("get wand")
        kill
        +makePush(
            InLocations.Push.moveLeftOfTwo,
            makeUp(lastMapLoc),
            LevelSpecBuilder.getItemLoc6,
            stairs = InLocations.StairsLocation.corner
        )

        seg("go down to other stair", ZeldaItem.Triforce)
        down //25
        down //41
        // pick up key in center
        goTo(InLocations.Level6.keyCenter)
        down //57
        kill
        rightk
        seg("center move stair")
        kill
        addNext(
            29, makePush(
                InLocations.Push.singleLeft,
                makeUp(lastMapLoc),
                LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outRight,
                stairs = InLocations.StairsLocation.corner
            )
        )
        down
        left
        level6TriggerDoorThenUp
        killArrowSpider
        goTo(InLocations.Level6.triforceHeart)
        // need
        uponlym
        getTri
    }
