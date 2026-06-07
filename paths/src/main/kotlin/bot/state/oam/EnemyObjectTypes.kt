package bot.state.oam

object EnemyObjectTypes {
    val alwaysHarmful = setOf(0x56) //, 0x5A)
    val blockableWithoutShield = setOf(0x53, 0x54, 0x5B, 0x5C)

    val unblockableFireball = 0x56
    val regularFireball = 0x55
    val projectileObjectTypeUnblockable = setOf(unblockableFireball)
    val fireball = setOf(regularFireball, unblockableFireball)
}