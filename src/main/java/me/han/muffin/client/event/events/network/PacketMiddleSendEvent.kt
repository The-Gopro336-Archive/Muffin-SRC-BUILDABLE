package me.han.muffin.client.event.events.network

import net.minecraft.network.Packet

data class PacketMiddleSendEvent(val packet: Packet<*>)