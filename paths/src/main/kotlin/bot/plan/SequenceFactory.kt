package bot.plan

import bot.state.map.MapCells
import bot.state.map.level.LevelMapCellsLookup
import sequence.AnalysisPlanBuilder

class SequenceFactory(
    val map: MapCells,
    private val levelData: LevelMapCellsLookup,
    private val optimizer: AnalysisPlanBuilder.MasterPlanOptimizer
) {
    fun make(phrase: String): LocationSequenceBuilder =
        LocationSequenceBuilder(map, levelData, optimizer, phrase)
}