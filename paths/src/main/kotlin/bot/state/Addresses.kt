package bot.state

object Addresses {
   // game
   /**
    * 0=Title/transitory    1=Selection Screen
   5=Normal              6=Preparing Scroll
   7=Scrolling           4=Finishing Scroll;
   E=Registration        F=Elimination
   MODE_REVEAL = 3
   MODE_SCROLL_COMPLETE = 4
   MODE_GAMEPLAY = 5
   MODE_SCROLL_START = 6
   MODE_SCROLL = 7
   MODE_GAME_OVER = 8
   MODE_UNDERGROUND = 9
   MODE_UNDERGROUND_TRANSITION = 10
   MODE_CAVE = 11
   MODE_CAVE_TRANSITION = 16
   MODE_DYING = 17
    */
   const val gameMode = 0x0012 // Game Mode

   /**
    * 0 no item screen
    * 7 on item screen
    * 8 exit scrolling
    */
   const val itemSelectionScroll = 0x00E1
   const val level = 0x0010 // level 0 overworld

   const val triforce = 0x0671

   // alternates between -128 and 0
   const val subPixel = 0x03A8
   // 0, 1, 3, 4, 6, 7
   // 0,0 x
   // 1,- x
   // 3,0 x
   // 4,- s
   // 6,0 x
   // 7,- s
   // 0,0 x
   // 1,- s
   // 3,0 x
   // 4,- s
   // 6,0 x
   // 7,- x

   // start at 55
   // how many moves to get move to 155
   // 75 or 76 moves to move 100
   // calculate link's speed
   // how many moves to go 100

   const val subTile = 0x0394
   const val collidingTile = 0x049E

   //Low Nibble = how many hearts are filled. High Nybble = No. of heart containers - 1
   //                                    Ex: $22 = 3 Heart Containers with all 3 filled
   //$FF = Full.
   const val heart1 = 0x0650
   const val heart1Full = 0x0651
   const val heart2 = 0x0652
   const val heart2Full = 0x0653
   const val heart3 = 0x0654
   const val heart3Full = 0x0655
   //C8, C7, C6(6 full hearts), C1(2 full hearts), c0 (1 heart full or 0.5)
   const val heartContainers = 0x066F
   //$00 = empty, $01 to $7F = half full, $80 to $FF = full.
   // empty: FD, 7E, FC (yes it is indeed full), FB, FA, F9, F4(2 full hearts), F3(1 heart)
   // BA
   // half: 7D, 7C, 7B, 7A, 79, 74 (1.5 hearts), 73, only 0.5 hearts
   const val heartContainersHalf = 0x0670

      // relative to each screen which is maybe 240 x 240?
   const val linkX = 0x0070
   const val linkY = 0x0084
   const val linkSwordProjectile = 0x007E

   // $08=North, $04=South, $01=East, $02=West
//   const val linkDir = 0x0089 //it's 98
   const val moveDir = 0x0F // 01 or FF // Link's Move Direction
   // enemy location is still set when the enemy is dead
   // or if link goes to another screen and there are no enemies
   // todo: precomipled knowledge is number of enemies on each
   // hack: just wait for the enemy to move x or y to really know if it exists
   const val enemyX1 = 0x0071
   const val tenthEnemyCount = 0x0050

   /**
    * 00FA:
    * 02, when horiz going lef
    * 01: going right
    * 08: up
    * 04: down
    */
   // not correct
   const val linkDirReal = 0x00FA
   //  $08=North, $04=South, $01=East, $02=West
   const val linkDir = 0x0098



   // i found
   // 5F if ladder deployed
   // 00 if not
   const val ladderDeployed = 0x035A

   // is it link or enemies
   val velocity = listOf(
      0x03BC,
      0x03BD,
      0x03BE,
      0x03BF,
      0x03C0,
      0x03C1,
      0x03C2,
      0x03C3,
      0x03C4,
      0x03C5,
      0x03C6,
      0x03C7,
   )

   val ememiesX = listOf(
      0x0071,
      0x0072,
      0x0073,
      0x0074,
      0x0075,
      0x0076,
      0x0077,
      0x0078,
      0x0079,
      0x007A,
      0x007B,
      0x007C,
   )
   val ememiesY = listOf(
      0x0085,
      0x0086,
      0x0087,
      0x0088,
      0x0089,
      0x008A,
      0x008B,
      0x008C,
      0x008D,
      0x008E,
      0x008F,
      0x0090,
   )
   val ememyDir = listOf(
      0x0099,
      0x009A,
      0x009B,
      0x009C,
      0x009D,
      0x009E,
      0x009F,
      0x00A0,
      0x00A1,
      0x00A2,
      0x00A3,
      0x00A4,
   )

   // maybe, looks like zeros
   // 40 is dead for snakes,
   // 6 is also dead
   // 40 dead for tricerotops
   // 1 is "throwing the boomerang"
   // 40
   // fire: 93, 65
   // not sure if this is lined up
   // with the other enemies
   val enemyStatus = listOf(
      0x0412,
      0x0413,
      0x0414,
      0x0415,
      0x0416,
      0x0417,
      0x0418,
      0x0419,
      0x041A,
      0x041B,
      0x041C,
      0x041D,
   )

   // 0x03A8 // link, looks related to movement
   // is this the one that is constantly counting up and down
   // if it is only when it moves then we can just delete
   val enemyPresence = listOf(
      0x03A9,
      0x03AA,
      0x03AB,
      0x03AC,
      0x03AD,
      0x03AE,
      0x03AF,
      0x03B0,
      0x03B1,
      0x03B2,
      0x03B3,
      0x03B4,
   )

   // the enemy cycles between 2 animations when alive
   // when it is dead, it is dead, no movement, i mean common that's what
   // dead is NOT moving, no animating! no need for this counter
   //       0x03E4, ?? link?
   val enemyAnimationOnOff = listOf(
      0x03E5,
      0x03E6,
      0x03E7,
      0x03E8,
      0x03E9,
      0x03EA,
      0x03EB,
      0x03EC,
      0x03ED,
      0x03EF,
      0x03F0,
      0x03F1,
   )

   // only when enemy moves
   val enemyMoves = listOf(
      0x0395,
   )

   // 128 seems to be dead
   // but it isn't always updated
   // looks like
   val enemyHp = listOf(
      0x0485,
      0x4856,
      0x4857,
      0x4858,
      0x4859,
      0x485A,
      0x485B,
      0x485C,
      0x485D,
      0x485E,
      0x486F,
      0x4860,
   )

   val enemyCountdowns = listOf(
      0x0029,
      0x002A,
      0x002B,
      0x002C,
      0x002D,
      0x002E,
      0x002F,
      0x0030,
      0x0031,
      0x0032,
      0x0033,
      0x0034,
   )

   // 10 enemies
   val dropItemType = listOf(
      0x00AD,
      0x00AE,
      0x00AF,
      0x00B0,
      0x00B1,
      0x00B2,
      // enemy projectile state
      // $00=Not Existant, $10=In Movement, $20=Start of Blowing Animation, $28=End of Blowing Animation, $30=Being Deflected by Shield
      0x00B3,
      0x00B4,
      0x00B5,
      0x00B6,
      0x00B7,
      0x00B8,
   )

   val dropEnemyItem = listOf(
      0x00C2,
      0x00C3,
      0x00C4,
      0x00C5,
      0x00C6,
      0x00C7,
      0x00C8,
      0x00C9,
      0x00CA,
      0x00CB,
      0x00CC,
      0x00CD,
   )

   // 25 key
   // 23 map
   // 29 boomerang
   // 3 nothing?
   const val dungeonTypeOfItem = 0x0AB
   const val dungeonFloorItem = 0x097

   const val clockActivated = 0x066C

   const val swordUseCountdown = 0x004C

   // inventory
   const val selectedItem = 0x0656
   const val numBombs = 0x0658
   const val numKeys = 0x066E //066E
   const val numRupees = 0x066D

   //0657    Current sword               $00=None, $01=Sword, $02=White Sword, $03=Magical Sword
   const val hasSword = 0x0657
   // $00=None, $01=Blue Candle, $02=Red Candle
   const val hasCandle = 0x065B
   const val hasBow = 0x065A
   const val hasArrow = 0x0659
   const val hasWhistle = 0x065C
   const val hasFood = 0x065D
   const val hasPotion = 0x065E
   const val hasRod = 0x065F
   const val hasRaft = 0x0660
   const val hasBook = 0x0661
   const val hasRing = 0x0662 //$00-None, $01-Blue Ring, $02-Red Ring.
   const val hasLadder = 0x0663
   const val hasBracelet = 0x0665
   const val hasLetter = 0x0666
   const val hasBoomerang = 0x0674
   const val hasMagicBoomerang = 0x0675
   const val hasShield = 0x0676
   const val hasMagicKey = 0x0664

   const val tunicColor = 0x6804

   // useful for predicting item drops
   // https://www.zeldaspeedruns.com/loz/generalknowledge/item-drops-chart
   const val enemiesKilledCount = 0x052A
   const val enemiesKilledWithoutTakingDamage = 0x0627

   object Ram {
      const val killedEnemyCount = 0x0627
      const val screenOptions = 0x04CD
      const val candleUsed = 0x0513
      // num hearts
      // need more info
      const val hearts = 0x0650
      // Low Nibble = how many hearts are filled. High Nybble = Number of heart containers - 1
      const val containers = 0x066F
   }

   object ZeroPage {
      const val mapLoc = 0x00EB // Value equals map x location + 0x10 * map y location
      // n
      const val subY = 0x00FC // ? Used for storyboard text and subscreen position
      const val subX = 0x00FD // ?
      const val screenScrolling = 0x00e8 //$00=No, $08=Northbound, $04=Southbound, $01=Eastbound, $02=Westbound
   }

   object Oam {
      const val start = 0x0000
   }

}