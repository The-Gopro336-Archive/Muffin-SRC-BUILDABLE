package me.han.muffin.client.module.modules.misc

import io.netty.buffer.ByteBuf
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import net.minecraft.network.play.client.CPacketCustomPayload
import net.minecraft.network.play.server.SPacketCustomPayload
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

internal object PayloadSpoofModule : Module("PayloadSpoof", Category.MISC, "Cancel packets sent by some mods") {

    val IGNORE_LIST = hashSetOf(
        "WDL|INIT",
        "WDL|CONTROL",
        "WDL|REQUEST",

        "wdl:init",
        "wdl:control",
        "wdl:request",

        "schematica",
        "journeymap_channel",
        "jm_dim_permission",
        "jm_init_login"
    )


    private fun isBlockedPacket(channel: String, buffer: ByteBuf): Boolean {
        if (IGNORE_LIST.contains(channel)) {
            return true
        } else if ("REGISTER" == channel) {
            Scanner(String(buffer.array())).use {
                it.useDelimiter("\\u0000")
                if (it.hasNext()) {
                    val next = it.next()
                    return !next.isNullOrEmpty() && IGNORE_LIST.contains(next)
                }
            }
        }
        return false
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is SPacketCustomPayload || event.packet is FMLProxyPacket) {
            val channel: String
            val packetBuffer: ByteBuf

            if (event.packet is SPacketCustomPayload) {
                channel = event.packet.channelName
                packetBuffer = event.packet.bufferData
            } else {
                channel = (event.packet as FMLProxyPacket).channel()
                packetBuffer = event.packet.payload()
            }
            if (isBlockedPacket(channel, packetBuffer)) {
                event.cancel()
            }
        }

    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        val channel: String
        val packetBuffer: ByteBuf

        if (event.packet is CPacketCustomPayload || event.packet is FMLProxyPacket) {
            if (event.packet is CPacketCustomPayload) {
                channel = event.packet.channelName
                packetBuffer = event.packet.bufferData
            } else {
                channel = (event.packet as FMLProxyPacket).channel()
                packetBuffer = event.packet.payload()
            }
            if (isBlockedPacket(channel, packetBuffer)) {
                event.cancel()
            }
        }

    }

}