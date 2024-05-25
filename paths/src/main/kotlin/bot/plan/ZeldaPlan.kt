package bot.plan

import bot.plan.action.*
import bot.plan.runner.MasterPlan
import bot.state.*
import bot.state.map.*
import bot.state.map.destination.Dest
import bot.state.map.destination.ZeldaItem
import bot.state.map.level.LevelMapCellsLookup
import bot.state.map.level.LevelSpecBuilder
import bot.state.map.level.LevelStartMapLoc
import java.awt.Frame

object ZeldaPlan {
    enum class PlanOption {
        MAGIC_SHIELD_EARLY,
    }
    val option = PlanOption.MAGIC_SHIELD_EARLY

    fun makeMasterPlan(hyrule: Hyrule, mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val router = OverworldRouter(hyrule)
        val factory = PlanInputs(mapData, levelData, router)
//        return real(factory)
        return realLev1(factory)
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
            obj(Dest.Secrets.bombHeartSouth)
            // position by routing
            val sec:MapLoc = 61
            routeTo(sec.up)
            phase(Phases.forest30)
            obj(Dest.Secrets.secretForest30NorthEast)
            phase(Phases.forest30 + "_end")
            obj(Dest.Secrets.bombSecret30North)
            obj(ZeldaItem.Letter)
            obj(Dest.Secrets.walk100)
            obj(Dest.Secrets.bombHeartNorth)
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
            obj(Dest.level(3))
            includeLevelPlan(levelPlan3(factory))
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
            obj(Dest.level(4))
            includeLevelPlan(levelPlan4(factory))

            phase("grab hearts")
            if (option != PlanOption.MAGIC_SHIELD_EARLY) {
                obj(Dest.Secrets.fire30GreenSouth)
                obj(Dest.Heart.fireHeart)
            }
            obj(Dest.Secrets.bombHeartSouth)
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
            obj(Dest.level(5))
            includeLevelPlan(levelPlan5(factory))
            phase("gear for level 6")
            obj(ZeldaItem.PowerBracelet, itemLoc = Objective.ItemLoc.None)
            goToAtPoint(33, FramePoint(11.grid, 3.grid))
            obj(ZeldaItem.MagicSword)

            obj(Dest.level(6))
            includeLevelPlan(levelPlan6(factory))

            phase("Gear for level 8")
//            obj(Dest.Shop.potionShopWest, itemLoc = Dest.Shop.ItemLocs.redPotion)

            // is the 10 secret necessary, eh
//            routeTo(78-16)
//            obj(Dest.Secrets.level2secret10)
            phase(Phases.afterLevel6)
            // grab level2 boomerang
            obj(Dest.level(2))
            includeLevelPlan(levelPlan2Boomerang(factory))
            obj(Dest.level(8))
            includeLevelPlan(levelPlan8(factory), Direction.Left)

            obj(Dest.Shop.blueRing, itemLoc = Dest.Shop.ItemLocs.bait, position = true)
            obj(Dest.level(7))
            // link starts outside the lake, no need to move down
            includeLevelPlan(levelPlan7(factory), consume = false)

            phase("go to level 9")
            obj(Dest.level(9))
            includeLevelPlan(levelPlan9(factory))

            // junk
            left
            right
            right
            end

//            include(real(factory)) // prevent crash
        }
    }

    private fun realLev1(factory: PlanInputs): MasterPlan {
        val builder = factory.make("begin!")
        val start: PlanBuilder.() -> Unit = {
            // 3 chances to get a bomb
//            routeTo(107+16)
//            killUntilGetBomb(1) // the monster in the water
            routeTo(107)
            killUntilGetBomb
            up
            killUntilGetBomb
            down
            down
            rightIfNeedBombs
            rightIfNeedBombs
            GoIn(50, GamePad.MoveRight)
            // wait until the monsters appear from smoke
            GoIn(2500, GamePad.None)
            killUntilGetBomb(1) // the monster in the water
            leftIfNeedBombs
            leftIfNeedBombs
            obj(Dest.Secrets.bombHeartSouth)
            // avoid getting stuck/ go right first?
            routeTo(107 - 16)
            routeTo(107 - 16 + 1)
        }

        return masterPlan(factory, start)
    }

    private fun real(factory: PlanInputs): MasterPlan {
        val builder = factory.make("begin!")
        val start: PlanBuilder.() -> Unit = {
            obj(Dest.level(2))
            includeLevelPlan(levelPlan2(factory))
        }

        return masterPlan(factory, start,
            endLevel2BoomerangPickup = true)
    }

    private fun testPlan(factory: PlanInputs): MasterPlan {
        val builder = factory.make("begin!")
        return builder {
            startAt(InLocations.Overworld.start)
            obj(Dest.Shop.candleShopMid)
            phase("get magic shield")
            obj(Dest.Shop.westTreeShopNearWater)
            phase("get heart and cash")
            obj(Dest.Heart.fireHeart)
            obj(Dest.Secrets.fire30GreenSouth)
            obj(Dest.level(2))
            includeLevelPlan(levelPlan2Boomerang(factory))
        }
    }

    private fun masterPlan(factory: PlanInputs, start: PlanBuilder.() -> Unit, endLevel2BoomerangPickup: Boolean = false): MasterPlan {
        val builder = factory.make("begin!")
        return builder {
            startAt(InLocations.Overworld.start)
            phase("Opening sequence")
            obj(Dest.item(ZeldaItem.WoodenSword))

            start()

            // position by routing
//            val sec:MapLoc = 61
//            routeTo(sec.up)
            phase(Phases.forest30)
            obj(Dest.Secrets.secretForest30NorthEast)
            phase(Phases.forest30 + "_end")
            obj(Dest.Secrets.bombSecret30North)
            obj(ZeldaItem.Letter)
            obj(Dest.Secrets.walk100)
            obj(Dest.Secrets.bombHeartNorth)
            // need a special plan for white sword guy to get killed less
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
            obj(Dest.level(3))
            includeLevelPlan(levelPlan3(factory))
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
            obj(Dest.level(4))
            includeLevelPlan(levelPlan4(factory))

            phase("grab hearts")
            if (option != PlanOption.MAGIC_SHIELD_EARLY) {
                obj(Dest.Secrets.fire30GreenSouth)
                obj(Dest.Heart.fireHeart)
            }
            obj(Dest.Secrets.bombHeartSouth)
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
            obj(Dest.level(5))
            includeLevelPlan(levelPlan5(factory))
            phase("gear for level 6")
            // i think this is not needed,  maybe it's after level5?
//            routeTo(83) // position so we don't go through the 100 secret forest and get stuck
//
            obj(ZeldaItem.PowerBracelet, itemLoc = Objective.ItemLoc.None)
//            // make up and objective to walk to high up
////            routeTo(32)
            goToAtPoint(33, FramePoint(11.grid, 3.grid))
//            // hard to get into position when its passable, maybe position it
            obj(ZeldaItem.MagicSword)
//
            obj(Dest.level(6))
            includeLevelPlan(levelPlan6(factory))

            phase("Gear for level 8")
//            obj(Dest.Shop.potionShopWest, itemLoc = Dest.Shop.ItemLocs.redPotion)

            // is the 10 secret necessary, eh
//            routeTo(78-16)
//            obj(Dest.Secrets.level2secret10)
            phase(Phases.afterLevel6)
            // grab level2 boomerang
            if (endLevel2BoomerangPickup) {
                obj(Dest.level(2))
                includeLevelPlan(levelPlan2Boomerang(factory))
            }
            obj(Dest.level(8))
            includeLevelPlan(levelPlan8(factory), Direction.Left)

            obj(Dest.Shop.blueRing, itemLoc = Dest.Shop.ItemLocs.bait, position = true)
            obj(Dest.level(7))
            // link starts outside the lake, no need to move down
            includeLevelPlan(levelPlan7(factory), consume = false)

            phase("go to level 9")
            obj(Dest.level(9))
            includeLevelPlan(levelPlan9(factory))

            // junk
            left
            right
            right
            end
        }
    }

    private fun levelPlan1(factory: PlanInputs): MasterPlan {
        val builder = factory.make("Destroy level 1")
        return builder {
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
            go(FramePoint(6.grid, 3.grid))
            killLev1Dragon // aim for the head
            rightm // triforce
            goIn(GamePad.MoveRight, 20)
            seg("get the triforce")
            getTri
        }
    }


    private fun levelPlan2(factory: PlanInputs): MasterPlan {
        val builder = factory.make("Destroy level 2")

        return builder {
            lev(2)
            startAt(LevelStartMapLoc.lev(2))
            seg("gather 3 keys")
            right
            kill
            goTo(InLocations.Level2.keyMid)
            up // nothing here
            seg("gather key 2")
            left
            kill
            left
            goTo(InLocations.Level2.keyMid)
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
            switchToBomb
            seg(Phases.Segment.lev2Boss)
            killLevel2Rhino
            seg("get the triforce")
            loot // in case there is a bomb
            wait
            goTo(InLocations.Level2.heartMid)
            leftm
            goIn(GamePad.MoveLeft, 20)
            getTri
        }
    }

    private fun levelPlan2Boomerang(factory: PlanInputs): MasterPlan {
        val builder = factory.make(Phases.reenterLevel2)

        return builder {
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
            down
            down
            left
            seg("Go to exit")
            down
            seg("move out")
            down // should exit
//            inOverworld
        }
    }

    private fun levelPlan3(factory: PlanInputs): MasterPlan {
        val builder = factory.make(Phases.level3)
        return builder {
            lev(3)
            startAt(LevelStartMapLoc.lev(3))
            seg("grab key")
            leftm
            goTo(InLocations.Level2.keyMid)
            seg("walk round corner")
            up // grab key it's easy
            kill
            loot
            up
            leftm
            seg("past the compass")
            killUntil2
            level3TriggerDoorThen // it's not great but ok
            goIn(GamePad.MoveLeft, 10)
            seg("fight swords")
            kill
            downk
            seg("get raft")
            goIn(GamePad.MoveDown, 10)
            // drop clearing bomb
            goIn(GamePad.B, 2)
            goTo(InLocations.rightStair)
            startAt(15)
            go(InLocations.getItem)
            upTo(105)
            seg("get to back to center")
            upm
            right
            rightNoP
            seg("get to boss")
            upm // option to get key up, but skip
            rightm
            seg("BOMB RIGHT")
            switchToBomb
            level3BombThenRight
            seg("kill boss")
            starKill
            //killFirstAttackBomb
            go(InLocations.Level3.heartMid)
            upm
            getTri
        }
    }

    private fun levelPlan4(factory: PlanInputs): MasterPlan {
        val builder = factory.make("Destroy level 4")
        return builder {
            lev(4)
            startAt(113)
            seg("go go go")
            //key to left but dont bother
            up
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
            kill // there will be 2 suns still running around
            seg("push")
            +makePush(InLocations.Push.moveLeftOfTwo, makeUp(50),
                96, InLocations.StairsLocation.corner)
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
    }

    private fun levelPlan5(factory: PlanInputs): MasterPlan {
        val builder = factory.make(Phases.level5)
        return builder {
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
            addNext(6, makeCenterPush(88, makeUp(6),
                out = InLocations.OutLocation.outRight))
            left
            seg("kill before getting item")
            kill
            seg("push center to get item")
            +makePush(
                InLocations.Push.singleLeft,
                makeUp(lastMapLoc),
                InLocations.Level5.mapLocGetItem,
                stairs = InLocations.StairsLocation.corner)
            seg("backtrack out")
            right
            addNext(100, makeCenterPush(88, makeUp(100),
                out = InLocations.OutLocation.outLeft))

            seg("get back")
            rightm
            right // possibly kill until get bomb IF need bombs
            seg("kill all zombie to open get key")
            killUntilGetKey
            seg("kill all zombie up move up to rhino")
            up
            seg("kill all zombie up move right")
            rightm //85
            seg("no head up to victory")
            upm // impossible maze
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
            seg("Now destroy him")
            kill // problem the projectiles are considered enemies
            seg("Get 5 triforce")
            goTo(InLocations.Level5.triforceHeart)
            upm
            getTri
        }
    }

    private fun levelPlan6(factory: PlanInputs): MasterPlan {
        val builder = factory.make(Phases.level6)
        return builder {
            lev(6)
            startAt(LevelStartMapLoc.lev(6))
            seg("move to level 6")
            left
            seg("first ghost")
            upm // skip
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
            // dont need this key
//            kill
//            loot // another key, this is hard though because of ladder
            up
            up
            seg("get want")
            kill
            +makePush(
                InLocations.Push.moveLeftOfTwo,
                makeUp(lastMapLoc),
                LevelSpecBuilder.getItemLoc6,
                stairs = InLocations.StairsLocation.corner)

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
            addNext(29, makePush(
                InLocations.Push.singleLeft,
                makeUp(lastMapLoc),
                LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outRight,
                stairs = InLocations.StairsLocation.corner)
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
    }

    private fun levelPlan7(factory: PlanInputs): MasterPlan {
        val builder = factory.make(Phases.level7)
        return builder {
            lev(7)
            startAt(LevelStartMapLoc.lev(7))
            upm
            bombUp
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
            upk
            bombRight
            seg("Kill hands")
            killHandsInLevel7
            goTo(FramePoint(2.grid, 8.grid))
            addNext(41, makePush(
                InLocations.Push.right,
                makeUp(41),
                LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.corner)
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
    }

    private fun levelPlan8(factory: PlanInputs): MasterPlan {
        val builder = factory.make(Phases.level8)
        return builder {
            lev(8)
            startAt(LevelStartMapLoc.lev(8))
            seg("run past")
            left
            seg("bomb guy")
            switchToBomb
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
            bombUp
            upm
            killArrowSpider // kill arrow guy
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
            rightk
            addNext(76, goNoPush(makeUp(76),
                startAt = LevelSpecBuilder.getItemMove6,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.rightStairGrid
                ))

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
    }

    private fun PlanBuilder.levelPlan9PhaseRedRing() {
        this.add {
            lev(9)
            startAt(LevelStartMapLoc.lev(9))
            // really dont want to accidently exit because link can't find way back to the entrance
            GoInConsume(30, GamePad.MoveUp)
            upm
            leftm
            bombUp
            kill
            addNext(20,
                makeCenterPush(LevelSpecBuilder.Companion.Nine.travel1,
                    makeUp(20),
                out = InLocations.OutLocation.outLeft))

            "spiral".seg()
            right // kill the pancakes, getting quite stuck
            rightm
            "go down to ring".seg()
            downm
            bombRight
            bombUp
            bombUp
            "ring spot".seg()
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
            GoIn(5, GamePad.MoveLeft)
            "kill travel 1".seg()
            kill
            // trigger trap first
            addNext(99, makePush(
                InLocations.Push.moveLeftOfTwo,
                makeUp(99),
                startAt = LevelSpecBuilder.Companion.Nine.travel2,
                out = InLocations.OutLocation.outLeft,
                stairs = InLocations.StairsLocation.corner)
            )
            "travel to arrow".seg()
            left
            "past bats".seg()
            leftm  //bats
            "circle monster kill".seg()
            kill
            "to in stair".seg()
            addNext(32, makeCenterPush(LevelSpecBuilder.Companion.Nine.travel3, makeUp(32),
                out = InLocations.OutLocation.outLeft))
            bombUp
            "acquire arrow".seg()
            kill
            "set the arrow".seg()
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
            addNext(97, makeCenterPush(88, makeUp(97),
                out = InLocations.OutLocation.outRight))
            upm
            "past first pancake".seg()
            upm
            "past second pancake".seg()
            upm
            "bomb left ok".seg()
            bombLeft
            kill
            "push to inbetween travel".seg()
            addNext(4, makePush(
                InLocations.Push.moveLeftOfTwo,
                makeUp(4),
                startAt = LevelSpecBuilder.Companion.Nine.travel4,
                out = InLocations.OutLocation.outRight,
                stairs = InLocations.StairsLocation.corner)
            )

            "get to final stair".seg() // save7
            bombLeft
            kill
            addNext(82, makeCenterPush(119, makeUp(82),
                out = InLocations.OutLocation.outLeft))
            "doorstep of gannon".seg()
            kill
            upk
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

    private fun levelPlan9(factory: PlanInputs): MasterPlan {
        val builder = factory.make(Phases.level9)
        return builder {
            lev(9)
            levelPlan9PhaseRedRing()
            levelPlan9PhaseSilverArrow()
            levelPlan9PhaseGannon()
            end
        }
    }
}