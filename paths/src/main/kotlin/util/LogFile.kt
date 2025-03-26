package util

import bot.DirectoryConstants
import bot.plan.runner.now
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter

class LogFile(fileNameRoot: String, private val addDate: Boolean = false) {
    val outputFileName = DirectoryConstants.file("zlog",
        "${fileNameRoot}_${System.currentTimeMillis()}.txt")

    fun write(vararg message: Any) {
        CsvWriter().open(outputFileName, true) {
            if (addDate) {
                writeRow(now(), *message)
            } else {
                writeRow(message)
            }
        }
    }
}