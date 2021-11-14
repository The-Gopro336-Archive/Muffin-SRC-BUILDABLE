package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketUseEntity;
import net.minecraft.network.play.client.CPacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = CPacketUseEntity.class)
public abstract class MixinCPacketUseEntity implements ICPacketUseEntity {

    @Override
    @Accessor(value = "entityId")
    public abstract int getEntityID();

    @Override
    @Accessor(value = "entityId")
    public abstract void setEntityID(int id);

    @Override
    @Accessor(value = "action")
    public abstract void setAction(@Nonnull CPacketUseEntity.Action action);

}