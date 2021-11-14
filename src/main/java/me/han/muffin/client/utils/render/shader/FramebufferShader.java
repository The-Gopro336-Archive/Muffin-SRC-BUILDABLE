package me.han.muffin.client.utils.render.shader;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.imixin.render.entity.IEntityRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 * @author TheSlowly
 */
public abstract class FramebufferShader extends Shader {

    private static Framebuffer framebuffer;

    protected float red, green, blue, alpha = 1F;
    protected float radius = 2F;
    protected float quality = 1F;

    private boolean entityShadows;

    public FramebufferShader(final String fragmentShader) {
        super(fragmentShader);
    }

    public void startDraw(final float partialTicks) {
        GlStateManager.enableAlpha();

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        framebuffer = setupFrameBuffer(framebuffer);
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        entityShadows = Globals.mc.gameSettings.entityShadows;
        Globals.mc.gameSettings.entityShadows = false;
        ((IEntityRenderer) Globals.mc.entityRenderer).setupCameraTransformVoid(partialTicks, 0);
    }

    public void stopDraw(final float red, final float green, final float blue, final float alpha, final float radius, final float quality) {
        Globals.mc.gameSettings.entityShadows = entityShadows;
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Globals.mc.getFramebuffer().bindFramebuffer(true);

        this.red = red / 255F;
        this.green = green / 255F;
        this.blue = blue / 255F;
        this.alpha = alpha / 255F;
        this.radius = radius;
        this.quality = quality;

        Globals.mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();

        startShader();
        Globals.mc.entityRenderer.setupOverlayRendering();
        drawFramebuffer(framebuffer);
        stopShader();

        Globals.mc.entityRenderer.disableLightmap();

        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
    }

    /**
     * @param frameBuffer
     * @return frameBuffer
     * @author TheSlowly
     */
    public Framebuffer setupFrameBuffer(Framebuffer frameBuffer) {
        if (frameBuffer != null)
            frameBuffer.deleteFramebuffer();

        frameBuffer = new Framebuffer(Globals.mc.displayWidth, Globals.mc.displayHeight, true);

        return frameBuffer;
    }

    /**
     * @author TheSlowly
     */
    public void drawFramebuffer(final Framebuffer framebuffer) {
        final ScaledResolution scaledResolution = new ScaledResolution(Globals.mc);
        glBindTexture(GL_TEXTURE_2D, framebuffer.framebufferTexture);
        glBegin(GL_QUADS);
        glTexCoord2d(0, 1);
        glVertex2d(0, 0);
        glTexCoord2d(0, 0);
        glVertex2d(0, scaledResolution.getScaledHeight());
        glTexCoord2d(1, 0);
        glVertex2d(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        glTexCoord2d(1, 1);
        glVertex2d(scaledResolution.getScaledWidth(), 0);
        glEnd();
        glUseProgram(0);
    }

}
