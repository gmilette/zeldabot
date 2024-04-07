package bot.state.oam

import bot.state.map.Direction

object ProjectileDirectionLookup {
    fun findDir(tileAttrib: TileAttribute): Direction =
        findElse(tileAttrib, listOf(::findDirArrow, ::findDirBoomerang, ::findDirWiz))

    private fun findDirBoomerang(tileAttrib: TileAttribute) =
        when (tileAttrib) {
            brownBoomerangSpinBendFacingUpPair -> Direction.Up
            else -> Direction.None
        }

    private fun findDirWiz(tileAttrib: TileAttribute) =
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