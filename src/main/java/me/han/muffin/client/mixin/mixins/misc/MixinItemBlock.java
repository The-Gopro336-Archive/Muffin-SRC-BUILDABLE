package me.han.muffin.client.mixin.mixins.misc;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.world.OnItemPlaceEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemBlock.class)
public abstract class MixinItemBlock {

    /*
    @Shadow
    @Dynamic
    public abstract boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState);

    @Redirect(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemBlock;placeBlockAt(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;FFFLnet/minecraft/block/state/IBlockState;)Z"))
    @Dynamic
    private boolean onBlockPlaceAtPre(ItemBlock itemBlock, ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        OnItemPlaceEvent event = new OnItemPlaceEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return world.isRemote && event.isCanceled() || placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }


     */


//    @ModifyArg(method = "canPlaceBlockOnSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;mayPlace(Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/util/EnumFacing;Lnet/minecraft/entity/Entity;)Z"), index = 2)
//    public boolean doIgnoreCollision(boolean oldValue) {
//        return SelfFillModule.INSTANCE.isEnabled() || oldValue;
//    }


}