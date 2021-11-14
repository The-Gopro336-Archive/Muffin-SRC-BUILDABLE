package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.render.*;
import me.han.muffin.client.event.events.world.block.BlockBreakEvent;
import me.han.muffin.client.imixin.render.IRenderGlobal;
import me.han.muffin.client.imixin.render.IViewFrustum;
import me.han.muffin.client.module.modules.player.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.Map;

@Mixin(value = RenderGlobal.class)
public abstract class MixinRenderGlobal implements IRenderGlobal {

    @Shadow private double frustumUpdatePosX;
    @Shadow private double frustumUpdatePosY;
    @Shadow private double frustumUpdatePosZ;

    @Shadow private int frustumUpdatePosChunkX;
    @Shadow private int frustumUpdatePosChunkY;
    @Shadow private int frustumUpdatePosChunkZ;

    @Shadow private int renderDistanceChunks;
    @Shadow private ViewFrustum viewFrustum;

    @Shadow protected abstract void fixTerrainFrustum(double x, double y, double z);

    @Nonnull
    @Override
    @Accessor(value = "damagedBlocks")
    public abstract Map<Integer, DestroyBlockProgress> getDamagedBlocks();

    /*
    private double _frustumUpdatePosX = Double.MIN_VALUE;
    private double _frustumUpdatePosY = Double.MIN_VALUE;
    private double _frustumUpdatePosZ = Double.MIN_VALUE;
    private int _frustumUpdatePosChunkX = Integer.MIN_VALUE;
    private int _frustumUpdatePosChunkY = Integer.MIN_VALUE;
    private int _frustumUpdatePosChunkZ = Integer.MIN_VALUE;

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V"))
    private void checkFreecamChunkUpdates(ViewFrustum viewFrustum, double viewEntityX, double viewEntityZ) {
        if (FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.getLoadUnloaded().getValue()) {
            Entity viewEntity = FreecamModule.INSTANCE.getCameraGuy() == null ? Globals.mc.player : FreecamModule.INSTANCE.getCameraGuy();
            double d0 = viewEntity.posX - this._frustumUpdatePosX;
            double d1 = viewEntity.posY - this._frustumUpdatePosY;
            double d2 = viewEntity.posZ - this._frustumUpdatePosZ;
            if (this._frustumUpdatePosChunkX != viewEntity.chunkCoordX || this._frustumUpdatePosChunkY != viewEntity.chunkCoordY || this._frustumUpdatePosChunkZ != viewEntity.chunkCoordZ || d0 * d0 + d1 * d1 + d2 * d2 > 16.0) {
                this._frustumUpdatePosX = viewEntity.posX;
                this._frustumUpdatePosY = viewEntity.posY;
                this._frustumUpdatePosZ = viewEntity.posZ;
                this._frustumUpdatePosChunkX = viewEntity.chunkCoordX;
                this._frustumUpdatePosChunkY = viewEntity.chunkCoordY;
                this._frustumUpdatePosChunkZ = viewEntity.chunkCoordZ;
                viewFrustum.updateChunkPositions(viewEntity.posX, viewEntity.posZ);
            }
        } else {
            this._frustumUpdatePosX = this.frustumUpdatePosX;
            this._frustumUpdatePosY = this.frustumUpdatePosY;
            this._frustumUpdatePosZ = this.frustumUpdatePosZ;
            this._frustumUpdatePosChunkX = this.frustumUpdatePosChunkX;
            this._frustumUpdatePosChunkY = this.frustumUpdatePosChunkY;
            this._frustumUpdatePosChunkZ = this.frustumUpdatePosChunkZ;
            viewFrustum.updateChunkPositions(viewEntityX, viewEntityZ);
        }
    }
     */

    // Can't use @ModifyVariable here because it crashes outside of a dev env with Optifine
    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;getRenderChunkOffset(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/chunk/RenderChunk;Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/client/renderer/chunk/RenderChunk;"))
    public RenderChunk onSettingUpChunkOffsetTerrain(RenderGlobal renderGlobal, BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            playerPos = new BlockPos(MathHelper.floor(Globals.mc.player.posX / 16.0D) * 16,
                    MathHelper.floor(Globals.mc.player.posY / 16.0D) * 16,
                    MathHelper.floor(Globals.mc.player.posZ / 16.0D) * 16);
        }

        // Can't use a @Shadow of getRenderChunkOffset because it crashes outside of a dev env with Optifine
        BlockPos blockpos = renderChunkBase.getBlockPosOffset16(facing);

        if (MathHelper.abs(playerPos.getX() - blockpos.getX()) > this.renderDistanceChunks * 16) {
            return null;
        } else if (blockpos.getY() >= 0 && blockpos.getY() < 256) {
            return MathHelper.abs(playerPos.getZ() - blockpos.getZ()) > this.renderDistanceChunks * 16 ? null : ((IViewFrustum) this.viewFrustum).getRenderChunkVoid(blockpos);
        } else {
            return null;
        }
    }

    /*
     * updateChunkPositions loadRenderers as well, but as long as you don't change your renderDistance in Freecam loadRenderers won't be called
     * One could add the same redirect for loadRenderers if needed
     */
    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V"))
    public void onUpdateSetupTerrainChunk(ViewFrustum viewFrustum, double viewEntityX, double viewEntityZ) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            viewEntityX = Globals.mc.player.posX;
            viewEntityZ = Globals.mc.player.posZ;
        }
        viewFrustum.updateChunkPositions(viewEntityX, viewEntityZ);
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;preRenderDamagedBlocks()V", shift = At.Shift.BEFORE))
    public void onRenderEntities(Entity entity, ICamera camera, float partialTicks, CallbackInfo callbackInfo) {
        final OutlineEvent event = new OutlineEvent(entity, camera, partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderSkyPre(float partialTicks, int pass, CallbackInfo ci) {
        RenderSkyEvent event = new RenderSkyEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "isRenderEntityOutlines", at = @At("HEAD"), cancellable = true)
    private void onIsRenderEntityOutlinesPre(CallbackInfoReturnable<Boolean> cir) {
        final RenderEntityOutlinesEvent event = new RenderEntityOutlinesEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "drawBlockDamageTexture", at = @At("HEAD"), cancellable = true)
    private void onDrawBlockDamageTexturePre(Tessellator tessellatorIn, BufferBuilder bufferBuilderIn, Entity entityIn, float partialTicks, CallbackInfo ci) {
        final RenderBlockDamageEvent event = new RenderBlockDamageEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    private RenderBlockLayerEvent event;

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderBlockLayerPre(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn, CallbackInfoReturnable<Integer> cir) {
        event = new RenderBlockLayerEvent(EventStageable.EventStage.PRE, blockLayerIn, partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.cancel();
        }
    }

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", at = @At(value = "RETURN"), cancellable = true)
    private void onRenderBlockLayerPost(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn, CallbackInfoReturnable<Integer> ci) {
        RenderBlockLayerEvent event = new RenderBlockLayerEvent(EventStageable.EventStage.POST, blockLayerIn, partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Redirect(method = "loadRenderers", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
    private Entity onGetRenderOrFreecamEntity(Minecraft mc) {
        return FreecamModule.INSTANCE.isEnabled() ? FreecamModule.INSTANCE.getCameraGuy() == null ? mc.player : FreecamModule.INSTANCE.getCameraGuy() : mc.getRenderViewEntity();
    }

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderBlockLayerPre(BlockRenderLayer blockLayerIn, CallbackInfo ci) {
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "sendBlockBreakProgress", at = @At("HEAD"))
    public void onSendingBlockBreakProgressPre(int breakerId, BlockPos pos, int progress, CallbackInfo ci) {
        BlockBreakEvent event = new BlockBreakEvent(breakerId, pos, progress);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "drawSelectionBox", at = @At("HEAD"), cancellable = true)
    public void onDrawSelectionBoxPre(EntityPlayer player, RayTraceResult result, int execute, float partialTicks, CallbackInfo ci) {
        DrawSelectionBoxEvent event = new DrawSelectionBoxEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @ModifyVariable(method = "setupTerrain", at = @At(value = "HEAD"))
    private boolean onSetupTerrainPre(boolean oldValue) {
        ShouldSetupTerrainEvent event = new ShouldSetupTerrainEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) return true;
        return oldValue;
    }

}