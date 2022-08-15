package bot

object Addresses {
   // game
   /**
    * 0=Title/transitory    1=Selection Screen
   5=Normal              6=Preparing Scroll
   7=Scrolling           4=Finishing Scroll;
   E=Registration        F=Elimination
    */
   const val gameMode = 0x0012 // Game Mode

   // relative to each screen which is maybe 240 x 240?
   const val linkX = 0x0070
   const val linkY = 0x0084
   // $08=North, $04=South, $01=East, $02=West
   const val linkDir = 0x0089
   const val moveDir = 0x0F // 01 or FF // Link's Move Direction
   // enemy location is still set when the enemy is dead
   // or if link goes to another screen and there are no enemies
   // todo: precomipled knowledge is number of enemies on each
   // hack: just wait for the enemy to move x or y to really know if it exists
   const val enemyX1 = 0x0071
   val ememiesX = listOf(
      0x0071,
      0x0072,
      0x0073,
      0x0074,
      0x0075,
      0x0076,
   )
   val ememiesY = listOf(
      0x0085,
      0x0086,
      0x0087,
      0x0088,
      0x0089,
      0x008A,
   )
   val ememyDir = listOf(
      0x0099,
      0x009A,
      0x009B,
      0x009C,
      0x009D,
      0x009E,
   )
   val enemyCountdowns = listOf(
      0x0029, //maybe 1?
      0x002A,
      0x002B,
      0x002C,
      0x002D,
      0x002E,
      0x002F, //?
   )

   val dropItemType = listOf(
      0x00AD,
      0x00AE,
      0x00AF,
      0x00B0,
      0x00B1,
      0x00B2,
   )

   val dropEnemyItem = listOf(
      0x00C2,
      0x00C3
   )


   object Ram {
      val killedEnemyCount = 0x0627
      // num hearts
      // need more info
      val hearts = 0x0650
      // Low Nibble = how many hearts are filled. High Nybble = Number of heart containers - 1
      val containers = 0x066F
   }

   object ZeroPage {
      val mapLoc = 0x00EB // Value equals map x location + 0x10 * map y location
      // n
      val subY = 0x00FC // ? Used for storyboard text and subscreen position
      val subX = 0x00FD // ?
      val screenScrolling = 0x00e8 //$00=No, $08=Northbound, $04=Southbound, $01=Eastbound, $02=Westbound
   }
}