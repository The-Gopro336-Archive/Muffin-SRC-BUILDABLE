package me.han.muffin.client.imixin.netty.packet.client

import net.minecraft.network.PacketBuffer

interface ICPacketCustomPayLoad {
    fun setData(data: PacketBuffer)
}