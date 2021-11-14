package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketCustomPayLoad;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = CPacketCustomPayload.class)
public abstract class MixinCPacketCustomPayLoad implements ICPacketCustomPayLoad {
    @Override
    @Accessor(value = "data")
    public abstract void setData(@Nonnull PacketBuffer data);
}