package util

import bot.DirectoryConstants
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter

class LogFile(fileNameRoot: String) {
    val outputFileName = DirectoryConstants.file("zlog",
        "${fileNameRoot}_${System.currentTimeMillis()}.txt")

    fun write(vararg message: Any) {
        CsvWriter().open(outputFileName, true) {
            writeRow(message.asList())
        }
    }
}