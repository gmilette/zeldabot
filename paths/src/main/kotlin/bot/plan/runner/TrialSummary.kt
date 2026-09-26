package bot.plan.runner

data class TrialSummary(
    val date: String,
    val label: String,
    val gitSha: String,
    val experiment: String,
    val file: String,
    val trial: Int,
    val start: String,
    val result: String,
    val percent: Int,
    val end: String,
    val elapsedFrames: Int,
    val totalFrames: Int,
    val heartsStart: Double,
    val heartsEnd: Double,
    val netHeartsLost: Double,
    val damagedEvents: Int,
    val damagedFrames: Int,
    val bombsUsed: Int,
    val rupees: Int,
    val rawHits: Int,
    val rawDamage: Double,
    val rawHeal: Double,
    val sword: String,
    val ring: String,
    val bombs: Int,
    val boom: String,
    val shield: Boolean
) {
    val cleared get() = result == "complete"
    val damagedFraction get() = if (totalFrames > 0) damagedFrames.toDouble() / totalFrames else 0.0
}
