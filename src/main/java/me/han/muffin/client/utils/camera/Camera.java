package me.han.muffin.client.utils.camera;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.imixin.render.entity.IEntityRenderer;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.util.glu.Project;

import static org.lwjgl.opengl.GL11.GL_QUADS;

public class Camera {

    private Vec3d pos;

    private Vec3d prevPos;

    private float yaw;

    private float pitch;

    private boolean recording;

    private boolean valid;

    private boolean rendering;

    private boolean firstUpdate;

    private float farPlaneDistance;

    private Framebuffer frameBuffer;

    private final int WIDTH_RESOLUTION = 420;
    private final int HEIGHT_RESOLUTION = 420;

    private int frameCount;

    public Camera() {
        this.pos = new Vec3d(0, 0, 0);
        this.yaw = 0;
        this.pitch = 0;
        this.frameBuffer = new Framebuffer(WIDTH_RESOLUTION, HEIGHT_RESOLUTION, true);
    }

    public void render(float x, float y, float w, float h) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            GlStateManager.pushMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.enableColorMaterial();

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            frameBuffer.bindFramebufferTexture();

            final Tessellator tessellator = Tessellator.getInstance();
            final BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(x, h, 0).tex(0, 0).endVertex();
            bufferbuilder.pos(w, h, 0).tex(1, 0).endVertex();
            bufferbuilder.pos(w, y, 0).tex(1, 1).endVertex();
            bufferbuilder.pos(x, y, 0).tex(0, 1).endVertex();
            tessellator.draw();

            frameBuffer.unbindFramebufferTexture();

            GlStateManager.popMatrix();
        }
    }

    public void renderWorld(float partialTicks, long nano) {
        //mc.entityRenderer.updateLightMap(partialTicks);

        if (Globals.mc.getRenderViewEntity() == null) {
            Globals.mc.setRenderViewEntity(Globals.mc.player);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.5F);
        renderWorldPass(2, partialTicks, nano);
    }

    public void setupCameraTransform(float partialTicks) {
        this.farPlaneDistance = (float) (Globals.mc.gameSettings.renderDistanceChunks * 16);

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();

        Project.gluPerspective(90.0f, (float) Globals.mc.displayWidth / (float) Globals.mc.displayHeight, 0.05F, this.farPlaneDistance * MathHelper.SQRT_2);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();

        ((IEntityRenderer) Globals.mc.entityRenderer).orientCameraVoid(partialTicks);
    }

    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano) {
        if (Globals.mc.getRenderViewEntity() == null)
            return;

        RenderGlobal renderglobal = Globals.mc.renderGlobal;
        GlStateManager.enableCull();
        GlStateManager.viewport(0, 0, Globals.mc.displayWidth, Globals.mc.displayHeight);
        //this.updateFogColor(partialTicks);
        GlStateManager.clear(16640);
        setupCameraTransform(partialTicks);
        ActiveRenderInfo.updateRenderInfo(Globals.mc.getRenderViewEntity(), Globals.mc.gameSettings.thirdPersonView == 2);
        ClippingHelperImpl.getInstance();
        ICamera icamera = new Frustum();
        Entity entity = Globals.mc.getRenderViewEntity();
        double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
        double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
        double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
        icamera.setPosition(d0, d1, d2);

        GlStateManager.shadeModel(7425);
        Globals.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        renderglobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, Globals.mc.player.isSpectator());

        if (pass == 0 || pass == 2) {
            Globals.mc.renderGlobal.updateChunks(finishTimeNano);
        }

        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlpha();
        renderglobal.renderBlockLayer(BlockRenderLayer.SOLID, partialTicks, pass, entity);
        GlStateManager.enableAlpha();
        Globals.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, Globals.mc.gameSettings.mipmapLevels > 0);
        renderglobal.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, partialTicks, pass, entity);
        Globals.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        Globals.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        renderglobal.renderBlockLayer(BlockRenderLayer.CUTOUT, partialTicks, pass, entity);
        Globals.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);

        //entities
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        RenderHelper.enableStandardItemLighting();
        ForgeHooksClient.setRenderPass(0);
        renderglobal.renderEntities(entity, icamera, partialTicks);
        ForgeHooksClient.setRenderPass(0);
        RenderHelper.disableStandardItemLighting();
        Globals.mc.entityRenderer.disableLightmap();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Globals.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getBuffer(), entity, partialTicks);
        Globals.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        GlStateManager.disableBlend();

        //particles
//        mc.entityRenderer.enableLightmap();
//        mc.effectRenderer.renderLitParticles(entity, partialTicks);
//        RenderHelper.disableStandardItemLighting();
//        mc.effectRenderer.renderParticles(entity, partialTicks);
//        mc.entityRenderer.disableLightmap();

        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        //this.renderRainSnow(partialTicks);
        GlStateManager.depthMask(true);
        //renderglobal.renderWorldBorder(entity, partialTicks);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        Globals.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.shadeModel(7425);
        renderglobal.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, partialTicks, pass, entity);

        //entities
        RenderHelper.enableStandardItemLighting();
        ForgeHooksClient.setRenderPass(1);
        renderglobal.renderEntities(entity, icamera, partialTicks);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        ForgeHooksClient.setRenderPass(-1);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();

        //Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventRender3D(mc.getRenderPartialTicks()));
        //Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventCameraRender3D(mc.getRenderPartialTicks()));
    }

    public void updateFbo() {
        if (!this.firstUpdate) {
            Globals.mc.renderGlobal.markBlockRangeForRenderUpdate(
                    (int) Globals.mc.player.posX - 256,
                    (int) Globals.mc.player.posY - 256,
                    (int) Globals.mc.player.posZ - 256,
                    (int) Globals.mc.player.posX + 256,
                    (int) Globals.mc.player.posY + 256,
                    (int) Globals.mc.player.posZ + 256);
            this.firstUpdate = true;
        }
        if (Globals.mc.player != null) {
            this.setPrevPos(new Vec3d(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ));
            double prevPosX = Globals.mc.player.prevPosX;
            double prevPosY = Globals.mc.player.prevPosY;
            double prevPosZ = Globals.mc.player.prevPosZ;
            double lastTickPosX = Globals.mc.player.lastTickPosX;
            double lastTickPosY = Globals.mc.player.lastTickPosY;
            double lastTickPosZ = Globals.mc.player.lastTickPosZ;

            float rotationYaw = Globals.mc.player.rotationYaw;
            float prevRotationYaw = Globals.mc.player.prevRotationYaw;
            float rotationPitch = Globals.mc.player.rotationPitch;
            float prevRotationPitch = Globals.mc.player.prevRotationPitch;
            boolean sprinting = Globals.mc.player.isSprinting();

            boolean hideGUI = Globals.mc.gameSettings.hideGUI;
            int clouds = Globals.mc.gameSettings.clouds;
            int thirdPersonView = Globals.mc.gameSettings.thirdPersonView;
            float gamma = Globals.mc.gameSettings.gammaSetting;
            int ambientOcclusion = Globals.mc.gameSettings.ambientOcclusion;
            boolean viewBobbing = Globals.mc.gameSettings.viewBobbing;
            int particles = Globals.mc.gameSettings.particleSetting;
            boolean shadows = Globals.mc.gameSettings.entityShadows;
            int frameLimit = Globals.mc.gameSettings.limitFramerate;
            float fovSetting = Globals.mc.gameSettings.fovSetting;

            int width = Globals.mc.displayWidth;
            int height = Globals.mc.displayHeight;

            Globals.mc.displayWidth = WIDTH_RESOLUTION;
            Globals.mc.displayHeight = HEIGHT_RESOLUTION;

            this.setCameraPos(this.getPos());
            this.setCameraAngle(this.yaw, this.pitch);

            Globals.mc.player.setSprinting(false);

            Globals.mc.gameSettings.hideGUI = true;
            Globals.mc.gameSettings.clouds = 0;
            Globals.mc.gameSettings.thirdPersonView = 0;
            Globals.mc.gameSettings.gammaSetting = 100;
            Globals.mc.gameSettings.ambientOcclusion = 0;
            Globals.mc.gameSettings.viewBobbing = false;
            Globals.mc.gameSettings.particleSetting = 0;
            Globals.mc.gameSettings.entityShadows = false;
            Globals.mc.gameSettings.limitFramerate = 10;
            Globals.mc.gameSettings.fovSetting = 90;

            this.setRecording(true);
            frameBuffer.bindFramebuffer(true);

            renderWorld(RenderUtils.getRenderPartialTicks(), System.nanoTime());
            //TODO force gui scale here?
            Globals.mc.entityRenderer.setupOverlayRendering();

            //final ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
            //Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventRender2D(mc.getRenderPartialTicks(), res));

            frameBuffer.unbindFramebuffer();
            this.setRecording(false);

            Globals.mc.player.posX = getPrevPos().x;
            Globals.mc.player.posY = getPrevPos().y;
            Globals.mc.player.posZ = getPrevPos().z;

            Globals.mc.player.prevPosX = prevPosX;
            Globals.mc.player.prevPosY = prevPosY;
            Globals.mc.player.prevPosZ = prevPosZ;

            Globals.mc.player.lastTickPosX = lastTickPosX;
            Globals.mc.player.lastTickPosY = lastTickPosY;
            Globals.mc.player.lastTickPosZ = lastTickPosZ;

            Globals.mc.player.rotationYaw = rotationYaw;
            Globals.mc.player.prevRotationYaw = prevRotationYaw;
            Globals.mc.player.rotationPitch = rotationPitch;
            Globals.mc.player.prevRotationPitch = prevRotationPitch;

            Globals.mc.player.setSprinting(sprinting);

            Globals.mc.gameSettings.hideGUI = hideGUI;
            Globals.mc.gameSettings.clouds = clouds;
            Globals.mc.gameSettings.thirdPersonView = thirdPersonView;
            Globals.mc.gameSettings.gammaSetting = gamma;
            Globals.mc.gameSettings.ambientOcclusion = ambientOcclusion;
            Globals.mc.gameSettings.viewBobbing = viewBobbing;
            Globals.mc.gameSettings.particleSetting = particles;
            Globals.mc.gameSettings.entityShadows = shadows;
            Globals.mc.gameSettings.limitFramerate = frameLimit;
            Globals.mc.gameSettings.fovSetting = fovSetting;

            Globals.mc.displayWidth = width;
            Globals.mc.displayHeight = height;

            this.setValid(true);
            this.setRendering(false);
        }
    }

    public void resize() {
        this.frameBuffer.createFramebuffer(WIDTH_RESOLUTION, HEIGHT_RESOLUTION);

        if (!isRecording() && isRendering()) {
            this.updateFbo();
        }
    }

    public void setCameraPos(Vec3d pos) {
        Globals.mc.player.posX = pos.x;
        Globals.mc.player.posY = pos.y;
        Globals.mc.player.posZ = pos.z;

        Globals.mc.player.prevPosX = pos.x;
        Globals.mc.player.prevPosY = pos.y;
        Globals.mc.player.prevPosZ = pos.z;

        Globals.mc.player.lastTickPosX = pos.x;
        Globals.mc.player.lastTickPosY = pos.y;
        Globals.mc.player.lastTickPosZ = pos.z;
    }

    public void setCameraAngle(float yaw, float pitch) {
        Globals.mc.player.rotationYaw = yaw;
        Globals.mc.player.prevRotationYaw = yaw;
        Globals.mc.player.rotationPitch = pitch;
        Globals.mc.player.prevRotationPitch = pitch;
    }

    public Framebuffer getFrameBuffer() {
        return frameBuffer;
    }

    public void setFrameBuffer(Framebuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
    }

    public Vec3d getPos() {
        return pos;
    }

    public void setPos(Vec3d pos) {
        this.pos = pos;
    }

    public Vec3d getPrevPos() {
        return prevPos;
    }

    public void setPrevPos(Vec3d prevPos) {
        this.prevPos = prevPos;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isRendering() {
        return rendering;
    }

    public void setRendering(boolean rendering) {
        this.rendering = rendering;
    }
}