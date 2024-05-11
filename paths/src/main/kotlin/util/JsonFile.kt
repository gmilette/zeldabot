package util

import bot.DirectoryConstants
import bot.state.MapCoordinates
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter

class JsonFile(private val root: String, private val type: String) {
    companion object {
        val gson = GsonBuilder().setPrettyPrinting().create()
//        fun makeMapJsonFile(mapCoordinates: MapCoordinates, type: String): JsonFile {
//            val name = "${mapCoordinates.level}_${mapCoordinates.loc}"
//            return JsonFile(name, type)
//        }
    }

    constructor(mapCoordinates: MapCoordinates, type: String): this("${mapCoordinates.level}_${mapCoordinates.loc}", type)

    fun fileName(): String {
        val fileName = "${root}_${type}.json"
        return DirectoryConstants.file(type, fileName)
    }

    inline fun <reified T> read(): T? {
        val file = File(fileName())
        if (!file.exists()) {
            return null
        }
        val json = file.readText()
        return gson.fromJson(json, T::class.java)
    }

    fun <T> write(data: T) {
        write(gson.toJson(data))
    }

    private fun write(data: String) {
        val writer = FileWriter(fileName())
        writer.write(data)
        writer.close()
    }
}