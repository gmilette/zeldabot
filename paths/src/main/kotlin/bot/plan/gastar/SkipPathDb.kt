package bot.plan.gastar

import bot.plan.PreviousMove
import bot.state.FramePoint
import bot.state.MapLoc
import com.github.doyaaaaaken.kotlincsv.client.CsvFileWriter
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import nintaco.gui.hexeditor.SearchQuery.Direction
import java.io.File

class SkipPathDb {
    // rotating list
    private val previousMoves = mutableListOf<PreviousMove>()

    private var previousMove: PreviousMove? = null
    private var previousMove2: PreviousMove? = null
    private var previousMove3: PreviousMove? = null

    //    private val writer: CsvFileWriter
    private val filename = "skips${File.separator}skips.csv"

    init {
        // only if this thing is empty do that
        if (!File(filename).exists()) {
            val csvWriter2 = CsvWriter()
            csvWriter2.open(filename, false) {
                this.writeRow(
                    "level", "loc", "move",
                    "actual",
                    "from", "fromDir",
                    "before1", "before1Dir",
                    "before2", "before2Dir",
                    "before3", "before3Dir",
                    "diff", "diffx", "diffy", "diffAlert"
                )
            }
        }
    }
    // https://github.com/Kotlin/dataframe
    fun save(level: Int, mapLoc: MapLoc, previous: PreviousMove) {
        if (previousMove == null || previousMove2 == null || previousMove3 == null ) {
            // rotate
            previousMove3 = previousMove2
            previousMove2 = previousMove
            previousMove = previous
            return
        }
        val prev = previousMove ?: return
        val prev2 = previousMove2 ?: return
        val prev3 = previousMove3 ?: return
//        val prevPrev = previous.previous.previous ?: return
        // skip whenever i was hit?
        if (true) {
            CsvWriter().open(filename, true) {
                this.writeRow(
                    level,
                    mapLoc,
                    previous.move.name,
                    // current move
                    previous.actual.oneStr, // tried to move to here
                    previous.from.oneStr, previous.dir, // but ended up here
                    previous.actual.oneStr,
                    prev.from.oneStr, prev.dir,
                    prev2.from.oneStr, prev2.dir,
                    prev3.from.oneStr, prev3.dir,
                    previous.distOff, previous.distOffx, previous.distOffy, previous.distAlert,
                )
            }
        }
        // rotate
        previousMove3 = previousMove2
        previousMove2 = previousMove
        previousMove = previous
    }
}