package bot.state.oam

class Enemies {
}

val deadEnemy2 = (0x64).toInt() // attrib 40, 02 43 3// big splash
val deadEnemy = (0x62).toInt() // attrib 40, small one

val orbProjectile = (0x44).toInt() // fire from go to next room, also projectiles flying around from ship guy
//val grabbyHands = 142
val grabbyHands = (0xAE).toInt() //158
val grabbyHands2 = (0xAC).toInt() // 172

val ghost = (0xBC).toInt()
val ghostWeak = (0xB8).toInt()
val ghostWeak2 = (0xBA).toInt()
val bombSmoke = 112
val monsterCloud = (0x70).toInt()
val bomb = 52
val key = (0x2E).toInt() //key

val shopkeeperAndBat = (0x9C).toInt()
val bat = (0x9A).toInt()

val boulder = (0x90).toInt()
val boulder2 = (0x92).toInt()
val boulder3 = (0xEA).toInt() // it is also triforce part sooo... // also rhino piece
val boulder4 = (0xE8).toInt() // also part of circle enemy // also rhino
val boulder4Pair = (0xE8).toInt() to (0x00).toInt() //??
val arrowTipShotByEnemy = (0x88).toInt()
val arrowButtShotByEnemy = (0x86).toInt()

val largeShield = (0x56).toInt()
val candle = (0x26).toInt()
val fire = (0x5e).toInt()
val ladder =(0x76).toInt() //118
val raft = (0x6C).toInt()
val triforceNormal = (0x6E).toInt()
// final triforce
val triforceTile = (0xF4).toInt()
val triforceTile2 = (0xF2).toInt()
val triforceDirt = (0xEC).toInt() // attrib 03 //236 // also part of fourMonster
val triforceDirt2 = (0xFA).toInt() // also circle enemy center
val triforceDirt3 = (0xEA).toInt() // attrib 03

val waterMonster = 0xEC to (0x43)

// verify
val triforceDirtPair = (0xEC).toInt() to (0x03)// attrib 03 //236 // also part of fourMonster
val starCenter = (0xEC).toInt() to (0x41) // center star monster
val starCenter2 = (0xEC).toInt() to (0x01) // center star monster


// added from online
// https://www.computerarcheology.com/NES/Zelda/Bank2.html
val magicBook = (0x42).toInt()
val magicSword = (0x48).toInt()
val rod = (0x4A).toInt()
val letter = (0x4C).toInt()
val powerBracelet = (0x4E).toInt()


val silverArrow = (0x28).toInt()
val compass = (0x6A).toInt()
val clockTile = (0x66).toInt()
val bigHeart = (0x68).toInt() //104
val redring = (0x46).toInt()
val brownBoomerang = (0x36).toInt() //trib 0, 40
val brownBoomerangSpin = (0x38).toInt() //attrib 40
val bow = (0x2A).toInt()
val map = (0x4C).toInt()
val fairy = (0x50).toInt()
val fairy2 = (0x52).toInt()
val wizard = (0x98).toInt() // 152
val flame1 = (0x5C).toInt() //92
const val flame2 = (0x5E).toInt() //94
const val oldWoman = (0x9a).toInt()
const val potion = (0x99).toInt() //?
val whistle = (0x24).toInt()
val masterKey = (0x2C).toInt()

val secretEverybodyMonsterOrCircleEnemyLeft = 250 // fa
val secretEverybodyMonsterOrCircleEnemyRight = 248 // f8

// enemies
//val dragonHead = (0xC0).toInt()
val dragonHead = (0xCC).toInt() //3
val dragonHead2 = (0xC0).toInt() //3
val dragonNeck = (0xC4).toInt()
// do not attack
// but these are also the pinwheel guys
//val dragonFeet = (0xC6).toInt() //wheel guy
//val dragonFeet2 = (0xCA).toInt() // and spiders
val dragonTail = (0xC8).toInt() //attribute 3 for dragon wheel guy
//val dragonBody = (0xC2).toInt()

val dragon4Head = (0xDC).toInt()
val dragon4Body = (0xD4).toInt() // attrib 3
val dragon4BodyWingRight = (0xD2).toInt()
val dragon4BodyWingLeft = (0xD8).toInt()
val dragon4BodyFoot = (0xD0).toInt() // attrib 3
val dragon4BodySide = (0xD6).toInt()
val dragon4BodySpine = (0xC6).toInt() // attribute 3
val dragon4FlamingHead = (0xDE).toInt()
val rhinoHeadDown = (0xF4).toInt()
val rhinoHeadDown2 = (0xF6).toInt()
val rhinoHeadMouthOpen = (0xE2).toInt()
val rhinoHeadHeadWithMouthOpen = (0xE0).toInt()
val rhinoHeadHeadWithMouthClosed = (0xE8).toInt()
val rhinoHeadMouthClosed = (0xEA).toInt()
val rhinoHead2 = (0xE0).toInt()

val fourMonster = (0xE2).toInt() to (0x41).toInt()
val fourMonster2 = (0xE0).toInt() to (0x41).toInt()
val fourMonsterBody = (0xEC).toInt() to (0x41).toInt()
val fourMonster3 = (0xE8).toInt() to (0xC1).toInt()
val fourMonster4 = (0xE8).toInt() to (0x41).toInt()
val fourMonster5 = (0xE8).toInt() to (0x01).toInt()

val rhinoTail = (0xDC).toInt()
val rhinoMid = (0xDE).toInt()

val circleMonster = (0xFC).toInt()
val circleMonster2 = (0xFE).toInt()
val circleMonsterCenter = (0xFA).toInt()
val circleMonsterCenter2 = (0xF8).toInt()
val spinCircleEnemy = (0xC8).toInt() // attribute 42 and 2

// enemies
// https://www.computerarcheology.com/NES/Zelda/Bank3.html