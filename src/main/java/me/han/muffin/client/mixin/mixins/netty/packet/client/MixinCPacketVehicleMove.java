package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketVehicleMove;
import net.minecraft.network.play.client.CPacketVehicleMove;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CPacketVehicleMove.class)
public abstract class MixinCPacketVehicleMove implements ICPacketVehicleMove {

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