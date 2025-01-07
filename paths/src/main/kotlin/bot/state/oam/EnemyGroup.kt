package bot.state.oam

import bot.state.Agent

object EnemyGroup {
    const val oldWoman = (0x9a).toInt()

    val longWaitEnemies = setOf(
        // only if in level 6, 9
        ghostProjectileUpDown,
        ghostProjectileLeft1,
        ghostProjectileLeft2,
        ghost,
        ghostWeak2,
        ghost2,
        ghost3,
//        ghostWeak, // do not use because it it same as boomerang guy
        rhinoHeadDown, rhinoHeadDown2 // head should be enough
//        bat,
//        254, 248 // the circle monster because I dont know why im stuck here
    )

    val boomerangs = setOf(
        brownBoomerang, // but it is also an item to be gotten, not avoided, oy!
        brownBoomerangSpin,
        brownBoomerangSpinBendFacingUp,
    )

    val swordProjectile = setOf(
        (0x84).toInt() to (0x00).toInt(), // tip
        (0x82).toInt() to (0x00).toInt(), // hilt
        // white
        (0x84).toInt() to (0x01).toInt(),
        (0x82).toInt() to (0x01).toInt(),
        // white
        (0x84).toInt() to (0x02).toInt(),
        (0x82).toInt() to (0x02).toInt(),
        // dark
        (0x84).toInt() to (0x03).toInt(),
        (0x82).toInt() to (0x03).toInt(),

        // flying white
        (0x84).toInt() to (0x42).toInt(), // tip
        (0x82).toInt() to (0x42).toInt(), // hilt
        (0x84).toInt() to (0x40).toInt(),
        (0x82).toInt() to (0x40).toInt(),
        (0x84).toInt() to (0x41).toInt(),
        (0x82).toInt() to (0x41).toInt(),
        (0x84).toInt() to (0x43).toInt(),
        (0x82).toInt() to (0x43).toInt(),

    )

    // link's sword
    val swordLink = setOf(
        // brown sword
        0x82 to 0x01,
        0x82 to 0x40,
        0x84 to 0x40,
        0x82 to 0x41,
        0x84 to 0x41,
        // magic sword?
        0x82 to 0x42,
        0x84 to 0x42,
        0x80 to 0x1,
    )

    val flame = setOf(flame1, flame2)

    fun ignoreFor(isOverworld: Boolean) = if (isOverworld) ignore else ignoreLevel

    val ignore = setOf(
        18, 16, // link shield shite
        12, 14, // facing up link
        4, 6, 8, // link // 0
        0, 2, // facing left link
        88, 10, // mor elink
        90, // brown link small shield
        20, 22, // i think link or maybe movable block
//        (0x80).toInt(), // link sword
//        (0x82).toInt(), // link sword
//        (0x82).toInt(), // sword hilt
//        (0x84).toInt(), // sword point
        84, // link i think //(0x54).toInt(), // link with shield
        86, // link i think //(0x54).toInt(), // link with shield
        /////32, // link's sword // 0x20 link sword is flying //// TEST!!
        96, // link again
        largeShield,
        (0x58).toInt(),
        (0x1A).toInt(), (0x18).toInt(), // link about to attack
        //
        90, // not sure what it is maybe link or his sword
        62, // blinking this
        deadEnemy2,
        deadEnemy,
        (0x48).toInt(), // magic sword
        48, // swirly thing
            //164, // not sure, that is a pancake
//            160, // this happens to be the left side of the snake monster in level 2, snake has attribute 2
        ladder,
        wizard,
        flame1,
        flame2,
        clockTile,
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
    ) + swordProjectile.map { it.tile } + swordLink.map { it.tile }

    val ignoreLevel = ignore + setOf(
        bombSmoke,
        bombSmokeLess,
    )

    val projectiles = setOf(
        flame1,
        flame2,
        144, 142, // sun
        40, orbProjectile, // ganons
        arrowTipShotByEnemy,
        arrowTipShotByEnemy2,
        fire,
        brownBoomerang, // but it is also an item to be gotten, not avoided, oy!
        brownBoomerangSpin,
        brownBoomerangSpinBendFacingUp,
        trap, // trap,
        dragon4FlamingHead,
        spinCircleEnemy,
        ghostProjectileUpDown,
        ghostProjectileLeft1,
        ghostProjectileLeft2,
        rockProjectile.tile,
        // not in level though
        bombSmoke,
        bombSmokeLess,
    )

    val projectilesLevel = projectiles - rockProjectile.tile

    val projectilePairs = setOf(
        arrowPair,
        boulder4Pair,
        boulder3Pair,
        arrowTipShotByEnemyPair,
        arrowButtShotByEnemy,
        arrowButtShotByEnemy2,
        rockProjectile
    ) // + swordProjectile

    val projectilePairsLevel = projectilePairs - rockProjectile

    val projectileMagicShieldBlockable = setOf(
        ghostProjectileUpDown,
        ghostProjectileLeft1,
        ghostProjectileLeft2,
        orbProjectile // only magic shield, unless from a dragon
    ) // + (swordProjectile.map { it.tile })

    val projectileUnblockable = setOf(
        144, 142, // sun
        dragon4FlamingHead,
        spinCircleEnemy,
        trap,
        fire,
        dragon4FlamingHead,
        spinCircleEnemy,
    )

    val projectilePairsUnblockable = setOf(
        boulder4Pair,
        boulder3Pair,
//        rockProjectile // it's shot by the overworld enemies
    )

    // don't use the projectile pairs
    val projectileBlockable = projectiles -
            projectileUnblockable -
            projectileMagicShieldBlockable -
            projectilePairsUnblockable.map { it.first }.toSet()

    // todo: need to add back these projectiles when I know attrib
//        boulder, boulder2, boulder3, boulder4,

//    // ghosts and sword guys
//    val avoidFrontEnemies = setOf(
//        ghost,
//        swordDir.up.first()
//    )
//
//    val projectilesAttackIfNear = setOf(sun, sun2)

    val largeProjectiles = setOf(
        sun, sun2, // sun
        fire,
        trap, // trap,
        boulder, boulder2,
        spinCircleEnemy
    )

    // gannon stuff
    // pile: EC, EA
    // triforce: F2, F4
    // but also a
    // blue rhino soldier f4, attribute 3
    val keepPairs = setOf(
        rhinoUpLeft,
        rhinoHeadMouthClosed
    )

    val ignorePairs = setOf(
        movingBlock,
        movingBlock2,
        triforceDirtPair,
        triforceDirt3Pair,
        starCenter,
        starCenter2,
        dragon4BodyWingRight,
        dragon4BodyWingLeft,
        dragon4BodySpinePair,
        dragon4BodyFoot,
        dragon4Body,
        dragonFeetPair,
        dragonFeet2,
        dragonTail,
        dragonTail2,
        dragon4NeckSegment,
        dragonNeck,
        waterMonsterPairUnder,
        waterMonsterPairUnder2,
        waterMonsterPair,
        waterMonsterPair2,
        waterMonsterPairAlt, // tile address = 0EE0
        waterMonsterPairAlt2
    ) + swordLink

    val lootPairs = setOf(
        bigCoinPair,
        bigCoinPair2,
        bait
    )

    val triforceTiles = setOf(triforceTileLeft, triforceTile)

    val loot = setOf(
        compass,
//        triforceTileLeft,
//        triforceTile,
        // this just breaks things
//        clockTile,
//        silverArrow,
        redring,
//            brownBoomerang,
        bow,
        map,
        masterKey,
        raft,
        fairy,
        fairy2,
        magicBook,
        rod,
        powerBracelet,
        whistle,
        candle,
        120, // not sure maybe something on triforce level , 78
        50,
        heart, // heart
        bigHeart,
        key, //key
        116, // bomb
        bomb, // bomb
        triforceNormal,
//            triforceDirt, // why?
//            triforceDirt2,
        // same as rhinoenemies
//        triforceTile,
//        triforceTile2,
        magicSword,
    )

    val dragon1 = setOf(
        dragonHead//, dragonNeckTile
    )

    val enemiesToNotAttackInOverworld = setOf(
        spinCircleEnemy,
        spinCircleEnemy2,
        waterMonsterPairUnderE.tile,
        waterMonsterPairUnder2E.tile,
        waterMonsterPair.tile,
        waterMonsterPairUnder.tile, // also it is ghost
        waterMonsterPairUnder2.tile,
        shopOwner.tile,
        shopOwner2.tile,
        shopkeeperAndBat.tile
    )

    val shopOwners = setOf(
        shopkeeperAndBat.tile,
        shopOwner.tile,
        shopOwner2.tile,
        oldWoman,
        wizard
    )

    val notAffectedByBoomerang = setOf(
        ghost
    )

    val enemiesWhoMightHaveBombs = listOf(
        // side
        (0xba).toInt(),
        (0xb8).toInt(),
        // down
        (0xb2).toInt(),
        (0xb0).toInt(),

        (0xb4).toInt(),
        (0xb6).toInt(),

        // right
        (0xf2).toInt(),
        (0xf0).toInt(),
        // left
        (0xf6).toInt(),
        (0xf4).toInt(),
        // facing down
        (0xfa).toInt(),
        (0xf8).toInt(), //02 is the light one, 43 is the blue
        // up
        (0xfc).toInt(),
        (0xfe).toInt(),
    )
}
