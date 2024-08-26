import bot.state.map.Hyrule
import sequence.AnalysisPlanBuilder

fun main() {
    println("Analyze")

    val hyrule: Hyrule = Hyrule()

    AnalysisPlanBuilder(hyrule).buildHalfPlan()
}
