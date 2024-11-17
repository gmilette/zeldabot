package bot

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DirectoryConstants {
    const val enable = false

    private val botOutputDir = "botoutput"

    private val outDir = "..${File.separator}..${File.separator}$botOutputDir${File.separator}".also {
        File(it).mkdirs()
    }

    // run output vs. global output
    fun outDir(dir: String) = "$outDir$dir${File.separator}".also {
        File(it).mkdirs()
    }

    fun file(dir: String, filename: String) = "${outDir(dir)}$filename"

    val states = "../Nintaco_bin_2020-05-01/states/"
    val statesFromExecuting = "../$states"

    private fun generateDateFilename(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        return now.format(formatter)
    }
}