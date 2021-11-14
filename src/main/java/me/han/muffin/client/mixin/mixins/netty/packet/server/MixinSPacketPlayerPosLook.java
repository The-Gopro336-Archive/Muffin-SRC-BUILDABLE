package me.han.muffin.client.mixin.mixins.netty.packet.server;

import me.han.muffin.client.imixin.netty.packet.server.ISPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SPacketPlayerPosLook.class)
public abstract class MixinSPacketPlayerPosLook implements ISPacketPlayerPosLook {

    @Override
    @Accessor(value = "x")
    public abstract void setX(double x);

    @Override
    @Accessor(value = "y")
    public abstract void setY(double y);

    @Override
    @Accessor(value = "z")
    public abstract void setZ(double z);

    @Override
    @Accessor(value = "yaw")
    public abstract void setYaw(float yaw);

    @Override
    @Accessor(value = "pitch")
    public abstract void setPitch(float pitch);
}
