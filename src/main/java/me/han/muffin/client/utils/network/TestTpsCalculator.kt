package me.han.muffin.client.utils.network

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.utils.ActionFrequency
import me.han.muffin.client.utils.CircularArray
import net.minecraft.network.play.server.SPacketTimeUpdate
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.min
import kotlin.math.round

object TestTpsCalculator {

    /** The Constant lagMaxTicks. */
    private var lagMaxTicks = 80

    /** Last n tick durations, measured from run to run. */
    private val tickDurations = LongArray(lagMaxTicks)

    /** Tick durations summed up in packs of n (nxn time covered).  */
    private val tickDurationsSq = LongArray(lagMaxTicks)

    /** Maximally covered time on ms for lag tracking, roughly.  */
    private val lagMaxCoveredMs = 50L * (1L + lagMaxTicks * (1L + lagMaxTicks))

    /** Lag spike durations (min) to keep track of.  */
    private val spikeDurations = longArrayOf(150, 450, 1000, 5000)

    /** Lag spikes > 150 ms counting (3 x 20 minutes). For lag spike length see spikeDurations.  */
    private val spikes = arrayOfNulls<ActionFrequency>(spikeDurations.size)

    /** The tick.  */
    var tick = 0

    /** The time last.  */
    private var timeLastTimeUpdate: Long = 0

    // Circular Buffer lasting ~60 seconds for tick storage
    private val tickRates = CircularArray.create(120, 20f)

    val averageTick = tickRates.average()

    val tickRate: Float
        get() = round(tickRates.average())

    val adjustTicks: Float get() = tickRates.average() - 20f
    val syncTicks: Float get() = 20.0F - tickRate
    val factor: Float get() = 20.0F / tickRate

    @Listener
    private fun onTicking(event: TickEvent) {
        tickRates.add(getLag(120, true))
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || event.packet !is SPacketTimeUpdate) return

        // Measure time after heavy stuff.
        val time = System.currentTimeMillis()
        var lastDur = 50L

        // Time running backwards check (not only players can!).
        if (timeLastTimeUpdate > time) {
            lastDur = 50
            for (i in spikeDurations.indices) {
                spikes[i]?.update(time)
            }
        } else if (tick > 0) {
            lastDur = time - timeLastTimeUpdate
        }

        // Update sums of sums of tick durations.
        if (tick > 0 && tick % lagMaxTicks == 0) {
            val sum = tickDurations[lagMaxTicks - 1]
            for (i in 1 until lagMaxTicks) {
                tickDurationsSq[i] = tickDurationsSq[i - 1] + sum
            }
            tickDurationsSq[0] = sum
        }

        // Update tick duration sums.
        for (i in 1 until lagMaxTicks) {
            tickDurations[i] = tickDurations[i - 1] + lastDur
        }
        tickDurations[0] = lastDur

        // Lag spikes counting. [Subject to adjustments!]
        if (lastDur > spikeDurations[0] && tick > 0) {
            spikes[0]?.add(time, 1f)
            for (i in 1 until spikeDurations.size) {
                if (lastDur > spikeDurations[i]) {
                    spikes[i]?.add(time, 1f)
                } else break
            }
        }

        // Finish.
        tick++
        timeLastTimeUpdate = time
    }

    @Listener
    private fun onConnect(event: ServerEvent.Connect) {
        reset()
    }

    /**
     * Get lag percentage for the last ms milliseconds.<br></br>
     * NOTE: Will not be synchronized, still can be called from other threads.
     * @param ms Past milliseconds to cover. A longer period of time may be used, up to two times if ms > lagMaxTicks * 50.
     * @return Lag factor (1.0 = 20 tps, 2.0 = 10 tps), excluding the current tick.
     */
    fun getLag(ms: Long): Float {
        return getLag(ms, false)
    }

    /**
     * Get lag percentage for the last ms milliseconds, if the specified ms is bigger than the maximally covered duration, the percentage will refer to the maximally covered duration, not the given ms.<br></br>
     * NOTE: Using "exact = true" is meant for checks in the main thread. If called from another thread, exact should be set to false.
     * @param ms Past milliseconds to cover. A longer period of time may be used, up to two times if ms > lagMaxTicks * 50.
     * @param exact If to include the currently running tick, if possible. Should only be set to true, if called from the main thread (or while the main thread is blocked).
     * @return Lag factor (1.0 = 20 tps, 2.0 = 10 tps).
     */
    fun getLag(ms: Long, exact: Boolean): Float {

        if (ms < 0) {
            // Account for freezing (i.e. check timeLast, might be an extra method)!
            return getLag(0, exact)
        } else if (ms > lagMaxCoveredMs) {
            return getLag(lagMaxCoveredMs, exact)
        }

        if (tick == 0) return 1F

        val add = if (ms > 0 && ms % 50 == 0L) 0 else 1
        // TODO: Consider: Put "exact" block here, subtract a tick if appropriate?
        val totalTicks = min(tick, add + (ms / 50).toInt())
        val maxTick = min(lagMaxTicks, totalTicks)
        var sum = tickDurations[maxTick - 1]
        var covered = (maxTick * 50).toLong()

        // Only count fully covered:
        if (totalTicks > lagMaxTicks) {
            var maxTickSq = min(lagMaxTicks, totalTicks / lagMaxTicks)
            if (lagMaxTicks * maxTickSq == totalTicks) {
                maxTickSq -= 1
            }
            sum += tickDurationsSq[maxTickSq - 1]
            covered += (lagMaxTicks * 50 * maxTickSq).toLong()
        }

        if (exact) {
            // Attempt to count in the current tick.
            val passed = System.currentTimeMillis() - timeLastTimeUpdate
            if (passed > 50) {
                // Only count in in the case of "overtime".
                covered += 50
                sum += passed
            }
        }

        // TODO: Investigate on < 1f.
        // return max(1f, sum.toFloat() / covered.toFloat())
        return (sum.toFloat() / covered.toFloat()).coerceIn(0.0F..25.0F)
    }

    /**
     * Get the stepping for lag spike duration tracking.
     *
     * @return the lag spike durations
     */
    fun getLagSpikeDurations(): LongArray {
        return spikeDurations.copyOf(spikeDurations.size)
    }

    /**
     * Get lag spike count according to getLagSpikeDurations() values. Entries
     * of lower indexes contain the entries of higher indexes (so subtraction
     * would be necessary to get spikes from...to).
     *
     * @return the lag spikes
     */
    fun getLagSpikes(): IntArray {
        val out = IntArray(spikeDurations.size)
        val now = System.currentTimeMillis()
        for (i in spikeDurations.indices) {
            spikes[i]?.update(now)
            out[i] = spikes[i]?.score(1f)?.toInt()!!
        }
        return out
    }

    /**
     * Reset tick and tick stats to 0 (!).
     */
    fun reset() {
        tick = 0
        timeLastTimeUpdate = -1L
        for (i in 0 until lagMaxTicks) {
            tickDurations[i] = 0
            tickDurationsSq[i] = 0
        }
        for (i in spikeDurations.indices) {
            spikes[i]?.clear(0)
        }
    }

    init {
        Muffin.getInstance().eventManager.addEventListener(this)

        for (i in spikeDurations.indices) {
            spikes[i] = ActionFrequency(3, 1000L * 60L * 20L)
        }
    }

}