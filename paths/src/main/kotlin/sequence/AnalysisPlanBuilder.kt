package sequence

import bot.plan.OverworldRouter
import bot.state.*
import bot.state.map.Hyrule
import bot.state.map.MapCell
import util.d

/**
 * optimize routes to various destinations
 */
class AnalysisPlanBuilder(private val hyrule: Hyrule) {
    private val optimizer = OverworldRouter(hyrule)

    companion object {
        const val DEBUG = false
    }

    fun buildHalfPlan() {
        // start plan (119)
//        * level 1 // 55
//        * bomb heart // 44
//        * bomb secret // 45
//        * letter // 14
//        * 100 secret // 15(-1?) (skip it)
//        * 30 secret (61)
//        * white sword (10)
//        * candle (102)
//        * 100 secret (98)
//        * white ring 52  (optional, could go to level 3 first)
//        * lev 3 116
//        * lev 4 69
//        * go down around for other bomb heart, 123
//        * buy arrow 111
//        * collect ladder heart 95
//        * raft heart 47
//        * level 2 60
//        * DONE! 27 min
        // it would be nice if these were destinations
        // like named map loc or something
        optimize()
//        val optimizer = MasterPlanOptimizer(map)
    }

    private fun generateZeldaPlanSearchSpace() {
        // the algorithm that expands nodes and terminates infeasible ones
        // reasons about how much gold we have
        // if next objective is, get arrow, then find nearest show and get it, if
        // don't have enough $$, just fail it
        // instead of putting money locations, maybe just insert "get nearest money"

        // expand all next objectives
    }

    private fun optimize() {
        val tour = Tour(listOf(119, 55, 44, 45, 14, 15, 61, 10, 102, 98, 52, 116, 69, 123, 111, 95, 47, 60))

        val levels = mutableListOf(55, 60, 46, 69, 11, 34, 66, 109)
        val start = listOf(119)
        val end = listOf(5)

        var best: TourEvaluation = evaluate(Tour(start + levels + end))
        var iter = 100000
        var explore: List<MapLoc> = levels
        repeat(iter) { iter ->
            var ind = listOf<MapLoc>()
            var nextExplore = Tour(start + explore + end)
            while (
                ind.isEmpty() ||
                ind[3] > ind[4] ||
                ind[4] > ind[6] ||
                ind[4] > ind[5] ||
                ind[4] > ind[7] || //need ladder for 7
                ind[5] > ind[7] ||
                ind[1] > ind[8] || // need to get magic key past a spider
                ind[1] > ind[6] || //you need a arrow to get past 6
    //                ind[4] > ind[8] || //just a pref
    //                ind[2] > ind[8] || //just a pref
    //                ind[1] > ind[3] || //just a pref
//                ind[4] > ind[2] || //just a pref
//                ind[4] > ind[1] || //just a pref
//                ind[8] != 8 || // force 8 at end
                ind[1] > ind[6]
            ) {
                // filter out more
                explore = explore.shuffled()
                nextExplore = Tour(start + explore + end)
                ind = sequenceOf(119, 55, 60, 46, 69, 11, 34, 66, 109, 5).map { nextExplore.destinations.indexOf(it) }.toList()
    //                d {"try $nextExplore $ind"}
            }

    //            d {"iter $iter"}
            val result = evaluate(nextExplore)
            if (result.total < best.total) {
                d { " better result ${result} ${result.total}" }
                result.log()
                best = result
            }
        }

        d { " best " }
        best.log()
    }

    private fun evaluate(plan: Tour): TourEvaluation {
        val cells = plan.destinations.map { hyrule.getMapCell(it) }
        val distances = mutableListOf<Int>()
        var total = 0
        var prev: MapCell? = null
        for (next in cells) {
            val add = prev?.let {
                val path = optimizer.findPath(it, next)
                path
            }?.length ?: 0
//                d { " from: ${prev?.mapData?.name ?: ""} ${prev?.mapLoc} to ${next.mapLoc} dist: ${add}" }
            distances.add(add)
            total += add
            prev = next
        }
        return TourEvaluation(distances, plan)
    }

    data class Tour(val destinations: List<MapLoc>)

    fun Tour.shuffle(): Tour =
        Tour(destinations.shuffled())

    data class TourEvaluation(val distances: List<Int>, val tour: Tour) {
        val total = distances.sum()

        fun log() {
            d { " total ${total}" }
            d { " tour $tour"}
            var ist = ""
            var i = 1
            for (destination in tour.destinations) {
//                val levels = Tour(listOf(55, 60, 46, 69, 11, 34, 66, 109, 5))
                val name = when (destination) {
                    119 -> 0
                    55 -> 1
                    60 -> 2
                    46 -> 3
                    69 -> 4
                    11 -> 5
                    34 -> 6
                    66 -> 7
                    109 -> 8
                    5 -> 9
                    else -> 0
                }
                ist = ist + "$name, "
                d {"$i: $name"}
                i++
            }
            d { ist }
        }
    }

}
