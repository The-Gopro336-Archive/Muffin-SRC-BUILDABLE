package me.han.muffin.client.mixin.mixins.block;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.world.CollisionBoxEvent;
import me.han.muffin.client.event.events.world.block.BlockCollidableEvent;
import me.han.muffin.client.event.events.world.block.BlockCollisionBoundingBoxEvent;
import me.han.muffin.client.event.events.world.block.CanPlaceBlockAtEvent;
import me.han.muffin.client.event.events.world.block.CanRenderInLayerEvent;
import me.han.muffin.client.module.modules.render.WallHackModule;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = Block.class)
public abstract class MixinBlock {
    @Shadow @Final protected Material material;
    private CollisionBoxEvent boxEvent;

    @Inject(method = "addCollisionBoxToList(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/entity/Entity;Z)V", at = @At("HEAD"), cancellable = true)
    @Deprecated
    private void onAddingCollisionPre(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entityIn, boolean isActualState, CallbackInfo ci) {
        if (Globals.mc.player == null || state == null || entityIn == null || worldIn == null || entityBox == null) return;
        Block block = (Block) (Object) this;
        boxEvent = new CollisionBoxEvent(block, pos, entityIn, state.getCollisionBoundingBox(worldIn, pos));
        Muffin.getInstance().getEventManager().dispatchEvent(boxEvent);
        if (boxEvent.getBoundingBox() == null) ci.cancel();

      //  if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getCameraGuy() != null && FreecamModule.INSTANCE.getCameraGuy() == entityIn) {
      //      ci.cancel();
      //  }
    }

    @Redirect(method = "addCollisionBoxToList(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/entity/Entity;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getCollisionBoundingBox(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/AxisAlignedBB;"))
    @Deprecated
    private AxisAlignedBB onGetBBPre(IBlockState state, IBlockAccess world, BlockPos pos) {
        return (boxEvent == null) ? state.getCollisionBoundingBox(world, pos) : boxEvent.getBoundingBox();
    }

    @Inject(method = "canRenderInLayer", at = @At(value = "RETURN"), cancellable = true, remap = false)
    @Dynamic
    public void onCanRenderInLayerPre(IBlockState state, BlockRenderLayer layer, CallbackInfoReturnable<Boolean> cir) {
        CanRenderInLayerEvent event = new CanRenderInLayerEvent((Block) (Object) this);
        if (Muffin.getInstance() != null) Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.getBlockRenderLayer() != null) cir.setReturnValue(event.getBlockRenderLayer() == layer);
    }

    @Inject(method = "getAmbientOcclusionLightValue", at =  @At(value = "HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightValuePre(CallbackInfoReturnable<Float> cir) {
        if (WallHackModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(1F);
        }
    }

    @Inject(method = "getLightValue(Lnet/minecraft/block/state/IBlockState;)I", at = @At("HEAD"), cancellable = true)
    public void onGetLightValuePre(CallbackInfoReturnable<Integer> cir) {
        if (WallHackModule.INSTANCE.isEnabled()) {
            WallHackModule.INSTANCE.processGetLightValue((Block) (Object) this, cir);
        }
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void onIsCollidablePre(CallbackInfoReturnable<Boolean> cir) {
        BlockCollidableEvent event = new BlockCollidableEvent((Block) (Object) this);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
        //    if (Blocks.OBSIDIAN != (Block) (Object) this && Blocks.BEDROCK != (Block) (Object) this)
            cir.setReturnValue(false);
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

    @Inject(method = "canPlaceBlockAt", at = @At(value = "HEAD"), cancellable = true)
    public void onCanPlaceBlockAtPre(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        CanPlaceBlockAtEvent event = new CanPlaceBlockAtEvent(world, pos, (Block) (Object) this);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}