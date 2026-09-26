import bot.DirectoryConstants
import bot.plan.runner.analysis.AbAnalysis
import bot.plan.runner.analysis.AbReportHtml
import bot.plan.runner.analysis.Metric
import bot.plan.runner.analysis.OutcomeRate
import java.awt.Desktop
import java.io.File
import kotlin.math.roundToInt
import kotlin.system.exitProcess

fun main(vararg args: String) {
    if (args.size < 2) {
        println("usage: analyzeAb <labelA> <labelB> [experiments.jsonl] [csv=<path>]")
        exitProcess(1)
    }
    val labelA = args[0]
    val labelB = args[1]
    val csvExport = args.firstOrNull { it.startsWith("csv=") }?.substringAfter("=")
    val positional = args.drop(2).filterNot { it.startsWith("csv=") }
    val csv = File(positional.firstOrNull() ?: "${DirectoryConstants.outDir("zexperiment")}experiments.jsonl")

    val rows = try {
        AbAnalysis.read(csv)
    } catch (e: IllegalArgumentException) {
        println("error: ${e.message}")
        exitProcess(1)
    }
    if (rows.isEmpty()) {
        println("error: ${csv.path} has no trial rows yet")
        exitProcess(1)
    }

    val report = try {
        AbAnalysis.compare(csv.path, rows, labelA, labelB)
    } catch (e: IllegalArgumentException) {
        println("error: ${e.message}")
        exitProcess(1)
    }

    println("source : ${csv.path}")
    println("room   : ${report.room}")
    println("arms   : ${report.a.label} n=${report.a.n} build=${report.a.builds.joinToString(",").ifBlank { "?" }}")
    println("         ${report.b.label} n=${report.b.n} build=${report.b.builds.joinToString(",").ifBlank { "?" }}")
    if (report.warnings.isNotEmpty()) {
        println()
        report.warnings.forEach { println("WARNING: $it") }
    }

    val o = report.outcome
    println()
    println("=== outcomes ===")
    println("%-10s %6s %6s %6s %6s".format("", "n", "clear", "dead", "t/out"))
    listOf(report.a, report.b).forEach { arm ->
        println("%-10s %6d %6d %6d %6d".format(arm.label, arm.n, arm.cleared.size, arm.died, arm.timedOut))
    }
    println()
    println("%-10s %22s %22s %9s".format("rate", report.a.label, report.b.label, "fisher p"))
    println("-".repeat(66))
    fun rateLine(name: String, ra: OutcomeRate, rb: OutcomeRate, p: Double) {
        fun show(r: OutcomeRate) =
            "%5.0f%% [%3.0f-%3.0f%%]".format(r.rate * 100, r.ci.first * 100, r.ci.second * 100)
        println("%-10s %22s %22s %9s".format(name, show(ra), show(rb),
            if (p.isNaN()) "n/a" else "%.4f".format(p)))
    }
    rateLine("cleared", o.clearA, o.clearB, o.clearP)
    rateLine("died", o.deathA, o.deathB, o.deathP)
    rateLine("timed out", o.timeoutA, o.timeoutB, o.timeoutP)
    println("brackets are 95% Wilson intervals; overlap means the difference is not established")

    if (o.deathA.count > 0 || o.deathB.count > 0) {
        println()
        println("deaths: %s died %d time(s), median %.0f frames in; %s died %d time(s), median %.0f frames in".format(
            report.a.label, o.deathA.count, o.medianFramesBeforeDeathA,
            report.b.label, o.deathB.count, o.medianFramesBeforeDeathB))
        listOf(report.a.label to o.deathPlacesA, report.b.label to o.deathPlacesB).forEach { (label, places) ->
            if (places.isNotEmpty()) {
                println("  $label died at: " + places.entries.sortedByDescending { it.value }
                    .joinToString(", ") { "${it.key} x${it.value}" })
            }
        }
    }

    println()
    println("%-20s %12s %12s %9s %9s".format("metric (cleared)", "median A", "median B", "change", "p"))
    println("-".repeat(68))
    report.comparisons.forEach { c ->
        println("%-20s %12.2f %12.2f %8.1f%% %9s".format(
            c.metric.label, c.medianA, c.medianB, c.relativeDelta * 100,
            if (c.p.isNaN()) "n/a" else "%.4f".format(c.p)))
    }

    println()
    println("median %s difference (B - A): %d to %d frames (95%% bootstrap CI)".format(
        Metric.primary.label.lowercase(), report.bootstrapLow.roundToInt(), report.bootstrapHigh.roundToInt()))
    if (report.bootstrapLow <= 0 && report.bootstrapHigh >= 0) {
        println("  interval spans zero: this sample does not show a difference")
    }
    println("sensitivity: detects a change of about %.0f%% or larger (alpha .05, power .8)".format(
        report.detectableEffect * 100))

    csvExport?.let { path ->
        val file = File(path)
        file.parentFile?.mkdirs()
        file.printWriter().use { w ->
            w.println("label,trial,gitSha,experiment,start,end,result,percent,elapsedFrames," +
                    "totalFrames,heartsStart,heartsEnd,netHeartsLost,damagedEvents,damagedFrames," +
                    "bombsUsed,rupees,rawHits,rawDamage,rawHeal,sword,ring,bombs,boom,shield,file")
            (report.a.rows + report.b.rows).forEach { t ->
                w.println(listOf(t.label, t.trial, t.gitSha, t.experiment, t.start, t.end, t.result,
                    t.percent, t.elapsedFrames, t.totalFrames, t.heartsStart, t.heartsEnd,
                    t.netHeartsLost, t.damagedEvents, t.damagedFrames, t.bombsUsed, t.rupees,
                    t.rawHits, t.rawDamage, t.rawHeal, t.sword, t.ring, t.bombs, t.boom,
                    t.shield, t.file).joinToString(","))
            }
        }
        println("csv    : ${file.absolutePath} (${report.a.n + report.b.n} trials)")
    }

    val out = File(csv.parentFile, "ab_${labelA}_vs_${labelB}.html")
    AbReportHtml.write(report, out)
    println()
    println("report: ${out.absolutePath}")
    try {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(out.toURI())
    } catch (e: Exception) {
    }
}
