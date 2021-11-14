package me.han.muffin.client.mixin.mixins.misc;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.entity.OnItemUsePassEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Item.class)
public abstract class MixinItem {

    @Inject(method = "onItemUse", at = @At(value = "RETURN"), cancellable = true)
    public void onItemUsePre(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ, CallbackInfoReturnable<EnumActionResult> cir) {
        Item item = Globals.mc.player.inventory.getCurrentItem().getItem();
        if (cir.getReturnValue() == EnumActionResult.PASS && !(item instanceof ItemBow) && (item instanceof ItemTool || item == Items.AIR || item == Items.TOTEM_OF_UNDYING || item instanceof ItemArrow || item instanceof ItemCompass || item instanceof ItemWrittenBook || item instanceof ItemGlassBottle || item instanceof ItemSaddle || item instanceof ItemSword)) {
            Muffin.getInstance().getEventManager().dispatchEvent(new OnItemUsePassEvent(cir));
        }
    }

}