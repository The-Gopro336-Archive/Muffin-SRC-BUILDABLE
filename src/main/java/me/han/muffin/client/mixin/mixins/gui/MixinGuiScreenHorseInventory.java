package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.imixin.gui.IGuiScreenHorseInventory;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(value = GuiScreenHorseInventory.class)
public abstract class MixinGuiScreenHorseInventory implements IGuiScreenHorseInventory {

    @Nonnull
    @Override
    @Accessor(value = "horseInventory")
    public abstract IInventory getHorseInventory();

}