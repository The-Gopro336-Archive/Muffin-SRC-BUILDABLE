package me.han.muffin.client.utils.network

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import net.minecraft.network.play.server.SPacketTimeUpdate
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

object LagCompensator {

    private val tickRates = FloatArray(100)
    private var index = 0
    private var timeLastTimeUpdate: Long = 0

    val tickRate: Float
        get() {
            var numTicks = 0.0f
            var sumTickRates = 0.0f
            for (tickRate in tickRates) {
                if (tickRate > 0.0F) {
                    sumTickRates += tickRate
                    numTicks += 1.0f
                }
            }
            val calcTickRate = (sumTickRates / numTicks).coerceIn(0.0F, 20.0F)
            return if (calcTickRate == 0.0f) 20.0f else calcTickRate
        }

    val adjustTicks: Float get() = tickRate - 20.0F
    val syncTicks: Float get() = 20.0F - tickRate
    val factor: Float get() = 20.0F / tickRate

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet !is SPacketTimeUpdate) return

        if (timeLastTimeUpdate != -1L) {
            val timeElapsed = (System.currentTimeMillis() - timeLastTimeUpdate).toFloat() / 1000.0F
            tickRates[index] = (20.0F / timeElapsed).coerceIn(0.0F..20.0F)
            index = (index + 1) % tickRates.size
        }

        timeLastTimeUpdate = System.currentTimeMillis()
    }

    @Listener
    private fun onConnect(event: ServerEvent.Connect) {
        reset()
    }

    private fun reset() {
        index = 0
        timeLastTimeUpdate = -1L
        Arrays.fill(tickRates, 0.0F)
    }

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

}