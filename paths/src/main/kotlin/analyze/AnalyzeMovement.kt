package analyze

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import java.io.File

class AnalyzeMovement {
    private val file = ""

    fun go() {
        val rows: List<List<String>> = CsvReader().readAll(file)

    }
}