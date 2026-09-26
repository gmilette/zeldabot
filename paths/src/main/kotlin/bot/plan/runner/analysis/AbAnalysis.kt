package bot.plan.runner.analysis

import bot.plan.runner.TrialSummary
import com.google.gson.JsonObject
import com.google.gson.JsonStreamParser
import java.io.File
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

data class Metric(
    val name: String,
    val label: String,
    val unit: String,
    val get: (TrialSummary) -> Double
) {
    companion object {
        val all = listOf(
            Metric("elapsedFrames", "Frames to clear", "frames") { it.elapsedFrames.toDouble() },
            Metric("rawDamage", "Damage taken", "hearts") { it.rawDamage },
            Metric("damagedEvents", "Times hit", "hits") { it.damagedEvents.toDouble() },
            Metric("damagePerKFrames", "Damage per 1k frames", "hearts") {
                if (it.elapsedFrames > 0) it.rawDamage * 1000 / it.elapsedFrames else 0.0
            },
            Metric("netHeartsLost", "Net hearts lost", "hearts") { it.netHeartsLost },
            Metric("rawHeal", "Healing picked up", "hearts") { it.rawHeal },
            Metric("totalFrames", "Decision frames", "frames") { it.totalFrames.toDouble() },
            Metric("damagedFraction", "Time damaged", "fraction") { it.damagedFraction },
            Metric("bombsUsed", "Bombs used", "bombs") { it.bombsUsed.toDouble() }
        )
        val primary = all.first()
    }
}

data class MetricComparison(
    val metric: Metric,
    val a: List<Double>,
    val b: List<Double>
) {
    val medianA = a.median()
    val medianB = b.median()
    val iqrA = a.iqr()
    val iqrB = b.iqr()
    val relativeDelta = if (medianA != 0.0) (medianB - medianA) / medianA else Double.NaN
    val p = Stats.mannWhitneyU(a, b)
}

data class ArmSummary(
    val label: String,
    val rows: List<TrialSummary>
) {
    val cleared = rows.filter { it.cleared }
    val n = rows.size
    val clearRate = if (n == 0) 0.0 else cleared.size.toDouble() / n
    val outcomes: Map<String, Int> = rows.groupingBy { it.result }.eachCount()
    val died = rows.count { it.result == "dead" }
    val timedOut = rows.count { it.result == "timeout" }
    val deathRate = if (n == 0) 0.0 else died.toDouble() / n
    val timeoutRate = if (n == 0) 0.0 else timedOut.toDouble() / n
    val framesBeforeDeath = rows.filter { it.result == "dead" }.map { it.elapsedFrames.toDouble() }
    val deathPlaces: Map<String, Int> =
        rows.filter { it.result == "dead" }.groupingBy { it.end }.eachCount()
    val builds = rows.map { it.gitSha }.filter { it.isNotBlank() }.distinct().sorted()
    val runIds = rows.map { it.runId.ifBlank { "(none)" } }.distinct().sorted()
    val rooms = rows.map { it.start }.distinct()
    val loadouts = rows.map { "${it.config}/${it.heartsStart}h" }.distinct()
}

data class RoomBreakdown(
    val room: String,
    val nA: Int,
    val nB: Int,
    val clearA: Double,
    val clearB: Double,
    val medianA: Double,
    val medianB: Double,
    val p: Double
)

data class RoomResult(
    val room: String,
    val nA: Int,
    val nB: Int,
    val medianA: Double,
    val medianB: Double,
    val p: Double
)

data class MetricStratified(
    val metric: Metric,
    val perRoom: List<RoomResult>,
    val stratifiedP: Double
)

data class AbReport(
    val source: String,
    val a: ArmSummary,
    val b: ArmSummary,
    val room: String,
    val warnings: List<String>,
    val fisherP: Double,
    val comparisons: List<MetricComparison>,
    val rooms: List<String>,
    val breakdown: List<RoomBreakdown>,
    val stratified: List<MetricStratified>,
    val stratifiedP: Double,
    val outcome: OutcomeComparison,
    val bootstrapLow: Double,
    val bootstrapHigh: Double,
    val detectableEffect: Double
)

data class OutcomeRate(
    val label: String,
    val rate: Double,
    val count: Int,
    val n: Int
) {
    val ci = Stats.wilson(count, n)
}

data class OutcomeComparison(
    val clearA: OutcomeRate,
    val clearB: OutcomeRate,
    val deathA: OutcomeRate,
    val deathB: OutcomeRate,
    val timeoutA: OutcomeRate,
    val timeoutB: OutcomeRate,
    val clearP: Double,
    val deathP: Double,
    val timeoutP: Double,
    val medianFramesBeforeDeathA: Double,
    val medianFramesBeforeDeathB: Double,
    val deathPlacesA: Map<String, Int>,
    val deathPlacesB: Map<String, Int>
)

object AbAnalysis {

    fun read(file: File): List<TrialSummary> {
        require(file.exists()) {
            "no trial file at ${file.absolutePath}. Run the bot with `dev label=<arm>` first."
        }
        val rows = mutableListOf<TrialSummary>()
        var bad = 0
        file.reader().use { reader ->
            val stream = JsonStreamParser(reader)
            while (true) {
                val element = try {
                    if (!stream.hasNext()) break
                    stream.next()
                } catch (e: Exception) {
                    bad++
                    break
                }
                try {
                    rows += parse(element.asJsonObject)
                } catch (e: Exception) {
                    bad++
                }
            }
        }
        if (bad > 0) {
            println("WARNING: ${file.name} had $bad unreadable entries, skipped")
        }
        return rows.filter { it.label.isNotBlank() }
    }

    private fun parse(o: JsonObject): TrialSummary {
        fun str(name: String, default: String = "") =
            o.get(name)?.takeIf { !it.isJsonNull }?.asString ?: default
        fun int(name: String, default: Int = 0) =
            o.get(name)?.takeIf { !it.isJsonNull }?.asInt ?: default
        fun dbl(name: String, default: Double = 0.0) =
            o.get(name)?.takeIf { !it.isJsonNull }?.asDouble ?: default
        fun bool(name: String, default: Boolean = false) =
            o.get(name)?.takeIf { !it.isJsonNull }?.asBoolean ?: default
        return TrialSummary(
            date = str("date"), label = str("label"), runId = str("runId"), config = str("config"), gitSha = str("gitSha"),
            experiment = str("experiment"), file = str("file"), trial = int("trial"),
            start = str("start", "?"), result = str("result", "other"),
            percent = int("percent"), end = str("end", "?"),
            elapsedFrames = int("elapsedFrames"), totalFrames = int("totalFrames"),
            heartsStart = dbl("heartsStart"), heartsEnd = dbl("heartsEnd"),
            netHeartsLost = dbl("netHeartsLost"), damagedEvents = int("damagedEvents"),
            damagedFrames = int("damagedFrames"), bombsUsed = int("bombsUsed"),
            rupees = int("rupees"), rawHits = int("rawHits"), rawDamage = dbl("rawDamage"),
            rawHeal = dbl("rawHeal"), sword = str("sword"), ring = str("ring"),
            bombs = int("bombs"), boom = str("boom"), shield = bool("shield")
        )
    }

    fun compare(
        source: String,
        all: List<TrialSummary>,
        labelA: String,
        labelB: String,
        roomsCombinedOnPurpose: Boolean = false
    ): AbReport {
        val a = ArmSummary(labelA, all.filter { it.label == labelA })
        val b = ArmSummary(labelB, all.filter { it.label == labelB })
        require(a.n > 0) { "no rows labelled '$labelA'. Labels present: ${all.map { it.label }.distinct()}" }
        require(b.n > 0) { "no rows labelled '$labelB'. Labels present: ${all.map { it.label }.distinct()}" }

        val warnings = mutableListOf<String>()
        for (arm in listOf(a, b)) {
            if (arm.rooms.size > 1 && !roomsCombinedOnPurpose) {
                warnings += "${arm.label} mixes rooms ${arm.rooms} - not one experiment"
            }
            if (arm.builds.size > 1) warnings += "${arm.label} mixes builds ${arm.builds} - the arm is not a single code state"
            if (arm.loadouts.size > 1) warnings += "${arm.label} mixes loadouts ${arm.loadouts} - trials did not start equal"
        }
        val loadouts = (a.loadouts + b.loadouts).distinct()
        if (loadouts.size > 1) warnings += "the arms do not share a loadout ($loadouts) - they are not comparable"
        val rooms = (a.rooms + b.rooms).distinct()
        if (rooms.size > 1 && !roomsCombinedOnPurpose) {
            warnings += "the arms are not the same room ($rooms) - they are not comparable"
        }
        if (minOf(a.n, b.n) < 10) warnings += "fewer than 10 trials in an arm - only very large effects will show"

        val comparisons = Metric.all.mapNotNull { m ->
            val av = a.cleared.map(m.get)
            val bv = b.cleared.map(m.get)
            if (av.isEmpty() || bv.isEmpty()) null else MetricComparison(m, av, bv)
        }

        val fisher = Stats.fisherExact(
            a.cleared.size, a.n - a.cleared.size,
            b.cleared.size, b.n - b.cleared.size
        )

        val outcome = OutcomeComparison(
            clearA = OutcomeRate(a.label, a.clearRate, a.cleared.size, a.n),
            clearB = OutcomeRate(b.label, b.clearRate, b.cleared.size, b.n),
            deathA = OutcomeRate(a.label, a.deathRate, a.died, a.n),
            deathB = OutcomeRate(b.label, b.deathRate, b.died, b.n),
            timeoutA = OutcomeRate(a.label, a.timeoutRate, a.timedOut, a.n),
            timeoutB = OutcomeRate(b.label, b.timeoutRate, b.timedOut, b.n),
            clearP = fisher,
            deathP = Stats.fisherExact(a.died, a.n - a.died, b.died, b.n - b.died),
            timeoutP = Stats.fisherExact(a.timedOut, a.n - a.timedOut, b.timedOut, b.n - b.timedOut),
            medianFramesBeforeDeathA = a.framesBeforeDeath.median(),
            medianFramesBeforeDeathB = b.framesBeforeDeath.median(),
            deathPlacesA = a.deathPlaces,
            deathPlacesB = b.deathPlaces
        )

        val pa = a.cleared.map(Metric.primary.get)
        val pb = b.cleared.map(Metric.primary.get)
        val (lo, hi) = if (pa.size > 2 && pb.size > 2) Stats.bootstrapMedianDiff(pa, pb)
        else 0.0 to 0.0

        val cv = if (pa.isNotEmpty() && pa.mean() != 0.0) pa.sd() / pa.mean() else 0.0
        val n = minOf(a.cleared.size, b.cleared.size)
        val detectable = if (cv > 0 && n > 0) sqrt(2 * 2.8 * 2.8 * cv * cv / n) else 0.0

        val roomList = (a.rooms + b.rooms).distinct().sorted()
        val breakdown = roomList.map { room ->
            val ra = a.cleared.filter { it.start == room }
            val rb = b.cleared.filter { it.start == room }
            val va = ra.map(Metric.primary.get)
            val vb = rb.map(Metric.primary.get)
            RoomBreakdown(
                room = room,
                nA = a.rows.count { it.start == room }, nB = b.rows.count { it.start == room },
                clearA = a.rows.filter { it.start == room }.let {
                    if (it.isEmpty()) 0.0 else it.count { r -> r.cleared }.toDouble() / it.size },
                clearB = b.rows.filter { it.start == room }.let {
                    if (it.isEmpty()) 0.0 else it.count { r -> r.cleared }.toDouble() / it.size },
                medianA = va.median(), medianB = vb.median(),
                p = Stats.mannWhitneyU(va, vb)
            )
        }
        val stratified = Metric.all.map { m ->
            val perRoom = roomList.map { room ->
                val va = a.cleared.filter { it.start == room }.map(m.get)
                val vb = b.cleared.filter { it.start == room }.map(m.get)
                RoomResult(room, va.size, vb.size, va.median(), vb.median(),
                    Stats.mannWhitneyU(va, vb))
            }
            val metricStrata = roomList.map { room ->
                a.cleared.filter { it.start == room }.map(m.get) to
                        b.cleared.filter { it.start == room }.map(m.get)
            }
            MetricStratified(m, perRoom,
                if (roomList.size > 1) Stats.vanElteren(metricStrata) else Double.NaN)
        }

        val strata = roomList.map { room ->
            a.cleared.filter { it.start == room }.map(Metric.primary.get) to
                    b.cleared.filter { it.start == room }.map(Metric.primary.get)
        }
        val stratifiedP = if (roomList.size > 1) Stats.vanElteren(strata) else Double.NaN

        return AbReport(
            source = source, a = a, b = b,
            rooms = roomList, breakdown = breakdown, stratified = stratified,
            stratifiedP = stratifiedP,
            room = rooms.firstOrNull() ?: "?",
            warnings = warnings, fisherP = fisher, comparisons = comparisons, outcome = outcome,
            bootstrapLow = lo, bootstrapHigh = hi, detectableEffect = detectable
        )
    }

}

fun List<Double>.median(): Double {
    if (isEmpty()) return 0.0
    val s = sorted()
    val mid = s.size / 2
    return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
}

fun List<Double>.quantile(q: Double): Double {
    if (isEmpty()) return 0.0
    val s = sorted()
    if (s.size == 1) return s[0]
    val pos = q * (s.size - 1)
    val lo = pos.toInt()
    val hi = minOf(lo + 1, s.size - 1)
    return s[lo] + (s[hi] - s[lo]) * (pos - lo)
}

fun List<Double>.iqr(): Pair<Double, Double> = quantile(0.25) to quantile(0.75)

fun List<Double>.mean(): Double = if (isEmpty()) 0.0 else sum() / size

fun List<Double>.sd(): Double {
    if (size < 2) return 0.0
    val m = mean()
    return sqrt(sumOf { (it - m) * (it - m) } / size)
}

object Stats {

    fun mannWhitneyU(a: List<Double>, b: List<Double>): Double {
        val n1 = a.size
        val n2 = b.size
        if (n1 < 3 || n2 < 3) return Double.NaN
        val all = (a.map { it to 0 } + b.map { it to 1 }).sortedBy { it.first }
        val ranks = DoubleArray(all.size)
        var tieTerm = 0.0
        var i = 0
        while (i < all.size) {
            var j = i
            while (j + 1 < all.size && all[j + 1].first == all[i].first) j++
            val rank = (i + j + 2) / 2.0 // midrank, 1 based
            for (k in i..j) ranks[k] = rank
            val t = (j - i + 1).toDouble()
            tieTerm += t * t * t - t
            i = j + 1
        }
        var r1 = 0.0
        all.forEachIndexed { idx, (_, group) -> if (group == 0) r1 += ranks[idx] }
        val u1 = r1 - n1 * (n1 + 1) / 2.0
        val n = (n1 + n2).toDouble()
        val mu = n1 * n2 / 2.0
        val variance = (n1 * n2 / 12.0) * ((n + 1) - tieTerm / (n * (n - 1)))
        if (variance <= 0) return Double.NaN
        val z = (abs(u1 - mu) - 0.5) / sqrt(variance)
        return (2 * (1 - normalCdf(maxOf(z, 0.0)))).coerceIn(0.0, 1.0)
    }

    fun fisherExact(a: Int, b: Int, c: Int, d: Int): Double {
        val row1 = a + b
        val row2 = c + d
        val col1 = a + c
        val total = row1 + row2
        if (total == 0) return Double.NaN
        val observed = hyper(a, row1, row2, col1)
        var p = 0.0
        val lo = maxOf(0, col1 - row2)
        val hi = minOf(col1, row1)
        for (x in lo..hi) {
            val prob = hyper(x, row1, row2, col1)
            if (prob <= observed * (1 + 1e-9)) p += prob
        }
        return p.coerceIn(0.0, 1.0)
    }

    private fun hyper(x: Int, row1: Int, row2: Int, col1: Int): Double {
        val total = row1 + row2
        val logP = logChoose(row1, x) + logChoose(row2, col1 - x) - logChoose(total, col1)
        return exp(logP)
    }

    private fun logChoose(n: Int, k: Int): Double {
        if (k < 0 || k > n) return Double.NEGATIVE_INFINITY
        return logFactorial(n) - logFactorial(k) - logFactorial(n - k)
    }

    private val logFactCache = HashMap<Int, Double>()

    private fun logFactorial(n: Int): Double = logFactCache.getOrPut(n) {
        var sum = 0.0
        for (i in 2..n) sum += ln(i.toDouble())
        sum
    }

    fun bootstrapMedianDiff(a: List<Double>, b: List<Double>, iterations: Int = 10_000): Pair<Double, Double> {
        val rng = Random(20260920)
        val diffs = DoubleArray(iterations)
        for (i in 0 until iterations) {
            val ra = List(a.size) { a[rng.nextInt(a.size)] }
            val rb = List(b.size) { b[rng.nextInt(b.size)] }
            diffs[i] = rb.median() - ra.median()
        }
        diffs.sort()
        return diffs[(0.025 * iterations).toInt()] to diffs[(0.975 * iterations).toInt()]
    }

    fun wilson(successes: Int, n: Int, z: Double = 1.96): Pair<Double, Double> {
        if (n == 0) return 0.0 to 0.0
        val p = successes.toDouble() / n
        val d = 1 + z * z / n
        val centre = p + z * z / (2 * n)
        val spread = z * sqrt(p * (1 - p) / n + z * z / (4.0 * n * n))
        return ((centre - spread) / d).coerceAtLeast(0.0) to ((centre + spread) / d).coerceAtMost(1.0)
    }

    /**
     * van Elteren: a Wilcoxon rank sum blocked by stratum. Ranks are formed inside each
     * room and only then combined, so rooms of different difficulty never get compared
     * against each other - which naive pooling does, and which can reverse the result
     * when the arms have uneven trial counts per room.
     */
    fun vanElteren(strata: List<Pair<List<Double>, List<Double>>>): Double {
        var numerator = 0.0
        var variance = 0.0
        for ((a, b) in strata) {
            val n1 = a.size
            val n2 = b.size
            if (n1 == 0 || n2 == 0) continue
            val total = n1 + n2
            val weight = 1.0 / (total + 1)
            val all = (a.map { it to 0 } + b.map { it to 1 }).sortedBy { it.first }
            val ranks = DoubleArray(all.size)
            var tieTerm = 0.0
            var i = 0
            while (i < all.size) {
                var j = i
                while (j + 1 < all.size && all[j + 1].first == all[i].first) j++
                val rank = (i + j + 2) / 2.0
                for (k in i..j) ranks[k] = rank
                val t = (j - i + 1).toDouble()
                tieTerm += t * t * t - t
                i = j + 1
            }
            var rankSumA = 0.0
            all.forEachIndexed { idx, (_, group) -> if (group == 0) rankSumA += ranks[idx] }
            val expected = n1 * (total + 1) / 2.0
            val varI = if (total > 1) {
                n1.toDouble() * n2 / (total * (total - 1.0)) *
                        (ranks.sumOf { it * it } - total * (total + 1.0) * (total + 1.0) / 4.0)
            } else 0.0
            numerator += weight * (rankSumA - expected)
            variance += weight * weight * varI
        }
        if (variance <= 0) return Double.NaN
        val z = abs(numerator) / sqrt(variance)
        return (2 * (1 - normalCdf(z))).coerceIn(0.0, 1.0)
    }

    fun normalCdf(z: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(z))
        val d = 0.3989422804014327 * exp(-z * z / 2.0)
        val p = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 +
                t * (-1.821255978 + t * 1.330274429))))
        return if (z >= 0) 1.0 - p else p
    }
}
