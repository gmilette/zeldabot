package bot.state.oam

import bot.state.map.Direction

object ProjectileDirectionLookup {
    fun hasKnownDirection(tileAttrib: TileAttribute): Boolean =
        findDir(tileAttrib) != Direction.None

    fun findDir(tileAttrib: TileAttribute): Direction =
        findElse(tileAttrib, listOf(::findDirArrow, ::findDirWiz))

//::findDirBoomerang
//    private fun findDirBoomerang(tileAttrib: TileAttribute) =
//        when (tileAttrib) {
//            brownBoomerangSpinBendFacingUpPair -> Direction.Up
//            else -> Direction.None
//        }

    private fun findDirWiz(projectile: TileAttribute): Direction =
        if (projectile.tile in setOf(ghostProjectileUpDown, ghostProjectileLeft1, ghostProjectileLeft2)) {
            when (projectile.tile) {
                ghostProjectileLeft1,
                ghostProjectileLeft2 -> if (projectile.xFlip) Direction.Left else Direction.Right
                ghostProjectileUpDown -> if (projectile.yFlip) Direction.Down else Direction.Up
                else -> Direction.None
            }
        } else {
            Direction.None
        }
        // 7e
//        when {
//            tileAttrib.matches(ghostProjectileRightPair) -> Direction.Right
//            tileAttrib.matches(ghostProjectileRightPair2) -> Direction.Right
//            tileAttrib.tile == ghostProjectileLeft2 ||
//                    tileAttrib.tile == ghostProjectileLeft1 -> Direction.Left
//            tileAttrib.matches(ghostProjectileUp) -> Direction.Up
//            tileAttrib.matches(ghostProjectileUp2) -> Direction.Up
//            tileAttrib.tile ==  ghostProjectileUpDown -> Direction.Down
//            else -> Direction.None
//        }

    private fun findDirWizo(tileAttrib: TileAttribute) =
        when {
            tileAttrib.matches(ghostProjectileRightPair) -> Direction.Right
            tileAttrib.matches(ghostProjectileRightPair2) -> Direction.Right
            tileAttrib.tile == ghostProjectileLeft2 ||
            tileAttrib.tile == ghostProjectileLeft1 -> Direction.Left
            tileAttrib.matches(ghostProjectileUp) -> Direction.Up
            tileAttrib.matches(ghostProjectileUp2) -> Direction.Up
            tileAttrib.tile ==  ghostProjectileUpDown -> Direction.Down
            else -> Direction.None
        }

    private fun findElse(tileAttrib: TileAttribute,
                 finders: List<(TileAttribute) -> Direction>): Direction {
        var result = Direction.None
        for (finder in finders) {
            result = finder(tileAttrib)
            if (result != Direction.None) {
                break
            }
        }
        return result
    }

    private fun findDirArrow(tileAttrib: TileAttribute) =
        when (tileAttrib) {
            arrowButtShotByEnemyPair,
            arrowTipShotByEnemyPair -> Direction.Left
            arrowButtShotByEnemyPairRight,
            arrowTipShotByEnemyPairRight -> Direction.Right
            arrowTipShotByEnemyPairDown -> Direction.Down
            arrowTipShotByEnemyPairUp -> Direction.Up
            else -> Direction.None
        }
}