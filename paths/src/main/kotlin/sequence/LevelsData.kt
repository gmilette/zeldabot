package sequence

data class LevelData(
    val level: Int,
    val numKeysIn: Int,
    val numKeysNeeded: Int,
    val numKeysPlanned: Int = 0
)

object LevelsData {
    // plan:
    // 2 -> 3, rup: 176
    // 1 -> 4, rup: 182 - candle 122
    // 3 -> 3, rup 122, 222 (not enough)
    // 4 -> 3, still at 3, 9 rup

    val levels = listOf(
        // option: level 2 left squishy, or level 8 right grab(is it a grab?)
        // plan
        //2: 2 keys (skip snake?, skip left squishy)
        //1: 4 keys: use 4, get 6, (get all - could skip boomerang)
        //3: 5 keys: get 2, use 1
        //4: 5 keys: get 2, use 2 (initial squishies)
        //5: 4 keys: get 1, use 2 // don't waste getting all the keys, just the one inside the zombie
        //6: 0 key: get 0, use 4 (if you have 4 all good)
        //8: 0 keys: get 2, use 2 (forced one, go right grab)
        // total 20 keys needed only
        // start with 2
        LevelData(
            level = 1,
            // 1: left start
            // 2: right start
            numKeysIn = 6,
            numKeysNeeded = 5 // only if use a bomb
        ),
        LevelData(
            // only got two
            level = 2,
            // 1: (mid, fast) right from start
            // 2: off path left
            // 3: (mid, fast)
            // 4: snake thing
            numKeysIn = 4, //visual 4 (3 if you skip snake)
            numKeysNeeded = 0 // no keys needed is confirmed
        ),
        // strategy, get first 2 keys, and up key for a total of 3
        LevelData(
            level = 3,
            numKeysIn = 4, // 2 not on path
            numKeysNeeded = 1 // if you have bombs you can skip 1 key// visual
        ),
        LevelData(
            level = 4,
            // 1: initial left, kill bats
            // 2: up 2, grab
            // 3: before water, kill squishies
            // 4: kill bats?, bomb up from coin
            numKeysIn = 4,
            numKeysNeeded = 2 // visual, confirmed
        ),
        LevelData(
            level = 5,
            // 1: (mid) kill right bunnies (? kill?
            // 2: (easy) inside mummy grab, Grab
            // 3: (high) have to kill zombie to get in, not worth it yep at 71
            // 4: (mid) kill zombie
            // 5: (mid) rabbits, zombies at 39. Grab
            // 6: (mid), zombies
            numKeysIn = 6,
            numKeysNeeded = 2 //visual
        ),
        LevelData(
            level = 6,
            // 1: (mid) start, ghosts
            // 2: (difficult, but no choice bats)
            // 3: (mid, hard) ghosts
            // 4: (mid, off path) ghosts
            numKeysIn = 4,
            numKeysNeeded = 4 //visual YES
        ),
        LevelData(
            level = 7,
            //1: (mid, off path): fight fast guys and fireball
            //2: (mid, waaay off path): Skeletons way beyond where we normally go
            //3: (mid, off path): fight boomerang guys and fireball
            numKeysIn = 3,
            // before bait
            // after bait (could skip by bombing and wasting time)
            // after red candle
            numKeysNeeded = 3
        ),
        LevelData(
            level = 8,
            // 1: grab? right from start
            // 2: (mid, no choice)
            // 3: far off near boss
            // 4: left from no choice, is it a grab?
            // 5: left, then left from no choice, kill sword guys
            numKeysIn = 5,
            numKeysNeeded = 2 // 2 needed before getting the magic key
        ),
        LevelData(
            level = 9,
            numKeysIn = 1,
            numKeysNeeded = 0
        ),
    )
}
