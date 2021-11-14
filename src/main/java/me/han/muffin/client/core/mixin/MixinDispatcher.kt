package me.han.muffin.client.core.mixin

import io.netty.channel.ChannelHandlerContext
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.ClientChatReceiveEvent
import me.han.muffin.client.event.events.entity.TotemPopEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.world.ChunkEvent
import me.han.muffin.client.event.events.world.ClientItemSpawnEvent
import me.han.muffin.client.gui.menu.ShaderMenu
import me.han.muffin.client.module.modules.other.MainMenuModule
import net.minecraft.client.renderer.texture.ITextureObject
import net.minecraft.network.Packet
import net.minecraft.network.play.server.*
import net.minecraft.util.ResourceLocation
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo


object MixinDispatcher {
    fun shouldDisplayCustomMenu(): Boolean = MainMenuModule.custom.value
    val customMenu get() = ShaderMenu()



    /** begin of netty mixins **/

    /** begin of MixinNetworkManager **/

    /**
     * Dispatch packet sent event PRE
     * @see me.han.muffin.mixin.mixins.netty.MixinNetworkManager.onSendPacketPre
     */
    fun onPacketSentPre(packet: Packet<*>, ci: CallbackInfo) {
        val event = PacketEvent.Send(packet, EventStageable.EventStage.PRE)
        Muffin.getInstance().eventManager.dispatchEvent(event)
        if (event.isCanceled) ci.cancel()
    }

    /**
     * Dispatch packet sent event POST
     * @see me.han.muffin.mixin.mixins.netty.MixinNetworkManager.onSendPacketPost
     */
    fun onPacketSentPost(packet: Packet<*>, ci: CallbackInfo) {
        val event = PacketEvent.Send(packet, EventStageable.EventStage.POST)
        Muffin.getInstance().eventManager.dispatchEvent(event)
        if (event.isCanceled) ci.cancel()
    }

    /**
     * Dispatch packet receive event PRE
     * @see me.han.muffin.mixin.mixins.netty.MixinNetworkManager.onChannelReadPre
     */
    fun onPacketReceivePre(packet: Packet<*>, ci: CallbackInfo) {
        val event = PacketEvent.Receive(packet, EventStageable.EventStage.PRE)
        Muffin.getInstance().eventManager.dispatchEvent(event)
        if (event.isCanceled) ci.cancel()
    }

    /**
     * Dispatch packet receive event POST
     * @see me.han.muffin.mixin.mixins.netty.MixinNetworkManager.onChannelReadPost
     */
    fun onPacketReceivePost(context: ChannelHandlerContext, packet: Packet<*>) {
        if (context.channel().isOpen) {
            val event = PacketEvent.Receive(packet, EventStageable.EventStage.POST)
            Muffin.getInstance().eventManager.dispatchEvent(event)
        }
    }

    /** end of MixinNetworkManager **/

    /** begin of MixinNetHandlerPlayClient **/

    /**
     * Dispatch chunk load event
     * @see me.han.muffin.mixin.mixins.netty.MixinNetHandlerPlayClient.onHandleChunkData
     */
    fun onHandleChunkData(packetIn: SPacketChunkData) {
        val event = ChunkEvent(ChunkEvent.ChunkType.LOAD, Globals.mc.world.getChunk(packetIn.chunkX, packetIn.chunkZ))
        Muffin.getInstance().eventManager.dispatchEvent(event)
    }

    /**
     * Dispatch item spawn event
     * @see me.han.muffin.mixin.mixins.netty.MixinNetHandlerPlayClient.handleSpawnObject
     */
    fun onHandleSpawnObject(packetIn: SPacketSpawnObject) {
        val event = ClientItemSpawnEvent(packetIn.x.toInt(), packetIn.y.toInt(), packetIn.z.toInt())
        Muffin.getInstance().eventManager.dispatchEvent(event)
    }

    /**
     * Dispatch totem pops event
     * @see me.han.muffin.mixin.mixins.netty.MixinNetHandlerPlayClient.onTotemPop
     */
    fun dispatchTotemPop(status: SPacketEntityStatus) {
        val event = TotemPopEvent(status.getEntity(Globals.mc.world))
        Muffin.getInstance().eventManager.dispatchEvent(event)
    }

    /**
     * Dispatch totem pops event
     * @see me.han.muffin.mixin.mixins.netty.MixinNetHandlerPlayClient.onHandleChat
     */
    fun dispatchClientChat(packetIn: SPacketChat) {
        val event = ClientChatReceiveEvent(packetIn)
        Muffin.getInstance().eventManager.dispatchEvent(event)
    }

    /** end of MixinNetHandlerPlayClient **/

    /** end of netty mixins **/

}

interface IPatchedTextureManager {
    val textures: Map<ResourceLocation, ITextureObject>
}