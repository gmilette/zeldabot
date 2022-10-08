package bot.state

import bot.plan.*
import util.d
import util.i

class MasterPlan(val segments: List<PlanSegment>) {
    private val giant = segments.flatMap { seg -> seg.plan.map { PlanStep(seg, it) } }.toMutableList().also {
        d { " created plan with ${it.size} actions"}
    }

    fun log() {
        val first = giant.firstOrNull()

        first?.inSegment?.apply {
            d { "** ${phase}: ${name}: ${first.action.name}"}
        }
    }

    fun current(): Action =
        giant.first().action

    fun pop(): Action =
        giant.removeFirst().action.also {
            i { "--> switch to ${it.name}"}
        }
}

data class PlanStep(val inSegment: PlanSegment, val action: Action)

data class PlanSegment(val phase: String, val name: String, val plan:
    List<Action>) {
    override fun toString(): String {
        return plan.fold("") { R, t -> "$R $t " }
    }
}

// master plan
// - plan phase (main thing doing, get to lev 1, gather stuff for lev 3, just
// a name
//  - plan segment (sub route
//  -- plan objective (per screen)

object PlanBuilder {
    fun makeMasterPlan(mapData: MapCells): MasterPlan {
        val level1Pt = FramePoint(112, 64) // 74
        val bomb = FramePoint(147, 112)
        val bombEntrance = FramePoint(144, 104) // wrong
        val bombHeartEntrance = FramePoint(114, 94) // wrong
        val selectHeart = FramePoint(152, 100) // still at location 44
        val level1 = levelPlan(mapData, 1)
        val getStuffMid = FramePoint(120,96)
        val letterEntry = FramePoint(80,64) // right before 80 80
        val builder = LocationSequenceBuilder(mapData, "get to lev 1")
//        builder.startAt(44).seg("bomb heart")
//            .bomb(bomb)
//            .go(bombEntrance)
//            .goIn()
//            .goShop(selectHeart) // still will be at 44
//            .right.end

        builder.startAt(119)
            .phase("get to level 1")
            .seg("move to level 1")
            .right.up.up.up.up.left
//            .goIn(level1Pt) // works
//            .include(level1)
            .phase("gather stuff and blue ring")
//            .seg("get to bomb heart")
            .right.upm.rightm.rightm.rightm.rightm // BOMB!
            .seg("bomb heart")
            .bomb(bomb)
            .go(bombEntrance)
            .goIn()
            .goShop(selectHeart) // still will be at 44
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

        return builder.build()
    }

    fun levelPlan(mapData: MapCells, level: Int): MasterPlan {
        return if (level == 1) {
            val builder = LocationSequenceBuilder(mapData, "Destroy level 1")
            builder.startAt(115).left.right.right // first rooms
                .up.up.left.up //key
                .up // dont push rock
                .right.up.up.left.left //push to go down stairs
                .up // from bow
                .seg("snag boomerang")
                .right.down.down
                .right // boomerang
                .seg("destroy dragon")
                .right.up
                .right // dragon
                .build()
        } else {
            MasterPlan(emptyList())
        }
//        val builder = LocationSequenceBuilder(mapData, "get boomerang")
//        builder.startAt(115).left.right.right // first rooms
//            .up.up.left.up //key
//            .up // dont push rock
//            .right.up.up.left.left //push to go down stairs
//            .up // from bow
//            .right.down.down
//            .right // boomerang
//            .right.up
//            .right // dragon

//        lev.exits(u, d, l, r).inside()

        // 66 -> push rock
        // 33 -> push to go down
        // 127 -> get bow
        // 69, before dragon
        // 54 get triforce at 120,88

        // each square starts 2 in and around
        // bottom is 160

        //8x12 plus the exits

        // exits always at the same place
    }

    fun build(mapData: MapCells) {
//        return Plan(listOf(mapData(119),
//        mapData(120), // right
//        mapData(119),
//        mapData(119-16),
//        mapData(72),
//        mapData(56),
//        mapData(55)
//        ))
        // objective for where level 1 is
        val level1 = FramePoint(100, 100)
        val bomb = FramePoint(100, 100)
        val bombEntrance = FramePoint(100, 100)

        val builder = LocationSequenceBuilder(mapData, "get to lev 1")
        builder.startAt(119).right.up.up.up.up.left //.end
            .go(level1)
            .goIn()
            .right.up.rightm.rightm.rightm.right // BOMB!
            .bomb(bomb)
            .go(bombEntrance)
            .getSecret()
            .depart()
            .right // bomb heart
            .up
            .right
            .right
            .up // get secret (need special procedure for this)
            .down.left.up // get letter
            .down
            .left
            .down
//        plan.steps.forEachIndexed { index, mapCell ->
//            if (mapCell == MapCell.end) {
//                d { " end " }
//            } else {
//                val next = plan.steps[index + 1]
//                val d = NavUtil.directionToDir(
//                    mapCell.point.toFrame(), next
//                        .point.toFrame()
//                )
//                d {
//                    " from ${mapCell} to ${next} $d exits ${
//                        mapCell.mapData
//                            .exits
//                    } ${mapCell.exitNames}"
//                }
//            }
//        }

//        mapData[119].loc.right.up.up.up.up.left,
//        PlanStep(
//            "get to level 1",
//            mapData[119],
//            mapData[120],
//            mapData[104],
//            mapData[88],
//            mapData[72],
//            mapData[56],
//            mapData[55],
//        )
    }

    class LocationSequenceBuilder(private val mapData: MapCells,
    private var phase: String = "") {
        private var segment: String = ""
        private var plan = mutableListOf<Action>()
        private var lastMapLoc = 0
        private val builder = this
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
        val upm: LocationSequenceBuilder
            get() {
                // don't try to fight
                val nextLoc = lastMapLoc.up
                add(nextLoc, MoveTo(mapData.cell(nextLoc)))
                return this
            }
        val down: LocationSequenceBuilder
            get() {
                add(lastMapLoc.down)
                return this
            }
        val left: LocationSequenceBuilder
            get() {
                add(lastMapLoc.left)
                return this
            }
        val right: LocationSequenceBuilder
            get() {
                add(lastMapLoc.right)
                return this
            }
        val rightm: LocationSequenceBuilder
            get() {
                // don't try to fight
                val nextLoc = lastMapLoc.right
                add(nextLoc, MoveTo(mapData.cell(nextLoc)))
                return this
            }
        fun go(to: FramePoint): LocationSequenceBuilder {
            add(lastMapLoc, InsideNav(to))
            return this
        }
        fun goShop(to: FramePoint): LocationSequenceBuilder {
            add(lastMapLoc, InsideNavShop(to))
            return this
        }
        fun goIn(): LocationSequenceBuilder {
            add(lastMapLoc, GoIn())
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
            add(lastMapLoc, Bomb(target))
            return this
        }

        fun build(): MasterPlan {
            return MasterPlan(segments.toList())
        }

        private fun add(nextLoc: MapLoc) {
            add(nextLoc, opportunityKillOrMove(mapData.cell(nextLoc)))
        }

        private fun add(loc: MapLoc, action: Action): LocationSequenceBuilder {
            lastMapLoc = loc
            plan.add(action)
            return this
        }
    }
}