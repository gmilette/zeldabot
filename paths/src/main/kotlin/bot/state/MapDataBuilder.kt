package bot.state

import sequence.*

class MapBuilder {
    val u = Direction.Up
    val r = Direction.Right
    val d = Direction.Down
    val l = Direction.Left
    val upRight = e(u, r)
    val upDown = e(u, d)

    fun e(vararg dir: Direction): ExitSet {
        return ExitSet(*dir)
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
            it.value.objectives to it.value
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
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (20, EntryType.Walk)
            ),
            exits = e(l, d, r)
        )
        objectives[3] = MapCellData(
            "topleftrock",
            Objective(
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (20, EntryType.Bomb)
            ),
            exits = e(l, d)
        )
        objectives[4] = MapCellData(
            "nexttolev9woman",
            Objective(
                FramePoint(100, 100), DestType.WOMAN
            ),
            exits = e(r)
        )
        objectives[5] = MapCellData(
            "lev9",
            Objective(
                FramePoint(100, 100), Dest.level(9)
            ),
            exits = e(r)
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
                DestType.SECRET_TO_EVERYBODY(20, EntryType.Bomb)
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
            Objective(
                100,
                100,
                DestType.ITEM(ZeldaItem.WhiteSword, EntryType.Walk)
            ),
            exits = e(d)
        )
        objectives[11] = MapCellData(
            "lev5",
            Objective(
                100, 100, Dest.level(5)
            ),
            exits = e(d)
        )
        objectives[12] = MapCellData(
            "nowhereshow",
            Objective(
                100, 100, DestType.SHOP()
            ),
            exits = e(d, r)
        )
        objectives[13] = MapCellData(
            "nowherepotion",
            Objective(
                100, 100, DestType.WOMAN
            ),
            exits = e(l, d)
        )
        objectives[14] = MapCellData(
            "letter",
            Objective(
                100, 100, DestType.ITEM(ZeldaItem.Letter)
            ),
            exits = e(d)
        )
        objectives[15] = MapCellData(
            "moneythroughwall",
            Objective(
                100, 100, DestType.SECRET_TO_EVERYBODY(100)
            ),
            exits = e(d)
        )
    }

    fun addRow1(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[16] = MapCellData(
            "leftshowTop",
            Objective(
                100, 100, DestType.SHOP()
            ),
            exits = e(u, r)
        )
        objectives[17] = MapCellData(
            "uselessnearshop",
            Objective(
                100, 100, DestType.SHOP()
            ),
            exits = e(l, u)
        )
        objectives[18] = MapCellData(
            "placeshop",
            Objective(
                100, 100, DestType.SHOP()
            ),
            exits = e(u, r)
        )
        objectives[19] = MapCellData(
            "placeshopsecret",
            Objective(
                100, 100, DestType.SECRET_TO_EVERYBODY(30, EntryType.Bomb)
            ),
            exits = e(l, r, u)
        )
        objectives[20] = MapCellData(
            "mttopsecret",
            Objective(
                100, 100, DestType.SECRET_TO_EVERYBODY(20, EntryType.Bomb)
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
                100, 100, DestType.SHOP()
            ),
            exits = e(l, r)
        )
        objectives[23] = MapCellData(
            "mountainstairriver",
            Objective(
                100, 100, DestType.SHOP()
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
            Objective(100, 100, DestType.MONEY_GAME),
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
            Objective(100, 100, DestType.MONEY_GAME),
            exits = e(l, d)
        )
        objectives[34] = MapCellData(
            "lev6",
            Objective(FramePoint(100, 100), Dest.level(6)),
            exits = e(d)
        )
        objectives[35] = MapCellData(
            "lev6travel",
            Objective(FramePoint(100, 100), DestType.Travel),
            exits = e(d) // special
        )
        objectives[36] = MapCellData(
            "lev6",
            Objective(FramePoint(100, 100), Dest.level(6)),
            exits = e(d)
        )
        objectives[36] = CellBuilder().invoke {
            aka("powerbracelet")
            e(l,r,d)
            this has DestType.ITEM(ZeldaItem.PowerBracelet, EntryType.Statue) at 100 a 100
        }
        objectives[37] = CellBuilder().invoke {
            aka("powerbraceletRight")
            e(l)
            this has DestType.SHOP(ShopType.B) at 100 a 100
        }
        objectives[38] = CellBuilder().invoke {
            aka("mountainWalk")
            e(r,d)
            this has DestType.SHOP(ShopType.C) at 100 a 100
        }
        objectives[39] = CellBuilder().invoke {
            aka("downfromboulder")
            e(u,r) // special
            this has DestType.SHOP(ShopType.Woman) at 100 a 100
        }
        objectives[40] = CellBuilder().invoke {
            aka("forestneardesert")
            e(l, r, d)
            this has DestType.SECRET_TO_EVERYBODY(30, EntryType.Fire) at 100 a 100
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
        objectives[44] = CellBuilder().invoke {
            aka("bombheartstone")
            e(u, l, r, d)
            this has DestType.ITEM(ZeldaItem.BombHeart, entry = EntryType.Bomb) at 100 a 100
        }
        objectives[45] = CellBuilder().invoke {
            aka("forestsecret")
            e(u, l, r, d)
            this has DestType.SECRET_TO_EVERYBODY(30, EntryType.Fire) at 100 a 100
        }
        objectives[46] = CellBuilder().invoke {
            aka("elbownearwater")
            e(l, d)
        }
        objectives[47] = CellBuilder().invoke {
            aka("raftHeartEntry")
            e(d)
            this has DestType.ITEM(ZeldaItem.RaftHeartEntry, entry =
                EntryType.Bomb) at 100 a 100
        }
    }

    fun addRow3(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[63] = MapCellData(
            "shorerafttoheart",
            exits = ExitSet(Direction.Up)
        )
        objectives[62] = MapCellData(
            "elbowshorenearheart",
            exits = ExitSet(Direction.Up, Direction.Right)
        )
        objectives[61] = MapCellData(
            "woodstwoguys",
            Objective(
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (30, EntryType.Statue)
            )
        )
        objectives[60] = MapCellData(
            "lev2",
            Objective(FramePoint(100, 100), Dest.level(2)),
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
            Objective(FramePoint(112, 64), Dest.level(1)),
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
            Objective(FramePoint(100, 100), DestType.SHOP(ShopType.BlueRing,
                EntryType.Statue)),
            exits = e(d)
        )
    }

    fun addRow4(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[73] = MapCellData(
            "undergrounddudes",
            Objective(
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (30, EntryType.Fire)
            )
        )
    }

    fun addRow5(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[88] = MapCellData(
            "boringForest",
        )
    }

    fun addRow6(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[96] = MapCellData(
            "after lost woods",
            Objective(
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (20, EntryType.Fire)
            )
        )
        objectives[104] = MapCellData(
            "upForest",
            Objective(
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (20, EntryType.Fire)
            )
        )
    }

    fun addRow7(objectives: MutableMap<MapLoc, MapCellData>) {
        objectives[112] = MapCellData(
                    "useless end has shop?",
                    Objective(
                        FramePoint(100, 100), DestType
                            .SHOP()
                    ),
                    exits = e(r)
                )
        objectives[113] =
                MapCellData(
                    "top bottom forest",
                    Objective(
                        FramePoint(100, 100), DestType
                            .SECRET_TO_EVERYBODY(30, EntryType.Bomb)
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
                        FramePoint(100, 100), Dest.level(3)
                    ),
                    exits = e(l)
                )
        objectives[117] =
                MapCellData(
                    "useless space",
                    Objective(
                        FramePoint(100, 100), DestType.MONEY_GAME
                    ),
                    exits = e(u, r)
                )
        objectives[118] =
                MapCellData(
                    "moneygame1",
                    Objective(
                        FramePoint(100, 100), DestType.MONEY_GAME
                    ),
                    exits = e(l, u, r)
                )
        objectives[119] =
                MapCellData(
                    "start",
                    Objective(
                        FramePoint(100, 100), Dest.item(
                            ZeldaItem
                                .WoodenSword)
                    ),
                    exits = ExitSet(Direction.Up, Direction.Right)
                )
        objectives[120] = MapCellData(
            "startRight",
            Objective(FramePoint(100, 100), DestType.WOMAN),
            exits = ExitSet(Direction.Up, Direction.Right)
        )
        objectives[121] = MapCellData(
            "lostwoods",
            Objective(FramePoint(100, 100), DestType.WOMAN),
            exits = ExitSetAll
        )
        objectives[122] = MapCellData(
            "brown wood 100",
            Objective(
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (100, entry = EntryType.Fire)
            ),
            exits = ExitSetAll
        )
        objectives[122] = MapCellData(
            "brown crossing",
            Objective(
                FramePoint(100, 100), DestType.SECRET_TO_EVERYBODY
                    (100, entry = EntryType.Fire)
            ),
            exits = ExitSetAll
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
        .Fairy, private val builder: CellBuilder) {
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
        return MapCellData(name, destTypes.map { it.build() }, exitSet)
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

