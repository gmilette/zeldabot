import bot.DirectoryConstants
import bot.plan.runner.analysis.AbAnalysis
import bot.plan.runner.analysis.AbReportHtml
import bot.plan.runner.analysis.Metric
import bot.plan.runner.analysis.OutcomeRate
import bot.plan.runner.analysis.RoomNames
import java.awt.Desktop
import java.io.File
import kotlin.math.roundToInt
import kotlin.system.exitProcess

fun main(vararg args: String) {
    if (args.size < 2) {
        println("usage: analyzeAb <labelA> <labelB> [file.jsonl] [room=<level_mapLoc>] [exclude=<runId,...>] [csv=<path>]")
        exitProcess(1)
    }
    val labelA = args[0]
    val labelB = args[1]
    fun opt(key: String) = args.firstOrNull { it.startsWith("$key=") }?.substringAfter("=")
    val csvExport = opt("csv")
    val roomFilter = opt("room")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
    val excluded = opt("exclude")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    val positional = args.drop(2).filterNot { it.contains("=") }
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

    val rooms = rows.map { it.start }.distinct().sorted()
    if (roomFilter.isNullOrEmpty() && rooms.size > 1) {
        println("error: ${csv.path} holds several rooms (${rooms.joinToString(", ")}).")
        println("       different rooms are different difficulties, so comparing across them is meaningless.")
        println("       pass room=<level_mapLoc> to pick one, or room=a,b to combine them.")
        exitProcess(1)
    }
    val selected = rows
        .filter { roomFilter.isNullOrEmpty() || it.start in roomFilter }
        .filter { it.runId !in excluded }
    if (selected.isEmpty()) {
        println("error: nothing left after filtering (room=$roomFilter exclude=$excluded)")
        exitProcess(1)
    }
    if (excluded.isNotEmpty()) {
        println("excluded runs: ${excluded.joinToString(", ")} (${rows.size - selected.size} trials dropped)")
    }

    val report = try {
        AbAnalysis.compare(csv.path, selected, labelA, labelB,
            roomsCombinedOnPurpose = (roomFilter?.size ?: 0) > 1)
    } catch (e: IllegalArgumentException) {
        println("error: ${e.message}")
        exitProcess(1)
    }

    println("source : ${csv.path}")
    println("room   : ${report.rooms.joinToString(", ") { RoomNames.label(it) }}")
    listOf(report.a, report.b).forEachIndexed { i, arm ->
        val prefix = if (i == 0) "arms   :" else "        "
        println("$prefix ${arm.label} n=${arm.n} build=${arm.builds.joinToString(",").ifBlank { "?" }}" +
                " runs=${arm.runIds.joinToString(",")}")
        arm.rows.map { it.config }.distinct().filter { it.isNotBlank() }.forEach {
            println("         config: $it")
        }
    }
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

    if (report.rooms.size > 1) {
        println()
        println("=== per room ===")
        val shortA = report.a.label.take(8)
        val shortB = report.b.label.take(8)
        println("%-20s %11s %11s %13s %13s %9s".format(
            "room", "n $shortA", "n $shortB",
            "clear $shortA/$shortB".take(13), "median $shortA/$shortB".take(13), "p"))
        println("-".repeat(82))
        report.breakdown.forEach { r ->
            println("%-20s %11d %11d %13s %13s %9s".format(
                RoomNames.label(r.room), r.nA, r.nB,
                "%.0f%%/%.0f%%".format(r.clearA * 100, r.clearB * 100),
                "%.0f/%.0f".format(r.medianA, r.medianB),
                if (r.p.isNaN()) "n/a" else "%.4f".format(r.p)))
        }

        println()
        println("=== per room, every metric: median $shortA -> $shortB, with p ===")
        fun num(v: Double) = if (kotlin.math.abs(v) >= 100) "%.0f".format(v) else "%.2f".format(v)
        val head = StringBuilder("%-24s".format("metric"))
        report.rooms.forEach { head.append("%24s".format(RoomNames.label(it).take(24))) }
        head.append("%12s".format("stratified"))
        println(head)
        println("-".repeat(24 + report.rooms.size * 24 + 12))
        report.stratified.forEach { ms ->
            val line = StringBuilder("%-24s".format(ms.metric.label))
            ms.perRoom.forEach { r ->
                val cell = "${num(r.medianA)}->${num(r.medianB)} p=" +
                        (if (r.p.isNaN()) "n/a" else "%.3f".format(r.p))
                line.append("%24s".format(cell))
            }
            line.append("%12s".format(
                if (ms.stratifiedP.isNaN()) "n/a" else "%.4f".format(ms.stratifiedP)))
            println(line)
        }
        println("ranks are formed inside each room, so rooms of different scale are never")
        println("ranked against each other. The stratified column is the one to quote.")
    }

    println()
    val nameA = report.a.label.take(12)
    val nameB = report.b.label.take(12)
    val pooledNote = if (report.rooms.size > 1)
        "metric (POOLED ${report.rooms.size} rooms)" else "metric (cleared)"
    println("%-22s %12s %12s %9s %9s %8s".format(pooledNote, nameA, nameB, "change", "p", "signif"))
    println("-".repeat(78))
    report.comparisons.forEach { c ->
        val sig = when {
            c.p.isNaN() -> "-"
            c.p < Metric.ALPHA -> "YES"
            else -> "no"
        }
        println("%-22s %12.2f %12.2f %8.1f%% %9s %8s".format(
            c.metric.label, c.medianA, c.medianB, c.relativeDelta * 100,
            "%.4f".format(c.p), sig))
    }
    println("signif = p < ${Metric.ALPHA}. Measured on self-comparisons, that bar keeps the")
    println("chance of a false positive anywhere in this table near 3%; p < 0.05 would make it 17%.")

    if (report.rooms.size > 1) {
        println("^ pooled across rooms of different scale - prefer the stratified column above")
    }

    println()
    println("median %s difference (B - A): %.2f to %.2f %s (95%% bootstrap CI)".format(
        Metric.primary.label.lowercase(), report.bootstrapLow, report.bootstrapHigh,
        Metric.primary.unit))
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
