package bot.plan

import bot.plan.levels.afterLevel2ItemsLetterEtcPhase
import bot.plan.levels.arrowAndHearts
import bot.plan.levels.gatherBombsFirstPhase
import bot.plan.levels.greenPotion
import bot.plan.levels.harvest
import bot.plan.levels.itemsNearLevel2CandleShieldPhase
import bot.plan.levels.level1
import bot.plan.levels.level2
import bot.plan.levels.level5sequence
import bot.plan.levels.ringLevels
import bot.plan.levels.whiteSword
import bot.plan.levels.woodenSwordPhase
import bot.plan.runner.MasterPlan
import bot.state.map.Hyrule
import bot.state.map.MapCells
import bot.state.map.destination.Dest
import bot.state.map.level.LevelMapCellsLookup

object ZeldaPlan {
    fun makeMasterPlan(hyrule: Hyrule, mapData: MapCells, levelData: LevelMapCellsLookup): MasterPlan {
        val router = OverworldRouter(hyrule)
        val factory = PlanInputs(mapData, levelData, router)
        return safety(factory)
    }

    private fun safety(factory: PlanInputs): MasterPlan {
        val builder = factory.make("begin!")

        return builder {
            woodenSwordPhase()

            "gather bombs".seg()
            gatherBombsFirstPhase()

            "gather heart".seg()
            obj(Dest.Heart.bombHeartNorth)
//            2 using level2
            // need the cash to get the candle
//            afterLevel2ItemsLetterEtcPhase(false)

            // should hae enough for candle and shield
            whiteSword()
            seg("get fairy before level 1")
            routeTo(39)
            obj(Dest.Fairy.greenForest) // 205
            1 using level1
            2 using level2
            // need the cash to get the candle
            afterLevel2ItemsLetterEtcPhase(false)
            itemsNearLevel2CandleShieldPhase()


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
            arrowAndHearts()

            // doesn't seem necessary
            phase("harvest after arrow")
            harvest()
            // has 223 coin now
            // should have enough coin for rest of game after
            // 10 harvests

            // then potion?
            phase("level 5 sequence")
            level5sequence()
        }
    }
}