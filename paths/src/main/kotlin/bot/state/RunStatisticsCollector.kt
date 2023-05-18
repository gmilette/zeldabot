package bot.state

class RunStatisticsCollector {
    private var startMillis = 0L
    private var totalFrames = 0

    fun start() {
        startMillis = System.currentTimeMillis()
    }

    fun collect(state: FrameStateUpdater) {
        //
        totalFrames++
    }
}