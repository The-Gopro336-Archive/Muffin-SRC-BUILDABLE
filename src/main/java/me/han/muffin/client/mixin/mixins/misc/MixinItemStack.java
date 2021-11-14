package me.han.muffin.client.mixin.mixins.misc;

import me.han.muffin.client.imixin.IItemStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemStack.class)
public abstract class MixinItemStack implements IItemStack {

    @Override
    @Accessor(value = "stackSize")
    public abstract int getStackSize();

    @Override
    @Accessor(value = "stackSize")
    public abstract void setStackSize(int size);

}