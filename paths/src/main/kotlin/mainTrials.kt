import bot.DirectoryConstants
import bot.plan.runner.analysis.AbAnalysis
import java.io.File
import kotlin.system.exitProcess

fun main(vararg args: String) {
    val file = File(args.firstOrNull { it.endsWith(".jsonl") }
        ?: "${DirectoryConstants.outDir("zexperiment")}experiments.jsonl")
    if (!file.exists()) {
        println("no trial file at ${file.absolutePath}")
        exitProcess(1)
    }
    val deleteId = args.firstOrNull { it.startsWith("delete=") }?.substringAfter("=")
    val rows = AbAnalysis.read(file)

    if (deleteId == null) {
        println("${file.absolutePath}  (${rows.size} labelled trials)")
        println()
        println("%-18s %-14s %-8s %5s %6s %5s %6s %s".format(
            "runId", "label", "room", "n", "clear", "dead", "t/out", "build"))
        println("-".repeat(86))
        rows.groupBy { Triple(it.runId, it.label, it.start) }
            .toSortedMap(compareBy({ it.first }, { it.second }))
            .forEach { (key, batch) ->
                println("%-18s %-14s %-8s %5d %6d %5d %6d %s".format(
                    key.first.ifBlank { "(none)" }, key.second, key.third, batch.size,
                    batch.count { it.result == "complete" },
                    batch.count { it.result == "dead" },
                    batch.count { it.result == "timeout" },
                    batch.map { it.gitSha }.distinct().joinToString(",")))
            }
        println()
        println("delete a batch:  ./gradlew trials -Pargs=\"delete=<runId>\"")
        return
    }

    val doomed = rows.filter { it.runId == deleteId }
    if (doomed.isEmpty()) {
        println("no trials with runId '$deleteId'")
        exitProcess(1)
    }
    val backup = File(file.parentFile, "${file.nameWithoutExtension}_backup_${System.currentTimeMillis()}.jsonl")
    file.copyTo(backup)
    val kept = file.readLines().filter { line ->
        line.isNotBlank() && !line.contains("\"runId\":\"$deleteId\"") && !line.contains("\"runId\": \"$deleteId\"")
    }
    file.writeText(kept.joinToString("\n") + "\n")
    println("removed ${doomed.size} trials with runId '$deleteId' (${doomed.map { it.label }.distinct()})")
    println("backup: ${backup.absolutePath}")
}
