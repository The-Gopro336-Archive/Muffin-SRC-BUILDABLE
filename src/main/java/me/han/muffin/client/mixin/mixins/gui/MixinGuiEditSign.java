package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.imixin.gui.IGuiEditSign;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = GuiEditSign.class)
public abstract class MixinGuiEditSign implements IGuiEditSign {

    @Nonnull
    @Override
    @Accessor(value = "tileSign")
    public abstract TileEntitySign getTileSign();

    @Override
    @Accessor(value = "editLine")
    public abstract int getEditLine();

}