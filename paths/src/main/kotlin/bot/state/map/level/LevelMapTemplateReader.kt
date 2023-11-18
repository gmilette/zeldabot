package bot.state.map.level

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import util.d

/**
 * read in the map templates
 */
class LevelMapTemplateReader {
    val templates = mutableMapOf<String, LevelTemplate>()

    enum class Temp {
        lev_block1mid,
        lev_block4mid,
        lev_block4out,
        lev_block4out2,
        lev_blockcenter,
        lev_blockside2,
        lev_dragon_right,
        lev_empty,
        lev_grid,
        lev_grid_center,
        lev_maze,
        lev_maze_path,
        lev_stairs_center,
        lev_stairs_center_blocked,
        lev_triforce,
        lev_getitem,
        lev_getitem_move,
        lev_corner,
        lev_cross,
        lev_stair_side,
        lev_u,
        lev_block2center,
        lev_corner_bottom4,
        lev_dragon_top,
        lev_water_center,
        lev_water_center_path,
        lev_water_maze,
        lev_water_maze_path,
        lev_water_line,
        lev_water_line_path,
        lev_water_circle,
        lev_water_circle_path,
        lev_water_line_right,
        lev_water_line_right_path,
        lev_triple_line,
        lev_side_angle,
        lev_spiral,
        lev_princess,
        lev_gannon,
        lev_water_round_center
    }

    init {
        d { " read the templates "}

        readAll()
    }

    private fun readAll() {
        for (template in Temp.values()) {
            val levTemplate = readTemplate(template.name)
            templates[template.name] = levTemplate
        }
    }

    private fun readTemplate(templateName: String): LevelTemplate {
        val fileContent = this::class.java.classLoader.getResource(templateName)?.readText() ?: throw IllegalArgumentException("can't " +
                "find $templateName")
        val rows: List<List<String>> = CsvReader().readAll(fileContent)
        //map { it.asSequence().map { it != 'X' }
        val row = rows.map { it.first() }.toMutableList()
        return LevelTemplate(templateName, row)
    }
}