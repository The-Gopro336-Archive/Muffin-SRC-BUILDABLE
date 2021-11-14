package me.han.muffin.client.mixin.mixins.misc;

import me.han.muffin.client.imixin.IItemTool;
import net.minecraft.item.ItemTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemTool.class)
public abstract class MixinItemTool implements IItemTool {
    @Override
    @Accessor(value = "attackDamage")
    public abstract float getAttackDamage();
}