package bot.state.oam

object EnemyObjectTypes {
    val unblockableFireball = 0x56
    val projectileObjectTypeUnblockable = setOf(unblockableFireball)
    val fireball = setOf(0x55, unblockableFireball)
}