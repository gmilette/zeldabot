package bot.plan

import bot.GamePad
import bot.plan.action.*
import bot.plan.runner.MasterPlan
import bot.plan.runner.PlanSegment
import bot.state.*
import bot.state.map.Direction
import bot.state.map.MapCell
import bot.state.map.MapCells
import bot.state.map.level.LevelMapCellsLookup
import sequence.AnalysisPlanBuilder
import util.d

class LocationSequenceBuilder(
    private val mapData: MapCells,
    private val levelData: LevelMapCellsLookup,
    private val optimizer: AnalysisPlanBuilder.MasterPlanOptimizer,
    private var phase: String = ""
) {
    private var segment: String = ""
    private var plan = mutableListOf<Action>()
    private var lastMapLoc = 0
    private val builder = this
    private val OVERWORLD_LEVEL = 0
    private var level: Int = OVERWORLD_LEVEL
    private var segments = mutableListOf<PlanSegment>()

    operator fun invoke(block: LocationSequenceBuilder.() -> Unit): LocationSequenceBuilder {
        this.block()
        return this
    }

    private fun makeSegment() {
        if (plan.isNotEmpty()) {
            segments.add(PlanSegment(phase, segment, plan.toList()))
            plan = mutableListOf()
        }
    }

    fun routeTo(loc: MapLoc, name: String = ""): LocationSequenceBuilder {
        d { " path from $lastMapLoc to $loc"}
        val path = optimizer.findPath(lastMapLoc, loc) ?: return this
        for (mapCell in optimizer.correct(path.vertexList)) {
            if (mapCell.mapLoc != lastMapLoc) {  //  && mapCell.mapLoc != loc
                add(mapCell.mapLoc, MoveTo(mapCell))
                d { " routeTo $name ${mapCell.mapLoc} to ${mapCell.mapLoc}"}
//                    add(mapCell.mapLoc, opportunityKillOrMove(mapCell))
            }
        }
        return this
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