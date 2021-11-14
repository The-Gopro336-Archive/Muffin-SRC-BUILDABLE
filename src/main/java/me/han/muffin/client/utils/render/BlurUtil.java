package me.han.muffin.client.utils.render;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.imixin.render.IShaderGroup;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class BlurUtil {

    private static ShaderGroup blurShader;
    private static Framebuffer buffer;
    private static int lastScale;
    private static int lastScaleWidth;
    private static int lastScaleHeight;
    private static final ResourceLocation BLUR_LOCATION = new ResourceLocation("shader/blur/blur.json");

    public static void initFboAndShader() {
        try {
            blurShader = new ShaderGroup(Globals.mc.getTextureManager(), Globals.mc.getResourceManager(), Globals.mc.getFramebuffer(), BLUR_LOCATION);
            blurShader.createBindFramebuffers(Globals.mc.displayWidth, Globals.mc.displayHeight);
            buffer = ((IShaderGroup) blurShader).getMainFramebuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setShaderConfigs(float intensity, float blurWidth, float blurHeight) {
        ((IShaderGroup) blurShader).getShaders().get(0).getShaderManager().getShaderUniform("Radius").set(intensity);
        ((IShaderGroup) blurShader).getShaders().get(1).getShaderManager().getShaderUniform("Radius").set(intensity);

        ((IShaderGroup) blurShader).getShaders().get(0).getShaderManager().getShaderUniform("BlurDir").set(blurWidth, blurHeight);
        ((IShaderGroup) blurShader).getShaders().get(1).getShaderManager().getShaderUniform("BlurDir").set(blurHeight, blurWidth);
    }

    public static void blurArea(int x, int y, int width, int height, float intensity, float blurWidth, float blurHeight) {
        ScaledResolution sr = new ScaledResolution(Globals.mc);

        int factor = sr.getScaleFactor();
        int factor2 = sr.getScaledWidth();
        int factor3 = sr.getScaledHeight();

        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null || blurShader == null) {
            initFboAndShader();
        }
        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;

        if (OpenGlHelper.isFramebufferEnabled()) {

          //  buffer.framebufferClear();

            GL11.glScissor(
                    x * factor,
                    (Globals.mc.displayHeight - (y * factor) - height * factor),
                    width * factor,
                    height * factor - 12
            );

            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            setShaderConfigs(intensity, blurWidth, blurHeight);
            buffer.bindFramebuffer(true);
            blurShader.render(Globals.mc.getRenderPartialTicks());

            Globals.mc.getFramebuffer().bindFramebuffer(true);

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            GlStateUtils.blend(true);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
            buffer.framebufferRenderExt(Globals.mc.displayWidth, Globals.mc.displayHeight, false);
            GlStateUtils.blend(false);
            GL11.glScalef(factor, factor, 0);

        }
    }

    public static void blurArea(int x, int y, int width, int height, float intensity) {
        ScaledResolution scale = new ScaledResolution(Globals.mc);
        int factor = scale.getScaleFactor();
        int factor2 = scale.getScaledWidth();
        int factor3 = scale.getScaledHeight();
        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null
                || blurShader == null) {
            initFboAndShader();
        }
        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;


        buffer.framebufferClear();

        GL11.glScissor(x * factor, (Globals.mc.displayHeight - (y * factor) - height * factor), width * factor,
                (height) * factor);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        setShaderConfigs(intensity, 1, 0);
        buffer.bindFramebuffer(true);
        blurShader.render(Globals.mc.getRenderPartialTicks());

        Globals.mc.getFramebuffer().bindFramebuffer(true);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        buffer.framebufferRenderExt(Globals.mc.displayWidth, Globals.mc.displayHeight, false);
        GlStateManager.disableBlend();
        GL11.glScalef(factor, factor, 0);
        RenderHelper.enableGUIStandardItemLighting();

    }

    public static void blurAreaGey(int x, int y, int width, int height, float intensity) {
        ScaledResolution scale = new ScaledResolution(Globals.mc);
        int factor = scale.getScaleFactor();
        int factor2 = scale.getScaledWidth();
        int factor3 = scale.getScaledHeight();
        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null
                || blurShader == null) {
            initFboAndShader();
        }
        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;

        buffer.framebufferClear();

        GL11.glScissor(x * factor, (Globals.mc.displayHeight - (y * factor) - height * factor), width * factor,
                (height) * factor);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        setShaderConfigs(intensity, 1, 0);
        buffer.bindFramebuffer(true);
        blurShader.render(Globals.mc.getRenderPartialTicks());

        Globals.mc.getFramebuffer().bindFramebuffer(true);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        buffer.framebufferRenderExt(Globals.mc.displayWidth, Globals.mc.displayHeight, false);
        GlStateManager.disableBlend();
        GL11.glScalef(factor, factor, 0);
        RenderHelper.enableGUIStandardItemLighting();
    }

    public static void blurAreaBoarder(float x, float f, float width, float height, float intensity, float blurWidth, float blurHeight) {
        ScaledResolution scale = new ScaledResolution(Globals.mc);
        int factor = scale.getScaleFactor();
        int factor2 = scale.getScaledWidth();
        int factor3 = scale.getScaledHeight();
        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null
                || blurShader == null) {
            initFboAndShader();
        }
        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;

        GL11.glScissor((int)(x * factor), (int)((Globals.mc.displayHeight - (f * factor) - height * factor)) +1, (int)(width * factor),
                (int)(height * factor));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

			/*Stencil.write(false);
			Gui.drawFloatRect(x, f, x+width, f+height, -1);
			Stencil.erase(true);*/

        setShaderConfigs(intensity, 1, 0);
        buffer.bindFramebuffer(true);

        blurShader.render(Globals.mc.getRenderPartialTicks());

        Globals.mc.getFramebuffer().bindFramebuffer(true);

        //Stencil.dispose();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void blurShape(float g, float f, float h, float height, float intensity, float blurWidth, float blurHeight) {
        ScaledResolution scale = new ScaledResolution(Globals.mc);
        int factor = scale.getScaleFactor();
        int factor2 = scale.getScaledWidth();
        int factor3 = scale.getScaledHeight();
        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null
                || blurShader == null) {
            initFboAndShader();
        }
        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;

        GL11.glScissor((int)(g * factor), (int)((Globals.mc.displayHeight - (f * factor) - height * factor)) +1, (int)(h * factor),
                (int)(height * factor));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        setShaderConfigs(intensity, 1, 0);
        buffer.bindFramebuffer(true);

        blurShader.render(Globals.mc.getRenderPartialTicks());

        Globals.mc.getFramebuffer().bindFramebuffer(true);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        //GlStateManager.enableBlend();
    }

    public static void blurAreaBoarder(int x, int y, int width, int height, float intensity, float blurWidth, float blurHeight) {
        ScaledResolution sr = new ScaledResolution(Globals.mc);
        int factor = sr.getScaleFactor();
        int factor2 = sr.getScaledWidth();
        int factor3 = sr.getScaledHeight();

        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null || blurShader == null) {
            initFboAndShader();
        }

        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;

        if (OpenGlHelper.isFramebufferEnabled()) {

            GL11.glScissor(x * factor,
                    (Globals.mc.displayHeight - (y * factor) - height * factor),
                    width * factor,
                    height * factor);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            setShaderConfigs(intensity, blurWidth, blurHeight);

            buffer.bindFramebuffer(true);

            blurShader.render(Globals.mc.getRenderPartialTicks());

            Globals.mc.getFramebuffer().bindFramebuffer(true);

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            GlStateUtils.matrix(true);
            GlStateUtils.blend(true);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);

            buffer.framebufferRenderExt(Globals.mc.displayWidth, Globals.mc.displayHeight, false);

            GlStateUtils.blend(false);
            GL11.glScalef(factor, factor, 0);
            RenderHelper.enableGUIStandardItemLighting();
            GlStateUtils.matrix(false);

        }

    }

    public static void blurAll(float intensity) {
        ScaledResolution scale = new ScaledResolution(Globals.mc);
        int factor = scale.getScaleFactor();
        int factor2 = scale.getScaledWidth();
        int factor3 = scale.getScaledHeight();
        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null || blurShader == null) {
            initFboAndShader();
        }
        lastScale = factor;
        lastScaleWidth = factor2;
        lastScaleHeight = factor3;

        setShaderConfigs(intensity, 1, 0);
        buffer.bindFramebuffer(true);
        blurShader.render(Globals.mc.getRenderPartialTicks());

        Globals.mc.getFramebuffer().bindFramebuffer(true);

    }

}