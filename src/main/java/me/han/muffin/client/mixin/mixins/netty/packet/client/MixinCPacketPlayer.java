package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CPacketPlayer.class)
public abstract class MixinCPacketPlayer implements ICPacketPlayer {

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

    @Override
    @Accessor(value = "onGround")
    public abstract void setOnGround(boolean onGround);

    @Override
    @Accessor(value = "rotating")
    public abstract boolean getRotating();

    @Override
    @Accessor(value = "rotating")
    public abstract void setRotating(boolean rotating);

    @Override
    @Accessor(value = "moving")
    public abstract boolean getMoving();

    @Override
    @Accessor(value = "moving")
    public abstract void setMoving(boolean moving);

}