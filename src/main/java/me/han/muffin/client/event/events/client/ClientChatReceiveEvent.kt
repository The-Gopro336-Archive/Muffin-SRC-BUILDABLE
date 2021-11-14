package me.han.muffin.client.event.events.client

import net.minecraft.network.play.server.SPacketChat

data class ClientChatReceiveEvent(val packet: SPacketChat)