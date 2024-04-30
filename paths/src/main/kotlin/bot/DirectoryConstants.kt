package bot

import java.io.File

object DirectoryConstants {
    val outDir = "..${File.separator}..${File.separator}botoutput${File.separator}".also {
        File(it).mkdirs()
    }

    fun outDir(dir: String) = "$outDir$dir${File.separator}".also {
        File(it).mkdirs()
    }

    fun file(dir: String, filename: String) = "${outDir(dir)}$filename"

    val states = "../Nintaco_bin_2020-05-01/states/"
}