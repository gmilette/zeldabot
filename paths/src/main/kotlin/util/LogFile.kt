package util

import bot.DirectoryConstants
import bot.plan.runner.now
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter

class LogFile(fileNameRoot: String, private val addDate: Boolean = false, private val devType: Boolean = false) {
    private val enabled: Boolean
        get() = if (devType) DirectoryConstants.enableDebug else DirectoryConstants.enableInfo
    val outputFileName = DirectoryConstants.file("zlog",
        "${fileNameRoot}_${System.currentTimeMillis()}.txt")

    fun write(vararg message: Any) {
        if (enabled) {
            CsvWriter().open(outputFileName, true) {
                if (addDate) {
                    writeRow(now(), *message)
                } else {
                    writeRow(message)
                }
            }
        }
    }
}