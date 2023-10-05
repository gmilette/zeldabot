package bot.state.oam

object EnemyGroup {
    const val oldWoman = (0x9a).toInt()

    val longWaitEnemies = setOf(
        184, 186, // ghost
        ghost,
        ghostWeak2,
        ghostWeak,
        rhinoHeadDown, rhinoHeadDown2, rhinoHeadMouthOpen, rhinoHead2,
        bat,
        254, 248 // the circle monster because I dont know why im stuck here
    )

    val ignore = setOf(
        18, 16, // link shield shite
        12, 14, // facing up link
        4, 6, 8, // link // 0
        0, 2, // facing left link
        88, 10, // mor elink
        90, // brown link small shield
        20, 22, // i think link or maybe movable block
        (0x80).toInt(), // link sword
        (0x82).toInt(), // link sword
        84, // link i think //(0x54).toInt(), // link with shield
        86, // link i think //(0x54).toInt(), // link with shield
        32, // link's sword
        96, // link again
        largeShield,
        (0x58).toInt(),
        (0x1A).toInt(), (0x18).toInt(), // link about to attack
        //
        (0x82).toInt(), // sword hilt
        (0x84).toInt(), // sword point
        90, // not sure what it is maybe link or his sword
        62, // blinking this
        deadEnemy2,
        deadEnemy,
        (0x48).toInt(), // magic sword
        48, // swirly thing
        bombSmoke, // yea but then it attacks its own bomb// 112, // bomb smoke, removed it so I can attack the monsters
        114, // i think bomb smoke

        //164, // not sure, that is a pancake
//            160, // this happens to be the left side of the snake monster in level 2, snake has attribute 2
        ladder,
        wizard,
        flame1,
        flame2,
        dragon4Body,
        dragon4BodyFoot,
        dragon4BodySpine,
//            triforceDirt,
        triforceDirt2,
//            oldWoman
//            dragonFeet,
//            dragonFeet2,
//            dragonTail,
//            dragonBody,
//            rhinoTail, // but it also the dragon's head
//            rhinoMid,
        // keep for now, but I have to make link not attack sometimes
//            secretEverybodyMonsterOrCircleEnemyLeft,
//            secretEverybodyMonsterOrCircleEnemyRight
    )
    val projectiles = setOf(
        124, 126, // ghost attack
        144, 142, // sun
        40, orbProjectile, // ganons
        arrowTipShotByEnemy,
        fire,
        brownBoomerang, // but it is also an item to be gotten, not avoided, oy!
        brownBoomerangSpin,
        (0x96).toInt(), // trap,
        boulder, boulder2, boulder3, boulder4,
        dragon4FlamingHead,
        spinCircleEnemy
    )

    val largeProjectiles = setOf(
        124, 126, // ghost attack
        144, 142, // sun
        fire,
        (0x96).toInt(), // trap,
        boulder, boulder2, boulder3, boulder4,
        spinCircleEnemy
    )

    // gannon stuff
    // pile: EC, EA
    // triforce: F2, F4

    val ignorePairs = setOf(
        triforceDirtPair,
        starCenter,
        starCenter2
    )

    val loot = setOf(
        compass,
        clockTile,
        silverArrow,
        redring,
//            brownBoomerang,
        bow,
        map,
        masterKey,
        raft,
        bigHeart,
        fairy,
        fairy2,
        magicBook,
        rod,
        powerBracelet,
        whistle,
        candle,
        120, // not sure maybe something on triforce level , 78
        50,
        243, // heart
        key, //key
        116, // bomb
        bomb, // bomb
        triforceNormal,
//            triforceDirt, // why?
//            triforceDirt2,
        triforceTile,
        triforceTile2,
        magicSword
    )
}
