package me.han.muffin.client.utils.extensions.mixin.netty

import me.han.muffin.client.imixin.netty.INetHandlerPlayClient
import net.minecraft.client.network.NetHandlerPlayClient

val NetHandlerPlayClient.doneLoadingTerrain: Boolean
    get() = (this as INetHandlerPlayClient).doneLoadingTerrain