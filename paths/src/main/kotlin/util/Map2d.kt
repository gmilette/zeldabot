package util

import bot.state.FramePoint
import bot.state.map.MapConstants
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter

class Map2d<T>(
    val map: List<MutableList<T>>
) {
    val maxX: Int
        get() = map[0].size

    val maxY: Int
        get() = map.size

    fun copy(): Map2d<T> {
        return this.map { it }
    }
    class Builder<T>(
        private val buildMap: MutableList<MutableList<T>> = mutableListOf()
    ) {
        fun add(numRows: Int, sizeOfRow: Int, value: T): Builder<T> {
            val empty = mutableListOf<T>()
            repeat(sizeOfRow) {
                empty.add(value)
            }

            repeat(numRows) {
                buildMap.add(empty)
            }

            return this
        }

        fun build(): Map2d<T> =
            Map2d(buildMap)
    }

    val empty: Boolean
        get() = map.isEmpty() || map.size == 1

    fun <V> map(transform: (T) -> V): Map2d<V> {
        // change to values
        return Map2d(map.map { it.map { toT -> transform(toT) }.toMutableList() })
    }

    fun <V> mapXy(transform: (Int, Int) -> V): Map2d<V> {
        // change to values
        return Map2d(map.mapIndexed{ x, r -> r.mapIndexed { y, c -> transform(x, y) }.toMutableList() })
    }

    fun modify(point: FramePoint, size: Int = 16, how: (T) -> T) {
        for (y in point.y..point.y + size) {
            for (x in point.x..point.x + size) {
                try {
                    val newVal = how(map[y][x])
                    map[y][x] = newVal
                } catch (e: IndexOutOfBoundsException) {
                    // just ignore this for now
                }
            }
        }
    }

    fun get(point: FramePoint): T =
        get(point.x, point.y)

    // - 61?
    fun get(x: Int, y: Int): T =
        map[(y).coerceIn(0, MapConstants.MAX_Y)][x.coerceIn(0, MapConstants
            .MAX_X)]

    fun write(name: String, render: (T, x: Int, y: Int) -> String) {
        val csvWriter2 = CsvWriter()
        csvWriter2.open("map_${name}.csv", false) {
            // how many rows to read for the first set
            map.forEachIndexed { y, row ->
                val rowData = mutableListOf<String>()
                row.forEachIndexed { x, item ->
                    rowData.add(render(item, x, y))
                }
                writeRow(rowData)
            }
            this.close()
        }
    }

}