package me.han.muffin.client.mixin.mixins.world;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.network.ServerEvent;
import me.han.muffin.client.event.events.world.WorldClientInitEvent;
import me.han.muffin.client.event.events.world.WorldPlaySoundEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = WorldClient.class)
public abstract class MixinWorldClient {
    @Shadow
    @Final
    private Minecraft mc;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInitWorldClientPost(NetHandlerPlayClient netHandler, WorldSettings settings, int dimension, EnumDifficulty difficulty, Profiler profilerIn, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new WorldClientInitEvent((WorldClient) (Object) this));
    }

    @Inject(method = "playSound(Lnet/minecraft/entity/player/EntityPlayer;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V", at = @At("HEAD"), cancellable = true)
    private void onWorldPlaySoundPre(@Nullable EntityPlayer player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch, CallbackInfo ci) {
        WorldPlaySoundEvent event = new WorldPlaySoundEvent(soundIn, volume, pitch);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "sendQuittingDisconnectingPacket", at = @At("HEAD"))
    private void onPreSendQuittingDisconnectingPacket(CallbackInfo ci) {
//        ServerData serverData = mc.getCurrentServerData();
//        if (serverData != null) {
//            SendDisconnectPacketEvent event = new SendDisconnectPacketEvent((WorldClient) (Object) this, serverData);
//            Muffin.getInstance().getEventManager().dispatchEvent(event);
//            if (event.isCanceled()) ci.cancel();
//        }
        Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Disconnect(EventStageable.EventStage.PRE, false, mc.getCurrentServerData()));
    }

}