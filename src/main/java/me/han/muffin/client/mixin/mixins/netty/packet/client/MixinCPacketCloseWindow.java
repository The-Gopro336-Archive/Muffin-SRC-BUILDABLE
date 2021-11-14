package me.han.muffin.client.mixin.mixins.netty.packet.client;

import me.han.muffin.client.imixin.netty.packet.client.ICPacketCloseWindow;
import net.minecraft.network.play.client.CPacketCloseWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CPacketCloseWindow.class)
public abstract class MixinCPacketCloseWindow implements ICPacketCloseWindow {

    @Override
    @Accessor(value = "windowId")
    public abstract int getWindowId();

    @Override
    @Accessor(value = "windowId")
    public abstract void setWindowId(int id);

}