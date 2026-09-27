package bot.plan.runner.analysis

import java.io.File
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object AbReportHtml {

    private const val SERIES_A_LIGHT = "#2a78d6"
    private const val SERIES_B_LIGHT = "#eb6834"
    private const val SERIES_A_DARK = "#3987e5"
    private const val SERIES_B_DARK = "#d95926"

    fun write(report: AbReport, out: File): File {
        out.parentFile?.mkdirs()
        out.writeText(render(report))
        return out
    }

    private fun render(r: AbReport): String = buildString {
        append(
            """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>A/B ${esc(r.a.label)} vs ${esc(r.b.label)} - ${esc(r.rooms.joinToString(",") { RoomNames.label(it) })}</title>
<style>
  :root {
    color-scheme: light;
    --plane: #f9f9f7; --surface: #fcfcfb;
    --ink: #0b0b0b; --ink-2: #52514e; --muted: #898781;
    --grid: #e1e0d9; --axis: #c3c2b7; --border: rgba(11,11,11,0.10);
    --series-a: $SERIES_A_LIGHT; --series-b: $SERIES_B_LIGHT;
    --good: #0ca30c; --warning: #fab219; --critical: #d03b3b;
  }
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme="light"]) {
      color-scheme: dark;
      --plane: #0d0d0d; --surface: #1a1a19;
      --ink: #ffffff; --ink-2: #c3c2b7; --muted: #898781;
      --grid: #2c2c2a; --axis: #383835; --border: rgba(255,255,255,0.10);
      --series-a: $SERIES_A_DARK; --series-b: $SERIES_B_DARK;
    }
  }
  :root[data-theme="dark"] {
    color-scheme: dark;
    --plane: #0d0d0d; --surface: #1a1a19;
    --ink: #ffffff; --ink-2: #c3c2b7; --muted: #898781;
    --grid: #2c2c2a; --axis: #383835; --border: rgba(255,255,255,0.10);
    --series-a: $SERIES_A_DARK; --series-b: $SERIES_B_DARK;
  }
  * { box-sizing: border-box; }
  body { margin: 0; background: var(--plane); color: var(--ink);
    font: 14px/1.5 ui-sans-serif, -apple-system, "Segoe UI", Roboto, sans-serif; }
  .wrap { max-width: 980px; margin: 0 auto; padding-block: 32px; padding-left: 20px; padding-right: 20px; }
  h1 { font-size: 20px; margin: 0 0 4px; letter-spacing: -0.01em; }
  h2 { font-size: 14px; margin: 0 0 2px; letter-spacing: 0.02em; text-transform: uppercase; color: var(--ink-2); }
  .sub { color: var(--muted); margin: 0 0 24px; font-size: 13px; }
  .card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px;
    padding: 18px 20px; margin-bottom: 18px; }
  .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 18px; }
  .tile { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 14px 16px; }
  .tile .k { font-size: 12px; color: var(--muted); }
  .tile .v { font-size: 26px; font-weight: 600; letter-spacing: -0.02em; margin-top: 2px; }
  .tile .n { font-size: 12px; color: var(--ink-2); margin-top: 2px; }
  .legend { display: flex; gap: 18px; flex-wrap: wrap; margin: 0 0 14px; font-size: 13px; color: var(--ink-2); }
  .legend span { display: inline-flex; align-items: center; gap: 7px; }
  .sw { width: 11px; height: 11px; border-radius: 3px; display: inline-block; }
  table { border-collapse: collapse; width: 100%; font-variant-numeric: tabular-nums; font-size: 13px; }
  th { text-align: right; font-weight: 600; color: var(--ink-2); padding: 7px 10px;
    border-bottom: 1px solid var(--axis); white-space: nowrap; }
  th:first-child, td:first-child { text-align: left; }
  td { text-align: right; padding: 7px 10px; border-bottom: 1px solid var(--grid); }
  tbody tr:last-child td { border-bottom: none; }
  .warn { background: var(--surface); border: 1px solid var(--critical); border-radius: 10px;
    padding: 12px 16px; margin-bottom: 18px; color: var(--ink); }
  .warn li { margin: 2px 0; }
  .note { color: var(--ink-2); font-size: 13px; margin: 12px 0 0; }
  .scroll { overflow-x: auto; }
  svg { display: block; max-width: 100%; height: auto; }
  .tip { position: fixed; pointer-events: none; opacity: 0; transition: opacity .1s;
    background: var(--ink); color: var(--plane); padding: 6px 9px; border-radius: 6px;
    font-size: 12px; white-space: nowrap; z-index: 10; }
  circle[data-tip] { cursor: crosshair; }
</style>
</head>
<body>
<div class="wrap">
"""
        )

        append("<h1>${esc(r.a.label)} vs ${esc(r.b.label)}</h1>\n")
        append("<p class=\"sub\">room ${esc(r.rooms.joinToString(", ") { RoomNames.label(it) })} &middot; ")
        append("${r.a.n} vs ${r.b.n} trials &middot; builds ")
        append("${esc(r.a.builds.joinToString(",").ifBlank { "?" })} / ")
        append("${esc(r.b.builds.joinToString(",").ifBlank { "?" })}</p>\n")

        if (r.warnings.isNotEmpty()) {
            append("<div class=\"warn\"><strong>Check before trusting this</strong><ul>")
            r.warnings.forEach { append("<li>${esc(it)}</li>") }
            append("</ul></div>\n")
        }

        appendTiles(r)
        appendLegend(r)
        appendOutcomeChart(r)
        appendRateChart(r)
        appendStripChart(r)
        appendTable(r)
        appendFooter(r)

        append(
            """
</div>
<div class="tip" id="tip"></div>
<script>
  const tip = document.getElementById('tip');
  document.querySelectorAll('[data-tip]').forEach(el => {
    el.addEventListener('mouseenter', e => {
      tip.textContent = el.getAttribute('data-tip');
      tip.style.opacity = 1;
    });
    el.addEventListener('mousemove', e => {
      tip.style.left = (e.clientX + 14) + 'px';
      tip.style.top = (e.clientY - 10) + 'px';
    });
    el.addEventListener('mouseleave', () => { tip.style.opacity = 0; });
  });
</script>
</body>
</html>
"""
        )
    }

    private fun StringBuilder.appendTiles(r: AbReport) {
        val primary = r.comparisons.firstOrNull { it.metric.name == "elapsedFrames" }
        append("<div class=\"tiles\">")
        val o = r.outcome
        tile("Cleared", "${pct(r.a.clearRate)} &rarr; ${pct(r.b.clearRate)}",
            "${r.a.cleared.size}/${r.a.n} &rarr; ${r.b.cleared.size}/${r.b.n} &middot; p = ${pval(o.clearP)}")
        tile("Died", "${pct(r.a.deathRate)} &rarr; ${pct(r.b.deathRate)}",
            "${o.deathA.count}/${r.a.n} &rarr; ${o.deathB.count}/${r.b.n} &middot; p = ${pval(o.deathP)}")
        tile("Timed out", "${pct(r.a.timeoutRate)} &rarr; ${pct(r.b.timeoutRate)}",
            "${o.timeoutA.count}/${r.a.n} &rarr; ${o.timeoutB.count}/${r.b.n} &middot; p = ${pval(o.timeoutP)}")
        if (primary != null) {
            tile(
                "Median frames to clear",
                "${primary.medianA.roundToInt()} &rarr; ${primary.medianB.roundToInt()}",
                "${signedPct(primary.relativeDelta)} &middot; Mann-Whitney p = ${pval(primary.p)}"
            )
        }
        append("</div>\n")
    }

    private fun StringBuilder.tile(k: String, v: String, n: String) {
        append("<div class=\"tile\"><div class=\"k\">$k</div><div class=\"v\">$v</div><div class=\"n\">$n</div></div>")
    }

    private fun StringBuilder.appendLegend(r: AbReport) {
        append("<div class=\"legend\">")
        append("<span><i class=\"sw\" style=\"background:var(--series-a)\"></i>${esc(r.a.label)}</span>")
        append("<span><i class=\"sw\" style=\"background:var(--series-b)\"></i>${esc(r.b.label)}</span>")
        append("<span><i class=\"sw\" style=\"background:var(--good)\"></i>cleared</span>")
        append("<span><i class=\"sw\" style=\"background:var(--warning)\"></i>timeout</span>")
        append("<span><i class=\"sw\" style=\"background:var(--critical)\"></i>died</span>")
        append("</div>\n")
    }

    private fun StringBuilder.appendOutcomeChart(r: AbReport) {
        val w = 900.0
        val left = 96.0
        val plotW = w - left - 20
        val rowH = 34.0
        val h = 2 * rowH + 44

        append("<div class=\"card\"><h2>How each trial ended</h2>")
        append("<div class=\"scroll\"><svg viewBox=\"0 0 ${fmt(w)} ${fmt(h)}\" width=\"${fmt(w)}\" role=\"img\" ")
        append("aria-label=\"Trial outcomes for each arm\">")

        listOf(r.a, r.b).forEachIndexed { i, arm ->
            val y = 18 + i * rowH
            append(text(left - 10, y + 15, esc(arm.label), "end", "var(--ink-2)", 13))
            var x = left
            val order = listOf("complete" to "var(--good)", "timeout" to "var(--warning)", "dead" to "var(--critical)")
            val known = order.map { it.first }.toSet()
            val other = arm.outcomes.filterKeys { it !in known }.values.sum()
            val segments = order.map { (k, c) -> Triple(k, arm.outcomes[k] ?: 0, c) } +
                    Triple("other", other, "var(--muted)")
            for ((name, count, color) in segments) {
                if (count == 0) continue
                val segW = plotW * count / max(arm.n, 1)
                append(
                    "<rect x=\"${fmt(x)}\" y=\"${fmt(y)}\" width=\"${fmt(max(segW - 2, 1.0))}\" height=\"22\" " +
                            "rx=\"4\" fill=\"$color\" data-tip=\"${esc(arm.label)}: $count $name of ${arm.n}\"></rect>"
                )
                if (segW > 42) {
                    append(text(x + segW / 2 - 1, y + 15, "$count", "middle", "var(--surface)", 12, 600))
                }
                x += segW
            }
        }
        append(text(left, h - 8, "each bar is ${r.a.n} / ${r.b.n} trials, left to right: cleared, timed out, died",
            "start", "var(--muted)", 12))
        append("</svg></div></div>\n")
    }

    private fun StringBuilder.appendRateChart(r: AbReport) {
        val o = r.outcome
        val w = 900.0
        val left = 96.0
        val right = 108.0
        val plotW = w - left - right
        val groupH = 62.0
        val h = 3 * groupH + 54
        fun x(v: Double) = left + plotW * v

        append("<div class=\"card\"><h2>Outcome rates, with 95% intervals</h2>")
        append("<div class=\"scroll\"><svg viewBox=\"0 0 ${fmt(w)} ${fmt(h)}\" width=\"${fmt(w)}\" role=\"img\" ")
        append("aria-label=\"Clear, death and timeout rate for each arm with confidence intervals\">")

        for (t in 0..5) {
            val v = t / 5.0
            append("<line x1=\"${fmt(x(v))}\" y1=\"14\" x2=\"${fmt(x(v))}\" y2=\"${fmt(3 * groupH + 6)}\" ")
            append("stroke=\"var(--grid)\" stroke-width=\"1\"></line>")
            append(text(x(v), 3 * groupH + 24, "${(v * 100).roundToInt()}%", "middle", "var(--muted)", 11))
        }

        val groups = listOf(
            Triple("cleared", o.clearA to o.clearB, o.clearP),
            Triple("died", o.deathA to o.deathB, o.deathP),
            Triple("timed out", o.timeoutA to o.timeoutB, o.timeoutP)
        )
        groups.forEachIndexed { gi, (name, rates, p) ->
            val top = 20 + gi * groupH
            val mid = top + groupH / 2 - 10
            append(text(left - 10, mid + 4, name, "end", "var(--ink-2)", 13))
            listOf(rates.first to "var(--series-a)", rates.second to "var(--series-b)")
                .forEachIndexed { i, (rate, color) ->
                    val y = mid - 10 + i * 20
                    append("<line x1=\"${fmt(x(rate.ci.first))}\" y1=\"${fmt(y)}\" ")
                    append("x2=\"${fmt(x(rate.ci.second))}\" y2=\"${fmt(y)}\" ")
                    append("stroke=\"$color\" stroke-width=\"2\" stroke-linecap=\"round\"></line>")
                    append("<circle cx=\"${fmt(x(rate.rate))}\" cy=\"${fmt(y)}\" r=\"5\" fill=\"$color\" ")
                    append("stroke=\"var(--surface)\" stroke-width=\"2\" data-tip=\"${esc(rate.label)} $name: ")
                    append("${rate.count} of ${rate.n} (${pct(rate.rate)}), 95% CI ${pct(rate.ci.first)}")
                    append("-${pct(rate.ci.second)}\"></circle>")
                    if (rate.rate < 0.04) {
                        append(text(x(rate.rate) + 10, y + 4, pct(rate.rate), "start", "var(--ink)", 11, 600))
                    } else {
                        append(text(x(rate.rate), y - 9, pct(rate.rate), "middle", "var(--ink)", 11, 600))
                    }
                }
            append(text(left + plotW + 10, mid + 4, "p = ${pval(p)}", "start", "var(--ink-2)", 12))
        }

        append("<line x1=\"${fmt(left)}\" y1=\"${fmt(3 * groupH + 6)}\" x2=\"${fmt(left + plotW)}\" ")
        append("y2=\"${fmt(3 * groupH + 6)}\" stroke=\"var(--axis)\" stroke-width=\"1\"></line>")
        append("</svg></div>")
        append("<p class=\"note\">Bars are 95% Wilson intervals. Where two bars overlap, this sample ")
        append("does not establish a difference. p is Fisher's exact test on that outcome alone, so ")
        append("dying more and getting stuck more are tested separately.")
        if (o.deathA.count > 0 || o.deathB.count > 0) {
            append(" Deaths came a median of ${o.medianFramesBeforeDeathA.roundToInt()} frames in for ")
            append("${esc(r.a.label)} and ${o.medianFramesBeforeDeathB.roundToInt()} for ${esc(r.b.label)}.")
            val places = (o.deathPlacesA.keys + o.deathPlacesB.keys)
            if (places.size > 1) {
                append(" Death rooms: ")
                append(places.joinToString(", ") { room ->
                    "${RoomNames.label(room)} (${o.deathPlacesA[room] ?: 0}/${o.deathPlacesB[room] ?: 0})"
                })
                append(".")
            }
        }
        append("</p></div>\n")
    }

    private fun StringBuilder.appendStripChart(r: AbReport) {
        val comp = r.comparisons.firstOrNull { it.metric.name == "elapsedFrames" } ?: return
        val w = 900.0
        val left = 96.0
        val right = 24.0
        val plotW = w - left - right
        val rowH = 96.0
        val h = 2 * rowH + 56

        val allValues = comp.a + comp.b
        val maxV = niceMax(allValues.maxOrNull() ?: 1.0)
        fun x(v: Double) = left + plotW * (v / maxV)

        append("<div class=\"card\"><h2>Frames to clear the room, one dot per cleared trial</h2>")
        append("<div class=\"scroll\"><svg viewBox=\"0 0 ${fmt(w)} ${fmt(h)}\" width=\"${fmt(w)}\" role=\"img\" ")
        append("aria-label=\"Distribution of frames to clear for each arm\">")

        val ticks = 6
        for (t in 0..ticks) {
            val v = maxV * t / ticks
            append("<line x1=\"${fmt(x(v))}\" y1=\"14\" x2=\"${fmt(x(v))}\" y2=\"${fmt(2 * rowH + 10)}\" " +
                    "stroke=\"var(--grid)\" stroke-width=\"1\"></line>")
            append(text(x(v), 2 * rowH + 28, "${v.roundToInt()}", "middle", "var(--muted)", 11))
            append(text(x(v), 2 * rowH + 42, "${fmt1(v / 60.0)}s", "middle", "var(--muted)", 11))
        }

        listOf(
            Triple(r.a.label, comp.a, "var(--series-a)"),
            Triple(r.b.label, comp.b, "var(--series-b)")
        ).forEachIndexed { i, (label, values, color) ->
            val top = 20 + i * rowH
            val band = rowH - 34
            append(text(left - 10, top + band / 2 + 4, esc(label), "end", "var(--ink-2)", 13))

            values.sorted().forEachIndexed { k, v ->
                val jitter = ((k * 37 % 17) / 16.0 - 0.5) * (band - 14)
                append(
                    "<circle cx=\"${fmt(x(v))}\" cy=\"${fmt(top + band / 2 + jitter)}\" r=\"5\" " +
                            "fill=\"$color\" fill-opacity=\"0.75\" stroke=\"var(--surface)\" stroke-width=\"2\" " +
                            "data-tip=\"${esc(label)}: ${v.roundToInt()} frames (${fmt1(v / 60.0)}s)\"></circle>"
                )
            }
            val med = values.median()
            append("<line x1=\"${fmt(x(med))}\" y1=\"${fmt(top - 2)}\" x2=\"${fmt(x(med))}\" " +
                    "y2=\"${fmt(top + band + 2)}\" stroke=\"var(--ink)\" stroke-width=\"2\"></line>")
            append(text(x(med), top - 6, "median ${med.roundToInt()}", "middle", "var(--ink)", 11, 600))
        }

        append("<line x1=\"${fmt(left)}\" y1=\"${fmt(2 * rowH + 10)}\" x2=\"${fmt(left + plotW)}\" " +
                "y2=\"${fmt(2 * rowH + 10)}\" stroke=\"var(--axis)\" stroke-width=\"1\"></line>")
        append("</svg></div>")

        val ciSpansZero = r.bootstrapLow <= 0 && r.bootstrapHigh >= 0
        append("<p class=\"note\">Median difference (${esc(r.b.label)} &minus; ${esc(r.a.label)}): ")
        append("<strong>${r.bootstrapLow.roundToInt()} to ${r.bootstrapHigh.roundToInt()} frames</strong> ")
        append("(95% bootstrap CI, ${fmt1(r.bootstrapLow / 60)}s to ${fmt1(r.bootstrapHigh / 60)}s). ")
        append(if (ciSpansZero) "The interval spans zero, so this sample does not show a difference."
        else "The interval excludes zero.")
        append("</p></div>\n")
    }

    private fun StringBuilder.appendTable(r: AbReport) {
        append("<div class=\"card\"><h2>All comparable metrics, cleared trials only</h2>")
        append("<div class=\"scroll\"><table><thead><tr>")
        append("<th>Metric</th><th>${esc(r.a.label)} median</th><th>IQR</th>")
        append("<th>${esc(r.b.label)} median</th><th>IQR</th><th>Change</th><th>p</th>")
        append("</tr></thead><tbody>")
        for (c in r.comparisons) {
            append("<tr><td>${esc(c.metric.label)}</td>")
            append("<td>${fmtMetric(c.medianA)}</td><td>${fmtMetric(c.iqrA.first)} &ndash; ${fmtMetric(c.iqrA.second)}</td>")
            append("<td>${fmtMetric(c.medianB)}</td><td>${fmtMetric(c.iqrB.first)} &ndash; ${fmtMetric(c.iqrB.second)}</td>")
            append("<td>${signedPct(c.relativeDelta)}</td><td>${pval(c.p)}</td></tr>")
        }
        append("</tbody></table></div>")
        append("<p class=\"note\">Deaths and timeouts excluded. Read the clear rate first: ")
        append("a faster arm that dies more is not better.</p></div>\n")
    }

    private fun StringBuilder.appendFooter(r: AbReport) {
        append("<div class=\"card\"><h2>Reading this</h2>")
        append("<p class=\"note\">Sensitivity: with these samples the design detects a change in frames ")
        append("to clear of about <strong>${pct(r.detectableEffect)}</strong> or larger ")
        append("(alpha 0.05, power 0.8). Smaller real effects will not reach significance here.</p>")
        append("<p class=\"note\">Source: <code>${esc(r.source)}</code></p></div>\n")
    }

    private fun text(x: Double, y: Double, s: String, anchor: String, fill: String,
                     size: Int, weight: Int = 400) =
        "<text x=\"${fmt(x)}\" y=\"${fmt(y)}\" text-anchor=\"$anchor\" fill=\"$fill\" " +
                "font-size=\"$size\" font-weight=\"$weight\" font-family=\"inherit\">$s</text>"

    private fun niceMax(v: Double): Double {
        if (v <= 0) return 1.0
        val mag = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(v)))
        return ceil(v / (mag / 2)) * (mag / 2)
    }

    private fun fmtMetric(v: Double) = if (abs(v) < 10 && v != v.roundToInt().toDouble()) fmt2(v) else "${v.roundToInt()}"
    private fun fmt(v: Double) = if (v == v.roundToInt().toDouble()) "${v.roundToInt()}" else String.format("%.1f", v)
    private fun fmt1(v: Double) = String.format("%.1f", v)
    private fun fmt2(v: Double) = String.format("%.2f", v)
    private fun pct(v: Double) = if (v.isNaN()) "-" else "${(v * 100).roundToInt()}%"
    private fun signedPct(v: Double) = if (v.isNaN()) "-" else
        "${if (v > 0) "+" else ""}${(v * 100).roundToInt()}%"

    private fun pval(p: Double) = when {
        p.isNaN() -> "n/a"
        p < 0.0001 -> "&lt;0.0001"
        else -> String.format("%.4f", p)
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")
}
