package me.han.muffin.client.imixin.netty

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.Packet

interface INetworkManager {

    fun invokeFlushOutboundQueue()
    fun invokeDispatchPacket(packet: Packet<*>, futureListeners: Array<GenericFutureListener<out Future<in Void?>?>>?)

}