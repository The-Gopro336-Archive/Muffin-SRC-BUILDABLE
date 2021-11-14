package me.han.muffin.client.imixin.netty.packet.client

import net.minecraft.network.play.client.CPacketUseEntity

interface ICPacketUseEntity {

    var entityID: Int
    fun setAction(action: CPacketUseEntity.Action)

}