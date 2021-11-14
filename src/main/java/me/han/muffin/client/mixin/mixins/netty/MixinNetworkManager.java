package me.han.muffin.client.mixin.mixins.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.core.mixin.MixinDispatcher;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.network.PacketExceptionEvent;
import me.han.muffin.client.event.events.network.PacketMiddleSendEvent;
import me.han.muffin.client.event.events.network.ServerEvent;
import me.han.muffin.client.imixin.netty.INetworkManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(value = NetworkManager.class)
public abstract class MixinNetworkManager implements INetworkManager {

    @Shadow @Final private static Logger LOGGER;

    @Override
    @Invoker(value = "flushOutboundQueue")
    public abstract void invokeFlushOutboundQueue();

    @Override
    //@Invoker(value = "dispatchPacket")
    public abstract void invokeDispatchPacket(@Nonnull Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>>[] futureListeners);

    @Inject(method = "dispatchPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacketPre(final Packet<?> packet, @Nullable final GenericFutureListener<? extends Future<? super Void >>[] futureListeners, CallbackInfo ci) {
        MixinDispatcher.INSTANCE.onPacketSentPre(packet, ci);
    }

    @Inject(method = "dispatchPacket", at = @At("RETURN"), cancellable = true)
    private void onSendPacketPost(final Packet<?> packet, @Nullable final GenericFutureListener<? extends Future<? super Void >>[] futureListeners, CallbackInfo ci) {
        MixinDispatcher.INSTANCE.onPacketSentPost(packet, ci);
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;dispatchPacket(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onSendPacketMiddle(final Packet<?> packet, CallbackInfo ci) {
        PacketMiddleSendEvent event = new PacketMiddleSendEvent(packet);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "channelRead0", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Packet;processPacket(Lnet/minecraft/network/INetHandler;)V"), cancellable = true)
    private void onChannelReadPre(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        MixinDispatcher.INSTANCE.onPacketReceivePre(packet, ci);
    }

    @Inject(method = "channelRead0", at = @At("RETURN"), cancellable = true)
    private void onChannelReadPost(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        MixinDispatcher.INSTANCE.onPacketReceivePost(context, packet);
    }

    @Inject(method = "exceptionCaught", at = @At(value = "HEAD"), cancellable = true)
    private void onExceptionCaughtPre(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
        LOGGER.info("Exception caught in context " + context.name(), throwable);

        final PacketExceptionEvent event = new PacketExceptionEvent(throwable);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

//    @Inject(method = "closeChannel", at = @At(value = "HEAD"), cancellable = true)
//    private void asd(ITextComponent message, CallbackInfo ci) {
//        ServerData serverData = Globals.mc.getCurrentServerData();
//        if (serverData != null) {
//            SendDisconnectPacketEvent event = new SendDisconnectPacketEvent(Globals.mc.world, serverData);
//            Muffin.getInstance().getEventManager().dispatchEvent(event);
//            if (event.isCanceled()) ci.cancel();
//        }
//    }

    @Inject(method = "handleDisconnection", at = @At(value = "INVOKE_ASSIGN", target = "net/minecraft/network/INetHandler.onDisconnect(Lnet/minecraft/util/text/ITextComponent;)V"))
    private void onHandleDisconnectAssign(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Disconnect(EventStageable.EventStage.POST, true, Globals.mc.getCurrentServerData()));
    }

}