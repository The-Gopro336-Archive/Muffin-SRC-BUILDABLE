package me.han.muffin.client.utils

/**
 * Keep track of frequency of some action,
 * put weights into buckets, which represent intervals of time.
 * @author mc_dev
 */
class ActionFrequency @JvmOverloads constructor(nBuckets: Int, durBucket: Long, noAutoReset: Boolean = false) {
    /** Reference time for the transition from the first to the second bucket.  */
    private var time: Long = 0

    /**
     * Time of last update (add). Should be the "time of the last event" for the
     * usual case.
     */
    private var lastUpdate: Long = 0
    private val noAutoReset: Boolean

    /**
     * Buckets to fill weights in, each represents an interval of durBucket duration,
     * index 0 is the latest, highest index is the oldest.
     * Weights will get filled into the next buckets with time passed.
     */
    private val buckets: FloatArray

    /** Duration in milliseconds that one bucket covers.  */
    private val durBucket: Long

    /**
     * Update and add (updates reference and update time).
     * @param now
     * @param amount
     */
    fun add(now: Long, amount: Float) {
        update(now)
        buckets[0] += amount
    }

    /**
     * Unchecked addition of amount to the first bucket.
     * @param amount
     */
    fun add(amount: Float) {
        buckets[0] += amount
    }

    /**
     * Update without adding, also updates reference and update time. Detects
     * time running backwards.
     *
     * @param now
     */
    fun update(now: Long) {
        val diff = now - time
        if (now < lastUpdate) {
            // Time ran backwards.
            if (noAutoReset) {
                // Only update time and lastUpdate.
                lastUpdate = now
                time = lastUpdate
            } else {
                // Clear all.
                clear(now)
                return
            }
        } else if (diff >= durBucket * buckets.size) {
            // Clear (beyond range).
            clear(now)
            return
        } else if (diff < durBucket) {
            // No changes (first bucket).
        } else {
            val shift = (diff.toFloat() / durBucket.toFloat()).toInt()
            // Update buckets.
            for (i in 0 until buckets.size - shift) {
                buckets[buckets.size - (i + 1)] = buckets[buckets.size - (i + 1 + shift)]
            }
            for (i in 0 until shift) {
                buckets[i] = 0F
            }
            // Set time according to bucket duration (!).
            time += durBucket * shift
        }
        // Ensure lastUpdate is set.
        lastUpdate = now
    }

    /**
     * Clear all counts, reset reference and update time.
     * @param now
     */
    fun clear(now: Long) {
        for (i in buckets.indices) {
            buckets[i] = 0f
        }
        lastUpdate = now
        time = lastUpdate
    }

    /**
     * @param factor
     * @return
     */
    @Deprecated(
        """Use instead: score(float).
      """
    )
    fun getScore(factor: Float): Float {
        return score(factor)
    }

    /**
     * @param factor
     * @return
     */
    @Deprecated(
        """Use instead: score(float).
      """
    )
    fun getScore(bucket: Int): Float {
        return bucketScore(bucket)
    }

    /**
     * Get a weighted sum score, weight for bucket i: w(i) = factor^i.
     * @param factor
     * @return
     */
    fun score(factor: Float): Float {
        return sliceScore(0, buckets.size, factor)
    }

    /**
     * Get score of a certain bucket. At own risk.
     * @param bucket
     * @return
     */
    fun bucketScore(bucket: Int): Float {
        return buckets[bucket]
    }

    /**
     * Get score of first end buckets, with factor.
     * @param end Number of buckets including start. The end is not included.
     * @param factor
     * @return
     */
    fun leadingScore(end: Int, factor: Float): Float {
        return sliceScore(0, end, factor)
    }

    /**
     * Get score from start on, with factor.
     * @param start This is included.
     * @param factor
     * @return
     */
    fun trailingScore(start: Int, factor: Float): Float {
        return sliceScore(start, buckets.size, factor)
    }

    /**
     * Get score from start on, until before end, with factor.
     * @param start This is included.
     * @param end This is not included.
     * @param factor
     * @return
     */
    fun sliceScore(start: Int, end: Int, factor: Float): Float {
        var score = buckets[start]
        var cf = factor
        for (i in start + 1 until end) {
            score += buckets[i] * cf
            cf *= factor
        }
        return score
    }

    /**
     * Set the value for a buckt.
     * @param n
     * @param value
     */
    fun setBucket(n: Int, value: Float) {
        buckets[n] = value
    }

    /**
     * Set the reference time and last update time.
     * @param time
     */
    fun setTime(time: Long) {
        this.time = time
        lastUpdate = time
    }

    /**
     * Get the reference time for the transition from the first to the second bucket.
     * @return
     */
    fun lastAccess(): Long { // TODO: Should rename this.
        return time
    }

    /**
     * Get the last time when update was called (adding).
     * @return
     */
    fun lastUpdate(): Long {
        return lastUpdate
    }

    /**
     * Get the number of buckets.
     * @return
     */
    fun numberOfBuckets(): Int {
        return buckets.size
    }

    /**
     * Get the duration of a bucket in milliseconds.
     * @return
     */
    fun bucketDuration(): Long {
        return durBucket
    }

    /**
     * Serialize to a String line.
     * @return
     */
    fun toLine(): String {
        // TODO: Backwards-compatible lastUpdate ?
        val buffer = StringBuilder(50)
        buffer.append(buckets.size.toString() + "," + durBucket + "," + time)
        for (i in buckets.indices) {
            buffer.append("," + buckets[i])
        }
        return buffer.toString()
    }

    companion object {
        /**
         * Update and then reduce all given ActionFrequency instances by the given
         * amount, capped at a maximum of 0 for the resulting first bucket score.
         *
         * @param amount
         * The amount to subtract.
         * @param freqs
         */
        fun reduce(time: Long, amount: Float, vararg freqs: ActionFrequency) {
            for (i in 0 until freqs.size) {
                val freq = freqs[i]
                freq.update(time)
                freq.setBucket(0, Math.max(0f, freq.bucketScore(0) - amount))
            }
        }

        /**
         * Update and then reduce all given ActionFrequency instances by the given
         * amount, without capping the result.
         *
         * @param amount
         * The amount to subtract.
         * @param freqs
         */
        fun subtract(time: Long, amount: Float, vararg freqs: ActionFrequency) {
            for (i in 0 until freqs.size) {
                val freq = freqs[i]
                freq.update(time)
                freq.setBucket(0, freq.bucketScore(0) - amount)
            }
        }

        /**
         * Deserialize from a string.
         * @param line
         * @return
         */
        fun fromLine(line: String): ActionFrequency {
            // TODO: Backwards-compatible lastUpdate ?
            val split = line.split(",".toRegex()).toTypedArray()
            if (split.size < 3) throw RuntimeException("Bad argument length.") // TODO
            val n = split[0].toInt()
            val durBucket = split[1].toLong()
            val time = split[2].toLong()
            val buckets = FloatArray(split.size - 3)
            if (split.size - 3 != buckets.size) throw RuntimeException("Bad argument length.") // TODO
            for (i in 3 until split.size) {
                buckets[i - 3] = split[i].toFloat()
            }
            val freq = ActionFrequency(n, durBucket)
            freq.setTime(time)
            for (i in buckets.indices) {
                freq.setBucket(i, buckets[i])
            }
            return freq
        }
    }
    /**
     *
     * @param nBuckets
     * @param durBucket
     * @param noAutoReset
     * Set to true, to prevent auto-resetting with
     * "time ran backwards". Setting this to true is recommended if
     * larger time frames are monitored, to prevent data loss.
     */
    /**
     * This constructor will set noAutoReset to false, optimized for short term
     * accounting.
     *
     * @param nBuckets
     * @param durBucket
     */
    init {
        buckets = FloatArray(nBuckets)
        this.durBucket = durBucket
        this.noAutoReset = noAutoReset
    }
}
