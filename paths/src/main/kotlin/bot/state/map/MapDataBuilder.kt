package bot.state.map

import bot.state.FramePoint
import bot.state.MapLoc
import bot.state.map.destination.*

val Int.grid
    get() = this * MapConstants.oneGrid

val Int.andAHalf
    get() = this + MapConstants.halfGrid

/**
 * constructs the overworld mapCells
 */
class MapBuilder {
    val u = Direction.Up
    val r = Direction.Right
    val d = Direction.Down
    val l = Direction.Left
    val upRight = e(u, r)
    val upDown = e(u, d)
    val eall = e(u, d, l, r)

    fun e(vararg dir: Direction): ExitSet {
        return ExitSet(*dir)
    }

    private object InDest {
        val centerLevel = FramePoint(112, 64)
        val centerLevel3 = FramePoint(128, 64)
        val centerLevel6 = FramePoint(120, 64)
        val centerRaftHeart = FramePoint(6.grid, 4.grid)
    }

    fun build(): Map<MapLoc, MapCellData> {
        val objectives = mutableMapOf<MapLoc, MapCellData>()
        addRow7(objectives)
        addRow6(objectives)
        addRow5(objectives)
        addRow4(objectives)
        addRow3(objectives)
        addRow2(objectives)
        addRow1(objectives)
        addRow0(objectives)

        // should flatmap it
        val objectiveToMapCell = objectives.mapValues {
            it.value.objective to it.value
        }

        return objectives
    }

    private fun addRow0(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[0] = MapCellData(
            "topleftrock"
        )
        objectives[1] = MapCellData(
            "topleftrock",
            Objective(
                FramePoint(100, 100), DestType.SecretToEverybody
                    (20, EntryType.Walk())
            )
        )
        objectives[3] = MapCellData(
            "topleftrock",
            Objective(
                FramePoint(100, 100), DestType.SecretToEverybody
                    (20, EntryType.Bomb)
            )
        )
        objectives[4] = CellBuilder().invoke {
            aka("level9 potion")
            this has Dest.Shop.potionShopLevel9 at 12.grid a 1.grid
        }
        objectives[5] = MapCellData(
            "lev9",
            Objective(FramePoint(5.grid, 6.grid), Dest.level(9))
        )
        objectives[6] = MapCellData(
            "lev9"
        )
        objectives[7] = MapCellData(
            "levright9",
            Objective(
                100,
                100,
                DestType.SecretToEverybody(20, EntryType.Bomb)
            )
        )
        objectives[8] = MapCellData(
            "lev9rightright"
        )
        objectives[9] = MapCellData(
            "lev9arrow"
        )
        objectives[10] = MapCellData(
            "lev9arrow",
            Objective(
                2.grid,
                1.grid,
                DestType.Item(ZeldaItem.WhiteSword, EntryType.Walk())
            )
        )
        objectives[11] = MapCellData(
            "lev5",
            Objective(7.grid, 4.grid, Dest.level(5))
        )
        objectives[12] = MapCellData(
            "nowhereshow",
            Objective(
                100, 100, DestType.Shop()
            )
        )
        objectives[13] = CellBuilder().invoke {
            aka("nowherepotion")
            this has Dest.Shop.potionShopTop at 9.grid a 1.grid
        }
        objectives[14] = MapCellData(
            "letter",
            Objective(
                5.grid, 4.grid, DestType.Item(ZeldaItem.Letter)
            )
        )
        objectives[15] = MapCellData(
            "moneythroughwall",
            //FramePoint(128, 64)
            Objective(128, 64, Dest.Secrets.walk100)
        )
    }

    private fun addRow1(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[16] = MapCellData(
            "leftshowTop",
            Objective(
                100, 100, DestType.Shop()
            )
        )
        objectives[17] = MapCellData(
            "uselessnearshop",
            Objective(
                100, 100, DestType.Shop()
            )
        )
        objectives[18] = MapCellData(
            "placeshop",
            Objective(
                100, 100, DestType.Shop()
            )
        )
        objectives[19] = MapCellData(
            "placeshopsecret",
            Objective(
                100, 100, DestType.SecretToEverybody(30, EntryType.Bomb)
            )
        )
        objectives[20] = MapCellData(
            "mttopsecret",
            Objective(
                100, 100, DestType.SecretToEverybody(20, EntryType.Bomb)
            )
        )
        objectives[21] = MapCellData(
            "mountainempty"
        )
        objectives[21] = MapCellData(
            "mountainshop",
            Objective(
                100, 100, DestType.Shop()
            )
        )
        objectives[23] = MapCellData(
            "mountainstairriver",
            Objective(
                100, 100, DestType.Shop()
            )
        )
        objectives[24] = MapCellData(
            "mountainboulder"
        )
        objectives[25] = MapCellData(
            "mountainboulder2"
        )
        objectives[26] = MapCellData(
            "mountainboulderupforwhitesword"
        )
        objectives[27] = MapCellData(
            "lostmountain"
        )
        objectives[28] = MapCellData(
            "lostmountainright"
        )
        objectives[29] = MapCellData(
            "lostmountainrightright",
            Objective(100, 100, DestType.Travel)
        )
        objectives[30] = MapCellData(
            "mountaingoupforletter"
        )
        objectives[31] = MapCellData(
            "mountainnear100secret",
            Objective(100, 100, DestType.MoneyGame)
        )
    }

    private fun addRow2(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[32] = MapCellData(
            "graveyardTopLeft"
        )
        objectives[33] = MapCellData(
            "graveyardMagicSword",
            Objective(9.grid, 5.grid, DestType.Item(ZeldaItem.MagicSword, entry = EntryType.Push())),
        )
        objectives[34] = MapCellData(
            "lev6",
            Objective(InDest.centerLevel6, Dest.level(6))
        )
        objectives[35] = MapCellData(
            "lev6travel",
            Objective(FramePoint(100, 100), DestType.Travel)
        )
        objectives[36] = MapCellData(
            "power bracelet",
            Objective(14.grid, 4.grid, DestType.Item(ZeldaItem.PowerBracelet, entry = EntryType.Statue)),
        )
        objectives[37] = CellBuilder().invoke {
            aka("powerbraceletRight")
            e(l)
            this has DestType.Shop(ShopType.B) at 100 a 100
        }
        objectives[38] = CellBuilder().invoke {
            aka("mountainWalk")
            e(r, d)
            this has DestType.Shop(ShopType.C) at 100 a 100
        }
        objectives[39] = CellBuilder().invoke {
            aka("downfromboulder")
            e(u, r)
            this has Dest.Shop.potionShopCornerNear1 at 14.grid a 1.grid
        }
        objectives[40] = CellBuilder().invoke {
            aka("forestneardesert")
            e(l, r, d)
            this has Dest.Secrets.forest30NearDesertForest at 13.grid a 6.grid
        }
        objectives[41] = CellBuilder().invoke {
            aka("grounddesert")
            e(l, r)
        }
        objectives[42] = CellBuilder().invoke {
            aka("grounddesert2")
            e(l, r, d)
        }
        objectives[43] = CellBuilder().invoke {
            aka("grounddesert3")
            e(l, r, d)
        }
        objectives[44] = MapCellData(
            "Bomb heart north",
            Objective(FramePoint(144, 96), Dest.Heart.bombHeartNorth, itemLoc = Objective.ItemLoc.Right)
        )
        //            ,
        objectives[45] = CellBuilder().invoke {
            aka("forestsecret")
            this has Dest.Secrets.bombSecret30North at 80 a 16
        }
        objectives[46] = CellBuilder().invoke {
            aka("elbownearwater")
            e(l, d)
        }
        objectives[47] = MapCellData(
            "raftHeartEntry",
            Objective(
                InDest.centerRaftHeart, Dest.Heart.raftHeart, Objective.ItemLoc.Right
            )
        )

        objectives[47] = CellBuilder().invoke {
            aka("raftHeartEntry")
            this has Dest.Heart.raftHeart at InDest.centerRaftHeart.x a InDest.centerRaftHeart.y
        }
    }

    private fun addRow3(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[63] = MapCellData(
            "shorerafttoheart",
        )
        objectives[62] = MapCellData(
            "elbowshorenearheart"
        )
        objectives[61] = MapCellData(
            "woodstwoguys",
            Objective(
                FramePoint(144, 64), Dest.Secrets.secretForest30NorthEast
            )
        )
        objectives[60] = MapCellData(
            "lev2",
            Objective(InDest.centerLevel, Dest.level(2))
        )
        objectives[59] = MapCellData(
            "desertbottomright",
        )
        objectives[58] = MapCellData(
            "desertbottomleft",
        )
        objectives[57] = MapCellData(
            "greenFairy",
            Objective(type = Dest.Fairy.greenForest)
        )
        objectives[56] = MapCellData(
            "lev1Entrybefore"
        )
        objectives[55] = MapCellData(
            "lev1Entry",
            Objective(InDest.centerLevel, Dest.level(1))
        )
        objectives[54] = MapCellData(
            "nowheremountain1"
        )
        objectives[53] = MapCellData(
            "nowheremountain2"
        )
        objectives[52] = MapCellData(
            "blueringshop",
            Objective(FramePoint(4.grid, 4.grid), Dest.Shop.blueRing),
        )
        objectives[51] = CellBuilder().invoke {
            aka("level6 potion")
            this has Dest.Shop.potionShopLevel6 at 10.grid a 1.grid //todo
        }
    }

    private fun addRow4(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[69] = MapCellData(
            "level4",
            Objective(InDest.centerLevel3, Dest.level(4))
        )
        objectives[66] = MapCellData(
            "lev7",
            Objective(6.grid, 5.grid, Dest.level(7))
        )
        objectives[67] = MapCellData(
            "brownFairy",
            Objective(type = Dest.Fairy.brownForest)
        )
        val shieldLeft = Objective.ItemLoc.Left
        objectives[70] = MapCellData(
            "cornertree",
            Objective(FramePoint(9.grid, 7.grid), Dest.Shop.westTreeShopNearWater, itemLoc = shieldLeft)
        )
        objectives[71] = MapCellData(
            "fireHeart",
            Objective(FramePoint(11.grid, 7.grid), Dest.Heart.fireHeart, itemLoc = Objective.ItemLoc.Right)
        )
        objectives[72] = MapCellData(
            "undergrounddudes",
            Objective(FramePoint(13.grid, 2.grid), Dest.Secrets.fire30GreenSouth)
        )
        objectives[73] = MapCellData("near fairy")
        objectives[75] = CellBuilder().invoke {
            aka("tree with fairy")
            this has Dest.Shop.potionShopForest at 11.grid a 2.grid
        }
        val shopCShieldLoc = Objective.ItemLoc.Left
        objectives[77] = MapCellData(
            "forest before 2",
            Objective(FramePoint(13.grid, 6.grid), Dest.Shop.eastTreeShop, itemLoc = shopCShieldLoc)
        )
        objectives[78] = MapCellData(
            "forest before 2 10 secret",
            Objective(FramePoint(10.grid, 4.grid), Dest.Secrets.level2secret10)
        )
        objectives[81] = MapCellData(
            "forest burn secret",
            Objective(FramePoint(9.grid, 6.grid), Dest.Secrets.forest10BurnBrown)
        )
    }

    private fun addRow5(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[86] = MapCellData(
            "squareforest",
            Objective(FramePoint(10.grid, 6.grid), Dest.Secrets.forest10Mid)
        )
        objectives[88] = MapCellData(
            "boringForest",
        )
        objectives[94] = MapCellData(
            "candelshop",
            // candle
            Objective(FramePoint(7.grid, 1.grid), Dest.Shop.candleShopEast, Objective.ItemLoc.Right)
        )
        objectives[95] = MapCellData(
            "ladderHeart",
            Objective(FramePoint(12.grid, 5.grid), Dest.Heart.ladderHeart)
        )
    }

    fun addRow6(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[96] = MapCellData(
            "after lost woods",
            Objective(
                FramePoint(100, 100), DestType.SecretToEverybody
                    (20, EntryType.Fire())
            )
        )
        objectives[98] = MapCellData(
            "woods with secret",
            Objective(FramePoint(8.grid, 2.grid), Dest.Secrets.fire100SouthBrown)
        )
        objectives[100] = MapCellData(
            "potion shop",
            Objective(FramePoint(7.grid, 1.grid), Dest.Shop.potionShopWest)
        )
        objectives[102] = MapCellData(
            "candle shop",
            Objective(FramePoint(7.grid, 1.grid), Dest.Shop.candleShopMid, Objective.ItemLoc.Right)
        )
        objectives[103] = MapCellData(
            "up start",
            Objective(FramePoint(7.grid, 1.grid), Dest.Secrets.bomb30Start)
        )
        objectives[104] = MapCellData(
            "upForest",
            Objective(
                FramePoint(2.grid, 6.grid), Dest.SecretsNegative.forest20NearStart)
            )
        objectives[107] = MapCellData(
            "forest burn 100",
            // need to check this
            Objective(FramePoint(8.grid, 6.grid), Dest.Secrets.forest100South)
        )
        objectives[109] = MapCellData(
            "lev 9",
            Objective(10.grid, 2.grid, Dest.level(8))
        )
    }

    fun addRow7(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[111] = MapCellData(
            "arrow shop",
            Objective(FramePoint(48, 16), Dest.Shop.arrowShop, itemLoc = Objective.ItemLoc.Right)
        )
        objectives[112] = MapCellData(
            "useless end has shop",
            Objective(
                FramePoint(100, 100), DestType
                    .Shop()
            )
        )
        objectives[113] = CellBuilder().invoke {
            aka("bottomforestsecret")
            // 8 is a guess
            this has Dest.Secrets.bombSecret30SouthWest at 5.grid a 1.grid
        }
        objectives[114] =
            MapCellData(
                "top bottom forest"
            )
        objectives[115] =
            MapCellData(
                "forest near lev3"
            )
        objectives[116] =
            MapCellData(
                "lev3",
                Objective(
                    InDest.centerLevel3, Dest.level(3)
                )
            )
        objectives[117] =
            MapCellData(
                "useless space"
            )
        objectives[118] =
            MapCellData(
                "moneygame1",
                Objective(
                    FramePoint(100, 100), DestType.MoneyGame
                )
            )
        objectives[119] =
            MapCellData(
                "start",
                Objective(FramePoint(64, 17), Dest.item(ZeldaItem.WoodenSword))
            )
        objectives[120] = CellBuilder().invoke {
            aka("startRight")
            this has Dest.Shop.potionShopNearStart at 4.grid a 6.grid
        }
        objectives[121] = MapCellData(
            "lostwoods travel spot",
        )
        objectives[122] = MapCellData(
            "stone nothing walk"
        )
        objectives[123] = MapCellData(
            "bombHeartSouth",
            Objective(FramePoint(9.grid, 1.grid), Dest.Heart.bombHeartSouth, itemLoc = Objective.ItemLoc.Right),
        )
    }
}

private class CellBuilder {
    operator fun invoke(block: CellBuilder.() -> Unit = {}): MapCellData {
        this.block()
        return build()
    }

    private var name: String = ""
    private var destTypes: MutableList<DestTypeBuilder> = mutableListOf()
    private var exitSet: ExitSet = ExitSetAll

    class DestTypeBuilder(
        private var destType: DestType = DestType.Princess, private val builder: CellBuilder
    ) {
        private var point: FramePoint = FramePoint(0, 0)
        private var x: Int = 0

        infix fun a(y: Int): CellBuilder {
            this.point = FramePoint(x, y)
            return builder
        }

        infix fun at(x: Int): DestTypeBuilder {
            this.x = x
            return this
        }

        fun build(): Objective {
            return Objective(point, destType)
        }
    }

    private fun build(): MapCellData {
        return MapCellData(name, destTypes.map { it.build() }.firstOrNull() ?: Objective.empty, exitSet)
    }

    fun aka(aka: String): CellBuilder {
        name = aka
        return this
    }

    fun e(vararg direction: Direction): CellBuilder {
        exitSet = ExitSet(*direction)
        return this
    }

    infix fun has(des: DestType): DestTypeBuilder {
        val builder = DestTypeBuilder(des, this)
        destTypes.add(builder)
        return builder
    }
}

