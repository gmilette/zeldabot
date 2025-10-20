package bot.plan.levels

import bot.plan.InLocations
import bot.plan.Phases
import bot.plan.PlanBuilder
import bot.plan.runner.Experiment
import bot.state.*
import bot.state.map.*
import bot.state.map.destination.Dest
import bot.state.map.destination.ZeldaItem

val raftLadderSetup: Experiment =
    Experiment(
        sword = ZeldaItem.WhiteSword,
        keys = 4,
        bombs = 8,
        rupees = 250,
        hearts = 8,
        shield = true,
        ladderAndRaft = true,
//            ring = ZeldaItem.BlueRing,
        potion = true,
        candle = true,
        arrowAndBow = true,
    )

fun PlanBuilder.itemsNearLevel2CandleShieldPhase() {
    add {
        "candle".seg()
        // more money
        obj(Dest.Shop.candleShopEast) // 199
        "money for shield".seg()
        obj(Dest.Secrets.forest10Mid91)
        obj(Dest.Secrets.forest100South) // 239
//            startHereAtAfterLevel5()
        "get shield".seg()
        obj(Dest.Shop.eastTreeShop) // 149
    }
}

fun PlanBuilder.afterLevel2ItemsLetterEtcPhase(withBombHeart: Boolean = true) {
    add {
        phase(Phases.forest30)
        obj(Dest.Secrets.secretForest30NorthEast)
        phase(Phases.forest30 + "_end")
        obj(Dest.Secrets.bombSecret30North)
        obj(ZeldaItem.Letter)
        obj(Dest.Secrets.walk100)
        if (withBombHeart) {
            obj(Dest.Heart.bombHeartNorth)
        }
    }
}

fun PlanBuilder.woodenSwordPhase() {
    add {
        startAt(InLocations.Overworld.start)
        phase("Opening sequence")
        obj(Dest.item(ZeldaItem.WoodenSword))
    }
}

/**
 * 5 should result in 200 ish
 * 4 = 119
 */
fun PlanBuilder.harvest(n: Int = 4) {
    if (DO_HARVEST) {
        add {
            repeat(n) {
                2 using levelPlan2Harvest2
            }
        }
    }
}

fun PlanBuilder.routeNearLevel8ToGetBombs() {
    add {
        // link routes up and around
        routeTo(127) // near sea corner
        routeTo(111) // one up
        routeTo(63) // corner
        routeTo(46) // corner

//            // good but it makes link walk into the shop it doesn't need to go in
//            routeTo(78) // up near lev2
//            routeTo(127) // near sea corner
//            routeTo(123) // bomb heart
    }
}

fun PlanBuilder.routeNearLevel3ToGetBombs() {
    add {
        routeTo(115) // near lev3
        routeTo(83) // near fairy
        routeTo(81) // burn heart
    }
}

fun PlanBuilder.whiteSword() {
    add {
        objective(ZeldaItem.WhiteSword)
        routeTo(10)
        down
        up
        whiteSwordDodgeRoutine()
        obj(ZeldaItem.WhiteSword)
        downm // do not attack, but it still does!
    }
}

fun PlanBuilder.fireBurn100() {
    add {
        val forestNextToLevel3: MapLoc = 115
        // prevent going down on tile 114
        seg("position burning")
        routeTo(forestNextToLevel3)
        seg("position burning up")
        routeTo(forestNextToLevel3.up)
        seg("position burning left")
        routeTo(forestNextToLevel3.up.left)
        seg("go to obj 100 brown fire")
        obj(Dest.Secrets.fire100SouthBrown)
        // works fine
        seg("away from fire secret")
        up
        right
        // otherwise link might walk back into the secret stairs
//            val forest: MapLoc = 98
//            routeTo(forest.up)
//            routeTo(forest.up.right)
    }
}

fun PlanBuilder.exitReenter() {
    add {
        seg("reenter to not use key")
        goInConsume(GamePad.MoveDown, 30)
        goInConsume(GamePad.MoveUp, 30)
    }
}

fun PlanBuilder.greenPotion() {
    add {
        enoughForPotion
        obj(Dest.Shop.potionShopWest, Objective.ItemLoc.Right)
    }
}

fun PlanBuilder.forestPotion() {
    add {
        enoughForPotion
        obj(Dest.Shop.potionShopForest, Objective.ItemLoc.Right)
    }
}

fun PlanBuilder.potionLevel6() {
    add {
        enoughForPotion
        obj(Dest.Shop.potionShopLevel6, Objective.ItemLoc.Right)
    }
}

fun PlanBuilder.potionLevel6NoBomb() {
    add {
        enoughForPotion
        obj(Dest.Shop.potionShopLevel6, Objective.ItemLoc.Right, action = false)
    }
}

fun PlanBuilder.ringLevels() {
    add {
        // why risk it? get the blue ring first
        // just skip this crap
        // it's currently buggy
        // todo: put back in
        fireBurn100()
        enoughForRing
        obj(Dest.Fairy.brownForest)
//            routeTo(98 - 16) // go up so it routes ok
        obj(Dest.Shop.blueRing, position = true)
        // avoid accidentally going back in
        goIn(GamePad.MoveRight, 25) // test it

        routeTo(115)
        goTo(FramePoint(12.grid, 7.grid))
        // then get closer
        goTo(FramePoint(8.grid, 8.grid))
        goTo(FramePoint(2.grid, 8.grid))
        // grab this cash now, then get fairy so ready to kill level 3
        obj(Dest.Secrets.bombSecret30SouthWest)
        routeTo(115)
        routeTo(99)
        routeTo(83)
        obj(Dest.Secrets.forest10BurnBrown)
        obj(Dest.Fairy.brownForest)

        3 using level3
        phase(Phases.level3After)

        // TODO: ideally if don't need, don't go, if need only half, buy only half
        // route to the potion spot
//            routeTo(100)
        // getPotionConditionally()
        // for now just always buy the big potion
        greenPotion()
        killUntilGetBomb
        // get the potion here if have enough cash
        // possibly only need to refill
        routeTo(102)
        cheatBombs
        4 using level4
    }
}

fun PlanBuilder.arrowAndHearts() {
    add {
        phase("harvest before arrow")
        harvest(5)
        enoughForArrow
        obj(Dest.Shop.arrowShop)
        phase(Phases.ladderHeart)
        obj(Dest.Heart.ladderHeart)
        // exit the heart area
        goIn(GamePad.MoveLeft, 70, monitor = false)
        obj(Dest.Heart.raftHeart, itemLoc = Objective.ItemLoc.Right)
    }
}

fun PlanBuilder.end() {
    add {
        // junk
        left
        right
        right
        end
    }
}

fun PlanBuilder.gatherBombsFirstPhase() {
    this.add {
        objective(ZeldaItem.Bomb)
        val bombLoc: MapLoc = 107
        // before there are bombs
        routeTo(bombLoc.down)
        // make it so your first attack could yield a bomb
        killUntilBombsLikely
        up
        killUntilGetBomb
        up
        killUntilGetBomb
        down
        down
        right
        right
        right
        left
        left
        left
        cheatBombs
//            rightIfNeedBombs
//            rightIfNeedBombs
//            // wait until the monsters appear from smoke
//            goIn(GamePad.MoveRight, 25)
//            killUntilGetBomb(1) // the monster in the water
//            rightIfNeedBombs
//            killUntilGetBomb(1) // the monster in the water
//            leftIfNeedBombs
//            leftIfNeedBombs
//            leftIfNeedBombs
//            routeTo(bombLoc)
        obj(Dest.Heart.bombHeartSouth)
        // avoid getting stuck/ go right first?
//            routeTo(107 - 16)
//            routeTo(107 - 16 + 1)
        routeTo(bombLoc.up)
        routeTo(bombLoc.up.right)
    }
}

fun PlanBuilder.level5sequence(endLevel2BoomerangPickup: Boolean = true) {
    phase("gear for level 6")
    // i think this is not needed,  maybe it's after level5?
//            routeTo(83) // position so we don't go through the 100 secret forest and get stuck
//
    obj(ZeldaItem.PowerBracelet, itemLoc = Objective.ItemLoc.None)
//        startHereAtAfterLevel4AndGather()

    seg("position for magic sword")
////            routeTo(32)
    goToAtPoint(33, FramePoint(11.grid, 3.grid))
//            // hard to get into position when its passable, maybe position it
    obj(ZeldaItem.MagicSword)
//

    6 using level6
    // maybe switch to boomerang here
    potionLevel6()

    phase("go to level 5")
    5 using level5

    phase("Gear for level 8")
    seg("pre 8 potion")
    forestPotion()
//        greenPotion() // for now, but there is a closer one for sure

//            obj(Dest.Shop.potionShopWest, itemLoc = Dest.Shop.ItemLocs.redPotion)

    // is the 10 secret necessary, eh
//            routeTo(78-16)
//            obj(Dest.Secrets.level2secret10)
    // grab level2 boomerang
    phase("get magic boomerang")
    if (endLevel2BoomerangPickup) {
        2 using levelPlan2Boomerang
    } //7.5, 8

    phase("pre 8 harvest")
    harvest(2) // didn't need this at all

    phase("and now level 8")
    // make sure approach from left
    // rather than right
    val aboveLevel8: MapLoc = 93
    routeTo(aboveLevel8)
    left
//        harvest()
    down
    right
    8 using level8
    seg("done with 8 get bombs")
    // was missing bombs
    if (DO_HARVEST) {
        routeNearLevel8ToGetBombs()
    }

    phase("items for level 7")
    enoughForBait
    obj(Dest.Shop.blueRing, itemLoc = Dest.Shop.ItemLocs.bait, position = true)
    greenPotion()
    obj(Dest.Fairy.brownForest)
    // prob do before level 7 too
    if (DO_HARVEST) {
        routeNearLevel3ToGetBombs()
    }
    7 using level7
    if (DO_HARVEST) {
        routeNearLevel3ToGetBombs()
    }
    greenPotion()
    right
    right

    phase("go to level 9")
    9 using level9
    end()
}

fun PlanBuilder.startHereAtAfterLevel4AndGather() {
    startHereAt(
        raftLadderSetup.copy(
            hearts = 12,
            ring = ZeldaItem.BlueRing,
            sword = ZeldaItem.WhiteSword,
            rupees = 16,
            keys = 5,
            bombs = 8,
            potion = true,
            candle = true,
            arrowAndBow = true,
            magicKey = false,
            whistle = false,
            bait = false,
            setTriforce = true,
            boomerang = ZeldaItem.Boomerang
        )
    )
}

fun PlanBuilder.startHereAtAfterLevel5() {
    startHereAt(
        raftLadderSetup.copy(
            hearts = 15,
            ring = ZeldaItem.BlueRing,
            sword = ZeldaItem.MagicSword,
            rupees = 200,
            keys = 4,
            bombs = 1,
            wand = true,
            potion = true,
            candle = true,
            arrowAndBow = true,
            magicKey = false,
            whistle = true,
            bait = false,
            setTriforce = true,
            boomerang = ZeldaItem.Boomerang
        )
    )
}

fun PlanBuilder.startHereAtLoaded(bomb: Boolean = true, rupee: Int = 0) {
    startHereAt(
        raftLadderSetup.copy(
            ladderAndRaft = false,
            hearts = 16,
            rupees = rupee,
            bombs = if (bomb) 8 else 0,
            ring = ZeldaItem.BlueRing,
            sword = ZeldaItem.MagicSword,
            potion = true,
            candle = true,
            arrowAndBow = true,
//            magicArrowAndBow = true,
            magicKey = true,
            whistle = true,
            bait = true,
            setTriforce = true,
            boomerang = ZeldaItem.MagicalBoomerang
        )
    )
}

private const val DO_HARVEST: Boolean = false
