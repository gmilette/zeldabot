package bot.plan.levels

import bot.plan.InLocations
import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.plan.action.*
import bot.state.*
import bot.state.map.*
import bot.state.map.destination.ZeldaItem
import bot.state.map.level.LevelStartMapLoc

val level5: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(5))
        lev(5)
        cheatBombs
        startAt(LevelStartMapLoc.lev(5))
        seg("move to level 5", ZeldaItem.Whistle)
        upm
        seg("left past key")
        bombLeft
        bombLeft
        seg("kill before going in")
        kill
        seg("go in")
        addNext(
            6, makeCenterPush(
                88, makeUp(6),
                out = InLocations.OutLocation.outRight
            )
        )
        left
        seg("kill before getting item")
        kill
        seg("push center to get item")
        +makePush(
            InLocations.Push.singleLeft,
            makeUp(lastMapLoc),
            InLocations.Level5.mapLocGetItem,
            stairs = InLocations.StairsLocation.corner
        )
        seg("backtrack out", ZeldaItem.Key)
        right
        addNext(
            100, makeCenterPush(
                88, makeUp(100),
                out = InLocations.OutLocation.outLeft
            )
        )

        seg("get back")
        rightm
        switchToBoomerang
        seg("get back extra")
        right // possibly kill until get bomb IF need bombs
        seg("kill all zombie to open get key")
        killUntilGetKey
        seg("rhino bypass", ZeldaItem.Triforce)
        kill // have to kill all to move up
        up
        seg("kill all zombie up move right")
        rightm //dont shoot
        seg("no head up to victory")
        up // impossible maze with the squishies
        seg("zombie1")
        upm
        seg("zombie2")
        upm
        seg("go left to victory")
        // key??
        goTo(FramePoint(8.grid, 6.grid))
        left
        left
        seg("get past bunnies")
        switchToArrow() // is it in right spot?
        leftm
        seg("Use Whistle")
        goIn(GamePad.MoveLeft, 10) // more in a bit before whistlin'
        switchToWhistle()
        goIn(GamePad.None, 50) // more in a bit before whistlin'
        goIn(GamePad.MoveLeft, 20) // move more in
        useItem()
        wait(100) // wait for whistle to happen, otherwise bot will route uselessly
//        switchToBoomerang // broke waiting
        seg("Now destroy him")
        kill // problem the projectiles are considered enemies
        seg("Get 5 triforce")
        goTo(InLocations.Level5.triforceHeart)
        uponlym
        getTri
    }
