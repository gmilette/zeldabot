package bot.state

data class MapCellPoint(val x: Int, val y: Int) {
    override fun toString(): String {
        return "$x, $y"
    }
}
