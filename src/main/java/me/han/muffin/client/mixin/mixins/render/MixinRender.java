package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.entity.RenderEntityTeamColorEvent;
import me.han.muffin.client.module.modules.render.EntityESPModule;
import me.han.muffin.client.utils.render.GlStateUtils;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

@Mixin(value = Render.class)
public abstract class MixinRender<T extends Entity> {
    private static final FloatBuffer color = GLAllocation.createDirectFloatBuffer(16);
    private static boolean texture2d = false;
    private static boolean colorLock = false;

    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    public void onRenderLivingLabelPre(T entityIn, String str, double x, double y, double z, int maxDistance, final CallbackInfo ci) {
        if (!EntityESPModule.renderNameTags) ci.cancel();
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

    @Inject(method = "renderLivingLabel", at = @At("RETURN"))
    protected void onRenderLivingLabelPost(T entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (colorLock) {
            GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
            GlStateUtils.colorLock(true);
        }
        if (!texture2d) {
            GlStateManager.disableTexture2D();
        }
    }

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void onGetTeamColor(Entity entity, CallbackInfoReturnable<Integer> ciR, int color, ScorePlayerTeam scorePlayerTeam) {
        RenderEntityTeamColorEvent event = new RenderEntityTeamColorEvent(entity, color);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ciR.setReturnValue(event.getColor());
    }

    @Inject(method = "doRenderShadowAndFire", at = @At(value = "HEAD"), cancellable = true)
    private void onDoRenderShadowAndFirePre(CallbackInfo ci) {
        if (!EntityESPModule.renderNameTags) ci.cancel();
    }

}