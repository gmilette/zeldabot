package bot.state

object EnemyMaxHpTable {
    private val hpByType = mapOf(
        0x01 to 96,   // Lynel blue
        0x02 to 64,   // Lynel red
        0x03 to 48,   // Moblin blue
        0x04 to 32,   // Moblin red
        0x05 to 80,   // Goriya blue
        0x06 to 48,   // Goriya red
        0x07 to 16,   // Octorock blue slow
        0x08 to 32,   // Octorock red slow
        0x09 to 64,   // Octorock blue fast
        0x0A to 32,   // Octorock red fast
        0x0B to 128,  // Darknut blue
        0x0C to 16,   // Darknut red
        0x0D to 64,   // Tektite blue
        0x0E to 32,   // Tektite red
        0x10 to 48,   // Red Leever
        0x11 to 240,  // Zora
        0x12 to 128,  // Vire
        0x13 to 240,  // Vire
        0x14 to 32,   // Zol
        0x17 to 160,  // Pols Voice
        0x18 to 144,  // Like Like
        0x1C to 16,   // Keese
        0x1F to 32,   // Armos
        0x23 to 240,  // Flying Ghini
        0x29 to 16,   // Rope (quest 1)
        0x2B to 32,   // Stalfos
        0x30 to 64,   // Gibdo
        0x31 to 96,   // Dodongo
        0x35 to 240,  // Grumble
    )

    fun maxHp(objType: Int): Int = hpByType[objType] ?: 999
}
