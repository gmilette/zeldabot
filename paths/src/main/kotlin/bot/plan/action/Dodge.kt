package bot.plan.action

import bot.state.FramePoint
import bot.state.map.grid


val dodge = OrderedActionSequence(listOf(
    InsideNavAbout(FramePoint(4.grid, 4.grid), about = 2),
    InsideNavAbout(FramePoint(11.grid, 4.grid), about = 2),
    InsideNavAbout(FramePoint(11.grid, 6.grid), about = 2),
    InsideNavAbout(FramePoint(4.grid, 6.grid), about = 2)
    )
)