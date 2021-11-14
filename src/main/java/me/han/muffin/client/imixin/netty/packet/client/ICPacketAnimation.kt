package me.han.muffin.client.imixin.netty.packet.client

import net.minecraft.util.EnumHand

interface ICPacketAnimation {
    fun setHand(hand: EnumHand)
}