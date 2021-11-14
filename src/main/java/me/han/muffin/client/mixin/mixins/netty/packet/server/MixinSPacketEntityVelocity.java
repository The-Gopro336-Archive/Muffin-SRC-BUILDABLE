package me.han.muffin.client.mixin.mixins.netty.packet.server;

import me.han.muffin.client.imixin.netty.packet.server.ISPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SPacketEntityVelocity.class)
public abstract class MixinSPacketEntityVelocity implements ISPacketEntityVelocity {
    @Override
    @Accessor(value = "motionX")
    public abstract void setMotionX(int motionX);

    @Override
    @Accessor(value = "motionY")
    public abstract void setMotionY(int motionY);

    @Override
    @Accessor(value = "motionZ")
    public abstract void setMotionZ(int motionZ);
}
