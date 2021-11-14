package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.RenderArmsEvent;
import me.han.muffin.client.event.events.render.RenderHeldItemEvent;
import me.han.muffin.client.event.events.render.item.RenderCustomSwingAnimationEvent;
import me.han.muffin.client.event.events.render.item.RenderItemAnimationEvent;
import me.han.muffin.client.event.events.render.item.RenderItemInFirstPersonEvent;
import me.han.muffin.client.event.events.render.item.RenderUpdateEquippedItemEvent;
import me.han.muffin.client.event.events.render.overlay.RenderOverlayEvent;
import me.han.muffin.client.imixin.render.IItemRenderer;
import me.han.muffin.client.module.modules.other.ItemRender;
import me.han.muffin.client.module.modules.render.ChamsRewriteModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;

@Mixin(value = ItemRenderer.class, priority = Integer.MAX_VALUE)
public abstract class MixinItemRenderer implements IItemRenderer {

    @Shadow @Final private Minecraft mc;

    @Shadow public abstract void renderItemInFirstPerson(AbstractClientPlayer player, float p_187457_2_, float p_187457_3_, EnumHand hand, float p_187457_5_, ItemStack stack, float p_187457_7_);
    @Shadow protected abstract void renderArmFirstPerson(float p_187456_1_, float p_187456_2_, EnumHandSide p_187456_3_);

    @Nonnull
    @Override
    @Accessor(value = "itemStackMainHand")
    public abstract ItemStack getItemStackMainHand();

    @Override
    @Accessor(value = "itemStackMainHand")
    public abstract void setItemStackMainHand(@Nonnull ItemStack main);

    @Nonnull
    @Override
    @Accessor(value = "itemStackOffHand")
    public abstract ItemStack getItemStackOffHand();

    @Override
    @Accessor(value = "itemStackOffHand")
    public abstract void setItemStackOffHand(@Nonnull ItemStack off);

    @Override
    @Accessor(value = "equippedProgressMainHand")
    public abstract float getEquippedProgressMainHand();

    @Override
    @Accessor(value = "equippedProgressMainHand")
    public abstract void setEquippedProgressMainHand(float progress);

    @Override
    @Accessor(value = "equippedProgressOffHand")
    public abstract float getEquippedProgressOffHand();

    @Override
    @Accessor(value = "equippedProgressOffHand")
    public abstract void setEquippedProgressOffHand(float progress);

    @Override
    @Accessor(value = "prevEquippedProgressMainHand")
    public abstract float getPrevEquippedProgressMainHand();

    @Override
    @Accessor(value = "prevEquippedProgressMainHand")
    public abstract void setPrevEquippedProgressMainHand(float progress);

    @Override
    @Accessor(value = "prevEquippedProgressOffHand")
    public abstract float getPrevEquippedProgressOffHand();

    @Override
    @Accessor(value = "prevEquippedProgressOffHand")
    public abstract void setPrevEquippedProgressOffHand(float progress);

    @Inject(method = "renderWaterOverlayTexture", at = @At("HEAD"), cancellable = true)
    public void onPreRenderWaterOverlayTexture(float partialTicks, CallbackInfo ci) {
        final RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayEvent.OverlayType.LIQUID);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    public void onPreRenderFireInFirstPerson(CallbackInfo ci) {
        final RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayEvent.OverlayType.FIRE);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderSuffocationOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderSuffocationOverlayPre(TextureAtlasSprite sprite, CallbackInfo ci) {
        final RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayEvent.OverlayType.BLOCK);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", at = @At("HEAD"), cancellable = true)
    private void onRenderItemAnimationPre(AbstractClientPlayer player, float p_187457_2_, float p_187457_3_, EnumHand hand, float p_187457_5_, ItemStack stack, float p_187457_7_, CallbackInfo ci) {
        if (Muffin.getInstance().getEventManager().dispatchEvent(new RenderItemAnimationEvent.Render(stack, hand)).isCanceled())
            ci.cancel();
    }

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemSide(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V"))
    private void onRenderItemTransformAnimationPre(AbstractClientPlayer player, float p_187457_2_, float p_187457_3_, EnumHand hand, float p_187457_5_, ItemStack stack, float p_187457_7_, CallbackInfo info) {
        Muffin.getInstance().getEventManager().dispatchEvent(new RenderItemAnimationEvent.Transform(stack, hand, p_187457_5_));
    }

    @Inject(method = "transformFirstPerson", at = @At(value = "HEAD"), cancellable = true)
    private void onTransformFirstPersonPre(EnumHandSide hand, float v, CallbackInfo ci) {
        RenderCustomSwingAnimationEvent event = new RenderCustomSwingAnimationEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

    /*
    @Inject(method = "renderArmFirstPerson", at = @At(value = "HEAD"), cancellable = true)
    private void onTransformSideFirstPerson2(float v, float b, EnumHandSide hand, CallbackInfo ci) {
        RenderCustomSwingAnimationEvent event = new RenderCustomSwingAnimationEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

     */


    @Redirect(method = "renderItemInFirstPerson(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", ordinal = 0))
    private void onRenderItemInvokeMainhand(ItemRenderer renderer, AbstractClientPlayer player, float partialTicks, float p_187457_3_, EnumHand hand, float width, ItemStack stack, float height) {
        RenderItemInFirstPersonEvent.MainHand event = new RenderItemInFirstPersonEvent.MainHand(partialTicks, width, height);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            width = event.getWidth();
            height = event.getHeight();
        }
        renderItemInFirstPerson(player, partialTicks, p_187457_3_, hand, width, stack, height);
    }

    @Redirect(method = "renderItemInFirstPerson(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", ordinal = 1))
    private void onRenderItemInvokeOffhand(ItemRenderer renderer, AbstractClientPlayer player, float partialTicks, float p_187457_3_, EnumHand hand, float width, ItemStack stack, float height) {
        RenderItemInFirstPersonEvent.OffHand event = new RenderItemInFirstPersonEvent.OffHand(partialTicks, width, height);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            width = event.getWidth();
            height = event.getHeight();
        }
        renderItemInFirstPerson(player, partialTicks, p_187457_3_, hand, width, stack, height);
    }

    @Redirect(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderArmFirstPerson(FFLnet/minecraft/util/EnumHandSide;)V"))
    private void doRenderArms(ItemRenderer renderer, float height, float width, EnumHandSide hand) {
        RenderArmsEvent event = new RenderArmsEvent(renderer, hand);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        if (event.isCanceled()) {
            ChamsRewriteModule.INSTANCE.doHandChamsPre();
            renderArmFirstPerson(height, width, hand);
            ChamsRewriteModule.INSTANCE.doHandChamsPost();
            return;
        }

        renderArmFirstPerson(height, width, hand);
    }

    @Redirect(method = "transformSideFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"))
    private void onRedirectTranslationHeldItem(float x, float y, float z) {
        RenderHeldItemEvent event = new RenderHeldItemEvent(x, y, z);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) {
            x += event.getX();
            y += event.getY();
            z += event.getZ();
        }
        GlStateManager.translate(x, y, z);
    }

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemSide(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V"))
    private void onDoItemModelTransforms(AbstractClientPlayer player, float p_187457_2_, float p_187457_3_, EnumHand hand, float p_187457_5_, ItemStack stack, float p_187457_7_, CallbackInfo ci) {
        if (ItemRender.INSTANCE.isEnabled()) {
            boolean flag = hand == EnumHand.MAIN_HAND;
            EnumHandSide handSide = flag ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
            ItemRender.INSTANCE.doItemRender(handSide);
        }
    }

    @Inject(method = "transformSideFirstPerson", at = @At(value = "HEAD"))
    private void onTransformSideFirstPersonPre(EnumHandSide hand, float p_187459_2_, CallbackInfo ci) {
        if (ItemRender.INSTANCE.isEnabled()) {
            ItemRender.INSTANCE.doHandRender(hand);
        }
    }

//    @Redirect(method =  "*" , at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/ItemRenderer.renderArmFirstPerson(FFLnet/minecraft/util/EnumHandSide;)V"))
//    private void onRedirectingEveryRenderHand(final ItemRenderer itemRenderer, float p_187456_1_, float p_187456_2_, EnumHandSide handSide) {
//        final Function<RenderItemHeldHandEvent, ?> renderArmMethod = renderer -> {
//            renderArmFirstPerson(p_187456_1_, p_187456_2_, EnumHandSide.LEFT);
//            return null;
//        };
//
//        final RenderItemHeldHandEvent event = new RenderItemHeldHandEvent(this.mc.player, renderArmMethod, false);
//        Muffin.getInstance().getEventManager().dispatchEvent(event);
//
//        if (!event.isCanceled()) {
//            renderArmMethod.apply(event);
//        }
//    }

    /*
    @Redirect(method = "renderArmFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", ordinal = 0))
    public void onRenderHand3(float x, float y, float z) {
        RenderHandInFirstPersonEvent event = new RenderHandInFirstPersonEvent(x, y, z);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled())
            GlStateManager.translate(event.getX(), event.getY(), event.getZ());
    }
     */

    /*
    @Inject(method = "renderItemInFirstPerson(F)V", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderItem(float partialTicks, CallbackInfo ci) {
        RenderItemInFirstPersonEvent.OffHand event = new RenderItemInFirstPersonEvent.OffHand(partialTicks);
        Muffin.getInstance().getEventManager().dispatchEvent(event);

        if (event.isCanceled()) {
            ci.cancel();

            AbstractClientPlayer abstractclientplayer = this.mc.player;
            float f = abstractclientplayer.getSwingProgress(partialTicks);
            EnumHand enumhand = MoreObjects.firstNonNull(abstractclientplayer.swingingHand, EnumHand.MAIN_HAND);
            float f1 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
            float f2 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
            boolean flag = true;
            boolean flag1 = true;

            if (abstractclientplayer.isHandActive()) {
                ItemStack itemstack = abstractclientplayer.getActiveItemStack();

                if (itemstack.getItem() instanceof net.minecraft.item.ItemBow) {
                    EnumHand enumhand1 = abstractclientplayer.getActiveHand();
                    flag = enumhand1 == EnumHand.MAIN_HAND;
                    flag1 = !flag;
                }
            }

            this.rotateArroundXAndY(f1, f2);
            this.setLightmap();
            this.rotateArm(partialTicks);
            GlStateManager.enableRescaleNormal();

            if (flag) {
                float f3 = enumhand == EnumHand.MAIN_HAND ? f : 0.0F;
                float f5 = 1.0F - (this.prevEquippedProgressMainHand + (this.equippedProgressMainHand - this.prevEquippedProgressMainHand) * partialTicks);
                int obsidian = ItemUtil.findBlock(Blocks.OBSIDIAN);

                if (!net.minecraftforge.client.ForgeHooksClient.renderSpecificFirstPersonHand(EnumHand.MAIN_HAND, partialTicks, f1, f3, f5, this.itemStackMainHand))
                    if (((obsidian != -1 && mc.player.inventory.currentItem == obsidian) || mc.player.getHeldItemMainhand().getItem() instanceof ItemPickaxe || mc.player.getHeldItemMainhand().getItem() instanceof ItemAppleGold || mc.player.getHeldItemMainhand().getItem() instanceof ItemEndCrystal)) {
                        this.renderItemInFirstPerson(abstractclientplayer, partialTicks, f1, EnumHand.MAIN_HAND, f3, this.itemStackMainHand, 0);
                        mc.playerController.updateController();
                    } else {
                        this.renderItemInFirstPerson(abstractclientplayer, partialTicks, f1, EnumHand.MAIN_HAND, f3, this.itemStackMainHand, f5);
                    }
            }

            if (flag1) {
                float f4 = enumhand == EnumHand.OFF_HAND ? f : 0.0F;
                float f6 = 1.0F - (this.prevEquippedProgressOffHand + (this.equippedProgressOffHand - this.prevEquippedProgressOffHand) * partialTicks);
                if (!net.minecraftforge.client.ForgeHooksClient.renderSpecificFirstPersonHand(EnumHand.OFF_HAND, partialTicks, f1, f4, f6, this.itemStackOffHand))
                    if ((mc.player.getHeldItemOffhand().getItem() instanceof ItemEndCrystal || mc.player.getHeldItemOffhand().getItem() instanceof ItemAppleGold)) {
                        this.renderItemInFirstPerson(abstractclientplayer, partialTicks, f1, EnumHand.OFF_HAND, f4, this.itemStackOffHand, 0);
                        mc.playerController.updateController();
                    } else {
                        this.renderItemInFirstPerson(abstractclientplayer, partialTicks, f1, EnumHand.OFF_HAND, f4, this.itemStackOffHand, f6);
                    }
            }

            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
        }
    }
     */


/*
    @Inject(method = "renderArmFirstPerson", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderArmFirstPerson(float a, float b, EnumHandSide hand, CallbackInfo ci) {
        ci.cancel();

        boolean flag = hand != EnumHandSide.LEFT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = MathHelper.sqrt(b);
        float f2 = -0.3F * MathHelper.sin(f1 * (float)Math.PI);
        float f3 = 0.4F * MathHelper.sin(f1 * ((float)Math.PI * 2F));
        float f4 = -0.4F * MathHelper.sin(b * (float)Math.PI);
        GlStateManager.translate(f * (f2 + 0.64000005F), f3 + -0.6F + a * -0.6F, f4 + -0.71999997F);
        GlStateManager.rotate(f * 45.0F, 0.0F, 1.0F, 0.0F);
        float f5 = MathHelper.sin(b * b * (float)Math.PI);
        float f6 = MathHelper.sin(f1 * (float)Math.PI);
        GlStateManager.rotate(f * f6 * 70.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f * f5 * -20.0F, 0.0F, 0.0F, 1.0F);
        AbstractClientPlayer abstractclientplayer = this.mc.player;
        this.mc.getTextureManager().bindTexture(abstractclientplayer.getLocationSkin());
      //  GlStateManager.translate(f * -1.0F, 3.6F, 3.5F);
     //   GlStateManager.rotate(f * 120.0F, 0.0F, 0.0F, 1.0F);
     //   GlStateManager.rotate(200.0F, 1.0F, 0.0F, 0.0F);
     //   GlStateManager.rotate(f * -135.0F, 0.0F, 1.0F, 0.0F);
    //    GlStateManager.translate(f * 5.6F, 0.0F, 0.0F);
        RenderPlayer renderplayer = (RenderPlayer)renderManager.<AbstractClientPlayer>getEntityRenderObject(abstractclientplayer);
        GlStateManager.disableCull();

        if (flag) {
            renderplayer.renderRightArm(abstractclientplayer);
        }
        else {
            renderplayer.renderLeftArm(abstractclientplayer);
        }

        GlStateManager.enableCull();

    }
 */

    @Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true)
    public void updateEquippedItem(CallbackInfo ci) {
        RenderUpdateEquippedItemEvent event = new RenderUpdateEquippedItemEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

}
