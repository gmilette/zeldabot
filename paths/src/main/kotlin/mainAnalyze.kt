import bot.state.map.Hyrule
import sequence.AnalysisPlanBuilder

fun main(args: Array<String>) {
    println("Analyze")

    val hyrule: Hyrule = Hyrule()

    AnalysisPlanBuilder(hyrule).buildHalfPlan()
}
