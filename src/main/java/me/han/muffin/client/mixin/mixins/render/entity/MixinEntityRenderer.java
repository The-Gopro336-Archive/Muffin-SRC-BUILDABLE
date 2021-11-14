package me.han.muffin.client.mixin.mixins.render.entity;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.entity.EntityTurnEvent;
import me.han.muffin.client.event.events.entity.MouseOverEntityEvent;
import me.han.muffin.client.event.events.render.*;
import me.han.muffin.client.event.events.render.entity.GetMouseOverPostEvent;
import me.han.muffin.client.event.events.render.entity.HurtCamEvent;
import me.han.muffin.client.module.modules.other.ItemRender;
import me.han.muffin.client.module.modules.player.InteractionTweaksModule;
import me.han.muffin.client.utils.color.Colour;
import me.han.muffin.client.utils.render.ProjectionUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Objects;

@Mixin(value = EntityRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityRenderer {
    @Final @Shadow private Minecraft mc;
    @Shadow private boolean lightmapUpdateNeeded;
    @Shadow protected abstract void applyBobbing(float partialTicks);

    @Shadow @Final private DynamicTexture lightmapTexture;
    @Shadow @Final private int[] lightmapColors;
    private boolean shouldIgnoreTrace;
    private float traceRange;

    OrientCameraEvent orientCameraEvent;

    @Inject(method = "orientCamera", at = @At(value = "HEAD"))
    public void onPreOrientCamera(float partialTicks, CallbackInfo ci) {
        OrientCameraPreEvent event = new OrientCameraPreEvent(false, 3.5F);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        shouldIgnoreTrace = event.getShouldIgnoreTrace();
        traceRange = event.getRange();
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "net/minecraft/client/multiplayer/WorldClient.rayTraceBlocks(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/RayTraceResult;"))
    private RayTraceResult onRaytraceBlocks(WorldClient worldClient, Vec3d start, Vec3d end) {
        return shouldIgnoreTrace ? null : this.mc.world.rayTraceBlocks(start, end);
    }

    @ModifyVariable(method = "orientCamera", ordinal = 3, at = @At(value = "STORE", ordinal = 0), require = 1)
    public double onChangeCameraDistance(final double n) {
        orientCameraEvent = new OrientCameraEvent(n);
        Muffin.getInstance().getEventManager().dispatchEvent(orientCameraEvent);
        return orientCameraEvent.getDistance();
    }

    @ModifyVariable(method = "orientCamera", ordinal = 7, at = @At(value = "STORE", ordinal = 0), require = 1)
    public double onCameraClip(final double n) {
        return orientCameraEvent != null ? orientCameraEvent.getDistance() : n;
    }

//    @ModifyArgs(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;viewport(IIII)V"))
//    private void onSetViewPort(Args args) {
//        if (ItemRender.isViewPortRatio()) {
//            final int width = (int) (ItemRender.INSTANCE.getAspectRatioWidth().getValue());
//            final int height = (int) (ItemRender.INSTANCE.getAspectRatioHeight().getValue());
//            args.set(2, width);
//            args.set(3, height);
//        }
//        if (ItemRender.isViewPortFov()) {
//            final int width = ItemRender.INSTANCE.getFovPortX().getValue() ;
//            final int height = ItemRender.INSTANCE.getFovPortY().getValue();
//            args.set(0, width);
//            args.set(1, height);
//        }
//    }
//
//    @ModifyArgs(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;viewport(IIII)V"))
//    private void onUwUABC(Args args) {
//        if (ItemRender.isViewPortRatio()) {
//            final int width = (int) (ItemRender.INSTANCE.getAspectRatioWidth().getValue());
//            final int height = (int) (ItemRender.INSTANCE.getAspectRatioHeight().getValue());
//            args.set(2, width);
//            args.set(3, height);
//        }
//        if (ItemRender.isViewPortFov()) {
//            final int width = ItemRender.INSTANCE.getFovPortX().getValue() ;
//            final int height = ItemRender.INSTANCE.getFovPortY().getValue();
//            args.set(0, width);
//            args.set(1, height);
//        }
//    }

//    @ModifyArgs(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;viewport(IIII)V"))
//    private void onSetViewPort(Args args) {
//        if (ItemRender.isViewPortRatio()) {
//            final int width = (int) (ItemRender.INSTANCE.getAspectRatio().getValue() * 100);
//            final int height = (int) (ItemRender.INSTANCE.getAspectRatio().getValue() * 50);
//            args.set(2, width);
//            args.set(3, height);
//        }
//        if (ItemRender.isViewPortFov()) {
//            final int width = ItemRender.INSTANCE.getFovPort().getValue() ;
//            final int height = ItemRender.INSTANCE.getFovPort().getValue();
//            args.set(0, width);
//            args.set(1, height);
//        }
//    }

    /*
    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", ordinal = 4))
    private void onApplyTranslate(float x, float y, float z) {
        GlStateManager.translate(x + ItemRender.INSTANCE.getTestX().getValue(), y + ItemRender.INSTANCE.getTestY().getValue(), z + ItemRender.INSTANCE.getTestZ().getValue());
    }
     */

    /*
    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;applyBobbing(F)V"))
    private void cancelFlyingBob(EntityRenderer entityRenderer, float partialTicks) {
        if (!mc.player.isElytraFlying() || !mc.player.movementInput.jump) {
            applyBobbing(partialTicks);
        }
    }

    @Redirect(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;applyBobbing(F)V"))
    private void cancelKnockBob(EntityRenderer entityRenderer, float partialTicks) {
        if (!mc.player.isElytraFlying() || !mc.player.movementInput.jump) {
            applyBobbing(partialTicks);
        }
    }
     */

    @Inject(method = "hurtCameraEffect", at = @At(value = "HEAD"), cancellable = true)
    public void onHurtCameraEffectPre(float ticks, CallbackInfo info) {
        final HurtCamEvent event = new HurtCamEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) info.cancel();
    }

    @Inject( method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void onRenderFogPre(int startCoords, float partialTicks, CallbackInfo ci) {
        final SetupFogEvent event = new SetupFogEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "displayItemActivation", at = @At(value = "HEAD"), cancellable = true)
    public void onDisplayItemActivationPre(ItemStack stack, CallbackInfo ci) {
        RenderTotemAnimationEvent event = new RenderTotemAnimationEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderItemActivation", at = @At(value = "HEAD"), cancellable = true)
    public void onRenderItemActivationPre(int p_190563_1_, int p_190563_2_, float p_190563_3_, CallbackInfo ci) {
        RenderTotemAnimationEvent event = new RenderTotemAnimationEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "updateLightmap", at = @At(value = "HEAD", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V"), require = 1)
    private void onUpdateLightmapWrapperTexture(float partialTicks, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new UpdateLightMapEvent());
    }

    @Inject(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPositionEyes(F)Lnet/minecraft/util/math/Vec3d;", shift = At.Shift.BEFORE), cancellable = true)
    public void onGetEntitiesInAABBExcluding(float partialTicks, CallbackInfo ci) {
        if (InteractionTweaksModule.shouldIgnoreHitBox()) {
            ci.cancel();
            Globals.mc.profiler.endSection();
        }
    }

//    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER), require = 1)
//    private void onRenderGameOverlay(float partialTicks, long nanoTime, CallbackInfo ci) {
//        Muffin.getInstance().getEventManager().dispatchEvent(new Render2DEvent(partialTicks, new ScaledResolution(this.mc)));
//        AWTFontRenderer.Companion.garbageCollectionTick();
//    }

/*
    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z"))
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new Render3DEvent(partialTicks));
    }
*/

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clear(I)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onRenderWorldPassClear(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (Display.isActive() || Display.isVisible()) {
            Globals.mc.profiler.startSection("muffinRender3D");
            ProjectionUtils.INSTANCE.updateMatrix();
            Muffin.getInstance().getEventManager().dispatchEvent(new Render3DEvent(partialTicks));
            Muffin.getInstance().getEventManager().dispatchEvent(new Render3DEventPost(partialTicks));
            Globals.mc.profiler.endSection();
        }
    }

    @Inject(method = "getFOVModifier", at = @At(value = "RETURN"), cancellable = true)
    public void onRenderFovPost(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        FovModifierEvent event = new FovModifierEvent(cir.getReturnValue());
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            cir.setReturnValue(event.getFov());
        }
    }

    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;", ordinal = 1))
    public AxisAlignedBB onPreventMousingOverRiddenEntityBB(Entity possiblyRidden) {
        if (possiblyRidden.isPassenger(Objects.requireNonNull(mc.getRenderViewEntity())))
            return new AxisAlignedBB(0, 0, 0, 0, 0, 0); else return possiblyRidden.getEntityBoundingBox();
    }

    @Inject(method = "updateFogColor", at = @At(value = "HEAD"), cancellable = true)
    public void onUpdateFogColorPre(float partialTicks, CallbackInfo ci) {
        final UpdateFogColorEvent event = new UpdateFogColorEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @ModifyArg(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V"))
    public boolean isPlayerSpectator(boolean value) {
        return Muffin.getInstance().getEventManager().dispatchEvent(new SetupTerrainEvent()).isCanceled();
    }

    @Inject(method = "renderHand", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderHandPre(float partialTicks, int pass, CallbackInfo ci) {
        RenderHandEvent event = new RenderHandEvent(partialTicks, pass);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    /*
    @Inject(method = "renderHand(FIZZZ)V", at = @At(value = "HEAD"), cancellable = true, remap = false)
    @Dynamic
    private void onRenderHandOptifine(float partialTicks, int pass, CallbackInfo ci) {
        RenderHandEvent event = new RenderHandEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }
     */

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/ActiveRenderInfo.getBlockStateAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;F)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState onSettingUpFogWhileInLiquid(World worldIn, Entity entityIn, float p_186703_2_) {
        final IBlockState iBlockState = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.world, entityIn, p_186703_2_);
        RenderLiquidVisionEvent event = new RenderLiquidVisionEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled() && (iBlockState.getMaterial() == Material.LAVA || iBlockState.getMaterial() == Material.WATER)) {
            return Blocks.AIR.getDefaultState();
        }
        return iBlockState;
    }

    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "net/minecraft/client/Minecraft.getRenderViewEntity()Lnet/minecraft/entity/Entity;", ordinal = 0))
    private Entity onRenderEntityMouseOver(Minecraft mc) {
        if (mc.getRenderViewEntity() == null) return mc.getRenderViewEntity();
        MouseOverEntityEvent event = new MouseOverEntityEvent(mc.getRenderViewEntity());
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.getEntity();
    }

    @Inject(method = "getMouseOver", at = @At(value = "RETURN"))
    public void onGetMouseOverPost(float partialTicks, CallbackInfo ci) {
        GetMouseOverPostEvent event = new GetMouseOverPostEvent(this.mc.objectMouseOver);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        this.mc.objectMouseOver = event.getResult();
    }

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"))
    private void onCameraUpdateTurnPlayerWrapper(EntityPlayerSP playerSP, float yaw, float pitch) {
        EntityTurnEvent.Pre preEvent = new EntityTurnEvent.Pre(playerSP, yaw, pitch);
        Muffin.getInstance().getEventManager().dispatchEvent(preEvent);
        if (!preEvent.isCanceled()) {
            preEvent.getEntity().turn(preEvent.getYaw(), preEvent.getPitch());
            EntityTurnEvent.Post postEvent = new EntityTurnEvent.Post(preEvent.getEntity(), preEvent.getYaw(), preEvent.getPitch());
            Muffin.getInstance().getEventManager().dispatchEvent(preEvent);
        }
    }

    @Inject(method = "updateLightmap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getSunBrightness(F)F"), cancellable = true)
    public void onUpdateLightmapColoursPre(float partialTicks, CallbackInfo ci) {
        if (ItemRender.isCustomWorldColour()) {
            final Colour colour = ItemRender.getCustomWorldColour();
            for (int i = 0; i < 256; ++i) lightmapColors[i] = colour.toHex();
            this.lightmapTexture.updateDynamicTexture();
            this.lightmapUpdateNeeded = false;
            this.mc.profiler.endSection();
            ci.cancel();
        }
    }


    /*
    @Redirect(
            method = "updateFogColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clearColor(FFFF)V", ordinal = 0)
    )
    public void onClearColorFog(float red, float green, float blue, float alpha) {
        RenderFogColorsEvent event = new RenderFogColorsEvent(red, green, blue, alpha);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        GlStateManager.clearColor(event.getRed(),event.getGreen(),event.getBlue(),event.getAlpha());
    }

    @ModifyVariable(
            method = "setupFog",
            at = @At(value = "STORE", ordinal = 0)
    )
    public float onFogDensity(float density) {
        RenderFogDensityEvent event = new RenderFogDensityEvent(density);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.getDensity();
    }


    @Redirect(
            method = "setupFog",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;setFogDensity(F)V")
    )
    public void onFogDensity(float density) {
        RenderFogDensityEvent event = new RenderFogDensityEvent(density);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        GlStateManager.setFogDensity(event.getDensity());
    }
     */

}