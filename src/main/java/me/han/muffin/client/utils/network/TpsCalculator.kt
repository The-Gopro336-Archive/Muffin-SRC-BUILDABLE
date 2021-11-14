package me.han.muffin.client.utils.network

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.utils.CircularArray
import net.minecraft.network.play.server.SPacketTimeUpdate
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object TpsCalculator {

    // Circular Buffer lasting ~60 seconds for tick storage
    private val tickRates = CircularArray.create(120, 20f)
    private var timeLastTimeUpdate: Long = 0

    val averageTick = tickRates.average()

    val tickRate: Float
        get() = tickRates.average().coerceIn(0.0f, 20.0f)

    val adjustTicks: Float get() = tickRates.average() - 20f
    val syncTicks: Float get() = 20.0F - tickRate
    val factor: Float get() = 20.0F / tickRate

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet !is SPacketTimeUpdate) return

        if (timeLastTimeUpdate != -1L) {
            val timeElapsed = (System.nanoTime() - timeLastTimeUpdate) / 1E9
            tickRates.add((20.0 / timeElapsed).coerceIn(0.0, 20.0).toFloat())
        }
        timeLastTimeUpdate = System.nanoTime()
    }

    @Listener
    private fun onConnect(event: ServerEvent.Connect) {
        reset()
    }

    private fun reset() {
        tickRates.reset()
        timeLastTimeUpdate = -1L
    }

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

}