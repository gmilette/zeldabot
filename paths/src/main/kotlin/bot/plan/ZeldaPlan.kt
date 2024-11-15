package bot.plan

import bot.plan.action.*
import bot.plan.runner.Experiment
import bot.plan.runner.MasterPlan
import bot.state.*
import bot.state.map.*
import bot.state.map.destination.Dest
import bot.state.map.destination.ZeldaItem
import bot.state.map.level.LevelMapCellsLookup
import bot.state.map.level.LevelSpecBuilder
import bot.state.map.level.LevelStartMapLoc

object ZeldaPlan {
    enum class PlanOption {
        MAGIC_SHIELD_EARLY,
    }

    val option = PlanOption.MAGIC_SHIELD_EARLY

    fun makeMasterPlan(hyrule: Hyrule, mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val router = OverworldRouter(hyrule)
        val factory = PlanInputs(mapData, levelData, router)
        return safety(factory)
//        return realLev1(factory)
    }

    private fun levelTour(factory: PlanInputs): MasterPlan {
        val builder = factory.make("tour of levels")
        return builder {
            startAt(InLocations.Overworld.start)
            seg("level1")
            for (i in 1..9) {
                phase("level $i")
                obj(Dest.level(i))
            }
        }
    }

    private fun realLevel1First(factory: PlanInputs): MasterPlan {
        // bomb heart, white sword, level 1, level 3, level 4, level 2
        val builder = factory.make("begin!")
        // go get bomb hearts and white sword
        return builder.invoke {
            startAt(InLocations.Overworld.start)
            phase("Opening sequence")
            obj(Dest.item(ZeldaItem.WoodenSword))
            // 3 chances to get a bomb
            routeTo(107)
            killUntilGetBomb
            up
            killUntilGetBomb
            down
            down
            rightIfNeedBombs
            rightIfNeedBombs
            leftIfNeedBombs
            leftIfNeedBombs
            killUntilGetBomb(1) // the monster in the water
            obj(Dest.Heart.bombHeartSouth)
            // position by routing
            val sec: MapLoc = 61
            routeTo(sec.up)
            phase(Phases.forest30)
            obj(Dest.Secrets.secretForest30NorthEast)
            phase(Phases.forest30 + "_end")
            obj(Dest.Secrets.bombSecret30North)
            obj(ZeldaItem.Letter)
            obj(Dest.Secrets.walk100)
            obj(Dest.Heart.bombHeartNorth)
            obj(ZeldaItem.WhiteSword)
            obj(Dest.level(1))
            includeLevelPlan(levelPlan1(factory))
            obj(Dest.Secrets.bomb30Start) // could move later if we can guarantee level 2 provides
            obj(Dest.Shop.candleShopMid)

            if (option == PlanOption.MAGIC_SHIELD_EARLY) {
                phase("get magic shield")
                obj(Dest.Shop.westTreeShopNearWater)
                phase("get heart and cash")
                obj(Dest.Heart.fireHeart)
                obj(Dest.Secrets.fire30GreenSouth)
            }
            phase(Phases.lev(3))
            3 using level3
            phase(Phases.level3After)
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
            // otherwise link might walk back into the secret stairs
            val forest: MapLoc = 98
            routeTo(forest.down)
            routeTo(forest.down.right)
            obj(Dest.Shop.blueRing, position = true)
            // avoid accidentally going back in
            goIn(GamePad.MoveRight, 25) // test it
//             position place right before level 4
//             go to the right so when link goes up, it will be the right way
            routeTo(102)
            4 using level4

            phase("grab hearts")
            if (option != PlanOption.MAGIC_SHIELD_EARLY) {
                obj(Dest.Secrets.fire30GreenSouth)
                obj(Dest.Heart.fireHeart)
            }
            obj(Dest.Heart.bombHeartSouth)
            obj(Dest.Secrets.forest100South)
            obj(Dest.Shop.arrowShop)
            phase(Phases.ladderHeart)
            obj(Dest.Heart.ladderHeart)
            // exit the heart area
            goIn(GamePad.MoveLeft, 70, monitor = false)
            obj(Dest.Heart.raftHeart, itemLoc = Objective.ItemLoc.Right)
            // go down and make sure to walk off.
            // something like this
//            goToAtPoint(33, FramePoint(11.grid, 3.grid))
            phase("go to level 5")
            5 using level5
            phase("gear for level 6")
            obj(ZeldaItem.PowerBracelet, itemLoc = Objective.ItemLoc.None)
            goToAtPoint(33, FramePoint(11.grid, 3.grid))
            obj(ZeldaItem.MagicSword)
            6 using level6
            // link keeps going back in
            goInConsume(GamePad.MoveDown, 20)

            phase("Gear for level 8")
//            obj(Dest.Shop.potionShopWest, itemLoc = Dest.Shop.ItemLocs.redPotion)

            // is the 10 secret necessary, eh
//            routeTo(78-16)
//            obj(Dest.Secrets.level2secret10)
            phase(Phases.afterLevel6)
            // grab level2 boomerang
            2 using levelPlan2Boomerang
            8 using level8

            obj(Dest.Shop.blueRing, itemLoc = Dest.Shop.ItemLocs.bait, position = true)
            7 using level7

            phase("go to level 9")
            9 using level9

            // junk
            left
            right
            right
            end

//            include(real(factory)) // prevent crash
        }
    }

    private fun safety(factory: PlanInputs): MasterPlan {
        val builder = factory.make("begin!")

        return builder {
            woodenSwordPhase()

            2 using levelPlan2Boomerang
            "gather bombs".seg()
            gatherBombsFirstPhase()

            "gather heart".seg()
            obj(Dest.Heart.bombHeartNorth)
//            2 using level2
            // need the cash to get the candle
//            afterLevel2ItemsLetterEtcPhase(false)

            // should hae enough for candle and shield
            whiteSword()
            routeTo(39)
            obj(Dest.Fairy.greenForest) // 205
            1 using level1
            2 using level2
            // need the cash to get the candle
            afterLevel2ItemsLetterEtcPhase(false)
            itemsNearLevel2CandleShieldPhase()

//            startHereAt(
//                Experiment(
//                    sword = ZeldaItem.WhiteSword,
//                    keys = 4,
//                    bombs = 4,
//                    rupees = 250
//                )
//            )
            // it' not the right save state

            phase("get heart and cash")
            // higher forest secret
            obj(Dest.Secrets.forest30NearDesertForest) // 180
            obj(Dest.Secrets.fire30GreenSouth) //213
            obj(Dest.Heart.fireHeart) // heart
            // now go back to level 1 with 6 hearts

//            obj(Dest.Fairy.greenForest) // 205
            // bomb secret.. later
//            1 using level1 // 238, 219 (after)

            // collect loot loop
            obj(Dest.Secrets.forest10Mid)
            // probably don't need this, but the margin is about 15 so yea get it
            obj(Dest.Secrets.bomb30Start) // 253
            greenPotion()
            ringLevels()
            greenPotion()
//            startHereAt(raftLadderSetup)
            arrowAndHearts()

            // then potion?
            phase("level 5 sequence")
            level5sequence()
        }
    }

    private val raftLadderSetup: Experiment =
        Experiment(
            sword = ZeldaItem.WhiteSword,
            keys = 4,
            bombs = 8,
            rupees = 250,
            hearts = 8,
            shield = true,
            ladderAndRaft = true,
        )

    private fun PlanBuilder.itemsNearLevel2CandleShieldPhase() {
        add {
            "candle".seg()
            // more money
            obj(Dest.Shop.candleShopEast) // 199
            "get shield".seg()
            obj(Dest.Secrets.forest100South) // 239
            obj(Dest.Shop.eastTreeShop) // 149
        }
    }

    private fun PlanBuilder.afterLevel2ItemsLetterEtcPhase(withBombHeart: Boolean = true) {
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

    private fun PlanBuilder.woodenSwordPhase() {
        add {
            startAt(InLocations.Overworld.start)
            phase("Opening sequence")
            obj(Dest.item(ZeldaItem.WoodenSword))
        }
    }

    private fun PlanBuilder.whiteSword() {
        add {
            routeTo(10)
            down
            up
            whiteSwordDodgeRoutine()
            obj(ZeldaItem.WhiteSword)
            downm // do not attack, but it still does!
        }
    }

    private fun PlanBuilder.fireBurn100() {
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

    private fun PlanBuilder.greenPotion() {
        add {
            enoughForPotion
            obj(Dest.Shop.potionShopWest, Objective.ItemLoc.Right)
        }
    }

    private fun PlanBuilder.potionLevel6() {
        add {
            enoughForPotion
            obj(Dest.Shop.potionShopLevel6, Objective.ItemLoc.Right)
        }
    }

    private fun PlanBuilder.potionLevel6NoBomb() {
        add {
            enoughForPotion
            obj(Dest.Shop.potionShopLevel6, Objective.ItemLoc.Right, action = false)
        }
    }

    private fun PlanBuilder.ringLevels() {
        add {
            // why risk it? get the blue ring first
            // just skip this crap
            // it's currently buggy
            // todo: put back in
            fireBurn100()
            enoughForRing
//            routeTo(98 - 16) // go up so it routes ok
            obj(Dest.Shop.blueRing, position = true)
            // avoid accidentally going back in
            goIn(GamePad.MoveRight, 25) // test it

            routeTo(115)
            goTo(FramePoint(12.grid, 7.grid))
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

            // get the potion here if have enough cash
            // possibly only need to refill
            routeTo(102)
            4 using level4
        }
    }

    private fun PlanBuilder.arrowAndHearts() {
        add {
            enoughForArrow
            obj(Dest.Shop.arrowShop)
            phase(Phases.ladderHeart)
            obj(Dest.Heart.ladderHeart)
            // exit the heart area
            goIn(GamePad.MoveLeft, 70, monitor = false)
            obj(Dest.Heart.raftHeart, itemLoc = Objective.ItemLoc.Right)
        }
    }

    private fun PlanBuilder.remaining() {
        add {

        }
    }

    private fun PlanBuilder.end() {
        add {
            // junk
            left
            right
            right
            end
        }
    }


    private fun PlanBuilder.gatherBombsFirstPhase() {
        this.add {
            val bombLoc: MapLoc = 107
            routeTo(bombLoc)
            killUntilGetBomb
            up
            killUntilGetBomb
            down
            down
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

    private fun gatherBombsFirst(factory: PlanInputs): MasterPlan {
        val start: PlanBuilder.() -> Unit = {
            this.gatherBombsFirstPhase()
        }

        return masterPlan(factory, start)
    }

    // get a candle, and magic shield

    private fun real(factory: PlanInputs): MasterPlan {
        val start: PlanBuilder.() -> Unit = {
            obj(Dest.level(2))
            includeLevelPlan(levelPlan2(factory))
        }

        return masterPlan(
            factory, start,
            endLevel2BoomerangPickup = true
        )
    }

    private fun masterPlan(
        factory: PlanInputs, start: PlanBuilder.() -> Unit,
        level2Later: Boolean = true,
        endLevel2BoomerangPickup: Boolean = false
    ): MasterPlan {
        val builder = factory.make("begin!")
        return builder {
            woodenSwordPhase()

            start()

            // position by routing
//            val sec:MapLoc = 61
//            routeTo(sec.up)
            afterLevel2ItemsLetterEtcPhase(true)

            if (level2Later) {
                obj(Dest.level(2))
                includeLevelPlan(levelPlan2(factory))
            }

            // need a special plan for white sword guy to get killed less
            // need to move around the sword guy
            whiteSword()

            obj(Dest.level(1))
            includeLevelPlan(levelPlan1(factory))
            obj(Dest.Secrets.bomb30Start) // could move later if we can guarantee level 2 provides
            obj(Dest.Shop.candleShopMid)

            if (option == PlanOption.MAGIC_SHIELD_EARLY) {
                phase("get magic shield")
                obj(Dest.Shop.westTreeShopNearWater)
                phase("get heart and cash")
                obj(Dest.Heart.fireHeart)
                obj(Dest.Secrets.fire30GreenSouth)
            }

            ringLevels()

            phase("grab hearts")
            if (option != PlanOption.MAGIC_SHIELD_EARLY) {
                obj(Dest.Secrets.fire30GreenSouth)
                obj(Dest.Heart.fireHeart)
            }
            obj(Dest.Heart.bombHeartSouth)
            obj(Dest.Secrets.forest100South)
            arrowAndHearts()
            // go down and make sure to walk off.
            // something like this
//            goToAtPoint(33, FramePoint(11.grid, 3.grid))
        }
    }

    private fun PlanBuilder.level5sequence(endLevel2BoomerangPickup: Boolean = true) {
        phase("go to level 5")
        5 using level5

        phase("gear for level 6")
        // i think this is not needed,  maybe it's after level5?
//            routeTo(83) // position so we don't go through the 100 secret forest and get stuck
//
        obj(ZeldaItem.PowerBracelet, itemLoc = Objective.ItemLoc.None)

        // level6 potion
        potionLevel6()
////            routeTo(32)
        goToAtPoint(33, FramePoint(11.grid, 3.grid))
//            // hard to get into position when its passable, maybe position it
        obj(ZeldaItem.MagicSword)
//
        6 using level6
        potionLevel6NoBomb()

        phase("Gear for level 8")
//            obj(Dest.Shop.potionShopWest, itemLoc = Dest.Shop.ItemLocs.redPotion)

        // is the 10 secret necessary, eh
//            routeTo(78-16)
//            obj(Dest.Secrets.level2secret10)
        // grab level2 boomerang
        if (endLevel2BoomerangPickup) {
            2 using levelPlan2Boomerang
        }
        seg("and now level 8")
        // make sure approach from left
        // rather than right
        val aboveLevel8: MapLoc = 93
        routeTo(aboveLevel8)
        left
        down
        right
        8 using level8

        enoughForBait
        obj(Dest.Shop.blueRing, itemLoc = Dest.Shop.ItemLocs.bait, position = true)
        greenPotion()
        7 using level7
        startHereAt(raftLadderSetup.copy(
            hearts = 15,
            ring = ZeldaItem.BlueRing,
            sword = ZeldaItem.MagicSword,
            potion = true,
            candle = true,
            arrowAndBow = true,
            magicKey = true,
            whistle = true
        )
        )
        greenPotion()
        right
        right

        phase("go to level 9")
        9 using level9
        end()
    }

    private val level1: PlanBuilder.() -> Unit
        get() = {
            add {
                phase(Phases.lev(1))
                lev(1)
                inLevel
                startAt(LevelStartMapLoc.lev(1))
                objective(ZeldaItem.Bow)
                seg("grab key")
                left
                goIn(GamePad.MoveLeft, 30)
                kill //164, 192
                goTo(InLocations.Level1.key114)
                right
                seg("go to key skeleton")
                rightm
                // need kill until loot
                seg("grab from skeleton")
                killUntilGetKey
//            pickupDeadItem
                seg("move to arrow")
                left // first rooms
                up //99
                up //83
                goIn(GamePad.MoveUp, 5)
                seg("get key from skeletons")
                kill // these skeletons provide a key
                goTo(InLocations.Level1.key83)
                seg("Bomb and move")
                bombUp
                upm // 51
                seg("grab key from zig")
                killUntilGetKey
                seg("get key from boomerang guys")
                up
                goIn(GamePad.MoveUp, 30)
                kill
                goTo(InLocations.Level1.key83)
                // get around a hard corner
                goTo(FramePoint(7.grid, 2.grid))
                goTo(FramePoint(3.grid, 2.grid))
                seg("get arrow")
                leftm
                seg("push action")
                goIn(GamePad.MoveLeft, MapConstants.oneGridPoint5) // dodge the traps by moving in
                +makeCenterPush(127, makeUp(34))
                // because of bad routing link still goes back and forth ad just the wrong time
                // try going up?
                // maybe i have to fake out the traps
                val beforeExit = FramePoint(12.grid, 5.grid)
                val bottomBefore = FramePoint(7.grid, 7.grid)
                val bottom = FramePoint(7.grid, 8.grid)
                // fake maneuver
                goTo(bottomBefore, ignoreProjectiles = true)
                goTo(bottom, ignoreProjectiles = true)
                goTo(bottomBefore, ignoreProjectiles = true)
                goIn(GamePad.None, 100)
                goTo(beforeExit, ignoreProjectiles = true)
                goIn(GamePad.MoveRight, MapConstants.twoGrid)
                rightNoP // don't attack
                seg("snag boomerang", ZeldaItem.Boomerang)
                down.down // at 67 now
                right // boomerang
                goIn(GamePad.MoveRight, 30)
                kill
                goAbout(InLocations.Level1.boomerang68, 1, 1, true, ignoreProjectiles = true)
                rightm //69 hand grabby, dont get loot
                seg("steal key from hand grabby", ZeldaItem.Triforce)
                go(InLocations.Level1.key114Position)
                go(InLocations.Level1.key114)
                // should do but too risky for now
//                .go(InLocations.Level1.key114)
                up
                seg("destroy dragon")
                // avoid first round of fireballs
                // no go up near the edge
//                go(FramePoint(6.grid, 3.grid))
                go(FramePoint(11 .grid, 6.grid))
                killLev1Dragon // aim for the head
                rightm // triforce
                goIn(GamePad.MoveRight, 20)
                seg("get the triforce")
                getTri
            }
        }


    private fun levelPlan1(factory: PlanInputs): MasterPlan {
        val builder = factory.make("Destroy level 1")
        return builder {
            level1()
        }
    }

    private val level2: PlanBuilder.() -> Unit
        get() = {
            add {
                phase(Phases.lev(2))
                lev(2)
                startAt(LevelStartMapLoc.lev(2))
                seg("gather 3 keys")
                right
                kill
                goTo(InLocations.Level2.keyMid)
                loot // maybe try to get loot
                up // nothing here
                seg("gather key 2")
                left
                kill
                left
                goTo(InLocations.Level2.keyMid)
                loot
                right
                right // grid room
                seg("sprint up from grid")
                up
                seg("go get blue boomerang")
                upm
                seg("gather key 3")
                kill
                loot // key 2
                seg("turn right to get boomerang")
//            right
//            kill
//            // the boomerang is a projectile! do not avoid it
//            goAbout(InLocations.Level2.keyMid, 1, 1, true, ignoreProjectiles = true)
//            left
                seg("resume sprint")
                up
                // skip getting key from squishy guy
//            .kill
//            .goTo(InLocations.Level2.keyMid)
                upNoBlock // the squishy guy appears like a projectile so do not block
                kill
                seg("bomb room")
                // no key I think
//            goTo(InLocations.Level2.keyMid)
                up
                kill // blocked before going // allow bombs
                goTo(InLocations.Level2.bombItemRight)
                up
                seg(Phases.Segment.lev2Boss)
                switchToBomb
                killLevel2Rhino
                seg("get the triforce")
                wait(200) // there might be a bomb appearing that I want
                goTo(InLocations.Level2.heartMid)
                loot // in case there is a bomb
                leftm
                goIn(GamePad.MoveLeft, 20)
                getTri
            }
        }

//    private fun PlanBuilder.level2() {
//        add {
//            lev(2)
//            startAt(LevelStartMapLoc.lev(2))
//            seg("gather 3 keys")
//            right
//            kill
//            goTo(InLocations.Level2.keyMid)
//            loot // maybe try to get loot
//            up // nothing here
//            seg("gather key 2")
//            left
//            kill
//            left
//            goTo(InLocations.Level2.keyMid)
//            loot
//            right
//            right // grid room
//            seg("sprint up from grid")
//            up
//            seg("go get blue boomerang")
//            upm
//            seg("gather key 3")
//            kill
//            loot // key 2
//            seg("turn right to get boomerang")
////            right
////            kill
////            // the boomerang is a projectile! do not avoid it
////            goAbout(InLocations.Level2.keyMid, 1, 1, true, ignoreProjectiles = true)
////            left
//            seg("resume sprint")
//            up
//            // skip getting key from squishy guy
////            .kill
////            .goTo(InLocations.Level2.keyMid)
//            upNoBlock // the squishy guy appears like a projectile so do not block
//            kill
//            seg("bomb room")
//            // no key I think
////            goTo(InLocations.Level2.keyMid)
//            up
//            kill // blocked before going // allow bombs
//            goTo(InLocations.Level2.bombItemRight)
//            up
//            seg(Phases.Segment.lev2Boss)
//            switchToBomb
//            killLevel2Rhino
//            seg("get the triforce")
////            wait // why?
//            goTo(InLocations.Level2.heartMid)
//            loot // in case there is a bomb
//            leftm
//            goIn(GamePad.MoveLeft, 20)
//            getTri
//        }
//    }

    private fun levelPlan2(factory: PlanInputs): MasterPlan {
        val builder = factory.make("Destroy level 2")

        return builder {
            lev(2)
            startAt(LevelStartMapLoc.lev(2))
            seg("gather 3 keys")
            right
            kill
            goTo(InLocations.Level2.keyMid)
            loot // maybe try to get loot
            up // nothing here
            seg("gather key 2")
            left
            kill
            left
            goTo(InLocations.Level2.keyMid)
            loot
            right
            right // grid room
            seg("sprint up from grid")
            up
            seg("go get blue boomerang")
            upm
            seg("gather key 3")
            kill
            loot // key 2
            seg("turn right to get boomerang")
//            right
//            kill
//            // the boomerang is a projectile! do not avoid it
//            goAbout(InLocations.Level2.keyMid, 1, 1, true, ignoreProjectiles = true)
//            left
            seg("resume sprint")
            up
            // skip getting key from squishy guy
//            .kill
//            .goTo(InLocations.Level2.keyMid)
            upNoBlock // the squishy guy appears like a projectile so do not block
            kill
            seg("bomb room")
            // no key I think
//            goTo(InLocations.Level2.keyMid)
            up
            kill // blocked before going // allow bombs
            goTo(InLocations.Level2.bombItemRight)
            up
            seg(Phases.Segment.lev2Boss)
            switchToBomb
            killLevel2Rhino
            seg("get the triforce")
//            wait // why?
            goTo(InLocations.Level2.heartMid)
            wait(200) // wait for a bomb to appear?
            loot // in case there is a bomb
            leftm
            goIn(GamePad.MoveLeft, 20)
            getTri
        }
    }

    private val levelPlan2Boomerang: PlanBuilder.() -> Unit
        get() = {
            phase(Phases.reenterLevel2)
            lev(2)
            startAt(LevelStartMapLoc.lev(2))
            seg("go to boomerang")
            up
            right
            up
            up
            right
            seg("get boomerang")
            kill
            goAbout(InLocations.Level2.keyMid, 1, 1, true, ignoreProjectiles = true)
            seg("depart")
            left
            kill // door is locked until all killed
            down
            down
            left
            seg("Go to exit")
            down
            seg("move out")
            // try inside nav instead
//            downTo(60)
            goInConsume(GamePad.MoveDown,82)
            startAt(60)
        }
    }

    private val level3: PlanBuilder.() -> Unit
        get() = {
            phase(Phases.lev(3))
            lev(3)
            startAt(LevelStartMapLoc.lev(3))
            seg("grab key")
            left
            goTo(InLocations.Level2.keyMid)
            seg("walk round corner")
            up // grab key it's easy
            kill
            loot
            up
            leftm
            // get past trap
            goIn(GamePad.MoveLeft, 30)
            seg("past the compass")
            killUntil2
            level3TriggerDoorThen // it's not great but ok
            goIn(GamePad.MoveLeft, 10)
            seg("fight swords")
            kill // don't attack half
            down  // downk, i think it as causing no completing
            seg("get raft")
            goIn(GamePad.MoveDown, 10)
            // drop clearing bomb
            goIn(GamePad.B, 6)
            goTo(InLocations.rightStair)
            startAt(15)
            go(InLocations.getItem)
            upTo(105)
            seg("get to back to center")
            upm
            rightNoP
            seg("right no p")
            goTo(FramePoint(12.grid, 5.grid))
            goIn(GamePad.MoveRight, MapConstants.twoGrid)
            rightNoP
            seg("get to boss")
            upm // option to get key up, but skip
            seg("Keys from squishy")
            kill
            goTo(InLocations.Level3.keyElbowSquishy)
            // walk past trap ??
            rightm
            goIn(GamePad.MoveRight, MapConstants.threeGrid)
            seg("BOMB RIGHT")
            switchToBomb
            level3BombThenRight
            seg("kill boss")
            starKill
            go(InLocations.Level3.heartMid)
            upm
            getTri
        }

private val level4: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(4))
        lev(4)
        startAt(113)
        seg("go go go")
        //key to left but dont bother
        up
        switchToBoomerang
        up // no get it because it is in the middle of the room
        goTo(InLocations.Level4.batKey)
        leftm
        up
        seg("get key from squishies")
        goTo(InLocations.Level4.squishyKey)
        up
        seg("go get ladder")
        rightm
        kill
        right
        // watch out for pancakes
        kill // there will be 2 suns still running around
        seg("push")
        +makePush(
            InLocations.Push.moveLeftOfTwo, makeUp(50),
            96, InLocations.StairsLocation.corner
        )
        leftm
        goIn(GamePad.MoveLeft, 11.grid, monitor = false) // custom action to walk across using the ladder
        leftm
        seg("get past 4 monster")
        up
        up
        seg("get to the dragon")
        bombRightExactly
        //skip key that is up
        bombRight
        switchToBoomerang
        kill
        seg("push near")
        pushActionThenGoRight(InLocations.Push.moveLeftOfTwo)
        seg("fight dragon")
        killLev4Dragon // dragon
        //get heart
        goTo(InLocations.Level4.triforceHeart)
        upm
        getTri
    }

private val level5: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(5))
        lev(5)
        startAt(LevelStartMapLoc.lev(5))
        seg("move to level 5")
        upm
        seg("left past key")
        bombLeft
        bombLeft
        seg("kill before going in")
        kill
        seg("go in")
        addNext(
            6, makeCenterPush(
                88, makeUp(6),
                out = InLocations.OutLocation.outRight
            )
        )
        left
        seg("kill before getting item")
        kill
        seg("push center to get item")
        +makePush(
            InLocations.Push.singleLeft,
            makeUp(lastMapLoc),
            InLocations.Level5.mapLocGetItem,
            stairs = InLocations.StairsLocation.corner
        )
        seg("backtrack out")
        right
        addNext(
            100, makeCenterPush(
                88, makeUp(100),
                out = InLocations.OutLocation.outLeft
            )
        )

        seg("get back")
        rightm
        seg("get back extra")
        right // possibly kill until get bomb IF need bombs
        seg("kill all zombie to open get key")
        killUntilGetKey
        seg("rhino bypass")
        kill // have to kill all to move up
        up
        seg("kill all zombie up move right")
        rightm //dont shoot
        seg("no head up to victory")
        up // impossible maze with the squishies
        seg("zombie1")
        upm
        seg("zombie2")
        upm
        seg("go left to victory")
        // key??
        goTo(FramePoint(8.grid, 6.grid))
        leftm
        leftm
        leftm
        seg("Use Whistle")
        goIn(GamePad.MoveLeft, 10) // more in a bit before whistlin'
        switchToWhistle()
        goIn(GamePad.None, 50) // more in a bit before whistlin'
        goIn(GamePad.MoveLeft, 20) // move more in
        useItem()
        wait(100) // wait for whistle to happen, otherwise bot will route uselessly
        switchToBoomerang
        seg("Now destroy him")
        kill // problem the projectiles are considered enemies
        seg("Get 5 triforce")
        goTo(InLocations.Level5.triforceHeart)
        upm
        getTri
    }

private val level6: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(6))
        lev(6)
        startAt(LevelStartMapLoc.lev(6))
        seg("move to level 6")
        left
        seg("first ghost")
        up // capture any loot dropped, there is usually some bombs
        switchToBoomerang
        seg("squishies")
        up // todo grab key in center
        seg("kill bats")
        // have to kill them to go up
        kill // can't there too many fireballs, just move on
        // get key after kill, not needed because kill gets loot
//            goTo(FramePoint(8.grid, 5.grid))
        up
        seg("kill and push to continue")
        goIn(GamePad.MoveUp, 6.grid, monitor = false) // custom action avoid traps, just walk straight
        upm
        killLongWait
        pushActionThenGoUp(InLocations.Push.moveLeftOfTwo)
        // don't need thisP
//            killUntil(5) // leave some alive, just do some damage so bomb is successful
        bombRightExactly
        seg("go up to get wand")
        switchToBoomerang // so i can get the pancakes
        // dont need this key
//            kill
//            loot // another key,  this is hard though because of ladder
        up
        up
        seg("get want")
        kill
        +makePush(
            InLocations.Push.moveLeftOfTwo,
            makeUp(lastMapLoc),
            LevelSpecBuilder.getItemLoc6,
            stairs = InLocations.StairsLocation.corner
        )

        seg("go down to other stair")
        down //25
        down //41
        // pick up key in center
        goTo(InLocations.Level6.keyCenter)
        down //57
        startAt(57)
        kill
        rightk
        startAt(58)//save6
        seg("center move stair")
        kill
        addNext(
            29, makePush(
                InLocations.Push.singleLeft,
                makeUp(lastMapLoc),
                LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outRight,
                stairs = InLocations.StairsLocation.corner
            )
        )
        down
        left
        level6TriggerDoorThenUp
        killArrowSpider
        goTo(InLocations.Level6.triforceHeart)
        // need
        upm
        getTri
    }

private val level7: PlanBuilder.() -> Unit
    get() = {
        // all boomerang
        phase(Phases.lev(7))
        lev(7)
        startAt(LevelStartMapLoc.lev(7))
        upm
        bombUp
        switchToBoomerang
        upm
        seg("past water")
        kill //2
        upk
        leftm
        up
        seg("bait spot")
        goIn(GamePad.MoveUp, 20)
        switchToBait()
        goIn(GamePad.None, 100)
        goIn(GamePad.MoveUp, 20) // move more in
        goTo(FramePoint(8.grid, 7.grid))
        useItem()
        switchToBoomerang
        upm
        rightm
        bombRight
        seg("red candle")
        // can't kill the guy inside
        // so skip
        killAllInCenter
        +makeCenterPush(88, makeUp(26))
        bombRight
        rightm
        seg("kill whistle")
        goIn(GamePad.MoveRight, 20) // more in a bit before whistlin'
        switchToWhistle()
        goIn(GamePad.None, 50)
        goIn(GamePad.MoveRight, 20) // move more in
        useItem()
        wait(300)
        kill
        upk
        bombRight
        seg("Kill hands")
        switchToWand
        killHandsInLevel7
        goTo(FramePoint(2.grid, 8.grid))
        addNext(
            41, makePush(
                InLocations.Push.right,
                makeUp(41),
                LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.corner
            )
        )
        seg("near dragon")
        // just wait until switching
        goIn(GamePad.None, 100)
        seg("dragon")
        bombRight
        killLev1Dragon // aim for the head
        goTo(InLocations.Level7.triforceHeart)
        rightm
        getTri
    }

private val level8: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(8))
        lev(8)
        startAt(LevelStartMapLoc.lev(8))
        seg("run past")
        goIn(GamePad.MoveUp, 10)
        switchToBomb
        left
        seg("bomb guy")
        killFirstAttackBomb
        // get the coin
        goTo(FramePoint(8.grid, 5.grid))
        left
        seg("go get book")
        kill
        +makeCenterPush(LevelSpecBuilder.getItemLoc8, makeUp(124))
        seg("get back to start")
        rightm
        rightm // at start
        seg("get to crossroads")
        up
        "bomb".seg()
        bombUp
        kill
        goTo(InLocations.Level8.keySpot)
        upm
        upm // master battle
        // clear out a little before moving on
        killUntil(4)
        bombUp
        upm
        killArrowSpider // kill arrow guy
        loot // spider tends to generate loot sometimes
        rightm
        seg("get key")
        killAllInCenter
        +makeCenterPush(LevelSpecBuilder.getItemLoc8Key, makeUp(31))
        seg("get back to master battle")
        left
        down
        down
        kill // master battle
        seg("take stair to end")
        right // master battle
        killUntil(4) // kill some, otherwise link just tries to run through them, which is not good
        // 31, 46, 62
        addNext(
            76, goNoPush(
                makeUp(76),
                startAt = LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.rightStairGrid
            )
        )

        switchToArrowConditionally()
        // give time to enter so that switching to bomb works
        goTo(FramePoint(11.grid, 2.grid))
        "bomb to get dragon".seg()
        bombUp
        "kill dragon".seg()
        killLev4Dragon // dragon
        goTo(InLocations.Level8.triforceHeartAbove)
        goTo(InLocations.Level8.triforceHeart)
        upm
        getTri
    }

private fun PlanBuilder.levelPlan9PhaseRedRing() {
    this.add {
        lev(9)
        startAt(LevelStartMapLoc.lev(9))
        // really dont want to accidently exit because link can't find way back to the entrance
        goInConsume(GamePad.MoveUp, 30)
        upm
        leftm
        bombUp
        switchToWand
        wait(100)
        kill
        switchToBoomerang
        addNext(
            20,
            makeCenterPush(
                LevelSpecBuilder.Companion.Nine.travel1,
                makeUp(20),
                out = InLocations.OutLocation.outLeft
            )
        )

        "spiral".seg()
        right // kill the pancakes, getting quite stuck
        rightm
        "go down to ring".seg()
        downm
        bombRight
        bombUp
        bombUp
        "ring spot".seg()
//        switchToWand // it doesnt wait and gets to pushing
        kill
        "ring spot push".seg()
        +makeCenterPush(LevelSpecBuilder.getItemLoc8, makeUp(lastMapLoc))
    }
}

private fun PlanBuilder.levelPlan9PhaseSilverArrow() {
    add {
        down // kill pancake
        downm
        leftm
        upm
        "go in next room".seg()
        upm
        bombLeft
        GoIn(10, GamePad.MoveLeft)
        "kill travel 1".seg()
        kill
        // trigger trap first
        addNext(
            99, makePush(
                InLocations.Push.moveLeftOfTwo,
                makeUp(99),
                startAt = LevelSpecBuilder.Companion.Nine.travel2,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.corner
            )
        )
        "travel to arrow".seg()
        kill // kill the squishies looking for hearts
        left
        "past bats".seg()
        leftm  //bats
        "circle monster kill".seg()
        kill
        "to in stair".seg()
        addNext(
            32, makeCenterPush(
                LevelSpecBuilder.Companion.Nine.travel3, makeUp(32),
                out = InLocations.OutLocation.outLeft
            )
        )
        bombUp
        "acquire arrow".seg()
        switchToWand
        wait(100)
        kill
        "set the arrow".seg()
        goTo(FramePoint(13.grid, 5.grid), ignoreProjectiles = true) // other side
        +makeStairsItemPush(
            startAt = LevelSpecBuilder.Companion.Nine.silverArrow,
            makeUp(lastMapLoc)
        )

    }
}

private fun PlanBuilder.levelPlan9PhaseGannon() {
    add {
        "return to center".seg()
        down
        kill
        "take stair back".seg()
        addNext(
            97, makeCenterPush(
                88, makeUp(97),
                out = InLocations.OutLocation.outRight
            )
        )
        // maybe it would be ok to use boomerang here
        upm
        "past first pancake".seg()
        upm
        "past second pancake".seg()
        upm
        "bomb left ok".seg()
        bombLeft
        kill
        "push to inbetween travel".seg()
        addNext(
            4, makePush(
                InLocations.Push.moveLeftOfTwo,
                makeUp(4),
                startAt = LevelSpecBuilder.Companion.Nine.travel4,
                out = InLocations.OutLocation.outRight,
                stairs = InLocations.StairsLocation.corner
            )
        )

        "get to final stair".seg() // save7
        bombLeft
        // not really needed but it would be nice
//        switchToBoomerang
        kill
        addNext(
            82, makeCenterPush(
                119, makeUp(82),
                out = InLocations.OutLocation.outLeft
            )
        )
        "doorstep of gannon".seg()
        goInConsume(GamePad.MoveLeft, 10)
        kill
//        killCenterMonster
        up
        "seg kill gannon".seg()
        goIn(GamePad.None, 100)
        switchToArrow()
        goTo(InLocations.Level9.centerGannonAttack)
        killG
        lootInside
        upm
        "seg get princess".seg()
        rescuePrincess()
        peaceReturnsToHyrule()
        // display some summary stats before ending
//            booya
        end
    }
}

private val level9: PlanBuilder.() -> Unit
    get() = {
        phase(Phases.lev(9))
        lev(9)
        levelPlan9PhaseRedRing()
        levelPlan9PhaseSilverArrow()
        levelPlan9PhaseGannon()
        end
    }
