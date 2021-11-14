package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.imixin.gui.IGuiChat;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen implements IGuiChat {

    @Nonnull
    @Override
    @Accessor(value = "historyBuffer")
    public abstract String getHistoryBuffer();

    @Override
    @Accessor(value = "sentHistoryCursor")
    public abstract int getSentHistoryCursor();

    @Override
    @Accessor(value = "historyBuffer")
    public abstract void setHistoryBuffer(@Nonnull String buffer);

    @Override
    @Accessor(value = "sentHistoryCursor")
    public abstract void setSentHistoryCursor(int sentHistoryCursor);

}