package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.event.events.network.OversizedProtocolEvent
import me.han.muffin.client.event.events.network.PacketExceptionEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.Value
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object PacketOptimizerModule: Module("PacketOptimizer", Category.MISC, "Stop an exception from being thrown a bad compressed packets.") {
    private val badPackets = Value(true, "BadPackets")
    private val chunkBan = Value(false, "ChunkBan")

    init {
        addSettings(badPackets, chunkBan)
    }

    @Listener
    private fun onPacketException(event: PacketExceptionEvent) {
        if (badPackets.value) event.cancel()
    }

    @Listener
    private fun onOversizeProtocol(event: OversizedProtocolEvent) {
        if (chunkBan.value) event.cancel()
    }

}