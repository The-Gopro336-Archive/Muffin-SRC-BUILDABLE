package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.RenderArmorLayerEvent;
import me.han.muffin.client.event.events.render.item.RenderEnchantedEvent;
import me.han.muffin.client.module.modules.render.CustomEnchantModule;
import me.han.muffin.client.utils.render.GlStateUtils;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

@Mixin(value = LayerArmorBase.class)
public abstract class MixinLayerArmorBase {

    private static final FloatBuffer color = GLAllocation.createDirectFloatBuffer(16);
    private static boolean texture2d = false;
    private static boolean colorLock = false;

    @Redirect(method = "renderEnchantedGlint", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/GlStateManager.color(FFFF)V", ordinal = 1))
    private static void onRenderEnchantedGlintColour(final float red, final float green, final float blue, final float alpha) {
        GlStateManager.color(
                CustomEnchantModule.INSTANCE.isEnabled() ? CustomEnchantModule.INSTANCE.getColour().getRed() : red,
                CustomEnchantModule.INSTANCE.isEnabled() ? CustomEnchantModule.INSTANCE.getColour().getGreen() : green,
                CustomEnchantModule.INSTANCE.isEnabled() ? CustomEnchantModule.INSTANCE.getColour().getBlue() : blue,
                CustomEnchantModule.INSTANCE.isEnabled() ? CustomEnchantModule.INSTANCE.getColour().getAlpha() : alpha);
    }

    @Inject(method = "renderArmorLayer", at = @At("HEAD"), cancellable = true)
    public void onRenderArmorLayerPre(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, EntityEquipmentSlot slotIn, CallbackInfo ci) {
        RenderArmorLayerEvent event = new RenderArmorLayerEvent(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale, slotIn);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();

        if (!ci.isCancelled()) {
            colorLock = GlStateUtils.getColorLock();
            texture2d = glGetBoolean(GL_TEXTURE_2D);
            if (colorLock) {
                glGetFloat(GL_CURRENT_COLOR, color);
                GlStateUtils.colorLock(false);
            }
            if (!texture2d) {
                GlStateManager.enableTexture2D();
            }
        }
    }

    @Inject(method = "renderArmorLayer", at = @At("RETURN"))
    public void onRenderArmorLayerPost(EntityLivingBase entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, EntityEquipmentSlot slotIn, CallbackInfo ci) {
        if (colorLock) {
            GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
            GlStateUtils.colorLock(true);
        }
        if (!texture2d) {
            GlStateManager.disableTexture2D();
        }
    }

    @Inject(method = "renderEnchantedGlint", at = @At("HEAD"), cancellable = true)
    private static void onRenderEnchantedGlintPre(RenderLivingBase<?> p_188364_0_, EntityLivingBase p_188364_1_, ModelBase model, float p_188364_3_, float p_188364_4_, float p_188364_5_, float p_188364_6_, float p_188364_7_, float p_188364_8_, float p_188364_9_, CallbackInfo ci) {
        final RenderEnchantedEvent event = new RenderEnchantedEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }

}
