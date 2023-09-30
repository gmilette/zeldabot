package bot.plan

import bot.state.map.MapCells
import bot.state.map.level.LevelMapCellsLookup
import sequence.AnalysisPlanBuilder

class PlanInputs(
    val map: MapCells,
    private val levelData: LevelMapCellsLookup,
    private val optimizer: OverworldRouter
) {
    fun make(phrase: String): PlanBuilder =
        PlanBuilder(map, levelData, optimizer, phrase)
}