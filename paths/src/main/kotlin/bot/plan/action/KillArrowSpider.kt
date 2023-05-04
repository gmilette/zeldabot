package bot.plan.action

import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.MapConstants
import bot.state.map.grid
import util.d

// assume switched to arrow
class KillArrowSpider : Action {
    object KillArrowSpiderData {
        // should be the middle of the attack area
        val attackAreaLeftSideMiddleVertical = FramePoint(8.grid, 7.grid)
        val attackAreaLeftSideMiddleVerticalBottom = FramePoint(8.grid, 8.grid)
    }

    // go in attack area
    // move up, then shoot
    private val positionShootO = ActionSequence(
        // todo: if at top of the grid, move to bottom first
        // but this works well enough
        InsideNavAbout(
            KillArrowSpiderData.attackAreaLeftSideMiddleVertical,
            MapConstants.oneGrid / 2, // stay in middle
            vertical = MapConstants.oneGrid,
            negVertical = MapConstants.oneGrid
        ),
        GoIn(3, GamePad.MoveUp, reset = true),
//        GoIn(3, GamePad.B, reset = true),
//        GoIn(3, GamePad.None, reset = true),
        AlwaysAttack(useB = true, 5)
    )

    // more debugging
    private val positionShootActions = mutableListOf<Action>(
        InsideNavAbout(
            KillArrowSpiderData.attackAreaLeftSideMiddleVerticalBottom,
            MapConstants.oneGrid / 2, // stay in middle
            vertical = 4,
            negVertical = 4
        )
    ).also { list ->
        repeat(5) {
            list.add(GoIn(3, GamePad.MoveUp, reset = true))
            list.add(GoIn(3, GamePad.B, reset = true))
            list.add(GoIn(3, GamePad.None, reset = true))
        }
    }

    private val positionShoot = OrderedActionSequence(positionShootActions)

    private val criteria = DeadForAWhile(limit = 200) {
        it.clearedWithMinIgnoreLoot(0)
    }

    override fun complete(state: MapLocationState): Boolean = criteria(state)

    override fun target(): FramePoint {
        return positionShoot.target()
    }

    override fun nextStep(state: MapLocationState): GamePad {
        d { "KillArrowSpider" }
        criteria.nextStep(state)

        // if I can detect the nose open then, I can dodge while that is happening
        // otherwise, just relentlessly attack

        return positionShoot.nextStep(state)
    }

    override val name: String
        get() = "KillArrowSpider ${criteria.frameCount}"
}
