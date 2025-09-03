package bot.plan.zstar.route

import bot.plan.action.RouteTo.RouteParam
import bot.state.Agent
import bot.state.FramePoint
import bot.state.GamePad
import bot.state.MapLocationState

class BreadthFirstRouteTo {
    fun routeTo(
        state: MapLocationState,
        to: List<FramePoint>,
        param: RouteParam = RouteParam(),
        // pass in attack targets
        attackableSpec: List<Agent> = emptyList()
    ): GamePad {
//        val search = BreadthFirstSearch(true, false)
        return GamePad.None
    }
}