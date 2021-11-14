package me.han.muffin.client.mixin.mixins.netty.packet.server;

import me.han.muffin.client.imixin.netty.packet.server.ISPacketCloseWindow;
import net.minecraft.network.play.server.SPacketCloseWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SPacketCloseWindow.class)
public abstract class MixinSPacketCloseWindow implements ISPacketCloseWindow {

    @Override
    @Accessor(value = "windowId")
    public abstract int getWindowId();

    @Override
    @Accessor(value = "windowId")
    public abstract void setWindowId(int id);

}