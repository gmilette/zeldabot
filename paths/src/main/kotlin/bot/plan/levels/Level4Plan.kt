package bot.plan.levels

import bot.plan.InLocations
import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.plan.action.*
import bot.state.*
import bot.state.map.*
import bot.state.map.destination.ZeldaItem

val level4: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(4))
        lev(4)
        startAt(113)
        seg("go go go", ZeldaItem.Key)
        //key to left but dont bother
        up
        switchToBoomerang
        up // no get it because it is in the middle of the room
        goTo(InLocations.Level4.batKey)
        leftm
        up
        seg("get key from squishies")
        goTo(InLocations.Level4.squishyKey)
        up
        seg("go get ladder", ZeldaItem.Ladder)
        rightm
        kill
        right
        switchToBoomerang
        // watch out for pancakes
        kill // there will be 2 suns still running around
        seg("push")
        +makePush(
            InLocations.Push.moveLeftOfTwo, makeUp(50),
            96, InLocations.StairsLocation.corner
        )
        leftm
        seg("across ladder", ZeldaItem.Triforce)
        goIn(GamePad.MoveLeft, 11.grid, monitor = false) // custom action to walk across using the ladder
        leftm
        seg("get past 4 monster")
        up
        up
        seg("get to the dragon")
        bombRightExactly
        //skip key that is up
        bombRight
        // move even more because I saw
        // link go backwards accidently
        goIn(GamePad.MoveRight, 20)
        switchToBoomerang
        kill
        seg("push near")
        pushActionThenGoRight(InLocations.Push.moveLeftOfTwo)
        seg("fight dragon")
        killLev4Dragon // dragon
        //get heart
        goTo(InLocations.Level4.triforceHeart)
        uponlym
        getTri
    }
