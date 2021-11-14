package me.han.muffin.client.imixin.netty.packet.client

import net.minecraft.util.EnumFacing

interface ICPacketPlayerTryUseItemOnBlock {
    var placedBlockDirection: EnumFacing
}