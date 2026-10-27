package com.wanderwildwood.okiba.core

/**
 * Which of a run of GPS positions to keep. The first position a GPS gives after waking is often
 * its roughest, tens of metres out; it settles over the next seconds. So the run is watched:
 * a position good to [goodEnoughM] or better ends it at once, and otherwise the best one seen
 * is taken [settleMs] after the first arrived. A position that says nothing of its accuracy
 * counts as the worst kind.
 */
class BestFix<T>(
    private val goodEnoughM: Float = GOOD_ENOUGH_M,
    private val settleMs: Long = SETTLE_MS,
) {
    var best: T? = null
        private set
    var bestAccuracy: Float = Float.MAX_VALUE
        private set
    private var firstAt: Long? = null

    /** Takes one position; true when it is good enough to stop looking. */
    fun offer(item: T, accuracyM: Float, now: Long): Boolean {
        if (firstAt == null) firstAt = now
        val acc = if (accuracyM > 0f) accuracyM else Float.MAX_VALUE
        if (best == null || acc < bestAccuracy) {
            best = item
            bestAccuracy = acc
        }
        return bestAccuracy <= goodEnoughM
    }

    /** When the best so far is taken anyway, or null while nothing has come. */
    fun settlesAt(): Long? = firstAt?.let { it + settleMs }

    companion object {
        /** About a car's length either way: enough to find it among the others. */
        const val GOOD_ENOUGH_M = 15f
        const val SETTLE_MS = 20_000L
    }
}
