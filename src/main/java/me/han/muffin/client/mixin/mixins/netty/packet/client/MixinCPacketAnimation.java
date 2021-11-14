package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketAnimation;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = CPacketAnimation.class)
public abstract class MixinCPacketAnimation implements ICPacketAnimation {

    @Override
    @Accessor(value = "hand")
    public abstract void setHand(@Nonnull EnumHand hand);

}