package bot.state

import bot.plan.*
import util.d

class MasterPlan(val segments: List<PlanSegment>) {
    private val giant = segments.flatMap { it.plan }.map { PlanStep(it, ActionPlan()) }.toMutableList()
    // what to do here
//    var actionPlan: ActionPlan = ActionPlan()

    // make a giant list?

    fun current(): Action =
        giant.first().actionPlan.current()

    fun pop(): ActionPlan =
        giant.removeFirst().actionPlan
}

data class PlanStep(val cell: MapCell, val actionPlan: ActionPlan)

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

        val level1 = levelPlan(mapData, 1)
        val builder = LocationSequenceBuilder(mapData, "get to lev 1")
        builder.startAt(119)
            .phase("get to level 1")
            .right.up.up.up.up.left
            .include(level1)
            .phase("gather stuff and blue ring")
            .seg("bomb heart")
            .right.up.right.right.right.right // BOMB!
            .seg("bomb secret")
            .right // bomb $
            .seg("get 30 secret")
            .seg("get 100 secret")
            .up
            .right
            .right
            .up // get secret (need special procedure for this)
            .seg("get letter")
            .down.left.up // get letter
            .down
            .left
            .down
            .seg("get candle")
            .end

        return builder.build()
//        val phaseName = "gather stuff and blue ring"
//        // envision parallel heads up display showing the plan sequence
//        val builder = PlanBuilder.LocationSequenceBuilder(mapData, phaseName)
//        builder.startAt(55).right.up.right.right.right
//            .right // bomb heart
//            .right // BOMB! (prob need to go right first, maybe bomb the things on the way back
//            .up
//            .up
//            .right
//            .right
//            .up // get secret (need special procedure for this)
//            .down.left.up // get letter
//            .down
//            .left
//            .down // back at 45


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
            .goIn(level1)
            .right.up.right.right.right.right // BOMB!
            .bomb(bomb)
            .goIn(bombEntrance)
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
            return add(loc)
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
                plan.add(MapCell.end)
                return this
            }
        val up: LocationSequenceBuilder
            get() {
                add(lastMapLoc.up)
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
                lastMapLoc.right.let {
                    add(it, opportunityKillOrMove(mapData.cell(it)))
                }
                return this
            }
        fun goIn(point: FramePoint): LocationSequenceBuilder {
            add(lastMapLoc, InsideNav(point))
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

        private fun add(loc: MapLoc, action: Action): LocationSequenceBuilder {
            lastMapLoc = loc
            plan.add(action)
            return this
        }
    }
}