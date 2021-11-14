package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = CPacketPlayerTryUseItemOnBlock.class)
public abstract class MixinCPacketPlayerTryUseItemOnBlock implements ICPacketPlayerTryUseItemOnBlock {

    @Nonnull
    @Override
    @Accessor(value = "placedBlockDirection")
    public abstract EnumFacing getPlacedBlockDirection();

    @Override
    @Accessor(value = "placedBlockDirection")
    public abstract void setPlacedBlockDirection(@Nonnull EnumFacing facing);

}