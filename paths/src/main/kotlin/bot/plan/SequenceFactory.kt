package bot.plan

import bot.state.map.MapCells
import bot.state.map.level.LevelMapCellsLookup
import sequence.AnalysisPlanBuilder

class SequenceFactory(
    private val mapData: MapCells,
    private val levelData: LevelMapCellsLookup,
    private val optimizer: AnalysisPlanBuilder.MasterPlanOptimizer
) {
    fun make(phrase: String): LocationSequenceBuilder =
        LocationSequenceBuilder(mapData, levelData, optimizer, phrase)
}