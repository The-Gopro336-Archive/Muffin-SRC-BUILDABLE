package me.han.muffin.client.mixin.mixins.block;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.world.block.BlockCollisionBoundingBoxEvent;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockAir.class)
public class MixinBlockAir {

    @Inject(method = "getCollisionBoundingBox", at = @At("HEAD"), cancellable = true)
    public void onGetCollisionBoundingBoxPre(IBlockState blockState, IBlockAccess worldIn, BlockPos pos, final CallbackInfoReturnable<AxisAlignedBB> cir) {
        BlockCollisionBoundingBoxEvent event = new BlockCollisionBoundingBoxEvent(pos);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(event.getBoundingBox());
            cir.cancel();
        }
    }

}
