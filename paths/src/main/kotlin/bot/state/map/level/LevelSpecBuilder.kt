package bot.state.map.level

import bot.plan.InLocations
import bot.state.*
import bot.state.map.Direction
import bot.state.map.ExitSet
import util.d

class LevelSpecBuilder {
    val u = Direction.Up
    val r = Direction.Right
    val d = Direction.Down
    val l = Direction.Left
    val upRight = e(u, r)
    val upDown = e(u, d)
    val eall = e(u, d, l, r)

    companion object {
        const val DEBUG = false
        const val getItemLoc: MapLoc = 127
        const val getItemLoc3: MapLoc = 15
        const val getItemLoc4: MapLoc = 96
        const val getItemLoc5: MapLoc = 88
        const val getItemLoc6: MapLoc = 117
        const val getItemMove6: MapLoc = 8
        const val getItemLoc7: MapLoc = 74
        const val getMoveLoc: MapLoc = 123
        const val getItemLoc8: MapLoc = 111 // book
        const val getItemLoc8Key: MapLoc = 15
        const val getItemLoc8Go: MapLoc = 47
        object Nine {
            const val travel1: MapLoc = 96
            const val travel2: MapLoc = 112
            const val travel3: MapLoc = 117
            const val travel4: MapLoc = 103
            const val travel5: MapLoc = 119
            const val redRing: MapLoc = 0
            const val silverArrow: MapLoc = 79
        }
        val getItemLoc5Item: MapLoc = InLocations.Level5.mapLocGetItem
    }

    fun e(vararg dir: Direction): ExitSet {
        return ExitSet(*dir)
    }

    //https://nesmaps.com/maps/Zelda/ZeldaLevel1Q1.html

    fun buildLevel1(): List<LevelSpec> {
        // .. build it
        val start: MapLoc = LevelStartMapLoc.lev(1)
        val split = start.up.up.left.up.right
        val specs = mutableListOf<LevelSpec>(
            LevelSpec(start, eall, "start", LevelMapTemplateReader.Temp.lev_grid),
            LevelSpec(start.left, e(r), "", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(start.right, e(l), "", LevelMapTemplateReader.Temp.lev_blockside2),
            LevelSpec(start.up, upDown, "", LevelMapTemplateReader.Temp.lev_blockcenter),
            LevelSpec(start.up.up, e(d, l, r, u), "", LevelMapTemplateReader.Temp.lev_block4mid), // i bomb up
            LevelSpec(start.up.up.left, e(r, u), "", LevelMapTemplateReader.Temp.lev_blockcenter),
            LevelSpec(start.up.up.left.up, e(d, r), "", LevelMapTemplateReader.Temp.lev_block1mid),
            LevelSpec(split, e(l, u, r), "", LevelMapTemplateReader.Temp.lev_block4out2),
            // go up to get arrow
            LevelSpec(split.up, e(d, u), "", LevelMapTemplateReader.Temp.lev_maze),
            LevelSpec(split.up.up, e(l, r, d), "", LevelMapTemplateReader.Temp.lev_water_center),
            LevelSpec(split.up.up.left, e(r), "", LevelMapTemplateReader.Temp.lev_stairs_center),
            LevelSpec(split.up.up.left.down, e(u), "", LevelMapTemplateReader.Temp.lev_getitem),

            // go right to get triforce
            LevelSpec(split.right, e(l, r), "", LevelMapTemplateReader.Temp.lev_block4mid),
            LevelSpec(split.right.right, e(l, u), "", LevelMapTemplateReader.Temp.lev_grid_center),

            LevelSpec(split.right.right.up, e(d, r), "", LevelMapTemplateReader.Temp.lev_dragon_right),
            LevelSpec(split.right.right.up.right, e(l, d), "", LevelMapTemplateReader.Temp.lev_triforce),

            LevelSpec(getItemLoc, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
        )

        log(specs, 1)

        return specs
    }

    fun buildLevel2(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(2)
        val grid = start.up.right

        val specs = mutableListOf(
            LevelSpec(start, e(u, d, r), "start", LevelMapTemplateReader.Temp.lev_grid),
            // key
            LevelSpec(start.right, e(l, u), "right guys", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(start.up, e(l, u, r), "up", LevelMapTemplateReader.Temp.lev_block4out),
            //key
            LevelSpec(start.up.left, e(r), "moreguys", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(grid, eall, "gridguys", LevelMapTemplateReader.Temp.lev_grid_center),
            LevelSpec(grid.up, e(u,d,r), "not", LevelMapTemplateReader.Temp.lev_block4out2),
            LevelSpec(grid.up.up, e(u,d,r), "lockback", LevelMapTemplateReader.Temp.lev_block4mid),
            LevelSpec(grid.up.up.right, eall, "boomerangkill", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(grid.up.up.up, e(u,d,r), "sand", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(grid.up.up.up.up, e(u,d,r), "blocked need to kill", LevelMapTemplateReader.Temp.lev_blockside2),
//            LevelSpec(grid.up.up.up.up, e(u,d,r), "blocked need to kill", LevelMapTemplateReader.Temp.lev_blockcenter),
            LevelSpec(grid.up.up.up.up.up, e(u,d,r), "enddrid", LevelMapTemplateReader.Temp.lev_grid_center),
            LevelSpec(grid.up.up.up.up.up.up, e(d,l), "boss", LevelMapTemplateReader.Temp.lev_empty), //?
            LevelSpec(grid.up.up.up.up.up.up.left, e(r, d), "triforce", LevelMapTemplateReader.Temp.lev_triforce),
        )

        log(specs, 2)

        return specs
    }

    fun buildLevel3(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(3)
        val inStart = start.left.up.up
        val specs = mutableListOf(
            LevelSpec(start, e(l), "start", LevelMapTemplateReader.Temp.lev_grid),
            // key
            LevelSpec(start.left, e(r, u), "fkey and squishy", LevelMapTemplateReader.Temp.lev_block4out2),
            LevelSpec(start.left.up, e(u, d), "key cross", LevelMapTemplateReader.Temp.lev_cross),
            //key
            LevelSpec(inStart, eall, "guys and bomb", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(inStart.left, e(u,r,l), "compass", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(inStart.left.left, e(u,d,r), "sword", LevelMapTemplateReader.Temp.lev_blockside2),
            LevelSpec(inStart.left.left.down, e(u,d,r), "stair to raft", LevelMapTemplateReader.Temp.lev_stair_side),
            LevelSpec(inStart.left.left.up, e(d,r), "free key with suns", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(inStart.up, eall, "u with squishy and key", LevelMapTemplateReader.Temp.lev_u),
            LevelSpec(inStart.up.right, e(l,u,r), "swordpass through", LevelMapTemplateReader.Temp.lev_empty),
//            LevelSpec(inStart.up.right.up, e(l,u,r), "squishbomb", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(inStart.up.right.right, e(l,d,u), "boss", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(inStart.up.right.right.up, e(l,d,u), "boss", LevelMapTemplateReader.Temp.lev_triforce),

            LevelSpec(getItemLoc3, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
       )

        log(specs, 3)
        return specs
    }

    fun buildLevel4(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(4)
        val laddercross = start.up.up.left.up.up
        val specs = mutableListOf(
            LevelSpec(start, e(l, u, d), "start", LevelMapTemplateReader.Temp.lev_grid),
            // key
            LevelSpec(start.left, e(r), "bat and key", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(start.up, e(u, d, r), "best four", LevelMapTemplateReader.Temp.lev_block4mid),
            LevelSpec(start.up.up, e(d, l), "bat key", LevelMapTemplateReader.Temp.lev_block4out),
            LevelSpec(start.up.up.left, e(r, u), "cross beast", LevelMapTemplateReader.Temp.lev_cross),
            LevelSpec(start.up.up.left.up, e(d, u), "squish key", LevelMapTemplateReader.Temp.lev_water_center),
            LevelSpec(laddercross, e(d, r, u), "ladder cross", LevelMapTemplateReader.Temp.lev_water_center),

            LevelSpec(laddercross.right, e(l, r, u), "beast maze", LevelMapTemplateReader.Temp.lev_maze, isHalfPassable = false),
            LevelSpec(laddercross.right.right, e(l, r, u), "ladder entry", LevelMapTemplateReader.Temp.lev_block2center),

            LevelSpec(laddercross.up, e(u, r, d), "beasts", LevelMapTemplateReader.Temp.lev_water_center_path),
            LevelSpec(laddercross.up.up, e(u, r), "fourmonster", LevelMapTemplateReader.Temp.lev_corner_bottom4),
            LevelSpec(laddercross.up.up.right, e(u, d, l, r), "money", LevelMapTemplateReader.Temp.lev_empty),
            //LevelSpec(laddercross.up.up.right.up, e(d, l, r), "money up batkey", LevelMapTemplateReader.Temp.lev_water_maze),
            LevelSpec(laddercross.up.up.right.right, e(u, d, l, r), "before dragon", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(laddercross.up.up.right.right.right, e(l, u), "dragon", LevelMapTemplateReader.Temp.lev_dragon_top),
            LevelSpec(laddercross.up.up.right.right.right.up, e(l, u, d), "triforce 4", LevelMapTemplateReader.Temp.lev_triforce),

            LevelSpec(getItemLoc4, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
        )

        log(specs, 4)
        return specs
    }

    fun buildLevel5(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(5)
        val watermaze = start.up.up.right
        val farup = watermaze.up.up.up
        val specs = mutableListOf(
            LevelSpec(start, e(r, u, d), "start", LevelMapTemplateReader.Temp.lev_grid),
            LevelSpec(start.up, e(l, u, r, d), "mid river bomb left", LevelMapTemplateReader.Temp.lev_water_line_path, isHalfPassable = false),
            LevelSpec(start.up.left, e(l, u, r), "zombie bomb left", LevelMapTemplateReader.Temp.lev_block4mid),
            LevelSpec(start.up.left.left, e(u, r), "stairs down", LevelMapTemplateReader.Temp.lev_stairs_center),

            LevelSpec(start.up.up.up, e(u, d, l, r), "circlepond", LevelMapTemplateReader.Temp.lev_water_round_center, isHalfPassable = false),

            LevelSpec(start.up.up, e(u, d, l, r), "dinos", LevelMapTemplateReader.Temp.lev_empty),
            // i think we remove the past??
//            LevelSpec(watermaze, e(u, l), "maze sq", LevelMapTemplateReader.Temp.lev_maze_path, isHalfPassable = false),
            LevelSpec(watermaze, e(u, l), "maze sq", LevelMapTemplateReader.Temp.lev_maze, isHalfPassable = false),

            LevelSpec(watermaze.up, e(u, l, d), "zombie center", LevelMapTemplateReader.Temp.lev_water_center_path, isHalfPassable = false),
            LevelSpec(watermaze.up.up, e(u, d), "maze 2", LevelMapTemplateReader.Temp.lev_maze_path, isHalfPassable = false),
            LevelSpec(farup, e(u, l, d), "water center", LevelMapTemplateReader.Temp.lev_water_center_path, isHalfPassable = false),

            LevelSpec(farup.left, e(l, r), "water center", LevelMapTemplateReader.Temp.lev_water_circle_path, isHalfPassable = false),
            LevelSpec(farup.left.left, e(l, r), "line bunnies", LevelMapTemplateReader.Temp.lev_triple_line),
            LevelSpec(farup.left.left.left, e(u, r, d), "whistle guy", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(farup.left.left.left.up, e(d), "triforce", LevelMapTemplateReader.Temp.lev_triforce),

            LevelSpec(farup.left.up.up, e(l, d), "get stuff", LevelMapTemplateReader.Temp.lev_stairs_center),
            LevelSpec(farup.left.up.up.left, e(r), "push spot", LevelMapTemplateReader.Temp.lev_block1mid),

//            LevelSpec(getItemLoc4, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            //LevelSpec(5, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isGetItem = true),
            LevelSpec(getItemLoc5, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isGetItem = true),
            LevelSpec(7, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isGetItem = true),
            LevelSpec(getItemLoc5Item, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
        )

        log(specs, 5)

        return specs
    }

    fun buildLevel6(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(6)
        val splitSpot = start.left.up.up.up.up.up.right
        val stairsRight = splitSpot.up.right.right.right.right
        val specs = mutableListOf(
            LevelSpec(start, e(l, r), "start", LevelMapTemplateReader.Temp.lev_grid),
            LevelSpec(start.right, e(l), "free key", LevelMapTemplateReader.Temp.lev_block4out),
            LevelSpec(start.left, e(u, r), "intro ghosts", LevelMapTemplateReader.Temp.lev_block4out2),
            LevelSpec(start.left.up, e(u, d), "inbetween", LevelMapTemplateReader.Temp.lev_blockside2),
            LevelSpec(start.left.up.up, e(u, d), "blocked arrow guy", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(start.left.up.up.up, e(u, d), "side angles", LevelMapTemplateReader.Temp.lev_side_angle),
            LevelSpec(start.left.up.up.up.up, e(u, d), "blocked two", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(start.left.up.up.up.up.up, e(u, d, r), "grid bomb right", LevelMapTemplateReader.Temp.lev_grid_center),
            LevelSpec(splitSpot, e(u, d), "split", LevelMapTemplateReader.Temp.lev_water_circle_path),
            LevelSpec(splitSpot.up, e(u, d, l, r), "split up", LevelMapTemplateReader.Temp.lev_water_line_right_path),
            LevelSpec(splitSpot.up.up, e(d), "two push", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(splitSpot.down, e(u, r), "split down", LevelMapTemplateReader.Temp.lev_water_line_right_path),
            LevelSpec(splitSpot.down.right, e(l), "right ghost move", LevelMapTemplateReader.Temp.lev_block1mid),

            LevelSpec(stairsRight, e(d), "Stairs out", LevelMapTemplateReader.Temp.lev_stair_side),
            LevelSpec(stairsRight.down, e(u, l), "water after stairs", LevelMapTemplateReader.Temp.lev_water_line_right),
            LevelSpec(stairsRight.down.left, e(d, u, r), "bad guy", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(stairsRight.down.left.up, e(d, u), "arrow guy boss", LevelMapTemplateReader.Temp.lev_corner_bottom4),
            LevelSpec(stairsRight.down.left.up.up, e(d), "lev 6 triforce", LevelMapTemplateReader.Temp.lev_triforce),

            LevelSpec(getItemLoc6, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(getItemMove6, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isGetItem = true, isHalfPassable = false),
        )

        log(specs, 6)

        return specs
    }

    fun buildLevel7(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(7)
        val grumble = start.up.up.up.up.left.up
        val specs = mutableListOf(
            LevelSpec(start, e(l, u, d), "start", LevelMapTemplateReader.Temp.lev_grid),
            LevelSpec(start.up, e(l, u, d, r), "bomb skip", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(start.up.up, e(l, u, d, r), "dudes with boomerange", LevelMapTemplateReader.Temp.lev_block4mid),
            // gets stuck sometimes so set to true
            LevelSpec(start.up.up.up, e(u, d), "cross river", LevelMapTemplateReader.Temp.lev_water_line_path, isHalfPassable = false), // i think should be false not true //
            LevelSpec(start.up.up.up.up, e(l, r, d), "whistle", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(start.up.up.up.up.left, e(u, r), "dudes before grumble", LevelMapTemplateReader.Temp.lev_grid_center),
            LevelSpec(grumble, e(u, l, r, d), "grumble", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(grumble.up, e(l, r, d), "maze", LevelMapTemplateReader.Temp.lev_water_maze_path, isHalfPassable = false),

            LevelSpec(grumble.up.right, e(l, r), "dude bomb", LevelMapTemplateReader.Temp.lev_block4out2),
            LevelSpec(grumble.up.right.right, e(l, r), "red candle", LevelMapTemplateReader.Temp.lev_stairs_center_blocked),
            LevelSpec(grumble.up.right.right.right, e(l, r, u), "guys with bomb", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(grumble.up.right.right.right.right, e(l, u), "whistle guy", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(grumble.up.right.right.right.right.up, e(d, r), "block bomb right", LevelMapTemplateReader.Temp.lev_block2center),
            // purposely do not show a left exit to encourage link not to exit the level
            LevelSpec(grumble.up.right.right.right.right.up.right, e(r), "u shape stair", LevelMapTemplateReader.Temp.lev_u),

            LevelSpec(grumble.right, e(l, r), "end stair", LevelMapTemplateReader.Temp.lev_stairs_center_blocked),
            LevelSpec(grumble.right.right, e(l, r), "dragon", LevelMapTemplateReader.Temp.lev_dragon_right),
            LevelSpec(grumble.right.right.right, e(l, r, d), "dragon", LevelMapTemplateReader.Temp.lev_triforce),

            LevelSpec(getItemLoc4, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(getItemLoc7, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(getItemLoc5, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isGetItem = true),

            LevelSpec(getMoveLoc, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isGetItem = true),
        )

        log(specs, 7)

        return specs
    }

    fun buildLevel8(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(8)
        val masterbattle = start.up.up.up.up
        val specs = mutableListOf(
            LevelSpec(start, e(l, u, d, r), "start", LevelMapTemplateReader.Temp.lev_grid),
            LevelSpec(start.left, e(l, r), "bomb dude", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(start.left.left, e(l, r), "stair to book", LevelMapTemplateReader.Temp.lev_stairs_center),

            LevelSpec(start.up, e(l, u, d, r), "star dude", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(start.up.up, e(l, u, d, r), "mixed battle", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(start.up.up.up, e(l, u, d, r), "mixed battle2", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(masterbattle, e(u, d, r), "master battle", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(masterbattle.up, e(l, u, d, r), "four bomb guy", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(masterbattle.up.up, e(l, u, d, r), "arrow guy", LevelMapTemplateReader.Temp.lev_corner),
            LevelSpec(masterbattle.up.up.right, e(l), "key stairs", LevelMapTemplateReader.Temp.lev_stairs_center_blocked),

            LevelSpec(masterbattle.right, e(l), "stairs to boss", LevelMapTemplateReader.Temp.lev_stair_side),

            LevelSpec(masterbattle.left.left, e(u, d), "dragon", LevelMapTemplateReader.Temp.lev_dragon_top),
            LevelSpec(masterbattle.left.left.down, e(u, l), "spiral bunnies", LevelMapTemplateReader.Temp.lev_spiral),

            LevelSpec(masterbattle.left.left.up, e(u, d), "triforce", LevelMapTemplateReader.Temp.lev_triforce),

            LevelSpec(getItemLoc8, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(getItemLoc8Key, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(getItemLoc8Go, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isHalfPassable = false, isGetItem = true),
//            LevelSpec(44, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(28, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
        )

        log(specs, 8)

        return specs
    }

    fun buildLevel9(): List<LevelSpec> {
        val start: MapLoc = LevelStartMapLoc.lev(9)
//        val first = start.left.up.up.up.up.up.left
        val first = start.left.up.up.up.up.up.up.left
        val pathdown = first.right.right

        val second = start.up.left.left.left
        val arrowstair = second.left.left
        val neararrow = 32 // 119 //second.left.up.up.up.up
        val stairbeforestair = second.up.up.up.up.up.up.right
        val specs = mutableListOf(
            LevelSpec(start, e(u, d), "start", LevelMapTemplateReader.Temp.lev_grid),
            LevelSpec(start.up, e(l, u), "triforce guy", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(start.up.left, e(u, d), "ghost water", LevelMapTemplateReader.Temp.lev_water_line_path),
            LevelSpec(start.up.left.up, e(u, d), "first stair", LevelMapTemplateReader.Temp.lev_stairs_center),

            LevelSpec(first, e(d, r), "spiral stair", LevelMapTemplateReader.Temp.lev_spiral),
            LevelSpec(first.right, e(l, d, r), "ghost circle", LevelMapTemplateReader.Temp.lev_water_circle_path),
            LevelSpec(pathdown, e(l, d, r, u), "circle enemy", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(pathdown.down, e(u, d, l, r), "four out bomb", LevelMapTemplateReader.Temp.lev_block4out),
            LevelSpec(pathdown.down.right, e(u, d, l), "two before", LevelMapTemplateReader.Temp.lev_block2center),
            LevelSpec(pathdown.down.right.up, e(u, d), "ghost blocks", LevelMapTemplateReader.Temp.lev_block4out2),
            LevelSpec(pathdown.down.right.up.up, e(d), "stair to ring", LevelMapTemplateReader.Temp.lev_stairs_center),

            LevelSpec(pathdown.up, e(d, l), "go to next room", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(pathdown.up.left, e(r), "ghost kill stairs", LevelMapTemplateReader.Temp.lev_block2center),

            LevelSpec(second, e(l, u), "squishy stair", LevelMapTemplateReader.Temp.lev_stair_side),
            LevelSpec(second.left, e(l, r), "dark place", LevelMapTemplateReader.Temp.lev_empty),
            // 97
            LevelSpec(second.left.left, e(l, u), "stair to near arrow", LevelMapTemplateReader.Temp.lev_stairs_center_blocked),

            LevelSpec(neararrow, e(u), "near arrow stair bomb up", LevelMapTemplateReader.Temp.lev_stairs_center_blocked),
            LevelSpec(neararrow.up, e(d), "elbow", LevelMapTemplateReader.Temp.lev_u),

            LevelSpec(arrowstair.up, e(l, d, u), "pancake", LevelMapTemplateReader.Temp.lev_cross),
            LevelSpec(arrowstair.up.up, e(l, u, d), "walk past pancake", LevelMapTemplateReader.Temp.lev_empty),
            LevelSpec(arrowstair.up.up.up, e(u, d, l), "pancake bomb", LevelMapTemplateReader.Temp.lev_empty),

            LevelSpec(arrowstair.up.left, e(u, r), "pancake", LevelMapTemplateReader.Temp.lev_water_circle_path),
            LevelSpec(arrowstair.up.left.up, e(u, d), "ghost maze", LevelMapTemplateReader.Temp.lev_water_maze_path),
            // this could be just a middle two pattern
            // lev_block2center NOT lev_stairs_center
            LevelSpec(arrowstair.up.left.up.up, e(d, r), "stair to last stair", LevelMapTemplateReader.Temp.lev_block2center),

            LevelSpec(stairbeforestair, e(l), "bomb left", LevelMapTemplateReader.Temp.lev_stairs_center),
            LevelSpec(stairbeforestair.left, e(r), "last path stair", LevelMapTemplateReader.Temp.lev_stairs_center),

            LevelSpec(second.left.up, e(u), "last path stair", LevelMapTemplateReader.Temp.lev_stair_side),
            LevelSpec(second.left.up.up, e(u), "!!!!GANNON!!!!", LevelMapTemplateReader.Temp.lev_gannon),
            LevelSpec(second.left.up.up.up, e(u), "Princess", LevelMapTemplateReader.Temp.lev_princess),

            LevelSpec(Nine.travel1, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isHalfPassable = false, isGetItem = true),
            LevelSpec(Nine.travel2, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isHalfPassable = false, isGetItem = true),
            LevelSpec(Nine.travel3, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isHalfPassable = false, isGetItem = true),
            LevelSpec(Nine.travel4, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isHalfPassable = false, isGetItem = true),
            LevelSpec(Nine.travel5, e(u), "", LevelMapTemplateReader.Temp.lev_getitem_move, isHalfPassable = false, isGetItem = true),

//            LevelSpec(6, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(Nine.redRing, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(Nine.silverArrow, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(44, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(88, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(0, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(0, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(136, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
            LevelSpec(40, e(u), "", LevelMapTemplateReader.Temp.lev_getitem, isGetItem = true),
        )

        log(specs, 9)

        return specs
    }

    private fun log(specs: List<LevelSpec>, lev: Int) {
        if (DEBUG) {
            d { " LEVEL $lev **START" }
            for (spec in specs) {
                d { "$lev spec ${spec.loc}" }
            }
            d { " LEVEL $lev **DONE" }
        }
    }
}

data class LevelSpec(val loc: MapLoc, val exits: ExitSet,
                     val name: String = "",
                     val template: LevelMapTemplateReader.Temp,
                     val isGetItem: Boolean = false,
                     val isHalfPassable: Boolean = true)
