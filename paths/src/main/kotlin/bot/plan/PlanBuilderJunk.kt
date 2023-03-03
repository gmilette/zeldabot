import bot.plan.InLocations
import bot.state.FramePoint
import bot.state.map.Direction

//package bot.plan
//
//import bot.GamePad
//import bot.plan.runner.MasterPlan
//import bot.state.FramePoint
//import bot.state.MapLoc
//import bot.state.map.*
//import bot.state.map.level.LevelMapCellsLookup
//import sequence.*
//import sequence.Dest.Shop
//
//// master plan
//// - plan phase (main thing doing, get to lev 1, gather stuff for lev 3, just
//// a name
////  - plan segment (sub route
////  -- plan objective (per screen)
//
//object InLocations {
//    val topMiddleBombSpot = FramePoint(120, 32)
//    val diamondLeftTopPush = FramePoint(96, 64)
//    val diamondLeftBottomPush = FramePoint(96, 96)
//    val middleStair = FramePoint(127, 80)
//    val getItem = FramePoint(135, 80) // right side
//    val rightStair = FramePoint(209, 81)
//    val rightTop = FramePoint(208, 32) //todo
//
//    object Overworld {
//        val woodSwordEntry = FramePoint(64, 17)
//        val bombHeartSouth = FramePoint(136, 24)
//        val arrowShop = FramePoint(48, 16)
//
//        val shopRightItem = FramePoint(152, 96) // 97 failed to get heart
////        val selectHeart = FramePoint(152, 90) // still at location 44
//        val centerItem = FramePoint(118, 88) // not 96
//        val centerItemLetter = FramePoint(120, 88)
//        val shopHeartItem = FramePoint(152, 96)
//        val shopLeftItem = FramePoint(88, 88)
//
//        val centerLevelEntry = FramePoint(112, 64)
//
//        val ladderHeartShore = FramePoint(127, 80)
//        val ladderHeart = FramePoint(191, 80)
//        val inDungeonRaftHeard = FramePoint(96, 65)
//
//        // use
//        val map61PushDown = FramePoint(144, 48) // ?
//        val map61PushDownEntrance = FramePoint(144, 64) // ?
//
//        val pushBlueRingShop = InGrid(4, 4).pt
//
//        val letterEntrance = FramePoint(80, 64)
//        val secret100Entrance = FramePoint(128, 64)
//        val centerGetCoin = FramePoint(120, 96) // same as center item
//
//        // ??
////        val bombSecret30NorthEntrance = FramePoint(80, 16)
//        val bombSecret30NorthEntrance = FramePoint(80, 18) // a little more?
////        val bombHeartTarget = FramePoint(147, 112)
//        val bombHeartEntrance = FramePoint(144, 96)
//
//        val whiteSwordEntrance = FramePoint(32, 16)
//        // push down spot
//        val powerBraceletEntrance = FramePoint(224, 48)
//        val powerBraceletItem = FramePoint(224, 62)
//
//        val level6PotionsBombFront = FramePoint(160, 32)
//        val level6PotionsBombEntrance = FramePoint(160, 16)
//        val level6Entrance = FramePoint(120, 64)
//        val magicSwordPushDown = FramePoint(144, 64)
//        val magicSwordEntrance = FramePoint(144, 80)
//
//        val burnHeartEntrance = InGrid(MapConstants.gridMaxX - 5, 6 + 1).pt
//
//        // modify map here
//        val burnHeartShop = InGrid(MapConstants.gridMaxX - 7, 6 + 1).pt
//        val burn30NearLev4 = InGrid(MapConstants.gridMaxX - 7, 6).pt
//
//        // 128, 32
//        val burn100Left = InGrid(8, 2).pt
//        val burn100Right = InGrid(9, 6).pt
//        val burn100RightNear = InGrid(9, 6).pt
//        val burnShopNearLev2 = InGrid(MapConstants.gridMaxX - 3, 6).pt
//
//        val candleShopEntrance = InGrid(7, 2).pt
//
//        object Map {
//            val burn100: MapName = MapName(98, "burn 100")
//            val burnHeart: MapName = MapName(71, "burn heart")
//            val burnSecret30NearLev1: MapName = MapName(72, "burnSecret30NearLev1")
//
//            val secretForest30: MapName = MapName(61, "secret forest")
//            val letter: MapName = MapName(14, "letter")
//            val secret100Right: MapName = MapName(15, "100Secret right")
//            val whiteSword: MapName = MapName(10, "white sword")
//            val candleShop: MapName = MapName(102, "candle shop")
//            val secret100Left: MapName = MapName(98, "100 secret left")
//            val shopBlueRing: MapName = MapName(52, "shopBlueRing")
//            val bombHeartNorth: MapName = MapName(44, "bombHeartNorth")
//            val bombSecret30North: MapName = MapName(45, "bombSecret30North")
//            val bombHeartSouth: MapName = MapName(123, "bombHeartSouth")
//            val arrowShop: MapName = MapName(111, "arrowShop")
//            val ladderHeartShore: MapName = MapName(95, "ladderHeartShore")
//            val raftHeartStart: MapName = MapName(63, "raftHeartStart")
//            val powerBracelet: MapName = MapName(36, "powerBracelet")
//            val magicSword: MapName = MapName(33, "magicSword")
//        }
//
//        val start: MapLoc = 119
//        fun level(level: Int): MapLoc =
//            levelMapLoc[level] ?: 0
//
//        val levelMapLoc = mapOf(
//            1 to 55,
//            2 to 60,
//            3 to 116,
//            4 to 69,
//            5 to 11,
//            6 to 34,
//            7 to 66,
//            8 to 119,
//            9 to 5
//        )
//    }
//
//    object Level1 {
//        val key114 = FramePoint(160, 128)
//        val key83 = FramePoint(128, 50)
//
//        // hands level1, prob same as key114
//        val key69 = FramePoint(164, 128)
//        val boomerang68 = FramePoint(128, 56)
//    }
//
//    object Level2 {
//        val start = 125
//        val heartMid = FramePoint(128, 88) //boss heart
//        val keyMid = FramePoint(128, 88)
//        val keyMidDown = FramePoint(128, 81)
//        val bombRight = FramePoint(208, 43)
//        val triforce = FramePoint(120, 88) // get the middle of the triangle at the top
//    }
//
//    object Level3 {
//        val heartMid = FramePoint(128, 88)
//        val levelEntry = FramePoint(128, 64)
//    }
//
//    object Level4 {
//        val batKey = FramePoint(144, 88)
//        val squishyKey = FramePoint(135, 64)
//        val moveLeftOfTwo = FramePoint(96, 64)
//        val triforceHeart = FramePoint(208, 123)
//    }
//
//    object BombDirection {
//        val right = FramePoint(200, 92) //?
//    }
//
//}
//
//object PlanBuilder {
//    val level1Pt = FramePoint(112, 64) // 74
//    val bombTarget = FramePoint(147, 112)
//    val bombEntrance = FramePoint(144, 104) // wrong
//    val bombHeartEntrance = FramePoint(114, 94) // wrong
//    val selectHeart = FramePoint(152, 90) // still at location 44
//    val getStuffMid = FramePoint(120, 96)
//    val letterEntry = FramePoint(80, 64) // right before 80 80
//
//    fun makeMasterPlan(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
//        val planO = AnalysisPlanBuilder.MasterPlanOptimizer(Hyrule())
//
//        val factory = SequenceFactory(mapData, levelData, planO)
//
////        return levelTour(factory)
//        return real3(factory)
//        // save game 6
////        builder.startAt(44).seg("bomb heart")
////            .bomb(bomb)
////            .go(bombEntrance)
//////            .goIn()
////            .goShop(selectHeart) // still will be at 44
////            .exitShop()
////            .right.end
////        return builder.build()
////        return levelPlan(factory, 4)
//        //val level1 = levelPlanDebug(factory, 1)
////        return gatherHearts(factory)
//
////        return real(factory)
//    }
//
//    private fun levelTour(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("tour of levels")
//        return builder {
//            startAt(119)
//            seg("level1")
//            routeTo(InLocations.Overworld.level(1))
//            seg("level2")
//            routeTo(InLocations.Overworld.level(2))
//            seg("level3")
//            routeTo(InLocations.Overworld.level(3))
//            seg("level4")
//            routeTo(InLocations.Overworld.level(4))
//            seg("level5")
//            routeTo(InLocations.Overworld.level(5))
//            seg("level6")
//            routeTo(InLocations.Overworld.level(6))
//            seg("level7")
//            routeTo(InLocations.Overworld.level(7))
//            seg("level8")
//            routeTo(InLocations.Overworld.level(8))
//            seg("level9")
//            routeTo(InLocations.Overworld.level(9))
//        }.build()
//    }
//
//    private fun real3(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("begin!")
//        return builder {
//            startAt(InLocations.Overworld.start)
//            phase("Opening sequence")
//            // //ZeldaItem.WoodenSword)
////            obj(Dest.itemLookup[ZeldaItem.WoodenSword] ?: Dest.level(1))
//            obj(ZeldaItem.WoodenSword)
//            obj(Dest.level(2))
//            obj(Dest.Secrets.secretForest30NorthEast)
//            obj(Dest.Secrets.bombSecret30North)
//            obj(ZeldaItem.Letter)
//            obj(Dest.Secrets.walk100)
//            obj(Dest.Secrets.bombHeartNorth)
//            obj(ZeldaItem.WhiteSword)
//            obj(Dest.level(1))
//            obj(Dest.Shop.candleShopMid)
//            obj(Dest.level(3))
//            obj(Dest.Secrets.fire100SouthBrown)
//            obj(Dest.Shop.blueRing, position = true)
//            obj(Dest.level(4))
//
//            phase("grab hearts")
//            obj(Dest.Secrets.fire30GreenSouth)
//            obj(Dest.Heart.fireHeart)
//            obj(Dest.Secrets.bombHeartSouth)
//            obj(Dest.Secrets.forest100South)
//            obj(Dest.Shop.arrowShop)
//            obj(Dest.Heart.ladderHeart)
//            obj(Dest.Heart.raftHeart)
//
//            phase("level 5 and 6")
//            obj(Dest.level(5))
//
//            obj(ZeldaItem.PowerBracelet, itemLoc = Objective.ItemLoc.None)
//            // hard to get into position when its passable, maybe position it
//            obj(ZeldaItem.MagicSword)
//
//            obj(Dest.level(6))
//
//            phase("level 7")
//            obj(Dest.Shop.blueRing, itemLoc = Dest.Shop.ItemLocs.bait, position = true)
//            obj(Dest.level(7))
//            phase("level 8 and grab shield")
//            // TODO: also get a potion
//            obj(Dest.Secrets.level2secret10)
//            // bait
//            obj(Dest.Shop.eastTreeShop, itemLoc = Dest.Shop.ItemLocs.magicShield)
//            obj(Dest.level(8))
//            phase("level 9")
//            obj(Dest.level(9))
//            // junk
//            left
//            right
//            right
//            end
//        }.build()
//    }
//
//    fun levelPlanDebug(
//        factory: SequenceFactory, optimizer: AnalysisPlanBuilder.MasterPlanOptimizer, level:
//        Int
//    ): MasterPlan {
//        // shared prob
//        val downPoint = FramePoint(120, 90)
//        val pushPoint = FramePoint()
//        return if (level == 1) {
//            val builder = factory.make("Destroy level 1")
////            builder.inLevel.startAt(34)
////                .push(InLocations.diamondLeftBottomPush, InLocations.diamondLeftTopPush)
////                .startAt(127)
////                .go(InLocations.getItem)
////                .upTo(34) // eh
////                .end // more junk
////                .build()
//
//            builder.inLevel.startAt(127)
//                .go(InLocations.getItem)
//                .upTo(34) // eh
//                .end // more junk
//                .build()
//
//        } else {
//            MasterPlan(emptyList())
//        }
//    }
//
//    private fun levelPlan(factory: SequenceFactory, level: Int): MasterPlan {
//        return when (level) {
//            1 -> {
//                levelPlan1(factory)
//            }
//
//            2 -> {
//                levelPlan2(factory)
//            }
//
//            3 -> {
//                levelPlan3(factory)
//            }
//
//            4 -> {
//                levelPlan4(factory)
//            }
//
//            5 -> {
//                levelPlan5(factory)
//            }
//
//            6 -> {
//                levelPlan6(factory)
//            }
//
//            7 -> {
//                levelPlan7(factory)
//            }
//
//            8 -> {
//                levelPlan8(factory)
//            }
//
//            9 -> {
//                levelPlan9(factory)
//            }
//
//            else -> {
//                MasterPlan(emptyList())
//            }
//        }
//    }
//
//    private fun levelPlan1(factory: SequenceFactory): MasterPlan {
//        val downPoint = FramePoint(120, 90)
//        val builder = factory.make("Destroy level 1")
//        return builder.inLevel.startAt(115)
//            .seg("grab key")
//            .left
//            .goIn(GamePad.MoveLeft, 30)
//            .kill
//            .go(InLocations.Level1.key114)
//            .right
//            .seg("grab from skeleton")
//            .right
//            .goIn(GamePad.MoveRight, 20)
//            .pickupDeadItem
//            .seg("move to arrow")
//            .left // first rooms
//            .up //99
//            .up //83
//            .goIn(GamePad.MoveUp, 30)
//            .seg("get key from skeletons")
//            .kill // these skeletons provide a key
//            .goAbout(InLocations.Level1.key83, 4, 2, true)
//            .seg("Bomb and move")
//            .bomb(InLocations.topMiddleBombSpot)
//            .seg("go up")
//            .up // 67
//            .up // 51
//            .seg("grab key from zig")
//            .pickupDeadItem
//            .seg("get key from boomerang guys")
//            .up //35
//            .goIn(GamePad.MoveUp, 30)
//            .kill
//            .goAbout(InLocations.Level1.key83, 4, 2, true)
//            .seg("get arrow")
//            .left
//            .push(InLocations.diamondLeftBottomPush, InLocations.diamondLeftTopPush)
//            .startAt(127)
//            .go(InLocations.getItem)
//            .upTo(34) // eh
//            .seg("snag boomerang")
//            .rightm // don't attack
//            .down.down // at 67 now
//            .right // boomerang
//            .goIn(GamePad.MoveRight, 30)
//            .kill
//            .goAbout(InLocations.Level1.boomerang68, 4, 2, true)
//            .seg("destroy dragon")
//            .right //69 hand grabby
//            // should do but too risky for now
////                .go(InLocations.Level1.key114)
//            .up
//            .kill
//            .right // triforce
//            .goIn(GamePad.MoveRight, 20)
//            .seg("get the triforce")
//            .goAbout(downPoint, 4)
//            .left // junk
//            .end // more junk
//            .build()
//    }
//
//
//    private fun levelPlan2(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Destroy level 2")
//
//        if (false) {
//            return builder.lev(2).startAt(14)
//                .killR // need special strategy for thse guys
//                .wait
//                .goTo(InLocations.Level2.heartMid)
//                .seg("get the trigorce")
//                .left
//                .goIn(GamePad.MoveLeft, 30)
//                .goTo(InLocations.Level2.triforce)
//                .right // fake
//                .end
//                .build()
//        }
//
//        return builder.lev(2).startAt(InLocations.Level2.start)
//            .seg("gather 3 keys")
//            .right
//            .kill
//            .goTo(InLocations.Level2.keyMid)
//            .up // nothing here
//            .seg("gather key 2")
//            .left
//            .kill
//            .seg("gather key 3")
//            .left
//            // opportunity kill
//            .goTo(InLocations.Level2.keyMid)
//            .right
//            .right // grid room
//            .seg("sprint up from grid")
//            // right is nothing
//            .up // need to be able to kill
//            .seg("go get blue boomerang")
//            .upm // which upm
//            .right
//            .kill
//            .goTo(InLocations.Level2.keyMid)
//            .left
//            .seg("resume sprint")
//            .up // skip squishy snake
//            // skip getting key from squishy guy
////            .kill
////            .goTo(InLocations.Level2.keyMid)
//            .upm
//            .kill
//            .seg("bomb room")
//            .goTo(InLocations.Level2.keyMid)
//            .up
//            .kill // blocked before going // allow bombs
//            .goTo(InLocations.Level2.bombRight)
//            .up
//            .seg("kill boss")
//            .killR // need special strategy for thse guys
//            .wait
//            .goTo(InLocations.Level2.heartMid)
//            .seg("get the trigorce")
//            .left
//            .goIn(GamePad.MoveLeft, 30)
//            .goTo(InLocations.Level2.triforce)
//            .right // fake
//            .end
//            .build()
//    }
//
//    private fun levelPlan3(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Destroy level 3")
//        // open questions: fighting the trap guys
//        return builder.lev(3).startAt(105) // .startAt(124)
//            .seg("grab key")
//            .leftm
//            .goTo(InLocations.Level2.keyMid) //confirm
//            .seg("walk round corner")
//            .up // skip key
//            .up
//            .left
//            .seg("past the compasS")
//            .left
//            .goIn(GamePad.MoveLeft, 30)
//            .seg("fight swords")
//            .kill
//            .down
//            .seg("get raft")
//            .goTo(InLocations.rightStair)
//            .startAt(15)
//            .go(InLocations.getItem)
//            .upTo(105) // ??
//            .seg("get to boss")
//            .upm
//            .right
//            .rightm // option to get key up, but skip
//            .bomb(InLocations.BombDirection.right) // right bomb
//            .right
//            .up
//            .bomb(InLocations.BombDirection.right) // right bomb
//            .right
//            .seg("kill boss")
//            .kill // need special strategy for the 4monster
//            .up
//            .goTo(InLocations.Level2.triforce)
//            .go(InLocations.Level3.heartMid)
//            .down // junk
//            .end
//            .build()
//
//    }
//
//    private fun levelPlan4(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Destroy level 4")
//        if (false) {
//            return builder.lev(4).startAt(33)
//                .seg("go go go")
//                //key to left but dont bother
//                .kill2 // there will be 2 suns still running around
//                .seg("push")
//                .pushWait(InLocations.Level4.moveLeftOfTwo)
//                .goTo(InLocations.rightTop)
//                .startAt(96) //?
//                .go(InLocations.getItem)
//                .upTo(50) // ??
//                .left
//                .end
//                .build()
//        }
//
//        // problems:
//        // sometimes when move to push, zelda gets shifted to right
//        // getting stuck on the ladder
//        // get stuck in middle of maze
//
//        return builder.lev(4).startAt(113)
//            .seg("go go go")
//            //key to left but dont bother
//            .up
//            .up // no get it because it is in the middle of the room
//            .goTo(InLocations.Level4.batKey)
//            .leftm
//            .up
//            .goTo(InLocations.Level4.squishyKey)
//            .up // get key? kill squishies?
//            .seg("go get ladder")
//            .rightm
//            .kill
//            .right
//            .kill2 // there will be 2 suns still running around
//            .seg("push")
//            .pushWait(InLocations.Level4.moveLeftOfTwo)
//            .goTo(InLocations.rightTop)
//            .startAt(96)
//            .go(InLocations.getItem)
//            .upTo(50)
//            .leftm
//            .leftm
//            .seg("get past 4 monster")
//            .up
//            .up
//            .bomb(InLocations.BombDirection.right) // right bomb
//            .seg("get to the dragon")
//            .right
//            //skip key that is up
//            .bomb(InLocations.BombDirection.right) // right bomb
//            .right
//            .killb
//            .pushWait(InLocations.Level4.moveLeftOfTwo)
//            .right
//            .seg("fight dragon")
//            .kill // dragon
//            //get heart
//            .goTo(InLocations.Level4.triforceHeart)
//            .up
//            .goTo(InLocations.Level2.triforce)
//            .down
//            .end
//            .build()
//    }
//
//    private fun startGetSword(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//
//        return builder {
//            startAt(119)
//            seg("get brown sword")
////            navI
//            end
//        }.build()
//    }
//
//    private fun goToLevel1(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//
//        return builder {
//            seg("level1")
//            routeTo(InLocations.Overworld.level(1))
//            end
//        }.build()
//    }
//
//    private fun getWoodSword(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get wood sword")
//
//        return builder {
//            seg("get wood sword")
//            goInGetCenterItem(InLocations.Overworld.woodSwordEntry)
////            goTo(InLocations.Overworld.woodSwordEntry)
////            goIn(GamePad.MoveUp, 5)
////            goTo(InLocations.Overworld.centerItem)
////            exitShop()
////            goIn(GamePad.MoveDown, 5)
//        }.build()
//    }
//
//    private fun goToLevelFromStart(factory: SequenceFactory, level: Int): MasterPlan {
//        val builder = factory.make("Get to level $level")
//
//        return builder {
//            seg("level $level")
//            startAt(InLocations.Overworld.start)
//            routeTo(InLocations.Overworld.level(level), "level $level")
//        }.build()
//    }
//
//    private fun levelPlan5(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//        return builder.lev(5).startAt(119)
//            .seg("move to level 1")
//            .right.up.up.up.up.left
////            .goIn(level1Pt) // works
//            .end
//            .build()
//    }
//
//    private fun levelPlan6(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//        return builder.lev(6).startAt(119)
//            .seg("move to level 1")
//            .right.up.up.up.up.left
////            .goIn(level1Pt) // works
//            .end
//            .build()
//    }
//
//    private fun levelPlan7(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//        return builder.lev(7).startAt(119)
//            .seg("move to level 1")
//            .right.up.up.up.up.left
////            .goIn(level1Pt) // works
//            .end
//            .build()
//    }
//
//    private fun levelPlan8(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//        return builder.lev(8).startAt(119)
//            .seg("move to level 1")
//            .right.up.up.up.up.left
//            .end
//            .build()
//    }
//
//    private fun levelPlan9(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//        return builder.lev(9).startAt(119)
//            .seg("move to level 1")
//            .right.up.up.up.up.left
//            .end
//            .build()
//    }
//
//    private fun goToLevel1L(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//        return builder.startAt(119)
//            .seg("move to level 1")
//            .routeTo(55)
//            .end
//            .build()
//    }
//
//    private fun goToLevel2(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Get to level 1")
//        return builder.startAt(119)
//            .seg("move to level 1")
//            .right.up.up.up.up.left
////            .goIn(level1Pt) // works
//            .end
//            .build()
//    }
//
//    private fun gatherHearts2ToLevel6(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Gather hearts for level 5")
//        return builder {
//            startAt(InLocations.Overworld.level(4))
////            startAt(69)
//            inOverworld
//            seg("go get hearts, and capture level 2")
//            seg("bomb heart")
//            routeTo(InLocations.Overworld.Map.bombHeartSouth)
//            bomb(InLocations.Overworld.bombHeartSouth)
//            // go in and get the heart
//            seg("get quick 100 secret")
//            up
//            // burn
//            seg("get arrow")
//            routeTo(InLocations.Overworld.Map.arrowShop)
//            go(InLocations.Overworld.arrowShop)
//            // arrow shop
//            seg("ladder heard")
//            up
//            routeTo(InLocations.Overworld.Map.ladderHeartShore)
//            go(InLocations.Overworld.ladderHeartShore)
//            goIn(GamePad.MoveRight, 100)
//            goIn(GamePad.MoveLeft, 2000)
//            seg("raft heart")
//            routeTo(InLocations.Overworld.Map.raftHeartStart)
//            go(InLocations.Overworld.inDungeonRaftHeard)
//            goShop(InLocations.Overworld.shopHeartItem)
//            down // exit shop
//            seg("proceed to level 5")
//            include(goToLevelFromStart(factory, 5))
//            routeTo(InLocations.Overworld.Map.powerBracelet)
//            pushDownGetItem(InLocations.Overworld.powerBraceletEntrance)
//            routeTo(InLocations.Overworld.Map.magicSword)
//            pushDownGetItem(InLocations.Overworld.magicSwordPushDown)
//
//            include(goToLevelFromStart(factory, 6))
//        }.build()
//    }
//
//
//    private fun gatherHearts(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Gather hearts for level 5")
//        return builder.startAt(69)
//            .seg("go get hearts, and capture level 2")
//            .down //69
//            .goIn(GamePad.MoveDown, 100)
//            .right //85
//            .right // burn here?
//            .right // 87
//            .right
//            .down
//            .down
//            .right
//            .seg("cross bottom mountains")
//            .right.right.right
//            .seg("bomb heart")
//            .bomb(InLocations.Overworld.bombHeartSouth)
//            // go in and get the heart
//            .seg("get quick 100 secret")
//            .up
//            // burn
//            .seg("get arrow")
//            .right
//            .right
//            .right //126
//            .up
//            .go(InLocations.Overworld.arrowShop)
//            // arrow shop
//            .seg("ladder heard")
//            .up
//            .go(InLocations.Overworld.ladderHeartShore)
//            .goIn(GamePad.MoveRight, 100)
//            .goIn(GamePad.MoveLeft, 2000)
//            .seg("raft heart")
//            .up
//            .up
//            .up // across river
//            .go(InLocations.Overworld.inDungeonRaftHeard)
//            .goShop(InLocations.Overworld.shopHeartItem)
//            .down // exit shop
//            .seg("proceed to level 5")
//            .down
//            .left
//            .up
//            .left
//            .left
//            .up // go up from the bomb heart
//            .left
//            .seg("up repeat")
//            // todo: need special handling
//            .up
////            .up
////            .up
////            .up
//            .seg("enter level 5")
//            .go(InLocations.Overworld.inDungeonRaftHeard)
//            .end
//            .build()
//    }
//
//    private fun threeLevelsToLevel4(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("3 level attack")
//        return builder {
//            inOverworld
//            startAt(InLocations.Overworld.Map.whiteSword.loc)
//            routeToLevel(1)
//            seg("get candle")
//            routeTo(InLocations.Overworld.Map.candleShop)
//            goInGetCenterItem(InLocations.Overworld.candleShopEntrance, itemLoc = InLocations.Overworld.shopHeartItem)
//            routeToLevel(3)
//            seg("get 100 secret")
//            routeTo(InLocations.Overworld.Map.secret100Left)
//            burn100Left()
//            seg("get blue ring")
//            routeTo(InLocations.Overworld.Map.shopBlueRing)
//            pushDownGetItem(InLocations.Overworld.pushBlueRingShop)
//            routeToLevel(4)
//            // from here go get the hearts
//        }.build()
//    }
//
////    private fun fiveSix(factory: SequenceFactory): MasterPlan {
////        val builder = factory.make("Destroy level 4")
////        return builder {
////            inOverworld
////
////        }.build()
////    }
//
//    private fun sevenEightNineFromLevel6(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Destroy level 4")
//        return builder {
//            inOverworld
//            startAt(InLocations.Overworld.level(6))
//            // get a bait
//            seg("get bait")
//
//            routeTo(InLocations.Overworld.Map.shopBlueRing)
//            routeToLevel(7)
//            // get a potion
//            // get a magic shield
////            routeTo(InLocations.Overworld.Map.powerBracelet)
////            routeTo(InLocations.Overworld.Map.magicSword)
//            routeToLevel(8)
//            routeToLevel(9)
//            // go in!
//            end
//        }.build()
//    }
//
//    private fun goToLevel3(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Destroy level 4")
//        return builder.startAt(115)
//            .seg("go go go")
//            .end
//            .build()
//    }
//
//    private fun goToLevel4(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Destroy level 4")
//        return builder.startAt(115)
//            .seg("go go go")
//            .end
//            .build()
//    }
//
//    private fun blankSegment(factory: SequenceFactory): MasterPlan {
//        val builder = factory.make("Destroy level 4")
//        return builder.lev(4).startAt(115)
//            .seg("go go go")
//            .end
//            .build()
//    }
//}

//fun testBurn() {
//    val target = FramePoint(192 + 16, 32)
//    burnFromGo(to = target, from = Direction.Left)
//}
//
//fun testBurnH() {
//    // from 176, 80
//    // to 176, 96 + 16
//    val target = InLocations.Overworld.burnHeartEntrance
//    burnFromGo(to = target, from = Direction.Up, itemLoc = InLocations.Overworld.shopHeartItem)
//}
//
//fun testBurnS() {
//    // from 176, 80
//    // to 176, 96 + 16
//    //(160, 80) to (160, 112)
//    //(144, 80) to (144, 112)
//    val target = InLocations.Overworld.burnHeartShop
//    burnFromGo(to = target, from = Direction.Up, itemLoc = InLocations.Overworld.shopHeartItem)
//}
//
//fun testBurnS4() {
//    //(112, 160) to (144, 160)
//    val target = InLocations.Overworld.burn30NearLev4
//    burnFromGo(to = target, from = Direction.Left)
//}
//
//fun burn100Left() {
//    val target = InLocations.Overworld.burn100Left
//    burnFromGo(to = target, from = Direction.Right)
//}
//
//fun testBurn100Right() {
//    val target = InLocations.Overworld.burn100Right
//    burnFromGo(to = target, from = Direction.Right)
//}
//
//fun testburnShopNearLev2() {
//    val target = InLocations.Overworld.burnShopNearLev2
//    // get shield
//    burnFromGo(to = target, from = Direction.Left, itemLoc = InLocations.Overworld.shopLeftItem)
//}