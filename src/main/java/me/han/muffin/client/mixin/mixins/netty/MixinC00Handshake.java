package me.han.muffin.client.mixin.mixins.netty;

import me.han.muffin.client.imixin.netty.IC00Handshake;
import net.minecraft.network.handshake.client.C00Handshake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = C00Handshake.class)
public abstract class MixinC00Handshake implements IC00Handshake {

    @Nonnull
    @Override
    @Accessor(value = "ip")
    public abstract String getIp();

    @Override
    @Accessor(value = "port")
    public abstract int getPort();

    @Override
    @Accessor(value = "ip")
    public abstract void setIp(@Nonnull String ip);

    @Override
    @Accessor(value = "port")
    public abstract void setPort(int port);

}