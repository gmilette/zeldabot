package bot.plan.action

import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState
import bot.state.map.MapConstants
import bot.state.map.andAHalf
import bot.state.map.grid
import bot.state.oam.spiderClaw
import bot.state.oam.spiderClaw2
import bot.state.oam.spiderHeadOpen
import bot.state.oam.spiderHeadOpening
import util.d

// assume switched to arrow
class KillArrowSpider : Action {
    object KillArrowSpiderData {
        // should be the middle of the attack area
        val attackAreaLeftSideMiddleVertical = FramePoint(8.grid, 7.grid)
        val attackAreaLeftSideMiddleVerticalBottom = FramePoint(8.grid, 8.grid)
        val safeArea = FramePoint(7.grid.andAHalf, 9.grid)
    }

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
            // only shoot if the nose is open
            list.add(GoIn(3, GamePad.B, reset = true) { state ->
                state.vulnerable()
            })
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


// start 8_62_m_b
fun killSpider(): Action {
    val kill = KillAll(useBombs = true,
        allowBlock = true, // but if spider head isn't open, do block
        targetOnly = listOf(spiderHeadOpen, spiderHeadOpening))
//        targetOnly = listOf(spiderHeadOpen, spiderHeadOpening, spiderClaw2, spiderClaw))
    return DecisionAction(kill, Optional(HideFromSpider())) {
        it.vulnerable() || it.aliveEnemies.isEmpty()
    }
}

// either
// 1. hide
// 2. get into vertical position
// 3. shoot!

private fun MapLocationState.vulnerable(): Boolean = frameState.enemiesUncombined
    .any { it.tile == spiderHeadOpen || it.tile == spiderHeadOpening }

class HideFromSpider : Action {
    private val goTo = InsideNav(KillArrowSpider.KillArrowSpiderData.safeArea, tag = "hide from spider")
    override fun complete(state: MapLocationState): Boolean {
        return goTo.complete(state)
    }

    override fun nextStep(state: MapLocationState): GamePad {
        return goTo.nextStep(state)
    }
}
