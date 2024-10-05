package bot.state.oam

class Enemies {
}

private val one = (0x01).toInt()
private val two = (0x02).toInt()

val deadEnemy2 = (0x64).toInt() // attrib 40, 02 43 3// big splash
val deadEnemy = (0x62).toInt() // attrib 40, small one

val orbProjectile = (0x44).toInt() // fire from go to next room, also projectiles flying around from ship guy
//val grabbyHands = 142
val grabbyHands = (0xAE).toInt() //158
val grabbyHands2 = (0xAC).toInt() // 172

val graveyardGhost = (0xe4).toInt() to (0x01).toInt()
val ghost = (0xBC).toInt()
val ghost2 = (0xB6).toInt()
val ghost3 = (0xB4).toInt()
const val ghostProjectileUpDown = (0x7a).toInt() // 1 and 41, 02/42, down 81,c1, 83, c3, 82,c2
const val ghostProjectileLeft1 = (0x7e).toInt() // right is 03
const val ghostProjectileLeft2 = (0x7c).toInt() // right is 03
val movingBlock = (0xb1).toInt() to (0x03).toInt()
val movingBlock2 = (0xb3).toInt() to (0x03).toInt()
// just ignore all of them if it's the overworld, rather than try to route to it
val waterMonsterPairUnder = (0xbc) to (0x03).toInt()
val waterMonsterPairUnder2 = (0xbc) to (0x43).toInt()
val waterMonsterPairUnderE = (0xbe) to (0x03).toInt()
val waterMonsterPairUnder2E = (0xbe) to (0x43).toInt()
val waterMonsterPair = (0xEC) to (0x03).toInt() // also is triforce dirt, but it's ok i think
val waterMonsterPair2 = (0xEC) to (0x43).toInt()
val waterMonsterPairAlt = (0xEE) to (0x03).toInt() // tile address = 0EE0
val waterMonsterPairAlt2 = (0xEE) to (0x43).toInt()
val ghostWeak = (0xB8).toInt() // also it is the back of boomerang guy
val ghostWeak2 = (0xBA).toInt()
val bombSmoke = (0x70).toInt() // 01
val bombSmokeLess = (0x72).toInt() // 41
val monsterCloud = (0x70).toInt() // 01 or 41
val bomb = 52
val key = (0x2E).toInt() //key
val heart = 243

val shopkeeperAndBat = (0x9C).toInt() to (0x00).toInt()
val bat = (0x9A).toInt()

// shot from the enemies
val rockProjectile = (0x9e).toInt() to (0x00).toInt()

val sun = 0x8e // 144
val sun2 = 0x90 //
val boulder = (0x90).toInt()
val boulder2 = (0x92).toInt()
//val boulder3 = (0xEA).toInt() // it is also triforce part sooo... // also rhino piece
//val boulder4 = (0xE8).toInt() // also part of circle enemy // also rhino
val boulder4Pair = (0xE8).toInt() to (0x02).toInt()
val boulder3Pair = (0xEA).toInt() to (0x02).toInt()
val arrowTipShotByEnemy = (0x88).toInt()
val arrowTipShotByEnemy2 = (0x86).toInt()
val arrowTipShotByEnemyPair = (0x88).toInt() to (0x42).toInt()
val arrowButtShotByEnemyPair = (0x86).toInt() to (0x42).toInt()
val arrowTipShotByEnemyPairRight = (0x88).toInt() to (0x02).toInt()
val arrowButtShotByEnemyPairRight = (0x86).toInt() to (0x02).toInt()
val arrowTipShotByEnemyPairDown = (0x28).toInt() to (0x82).toInt()
val arrowTipShotByEnemyPairUp = (0x28).toInt() to (0x02).toInt()
val arrowButtShotByEnemy = (0x86).toInt() to (0x42).toInt()
val arrowButtShotByEnemy2 = (0x86).toInt() to (0x02).toInt()

val largeShield = (0x56).toInt()
val candle = (0x26).toInt()
val fire = (0x5e).toInt()
val ladder =(0x76).toInt() //118
val raft = (0x6C).toInt()
val triforceNormal = (0x6E).toInt()
// final triforce
val triforceTile = (0xF4).toInt()
val triforceTilePair = (0xF4).toInt() to (0x01).toInt()
val triforceTile2Pair = (0xF2).toInt() to (0x01).toInt()
val triforceTile2PairAlt = (0xF2).toInt() to (0x02).toInt()
val triforceDirt = (0xEC).toInt() // attrib 03 //236 // also part of fourMonster
val triforceDirt2 = (0xFA).toInt() // also circle enemy center, also rhino up
val triforceDirt3 = (0xEA).toInt() // attrib 03

val triforceDirt3Pair = (0xEA).toInt() to (0x03).toInt() // attrib 03

// verify
val triforceDirtPair = (0xEC).toInt() to (0x03)// attrib 03 //236 //0EC0 // also part of fourMonster
val starCenter = (0xEC).toInt() to (0x41) // center star monster
val starCenter2 = (0xEC).toInt() to (0x01) // center star monster


val magicBook = (0x42).toInt()
val magicSword = (0x48).toInt()
val rod = (0x4A).toInt()
val letter = (0x4C).toInt()
val powerBracelet = (0x4E).toInt()

val arrowPair = (0x28).toInt() to (0x82).toInt()
val silverArrow = (0x28).toInt()
val compass = (0x6A).toInt()
val clockTile = (0x66).toInt()
val bigHeart = (0x68).toInt() //104
val redring = (0x46).toInt()
val brownBoomerang = (0x36).toInt() //trib 0, 40
val brownBoomerangSpin = (0x38).toInt() //attrib 40
val brownBoomerangSpinBendFacingUp = (0x3a).toInt() //attrib 0
val brownBoomerangSpinBendFacingUpPair = (0x3a).toInt() to 0
val bow = (0x2A).toInt()
val trap = (0x96).toInt()
val map = (0x4C).toInt()
val fairy = (0x50).toInt()
val fairy2 = (0x52).toInt()
val bigCoin = (0x32).toInt() //2, 01
val bigCoinPair = (0x32).toInt() to (0x02) //2, 01
val bigCoinPair2 = (0x32).toInt() to (0x01) //2, 01
// same as big coin
//val smallCoin = (0x32).toInt() //2, 01
val wizard = (0x98).toInt() // 152
val flame1 = (0x5C).toInt() //92
const val flame2 = (0x5E).toInt() //94
const val oldWoman = (0x9a).toInt()
const val potion = (0x99).toInt() //?
val whistle = (0x24).toInt()
val masterKey = (0x2C).toInt()
val bait = (0x22).toInt() to (0x02).toInt()

val secretEverybodyMonsterOrCircleEnemyLeft = 250 // fa
val secretEverybodyMonsterOrCircleEnemyRight = 248 // f8

private val three = (0x03).toInt()

// enemies
//val dragonHead = (0xC0).toInt()
val dragonHead = (0xCC).toInt()
val dragonHead2 = (0xC0).toInt()
val dragonNeck = (0xC4).toInt() to three
val dragonNeckTile = (0xC4).toInt()
// do not attack
// but these are also the pinwheel guys
//val dragonFeet = (0xC6).toInt() //wheel guy
val dragonFeetPair = (0xCA).toInt() to three // and spider
val dragonFeet2 = (0xC2).toInt() to three // and spider
val dragonTail = (0xC8).toInt() to three //attribute 3 for dragon
val dragonTail2 = (0xC0).toInt() to three //attribute 3 for dragon wheel guy
//val dragonBody = (0xC2).toInt()
val dragon4Head = (0xDC).toInt() // 03
val dragon4NeckSegment = (0xDA).toInt() to three
val dragon4Body = (0xD4).toInt() to (0x03).toInt() // attrib 3
val dragon4BodyWingRight = (0xD2).toInt() to (0x03).toInt()
val dragon4BodyWingLeft = (0xD8).toInt() to (0x03).toInt()
val dragon4BodyFoot = (0xD0).toInt() to (0x03).toInt() // attrib 3
val dragon4BodySide = (0xD6).toInt()
val dragon4BodySpinePair = (0xC6).toInt() to (0x03).toInt() // attribute 3
val dragon4FlamingHead = (0xDE).toInt()

val spiderBlueHeadLeft = (0xFC).toInt() to (0x01).toInt()
val spiderBlueHeadRight = (0xFC).toInt() to (0x41).toInt()
val spiderHeadLeft = (0xFC).toInt() to (0x02).toInt()
val spiderHeadRight = (0xFC).toInt() to (0x42).toInt()
val spiderHeadLeftClosed = (0xFE).toInt() to (0x02).toInt()
val spiderHeadRightClosed = (0xFE).toInt() to (0x42).toInt()

val lynelSwordGuy = (0xD4).toInt() to (0x41).toInt()
val lynelSwordGuy2 = (0xD2).toInt() to (0x41).toInt()
val lynelSwordGuyStride = (0xD0).toInt() to (0x41).toInt()
val lynelSwordGuyStride2 = (0xCE).toInt() to (0x41).toInt()
val lynelSwordGuyDown = (0xD8).toInt() to (0x01).toInt() // or 41
val lynelSwordGuyDown2 = (0xD6).toInt() to (0x01).toInt() // or 41

val lynels = setOf(
    lynelSwordGuy,
    lynelSwordGuy2,
    lynelSwordGuyStride,
    lynelSwordGuyStride2,
    lynelSwordGuyDown,
    lynelSwordGuyDown2
)

val lynelsTile = setOf(
    lynelSwordGuy,
    lynelSwordGuy2,
    lynelSwordGuyStride,
    lynelSwordGuyStride2
).map { it.tile }

val rhinoHeadDown = (0xF4).toInt()
val rhinoHeadDown2 = (0xF6).toInt()
val rhinoEatingHeadDown = (0xF8).toInt()
val rhinoHeadMouthOpen = (0xE2).toInt()
val rhinoHeadHeadWithMouthOpen = (0xE0).toInt()
val rhinoHeadHeadWithMouthClosed = (0xE8).toInt()
val rhinoHeadMouthClosed = (0xEA).toInt() to (0x03).toInt()
val rhinoHead2 = (0xE0).toInt()

val fourMonster = (0xE2).toInt() to (0x41).toInt()
val fourMonster2 = (0xE0).toInt() to (0x41).toInt()
val fourMonsterBody = (0xEC).toInt() to (0x41).toInt()
val fourMonster3 = (0xE8).toInt() to (0xC1).toInt()
val fourMonster4 = (0xE8).toInt() to (0x41).toInt()
val fourMonster5 = (0xE8).toInt() to (0x01).toInt()

val rhinoTail = (0xDC).toInt()
val rhinoMid = (0xDE).toInt()
val rhinoUpLeft = (0xFA).toInt() to (0x03).toInt()

val circleMonster = (0xFC).toInt()
val circleMonster2 = (0xFE).toInt()
val circleMonsterCenter = (0xFA).toInt()
val circleMonsterCenter2 = (0xF8).toInt()
val circleMonsterCenters = setOf(circleMonsterCenter, circleMonsterCenter2)
val shopOwner = (0xF8).toInt() to (0x02).toInt()
val shopOwner2 = (0xFA).toInt() to (0x02).toInt()
// on map locations (anywhere in overworld) 31, map 29
val spinCircleEnemy = (0xC8).toInt() // attribute 42 and 2
val spinCircleEnemy2 = (0xC6).toInt() // attribute 42 and 2

// some tile information
// https://www.computerarcheology.com/NES/Zelda/Bank3.html
// https://www.computerarcheology.com/NES/Zelda/Bank2.html
