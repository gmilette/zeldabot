package util

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter

class LogFile(private val fileNameRoot: String) {
    private val experimentRoot = "../../zlog/"
    val outputFileName = "${experimentRoot}/${fileNameRoot}_${System.currentTimeMillis()}.txt"

    fun write(vararg message: String) {
        CsvWriter().open(outputFileName, true) {
            writeRow(message.asList())
        }
    }
}