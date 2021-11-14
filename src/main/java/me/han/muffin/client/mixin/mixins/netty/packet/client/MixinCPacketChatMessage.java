package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketChatMessage;
import net.minecraft.network.play.client.CPacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = CPacketChatMessage.class)
public abstract class MixinCPacketChatMessage implements ICPacketChatMessage {

    @Override
    @Accessor(value = "message")
    public abstract void setMessage(@Nonnull String message);

}
