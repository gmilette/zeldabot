data class Destination(
    val point: Point,
    val type: DestType
)

data class Point(val x: Int, val y: Int, val data: String) {
}

sealed class DestType {
    data class LEVEL(val which: Int): DestType()
    data class ITEM(val name: String): DestType()
    data class SHOP(val wares: List<String> = listOf()): DestType()
    data class SECRET_TO_EVERYBODY(val rupees: Int): DestType()
}