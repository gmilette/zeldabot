package bot.state.map

import bot.state.FramePoint
import bot.state.MapLoc
import sequence.*

val Int.grid
    get() = this * 16

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

        // if entryType == fire, requires candle

//        119 -> {
//            "lev1Entry"
//            100, 100 -> Level(1)
//        }
        return objectives
    }

    fun addRow0(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[0] = MapCellData(
            "topleftrock",
            exits = e(d, r)
        )
        objectives[1] = MapCellData(
            "topleftrock",
            Objective(
                FramePoint(100, 100), DestType.SecretToEverybody
                    (20, EntryType.Walk)
            ),
            exits = e(l, d, r)
        )
        objectives[3] = MapCellData(
            "topleftrock",
            Objective(
                FramePoint(100, 100), DestType.SecretToEverybody
                    (20, EntryType.Bomb)
            ),
            exits = e(l, d)
        )
        objectives[4] = MapCellData(
            "nexttolev9woman",
            Objective(
                FramePoint(100, 100), DestType.Woman
            ),
            exits = e(r)
        )
        objectives[5] = MapCellData(
            "lev9",
            Objective(FramePoint(5.grid, 6.grid), Dest.level(9))
        )
        objectives[6] = MapCellData(
            "lev9",
            exits = e(l, r)
        )
        objectives[7] = MapCellData(
            "levright9",
            Objective(
                100,
                100,
                DestType.SecretToEverybody(20, EntryType.Bomb)
            ),
            exits = e(d, l, r)
        )
        objectives[8] = MapCellData(
            "lev9rightright",
            exits = e(l, r)
        )
        objectives[9] = MapCellData(
            "lev9arrow",
            exits = e(l)
        )
        objectives[10] = MapCellData(
            "lev9arrow",
//                32,
//                16,
            Objective(
                2.grid,
                1.grid,
                DestType.Item(ZeldaItem.WhiteSword, EntryType.Walk)
            ),
            exits = e(d)
        )
        objectives[11] = MapCellData(
            "lev5",
            Objective(7.grid, 4.grid, Dest.level(5)),
            exits = e(d)
        )
        objectives[12] = MapCellData(
            "nowhereshow",
            Objective(
                100, 100, DestType.Shop()
            ),
            exits = e(d, r)
        )
        objectives[13] = MapCellData(
            "nowherepotion",
            Objective(
                100, 100, DestType.Woman
            ),
            exits = e(l, d)
        )
        objectives[14] = MapCellData(
            "letter",
    //        val letterEntrance = FramePoint(80, 64)
            Objective(
                5.grid, 4.grid, DestType.Item(ZeldaItem.Letter)
            ),
            exits = e(d)
        )
        objectives[15] = MapCellData(
            "moneythroughwall",
            //FramePoint(128, 64)
            Objective(128, 64, Dest.Secrets.walk100),
            exits = e(d)
        )
    }

    fun addRow1(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[16] = MapCellData(
            "leftshowTop",
            Objective(
                100, 100, DestType.Shop()
            ),
            exits = e(u, r)
        )
        objectives[17] = MapCellData(
            "uselessnearshop",
            Objective(
                100, 100, DestType.Shop()
            ),
            exits = e(l, u)
        )
        objectives[18] = MapCellData(
            "placeshop",
            Objective(
                100, 100, DestType.Shop()
            ),
            exits = e(u, r)
        )
        objectives[19] = MapCellData(
            "placeshopsecret",
            Objective(
                100, 100, DestType.SecretToEverybody(30, EntryType.Bomb)
            ),
            exits = e(l, r, u)
        )
        objectives[20] = MapCellData(
            "mttopsecret",
            Objective(
                100, 100, DestType.SecretToEverybody(20, EntryType.Bomb)
            ),
            exits = e(l, r, d)
        )
        objectives[21] = MapCellData(
            "mountainempty",
            exits = e(l, r)
        )
        objectives[21] = MapCellData(
            "mountainshop",
            Objective(
                100, 100, DestType.Shop()
            ),
            exits = e(l, r)
        )
        objectives[23] = MapCellData(
            "mountainstairriver",
            Objective(
                100, 100, DestType.Shop()
            ),
            exits = e(l, u, r, d) // can't go from r to l, unless have ladder
        )
        objectives[24] = MapCellData(
            "mountainboulder",
            exits = e(l, r)
        )
        objectives[25] = MapCellData(
            "mountainboulder2",
            exits = e(l, r)
        )
        objectives[26] = MapCellData(
            "mountainboulderupforwhitesword",
            exits = e(l, r, u)
        )
        objectives[27] = MapCellData(
            "lostmountain",
            exits = e(l, r, u, d) // special routing
        )
        objectives[28] = MapCellData(
            "lostmountainright",
            exits = e(l, r, u, d)
        )
        objectives[29] = MapCellData(
            "lostmountainrightright",
            Objective(100, 100, DestType.Travel),
            exits = e(l, d, r) // can't go r to left, just skip the right
        )
        objectives[30] = MapCellData(
            "mountaingoupforletter",
            exits = e(l, r, u)
        )
        objectives[31] = MapCellData(
            "mountainnear100secret",
            Objective(100, 100, DestType.MoneyGame),
            exits = e(l, r, u)
        )
    }

    fun addRow2(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[32] = MapCellData(
            "graveyardTopLeft",
            exits = e(r, d)
        )
        objectives[33] = MapCellData(
            "graveyardMagicSword",
            Objective(9.grid, 5.grid, DestType.Item(ZeldaItem.MagicSword, entry = EntryType.Push())),
        )
        objectives[34] = MapCellData(
            "lev6",
            Objective(InDest.centerLevel6, Dest.level(6)),
            exits = e(d)
        )
        objectives[35] = MapCellData(
            "lev6travel",
            Objective(FramePoint(100, 100), DestType.Travel),
            exits = e(d) // special
        )
        // push down spot
//        val powerBraceletEntrance = FramePoint(224, 48)
//        val powerBraceletItem = FramePoint(224, 62)
        objectives[36] = MapCellData(
            "power bracelet",
            Objective(14.grid, 4.grid, DestType.Item(ZeldaItem.PowerBracelet, entry = EntryType.Statue)),
        )
//        objectives[36] = CellBuilder().invoke {
//            aka("powerbracelet")
//            e(l,r,d)
//            this has DestType.ITEM(ZeldaItem.PowerBracelet, EntryType.Statue) at 100 a 100
//        }
        objectives[37] = CellBuilder().invoke {
            aka("powerbraceletRight")
            e(l)
            this has DestType.Shop(ShopType.B) at 100 a 100
        }
        objectives[38] = CellBuilder().invoke {
            aka("mountainWalk")
            e(r,d)
            this has DestType.Shop(ShopType.C) at 100 a 100
        }
        objectives[39] = CellBuilder().invoke {
            aka("downfromboulder")
            e(u,r) // special
            this has DestType.Shop(ShopType.Woman) at 100 a 100
        }
        objectives[40] = CellBuilder().invoke {
            aka("forestneardesert")
            e(l, r, d)
            this has DestType.SecretToEverybody(30, EntryType.Fire(Direction.Right)) at 100 a 100
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
            Objective(FramePoint(144, 96), Dest.Secrets.bombHeartNorth, itemLoc = Objective.ItemLoc.Right)
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

    fun addRow3(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[63] = MapCellData(
            "shorerafttoheart",
        )
        objectives[62] = MapCellData(
            "elbowshorenearheart",
            exits = ExitSet(Direction.Up, Direction.Right)
        )
        objectives[61] = MapCellData(
            "woodstwoguys",
            Objective(
                FramePoint(144, 64), Dest.Secrets.secretForest30NorthEast
            )
        )
        objectives[60] = MapCellData(
            "lev2",
            Objective(InDest.centerLevel, Dest.level(2)),
            exits = ExitSet(Direction.Down)
        )
        objectives[59] = MapCellData(
            "desertbottomright",
        )
        objectives[58] = MapCellData(
            "desertbottomleft",
        )
        objectives[57] = MapCellData(
            "lev1fairy",
            Objective(
                FramePoint(100, 100), DestType.Fairy
            )
        )
        objectives[56] = MapCellData(
            "lev1Entrybefore",
            exits = ExitSet(Direction.Left, Direction.Up, Direction.Down)
        )
        objectives[55] = MapCellData(
            "lev1Entry",
            Objective(InDest.centerLevel, Dest.level(1)),
            exits = ExitSet(Direction.Right)
        )
        objectives[54] = MapCellData(
            "nowheremountain1",
            exits = ExitSet(Direction.Up, Direction.Left)
        )
        objectives[53] = MapCellData(
            "nowheremountain2",
            exits = ExitSet(Direction.Up, Direction.Right)
        )
        objectives[52] = MapCellData(
            "blueringshop",
            Objective(FramePoint(4.grid, 4.grid), Dest.Shop.blueRing),
        )
    }

    fun addRow4(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[69] = MapCellData(
            "level4",
            Objective(InDest.centerLevel3, Dest.level(4))
        )
        //
        objectives[66] = MapCellData(
            "lev7",
            Objective(6.grid, 5.grid, Dest.level(7))
        )
        objectives[71] = MapCellData(
            "fireHeart",
            Objective(FramePoint(11.grid, 7.grid), Dest.Heart.fireHeart, itemLoc = Objective.ItemLoc.Right)
        )
        objectives[72] = MapCellData(
            "undergrounddudes",
            Objective(FramePoint(13.grid, 2.grid), Dest.Secrets.fire30GreenSouth)
        )
        objectives[73] = MapCellData("near fary")
        val shopCShieldLoc = Objective.ItemLoc.Right
        objectives[77] = MapCellData(
            "forest before 2",
            Objective(FramePoint(13.grid, 6.grid), Dest.Shop.eastTreeShop, itemLoc = shopCShieldLoc)
        )
        objectives[78] = MapCellData(
            "forest before 2 10 secret",
            Objective(FramePoint(10.grid, 4.grid), Dest.Secrets.level2secret10)
        )
    }

    fun addRow5(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[88] = MapCellData(
            "boringForest",
        )
        //
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
        objectives[102] = MapCellData(
            "candle shop",
            Objective(FramePoint(7.grid, 1.grid), Dest.Shop.candleShopMid, Objective.ItemLoc.Right)
        )
        //        val candleShopEntrance = InGrid(7, 2).pt
        objectives[104] = MapCellData(
            "upForest",
            Objective(
                FramePoint(100, 100), DestType.SecretToEverybody
                    (20, EntryType.Fire())
            )
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
                    "useless end has shop?",
                    Objective(
                        FramePoint(100, 100), DestType
                            .Shop()
                    ),
                    exits = e(r)
                )
        objectives[113] =
                MapCellData(
                    "top bottom forest",
                    Objective(
                        FramePoint(100, 100), DestType.SecretToEverybody(30, EntryType.Bomb)
                    ),
                    exits = e(u, r)
                )
        objectives[114] =
                MapCellData(
                    "top bottom forest",
                    exits = e(u, r, l)
                )
        objectives[115] =
                MapCellData(
                    "forest near lev3",
                    exits = e(u, r, l)
                )
        objectives[116] =
                MapCellData(
                    "lev3",
                    Objective(
                        InDest.centerLevel3, Dest.level(3)
                    ),
                    exits = e(l)
                )
        objectives[117] =
                MapCellData(
                    "useless space",
                    exits = e(u, r)
                )
        objectives[118] =
                MapCellData(
                    "moneygame1",
                    Objective(
                        FramePoint(100, 100), DestType.MoneyGame
                    ),
                    exits = e(l, u, r)
                )
        objectives[119] =
                MapCellData(
                    "start",
                    Objective(FramePoint(64, 17), Dest.item(ZeldaItem.WoodenSword))
                )
        objectives[120] = MapCellData(
            "startRight",
            Objective(FramePoint(100, 100), DestType.Woman),
            exits = ExitSet(Direction.Up, Direction.Right)
        )
        objectives[121] = MapCellData(
            "lostwoods travel spot",
        )
        objectives[122] = MapCellData(
            "stone nothing walk",
            exits = ExitSetAll
        )
        objectives[123] = MapCellData(
            "bombHeartSouth",
            Objective(FramePoint(9.grid, 1.grid), Dest.Secrets.bombHeartSouth, itemLoc = Objective.ItemLoc.Right),
        )

    }
}
class CellBuilder() {
    operator fun invoke(block: CellBuilder.() -> Unit = {}): MapCellData {
        this.block()
        return build()
    }

    private var name: String = ""
    private var destTypes: MutableList<DestTypeBuilder> = mutableListOf()
    private var exitSet: ExitSet = ExitSetAll

    class DestTypeBuilder(private var destType: DestType = DestType
        .Fairy, private val builder: CellBuilder
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

    fun build(): MapCellData {
        // hack, there is probably only 1
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

