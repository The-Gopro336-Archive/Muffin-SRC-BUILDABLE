package me.han.muffin.client.mixin.mixins.block;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.world.block.BlockCollisionBoundingBoxEvent;
import me.han.muffin.client.module.modules.movement.VelocityModule;
import me.han.muffin.client.module.modules.player.InteractionTweaksModule;
import me.han.muffin.client.module.modules.render.WallHackModule;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = BlockLiquid.class)
public abstract class MixinBlockLiquid extends MixinBlock {

    @Inject(method = "modifyAcceleration", at = @At("HEAD"), cancellable = true)
    public void onModifyAccelerationPre(World worldIn, BlockPos pos, Entity entityIn, Vec3d motion, CallbackInfoReturnable<Vec3d> cir) {
        if (VelocityModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(motion);
            cir.cancel();
        }
    }

    @Inject(method = "canCollideCheck", at = @At("HEAD"), cancellable = true)
    public void onCanCollideCheckPre(IBlockState state, boolean hitIfLiquid, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionTweaksModule.isLiquidInteractEnabled()) cir.setReturnValue(true);
    }

    @Inject(method = "shouldSideBeRendered", at = @At(value = "HEAD"), cancellable = true)
    private void onShouldSideBeRenderedPre(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        if (WallHackModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(blockAccess.getBlockState(pos.offset(side)).getMaterial() != material);
        }
    }

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