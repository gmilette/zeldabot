package bot.state

import bot.GamePad
import bot.plan.*
import bot.state.map.Direction
import bot.state.map.MapCell
import bot.state.map.MapCells
import bot.state.map.level.LevelMapCellsLookup


// master plan
// - plan phase (main thing doing, get to lev 1, gather stuff for lev 3, just
// a name
//  - plan segment (sub route
//  -- plan objective (per screen)

object InLocations {
    val topMiddleBombSpot = FramePoint(120, 32)
    val diamondLeftTopPush = FramePoint(96, 64)
    val diamondLeftBottomPush = FramePoint(96, 96)
    val middleStair = FramePoint(127, 80)
    val getItem = FramePoint(135, 80) // right side
    val rightStair = FramePoint(209, 81)
    val rightTop = FramePoint(208, 32) //todo

    object Overworld {
        val bombHeartSouth = FramePoint(136, 24)
        val arrowShop = FramePoint(48, 16)
        val shopRightItem = FramePoint(152, 97)

        val ladderHeartShore = FramePoint(127, 80)
        val ladderHeart = FramePoint(191, 80)
        val inDungeonRaftHeard = FramePoint(96, 65)
        val shopHeartItem = FramePoint(152, 96)
    }

    object Level1 {
        val key114 = FramePoint(160, 128)
        val key83 = FramePoint(128, 50)
        // hands level1, prob same as key114
        val key69 = FramePoint(164, 128)
        val boomerang68 = FramePoint(128, 56)
    }
    object Level2 {
        val heartMid = FramePoint(128, 88) //boss heart
        val keyMid = FramePoint(128, 88)
        val keyMidDown = FramePoint(128, 81)
        val bombRight = FramePoint(208, 43)
        val triforce = FramePoint(120, 88) // get the middle of the triangle at the top
    }

    object Level3 {
        val heartMid = FramePoint(128, 88)
    }

    object Level4 {
        val batKey = FramePoint(144, 88)
        val squishyKey = FramePoint(135, 64)
        val moveLeftOfTwo = FramePoint(96, 64)
        val triforceHeart = FramePoint(208, 123)
    }

    object BombDirection {
        val right = FramePoint(200, 92) //?
     }

}

object PlanBuilder {
    val level1Pt = FramePoint(112, 64) // 74
    val bomb = FramePoint(147, 112)
    val bombEntrance = FramePoint(144, 104) // wrong
    val bombHeartEntrance = FramePoint(114, 94) // wrong
    val selectHeart = FramePoint(152, 90) // still at location 44
    val getStuffMid = FramePoint(120, 96)
    val letterEntry = FramePoint(80, 64) // right before 80 80

    fun makeMasterPlan(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {

        val builder = LocationSequenceBuilder(mapData, levelData, "get to lev 1")
        // save game 6
//        builder.startAt(44).seg("bomb heart")
//            .bomb(bomb)
//            .go(bombEntrance)
////            .goIn()
//            .goShop(selectHeart) // still will be at 44
//            .exitShop()
//            .right.end
//        return builder.build()
//        return levelPlan(mapData, levelData, 4)
        //val level1 = levelPlanDebug(mapData, levelData, 1)
        return gatherHearts(mapData, levelData)

//        return real(mapData, levelData)
    }

    private fun real(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val level1 = levelPlan(mapData, levelData, 1)
        val builder = LocationSequenceBuilder(mapData, levelData, "get to lev 1")
        return builder.startAt(119)
            .phase("get to level 1")
            .include(goToLevel1(mapData, levelData))
            .include(level1)
            .include(gatherBeginningItems(mapData, levelData))
            .include(goToLevel3(mapData, levelData))
            .include(levelPlan(mapData, levelData, 3))
            .include(goToLevel4(mapData, levelData))
            .include(levelPlan(mapData, levelData, 4))
            .include(gatherHearts(mapData, levelData))
            .include(goToLevel2(mapData, levelData))
            .include(levelPlan(mapData, levelData, 2))
            .end.build()

    }

    fun levelPlanDebug(mapData: MapCells, levelData: LevelMapCellsLookup, level: Int): MasterPlan {
        // shared prob
        val downPoint = FramePoint(120, 90)
        val pushPoint = FramePoint()
        return if (level == 1) {
            val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 1")
//            builder.inLevel.startAt(34)
//                .push(InLocations.diamondLeftBottomPush, InLocations.diamondLeftTopPush)
//                .startAt(127)
//                .go(InLocations.getItem)
//                .upTo(34) // eh
//                .end // more junk
//                .build()

            builder.inLevel.startAt(127)
                .go(InLocations.getItem)
                .upTo(34) // eh
                .end // more junk
                .build()

        } else {
            MasterPlan(emptyList())
        }
    }

    private fun levelPlan(mapData: MapCells, levelData: LevelMapCellsLookup, level: Int): MasterPlan {
        return when (level) {
            1 -> {
                levelPlan1(mapData, levelData)
            }
            2 -> {
                levelPlan2(mapData, levelData)
            }
            3 -> {
                levelPlan3(mapData, levelData)
            }
            4 -> {
                levelPlan4(mapData, levelData)
            }
            else -> {
                MasterPlan(emptyList())
            }
        }
    }

    private fun levelPlan1(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val downPoint = FramePoint(120, 90)
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 1")
        return builder.inLevel.startAt(115)
            .seg("grab key")
            .left
            .goIn(GamePad.MoveLeft, 30)
            .kill
            .go(InLocations.Level1.key114)
            .right
            .seg("grab from skeleton")
            .right
            .goIn(GamePad.MoveRight, 20)
            .pickupDeadItem
            .seg("move to arrow")
            .left // first rooms
            .up //99
            .up //83
            .goIn(GamePad.MoveUp, 30)
            .seg("get key from skeletons")
            .kill // these skeletons provide a key
            .goAbout(InLocations.Level1.key83, 4, 2, true)
            .seg("Bomb and move")
            .bomb(InLocations.topMiddleBombSpot)
            .seg("go up")
            .up // 67
            .up // 51
            .seg("grab key from zig")
            .pickupDeadItem
            .seg("get key from boomerang guys")
            .up //35
            .goIn(GamePad.MoveUp, 30)
            .kill
            .goAbout(InLocations.Level1.key83, 4, 2, true)
            .seg("get arrow")
            .left
            .push(InLocations.diamondLeftBottomPush, InLocations.diamondLeftTopPush)
            .startAt(127)
            .go(InLocations.getItem)
            .upTo(34) // eh
            .seg("snag boomerang")
            .rightm // don't attack
            .down.down // at 67 now
            .right // boomerang
            .goIn(GamePad.MoveRight, 30)
            .kill
            .goAbout(InLocations.Level1.boomerang68, 4, 2, true)
            .seg("destroy dragon")
            .right //69 hand grabby
            // should do but too risky for now
//                .go(InLocations.Level1.key114)
            .up
            .kill
            .right // triforce
            .goIn(GamePad.MoveRight, 20)
            .seg("get the triforce")
            .goAbout(downPoint, 4)
            .left // junk
            .end // more junk
            .build()
    }


    private fun levelPlan2(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 2")

        if (false) {
            return builder.lev(2).startAt(14)
                .killR // need special strategy for thse guys
                .wait
                .goTo(InLocations.Level2.heartMid)
                .seg("get the trigorce")
                .left
                .goIn(GamePad.MoveLeft, 30)
                .goTo(InLocations.Level2.triforce)
                .right // fake
                .end
                .build()
        }

        return builder.lev(2).startAt(125)
            .seg("gather 3 keys")
            .right
            .kill
            .goTo(InLocations.Level2.keyMid)
            .up // nothing here
            .seg("gather key 2")
            .left
            .kill
            .seg("gather key 3")
            .left
            // opportunity kill
            .goTo(InLocations.Level2.keyMid)
            .right
            .right // grid room
            .seg("sprint up from grid")
            // right is nothing
            .up // need to be able to kill
            .seg("go get blue boomerang")
            .upm // which upm
            .right
            .kill
            .goTo(InLocations.Level2.keyMid)
            .left
            .seg("resume sprint")
            .up // skip squishy snake
                // skip getting key from squishy guy
//            .kill
//            .goTo(InLocations.Level2.keyMid)
            .upm
            .kill
            .seg("bomb room")
            .goTo(InLocations.Level2.keyMid)
            .up
            .kill // blocked before going // allow bombs
            .goTo(InLocations.Level2.bombRight)
            .up
            .seg("kill boss")
            .killR // need special strategy for thse guys
            .wait
            .goTo(InLocations.Level2.heartMid)
            .seg("get the trigorce")
            .left
            .goIn(GamePad.MoveLeft, 30)
            .goTo(InLocations.Level2.triforce)
            .right // fake
            .end
            .build()
    }

    private fun levelPlan3(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 3")
        // open questions: fighting the trap guys
        return builder.lev(3).startAt(105) // .startAt(124)
            .seg("grab key")
            .leftm
            .goTo(InLocations.Level2.keyMid) //confirm
            .seg("walk round corner")
            .up // skip key
            .up
            .left
            .seg("past the compasS")
            .left
            .goIn(GamePad.MoveLeft, 30)
            .seg("fight swords")
            .kill
            .down
            .seg("get raft")
            .goTo(InLocations.rightStair)
            .startAt(15)
            .go(InLocations.getItem)
            .upTo(105) // ??
            .seg("get to boss")
            .upm
            .right
            .rightm // option to get key up, but skip
            .bomb(InLocations.BombDirection.right) // right bomb
            .right
            .up
            .bomb(InLocations.BombDirection.right) // right bomb
            .right
            .seg("kill boss")
            .kill // need special strategy for the 4monster
            .up
            .goTo(InLocations.Level2.triforce)
            .go(InLocations.Level3.heartMid)
            .down // junk
            .end
            .build()

    }

    private fun levelPlan4(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 4")
        if (false) {
            return builder.lev(4).startAt(33)
                .seg("go go go")
                //key to left but dont bother
                .kill2 // there will be 2 suns still running around
                .seg("push")
                .pushWait(InLocations.Level4.moveLeftOfTwo)
                .goTo(InLocations.rightTop)
                .startAt(96) //?
                .go(InLocations.getItem)
                .upTo(50) // ??
                .left
                .end
                .build()
        }

        // problems:
        // sometimes when move to push, zelda gets shifted to right
        // getting stuck on the ladder
        // get stuck in middle of maze

        return builder.lev(4).startAt(113)
            .seg("go go go")
                //key to left but dont bother
            .up
            .up // no get it because it is in the middle of the room
            .goTo(InLocations.Level4.batKey)
            .leftm
            .up
            .goTo(InLocations.Level4.squishyKey)
            .up // get key? kill squishies?
            .seg("go get ladder")
            .rightm
            .kill
            .right
            .kill2 // there will be 2 suns still running around
            .seg("push")
            .pushWait(InLocations.Level4.moveLeftOfTwo)
            .goTo(InLocations.rightTop)
            .startAt(96)
            .go(InLocations.getItem)
            .upTo(50)
            .leftm
            .leftm
            .seg("get past 4 monster")
            .up
            .up
            .bomb(InLocations.BombDirection.right) // right bomb
            .seg("get to the dragon")
            .right
            //skip key that is up
            .bomb(InLocations.BombDirection.right) // right bomb
            .right
            .killb
            .pushWait(InLocations.Level4.moveLeftOfTwo)
            .right
            .seg("fight dragon")
            .kill // dragon
            //get heart
            .goTo(InLocations.Level4.triforceHeart)
            .up
            .goTo(InLocations.Level2.triforce)
            .down
            .end
            .build()
    }

    private fun goToLevel1(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Get to level 1")
        return builder.startAt(119)
            .seg("move to level 1")
            .right.up.up.up.up.left
//            .goIn(level1Pt) // works
            .end
            .build()
    }

    private fun goToLevel2(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Get to level 1")
        return builder.startAt(119)
            .seg("move to level 1")
            .right.up.up.up.up.left
//            .goIn(level1Pt) // works
            .end
            .build()
    }

    private fun gatherHearts(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Gather hearts for level 5")
        return builder.startAt(69)
            .seg("go get hearts, and capture level 2")
            .down //69
            .goIn(GamePad.MoveDown, 100)
            .right //85
            .right // burn here?
            .right // 87
            .right
            .down
            .down
            .right
            .seg("cross bottom mountains")
            .right.right.right
            .seg("bomb heart")
            .bomb(InLocations.Overworld.bombHeartSouth)
                // go in and get the heart
            .seg("get quick 100 secret")
            .up
                // burn
            .seg("get arrow")
            .right
            .right
            .right //126
            .up
            .go(InLocations.Overworld.arrowShop)
                // arrow shop
            .seg("ladder heard")
            .up
            .go(InLocations.Overworld.ladderHeartShore)
            .goIn(GamePad.MoveRight, 100)
            .goIn(GamePad.MoveLeft, 2000)
            .seg("raft heart")
            .up
            .up
            .up // across river
            .go(InLocations.Overworld.inDungeonRaftHeard)
            .goShop(InLocations.Overworld.shopHeartItem)
            .down // exit shop
            .seg("proceed to level 5")
            .down
            .left
            .up
            .left
            .left
            .up // go up from the bomb heart
            .left
            .seg("up repeat")
                // todo: need special handling
            .up
//            .up
//            .up
//            .up
            .seg("enter level 5")
            .go(InLocations.Overworld.inDungeonRaftHeard)
            .end
            .build()
    }

    private fun gatherBeginningItems(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 4")
        return builder.startAt(115)
            .seg("go go go")
            .phase("gather stuff and blue ring")
//            .seg("get to bomb heart")
            .right.upm.rightm.rightm.rightm.rightm // BOMB!
            .seg("bomb heart")
            .bomb(bomb)
            .go(bombEntrance)
//            .goIn() // bad idea
            .goShop(selectHeart) // still will be at 44
            .exitShop()
            .right
            .phase("end")
//            .rightm // "now at the bomb
//            .seg("bomb heart")
//            .bomb(bomb)
//            .goIn(bombEntrance)
////            .getSecret()
////            .depart()
//            .seg("bomb secret")
//            .right // bomb $
//            .seg("gather more bombs")
//            .right // bomb $
//            .seg("get 30 secret")
//            .left
//            .down
//            .seg("go to 100 secret")
//            .up
//            .up
//            .right
//            .right
//            .seg("get 100 secret")
//            .up // get secret (need special procedure for this)
//            .seg("get letter")
//            .down.left.up // get letter
//            .down
//            .left
//            .down
//            .seg("get candle")
            .end
            .build()
    }

    private fun goToLevel3(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 4")
        return builder.startAt(115)
            .seg("go go go")
            .end
            .build()
    }

    private fun goToLevel4(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 4")
        return builder.startAt(115)
            .seg("go go go")
            .end
            .build()
    }

    private fun blankSegment(mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val builder = LocationSequenceBuilder(mapData, levelData, "Destroy level 4")
        return builder.lev(4).startAt(115)
            .seg("go go go")
            .end
            .build()
    }


    class LocationSequenceBuilder(
        private val mapData: MapCells,
        private val levelData: LevelMapCellsLookup,
        private var phase: String = ""
    ) {
        private var segment: String = ""
        private var plan = mutableListOf<Action>()
        private var lastMapLoc = 0
        private val builder = this
        private val OVERWORLD_LEVEL = 0
        private var level: Int = OVERWORLD_LEVEL
        private var segments = mutableListOf<PlanSegment>()

        private fun makeSegment() {
            if (plan.isNotEmpty()) {
                segments.add(PlanSegment(phase, segment, plan.toList()))
                plan = mutableListOf()
            }
        }

        fun startAt(loc: MapLoc): LocationSequenceBuilder {
            lastMapLoc = loc
            return this
        }

        fun include(other: MasterPlan): LocationSequenceBuilder {
            makeSegment()
            segments.addAll(other.segments)
            return this
        }

        fun phase(name: String): LocationSequenceBuilder {
            makeSegment()
            phase = name
            return this
        }

        fun seg(name: String): LocationSequenceBuilder {
            makeSegment()
            segment = name
            return this
        }

        val end: LocationSequenceBuilder
            get() {
                makeSegment()
                plan.add(EndAction())
                return this
            }
        val up: LocationSequenceBuilder
            get() {
                add(lastMapLoc.up)
                return this
            }
        fun upTo(loc: MapLoc): LocationSequenceBuilder {
            val nextLoc = loc
            add(nextLoc, Unstick(MoveTo(mapCell(nextLoc), Direction.Up)))
            return this
        }
        val loot: LocationSequenceBuilder
            get() {
                add(lastMapLoc, GetLoot())
                return this
            }
        val pickupDeadItem: LocationSequenceBuilder
            get() {
                add(lastMapLoc, PickupDroppedDungeonItemAndKill())
                return this
            }
        val wait: LocationSequenceBuilder
            get() {
                add(lastMapLoc, Wait(2500))
                return this
            }
        val killb: LocationSequenceBuilder
            get() {
                add(lastMapLoc, KillAll(useBombs = true, waitAfterAttack = false))
                return this
            }
        val killR: LocationSequenceBuilder
            get() {
                add(lastMapLoc, KillAll(useBombs = true, waitAfterAttack = true))
                return this
            }
        val kill: LocationSequenceBuilder
            get() {
                add(lastMapLoc, KillAll())
                return this
            }
        val kill2: LocationSequenceBuilder
            get() {
                add(lastMapLoc, KillAll(numberLeftToBeDead = 2))
                return this
            }
        val upLootThenMove: LocationSequenceBuilder
            get() {
                val nextLoc = lastMapLoc.up
                add(nextLoc, lootThenMove(mapCell(nextLoc)))
                return this
            }
        val upk: LocationSequenceBuilder
            get() {
                addKill(lastMapLoc.up)
                return this
            }
        val upm: LocationSequenceBuilder
            get() {
                // don't try to fight
                val nextLoc = lastMapLoc.up
                add(nextLoc, Unstick(MoveTo(mapCell(nextLoc))))
                return this
            }
        val down: LocationSequenceBuilder
            get() {
                add(lastMapLoc.down)
                return this
            }
        val inLevel: LocationSequenceBuilder
            get() {
                level = 1
                return this
            }
        fun lev(levelIn: Int): LocationSequenceBuilder {
            level = levelIn
            return this
        }
        val inOverworld: LocationSequenceBuilder
            get() {
                level = OVERWORLD_LEVEL
                return this
            }
        val left: LocationSequenceBuilder
            get() {
                add(lastMapLoc.left)
                return this
            }
        val leftLoot: LocationSequenceBuilder
            get() {
                val nextLoc = lastMapLoc.left
                add(nextLoc, GetLoot())
                return this
            }
        val leftm: LocationSequenceBuilder
            get() {
                val nextLoc = lastMapLoc.left
                add(nextLoc, Unstick(MoveTo(mapCell(nextLoc))))
                return this
            }
        val rightKillForKey: LocationSequenceBuilder
            get() {
                val nextLoc = lastMapLoc.right
                // todo: can stop after get a key
                add(nextLoc, killThenLootThenMove(mapCell(nextLoc)))
                return this
            }
        val right: LocationSequenceBuilder
            get() {
                add(lastMapLoc.right)
                return this
            }
        val rightk: LocationSequenceBuilder
            get() {
                lootKill()
                add(lastMapLoc.right)
                return this
            }
        val rightm: LocationSequenceBuilder
            get() {
                // don't try to fight
                val nextLoc = lastMapLoc.right
                add(nextLoc, Unstick(MoveTo(mapCell(nextLoc))))
                return this
            }

        fun push(toB: FramePoint, toT: FramePoint): LocationSequenceBuilder {
            add(lastMapLoc, InsideNavAbout(toB, 4))
            add(lastMapLoc, GoDirection(GamePad.MoveUp, 100))
            add(lastMapLoc, GoDirection(GamePad.MoveRight, 100))
            add(lastMapLoc, InsideNav(InLocations.middleStair))
            return this
        }
        fun pushWait(toB: FramePoint): LocationSequenceBuilder {
            // if it fails need to retry
            add(lastMapLoc, InsideNavAbout(toB, 4))
            add(lastMapLoc, GoDirection(GamePad.MoveDown, 100))
            add(lastMapLoc, Wait(300))
            return this
        }
        fun go(to: FramePoint): LocationSequenceBuilder {
            add(lastMapLoc, InsideNav(to))
            return this
        }

        fun goTo(to: FramePoint): LocationSequenceBuilder {
            goAbout(to, 4, 2, true)
            return this
        }

        fun goAbout(to: FramePoint, horizontal: Int, vertical: Int = 1, negVertical: Boolean = false): LocationSequenceBuilder {
            add(lastMapLoc, InsideNavAbout(to, horizontal, vertical, negVertical))
            return this
        }

        fun goShop(to: FramePoint): LocationSequenceBuilder {
            add(lastMapLoc, InsideNavShop(to))
            return this
        }

        fun exitShop(): LocationSequenceBuilder {
            add(lastMapLoc, ExitShop())
            return this
        }

        fun lootKill(): LocationSequenceBuilder {
            add(lastMapLoc, lootAndKill)
            return this
        }

        fun goIn(dir: GamePad = GamePad.MoveUp, num: Int): LocationSequenceBuilder {
            add(lastMapLoc, GoIn(moves = num, dir = dir))
            return this
        }

        fun getSecret(): LocationSequenceBuilder {
            val secretMoneyLocation: FramePoint = FramePoint(100, 100)
            add(lastMapLoc, InsideNav(secretMoneyLocation))
            return this
        }

        fun depart(): LocationSequenceBuilder {
            val departSecretLocation: FramePoint = FramePoint(100, 100)
            add(lastMapLoc, InsideNav(departSecretLocation)) // something different
            return this
        }

        fun bomb(target: FramePoint): LocationSequenceBuilder {
            add(lastMapLoc, Unstick(Bomb(target)))
            return this
        }

        fun build(): MasterPlan {
            return MasterPlan(segments.toList())
        }

        private fun mapCell(nextLoc: MapLoc): MapCell =
            if (level == OVERWORLD_LEVEL) {
                mapData.cell(nextLoc)
            } else {
                levelData.cell(level, nextLoc)
            }

        private fun add(nextLoc: MapLoc) {
            add(nextLoc, opportunityKillOrMove(mapCell(nextLoc)))
        }

        private fun addKill(nextLoc: MapLoc) {
            add(nextLoc, killThenLootThenMove(mapCell(nextLoc)))
        }

        private fun add(loc: MapLoc, action: Action): LocationSequenceBuilder {
            lastMapLoc = loc
            plan.add(action)
            return this
        }
    }
}