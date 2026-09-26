package util

import kotlin.random.Random

object ZRandom {
    var seed: Long? = null

    private var current: Random = Random.Default

    fun startTrial(trial: Int) {
        val base = seed ?: return
        current = Random(base + trial)
        d { " seeded trial $trial with ${base + trial}" }
    }

    fun nextInt(bound: Int): Int = current.nextInt(bound)

    fun nextBoolean(): Boolean = current.nextBoolean()

    fun <T> shuffled(list: List<T>): List<T> = list.shuffled(current)
}
