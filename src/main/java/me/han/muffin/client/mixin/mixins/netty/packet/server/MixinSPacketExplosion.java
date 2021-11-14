package me.han.muffin.client.mixin.mixins.netty.packet.server;

import me.han.muffin.client.imixin.netty.packet.server.ISPacketExplosion;
import net.minecraft.network.play.server.SPacketExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SPacketExplosion.class)
public abstract class MixinSPacketExplosion implements ISPacketExplosion {
    @Override
    @Accessor(value = "motionX")
    public abstract void setMotionX(float motionX);

    @Override
    @Accessor(value = "motionY")
    public abstract void setMotionY(float motionY);

    @Override
    @Accessor(value = "motionZ")
    public abstract void setMotionZ(float motionZ);
}
