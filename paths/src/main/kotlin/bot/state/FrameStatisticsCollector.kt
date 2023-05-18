package bot.state

class FrameStatisticsCollector {
    private var startMillis = 0L
    private var totalFrames = 0

    private val eventList: List<String> = mutableListOf()

    fun start() {
        startMillis = System.currentTimeMillis()
    }

    fun collect(state: FrameStateUpdater) {
        //
        totalFrames++
    }
}