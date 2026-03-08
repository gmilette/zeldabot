package bot.plan.levels

import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.plan.action.makeCenterPush
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.map.MapConstants
import bot.state.map.destination.ZeldaItem
import bot.state.map.grid
import bot.state.map.level.LevelStartMapLoc

object Level1Plan {
    private object Loc {
        val key114Position = FramePoint(9.grid, 6.grid)
        val key114 = FramePoint(10.grid, 8.grid)
        val key83 = FramePoint(8.grid, 3.grid)

        // hands level1, prob same as key114
        val key69 = FramePoint(164, 128)
        val boomerang68 = FramePoint(8.grid, 3.grid) //try grid // 3.5?? //56
    }

    val level1: PlanBuilder.() -> Unit
        get() = {
            add {
                phase(Phases.lev(1))
                lev(1)
                inLevel
                startAt(LevelStartMapLoc.lev(1))
                exitReenter()
                objective(ZeldaItem.Bow)
                seg("grab key")
                left
                goIn(GamePad.MoveLeft, 30)
                kill //164, 192
                goTo(Loc.key114)
                right
                seg("go to key skeleton")
                right
                seg("grab from skeleton")
                killUntilGetKey
                seg("move to arrow")
                left // first rooms
                up //99
                up //83
                goIn(GamePad.MoveUp, 5)
                seg("get key from skeletons")
                kill // these skeletons provide a key
                goTo(Loc.key83)
                seg("Bomb and move")
                bombUp
                up // 51
                seg("grab key from zig")
                killUntilGetKey
                seg("get key from boomerang guys")
                up
                goIn(GamePad.MoveUp, 30)
                kill
                goTo(Loc.key83)
                // get around a hard corner
                goTo(FramePoint(7.grid, 2.grid))
                goTo(FramePoint(3.grid, 2.grid))
                seg("get arrow")
                leftm
                seg("push action")
                goIn(GamePad.MoveLeft, MapConstants.oneGridPoint5) // dodge the traps by moving in
                +makeCenterPush(127, makeUp(34))
                seg("snag boomerang", ZeldaItem.Boomerang)
                // because of bad routing link still goes back and forth ad just the wrong time
                // try going up?
                // maybe i have to fake out the traps
                val beforeExit = FramePoint(12.grid, 5.grid)
                val bottomBefore = FramePoint(7.grid, 7.grid)
                val bottom = FramePoint(7.grid, 8.grid)
                // fake maneuver
                goTo(bottomBefore, ignoreProjectiles = true)
                goTo(bottom, ignoreProjectiles = true)
                goTo(bottomBefore, ignoreProjectiles = true)
                goIn(GamePad.None, 100)
                goTo(beforeExit, ignoreProjectiles = true)
                goIn(GamePad.MoveRight, MapConstants.twoGrid)
                rightNoP // don't attack
                down.down // at 67 now
                right // boomerang
                goIn(GamePad.MoveRight, 30)
                kill // could prevent look at the boomerang position from being considered loot
                goAbout(Loc.boomerang68, 1, 1, true, ignoreProjectiles = true)
                rightm //69 hand grabby, dont get loot
                seg("steal key from hand grabby", ZeldaItem.Triforce)
                go(Loc.key114Position)
                go(Loc.key114)
                // should do but too risky for now
//                .go(Loc.key114)
                up
                seg("dragon position")
                // avoid first round of fireballs
                // no go up near the edge
//                go(FramePoint(6.grid, 3.grid))
                go(FramePoint(11 .grid, 6.grid))
                seg("destroy dragon")
                killLev1Dragon // aim for the head
                seg("dragon destroyed")
                rightonlym // triforce
                seg("go in")
                goIn(GamePad.MoveRight, 20)
                seg("get the triforce")
                getTri
            }
        }

}