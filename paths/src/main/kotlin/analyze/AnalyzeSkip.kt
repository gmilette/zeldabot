package analyze

import bot.state.*
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.read

//private val pathToCsv = "skips/skips.csv"
private val pathToCsv = "skips/skips_sm.csv"

//level,loc,move,actual,from,fromDir,before1,before1Dir,before2,before2Dir,before3,before3Dir,diff,diffx,diffy,diffAlert

data class Pd(
    val level: Int,
    val loc: Int,
    val move: String,
    val actual: String,
    val from: String,
    val fromDir: String,
    val before1: String,
    val before1Dir: String,
    val before2: String,
    val before2Dir: String,
    val before3: String,
    val before3Dir: String,
    val donno: String,
    val diff: Int,
    val diffx: Int,
    val diffy: Int,
    val diffAlert: String
)


fun main() {
    println("***Start***")
//        val genres by column<String>()
//        val title by column<String>()
//        val year by column<Int>()

    DataFrame
        .read(pathToCsv)
//        .groupBy(PreviousData::loc)
//
        //        .filter { it[PreviousData::diffAlert] == 0}
        .filter {
            it[Pd::diffx] < 3
            //it[Pd::diffx] == 2 &&
            //    it[Pd::diffy] == 0 &&
//                it[Pd::from] == "200_32"
//                && it[Pd::before1] == "199_32"
                    && it[Pd::actual] != it[Pd::from]
//                && it[Pd::loc] == 7
                && it[Pd::fromDir] == "Down"
        }
        .sortBy(Pd::from)
//        .groupBy(Pd::from)
//        .aggregate {
//            }
        .print(40)
//            .split { genres }.by("|").inplace()
//            .split { title }.by {
//                listOf(
//                    """\s*\(\d{4}\)\s*$""".toRegex().replace(it, ""),
//                    "\\d{4}".toRegex().findAll(it).lastOrNull()?.value?.toIntOrNull() ?: -1
//                )
//            }.into(title, year)
//            .explode { genres }
//            .filter { year() >= 0 && genres() != "(no genres listed)" }
//            .groupBy { year }
//            .sortBy { year }
//            .pivot(inward = false) { genres }
//            .aggregate {
//                count() into "count"
//                mean() into "mean"
//            }.print(10)

    println("***Complete***")
}