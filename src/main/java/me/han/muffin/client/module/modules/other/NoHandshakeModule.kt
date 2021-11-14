package me.han.muffin.client.module.modules.other

import io.netty.buffer.Unpooled
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mixin.netty.data
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

internal object NoHandshakeModule: Module("NoHandshake", Category.OTHERS, true, "Prevents forge from sending your mod list to the server while connecting.") {

    @Listener
    private fun onPacketSend(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is FMLProxyPacket && !Globals.mc.isSingleplayer) event.cancel()

        if (event.packet is CPacketCustomPayload) {
            if (event.packet.channelName == "MC|Brand") event.packet.data = PacketBuffer(Unpooled.buffer()).writeString("vanilla")
        }
    }

    private fun getRandomIpPart(): String {
        return Random().nextInt(2).toString() + "" + Random().nextInt(5) + "" + Random().nextInt(5)
    }

}