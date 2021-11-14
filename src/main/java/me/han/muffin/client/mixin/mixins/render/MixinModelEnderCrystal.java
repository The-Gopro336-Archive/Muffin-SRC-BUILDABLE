package me.han.muffin.client.mixin.mixins.render;

import net.minecraft.client.model.ModelEnderCrystal;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ModelEnderCrystal.class)
public abstract class MixinModelEnderCrystal {
/*
    private static final int ANIMATION_LENGTH = 400;
    private static final double CUBELET_SCALE = 0.4;

    // Currently rotating side
    private int rotatingSide = 0;
    // front - 0
    // back - 1
    // top - 2
    // bottom - 3
    // left - 4
    // right - 5

    private long lastTime = 0;

    @Shadow private ModelRenderer base;

    @Shadow @Final private ModelRenderer cube;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", ordinal = 1))
    private void onDisableBounce(float x, float y, float z) {
        if (base != null) GlStateManager.translate(0.0F, 1.2F, 0.0F);
        else GlStateManager.translate(0.0F, 1.0F, 0.0F);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void onDo(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        GlStateManager.scale(0.875F, 0.875F, 0.875F);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onDo2(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        GlStateManager.rotate(limbSwingAmount, 0.0F, 1.0F, 0.0F);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;render(F)V", ordinal = 3))
    private void onDoModel(ModelRenderer modelRenderer, float scale) {
        GlStateManager.scale(CUBELET_SCALE, CUBELET_SCALE, CUBELET_SCALE);
        scale *= CUBELET_SCALE * 2;

        long currentTime = Minecraft.getSystemTime();
        if (currentTime-ANIMATION_LENGTH > lastTime){
            // rotate sides and corners
            int[] currentSide = CrystalRenderUtils.INSTANCE.getCubeSides()[rotatingSide];
            Quaternion[] cubletsTemp = {
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[0]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[1]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[2]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[3]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[4]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[5]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[6]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[7]],
                    CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[8]]
            };

            // rotation direction
            if (true) {
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[0]] = cubletsTemp[6];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[1]] = cubletsTemp[3];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[2]] = cubletsTemp[0];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[3]] = cubletsTemp[7];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[4]] = cubletsTemp[4];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[5]] = cubletsTemp[1];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[6]] = cubletsTemp[8];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[7]] = cubletsTemp[5];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[8]] = cubletsTemp[2];
            } else {
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[0]] = cubletsTemp[2];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[1]] = cubletsTemp[5];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[2]] = cubletsTemp[8];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[3]] = cubletsTemp[1];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[4]] = cubletsTemp[4];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[5]] = cubletsTemp[7];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[6]] = cubletsTemp[0];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[7]] = cubletsTemp[3];
                CrystalRenderUtils.INSTANCE.getCubeletStatus()[currentSide[8]] = cubletsTemp[6];
            }
            int[] trans = CrystalRenderUtils.INSTANCE.getCubeSideTransforms()[rotatingSide];
            for (int x = -1; x < 2; x++){
                for (int y = -1; y < 2; y++) {
                    for (int z = -1; z < 2; z++) {
                        if (x != 0 || y != 0 || z != 0)
                            applyCubeletRotation(x, y, z, trans[0], trans[1], trans[2]);
                    }
                }
            }
            rotatingSide = ThreadLocalRandom.current().nextInt(0, 5 + 1);
            lastTime = currentTime;
        }

        // Draw non-rotating cubes
        for (int x = -1; x < 2; x++){
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    if (x != 0 || y != 0 || z != 0)
                        drawCubeletStatic(scale, x, y, z);
                }
            }
        }

        // Draw rotating cubes
        int[] trans = CrystalRenderUtils.INSTANCE.getCubeSideTransforms()[rotatingSide];
        GlStateManager.pushMatrix();
        GlStateManager.translate(trans[0]*CUBELET_SCALE, trans[1]*CUBELET_SCALE, trans[2]*CUBELET_SCALE);
        //GlStateManager.rotate((currentTime - lastTime) * 90 / ANIMATION_LENGTH, trans[0], trans[1], trans[2]);
        float RotationAngle = (float) Math.toRadians(CrystalRenderUtils.INSTANCE.easeInOutCubic(((float)(currentTime - lastTime)) / ANIMATION_LENGTH)*90);
        float xx = (float) (trans[0] * Math.sin(RotationAngle / 2));
        float yy = (float) (trans[1] * Math.sin(RotationAngle / 2));
        float zz = (float) (trans[2] * Math.sin(RotationAngle / 2));
        float ww = (float) Math.cos(RotationAngle / 2);
        Quaternion q = new Quaternion(xx,yy,zz,ww);
        GlStateManager.rotate(q);
        for (int x = -1; x < 2; x++){
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    if (x != 0 || y != 0 || z != 0)
                        drawCubeletRotating(scale, x, y, z);
                }
            }
        }
        GlStateManager.popMatrix();
    }

    private void drawCubeletStatic(float scale, int x, int y, int z){
        int cubletId = CrystalRenderUtils.INSTANCE.getCubletLookup()[x+1][y+1][z+1];
        if (Arrays.stream(CrystalRenderUtils.INSTANCE.getCubeSides()[rotatingSide]).anyMatch(i -> i == cubletId)) return;
        drawCubelet(scale, x, y, z, cubletId);
    }

    private void drawCubeletRotating(float scale, int x, int y, int z){
        int cubletId = CrystalRenderUtils.INSTANCE.getCubletLookup()[x+1][y+1][z+1];
        if (Arrays.stream(CrystalRenderUtils.INSTANCE.getCubeSides()[rotatingSide]).noneMatch(i -> i == cubletId)) return;
        int[] trans = CrystalRenderUtils.INSTANCE.getCubeSideTransforms()[rotatingSide];
        drawCubelet(scale, x - trans[0], y - trans[1], z - trans[2], cubletId);
    }

    private void applyCubeletRotation(int x, int y, int z, int rX, int rY, int rZ){
        int cubletId = CrystalRenderUtils.INSTANCE.getCubletLookup()[x+1][y+1][z+1];
        if (Arrays.stream(CrystalRenderUtils.INSTANCE.getCubeSides()[rotatingSide]).noneMatch(i -> i == cubletId))
            return;
        float RotationAngle = (float) Math.toRadians(90);
        float xx = (float) (rX * Math.sin(RotationAngle / 2));
        float yy = (float) (rY * Math.sin(RotationAngle / 2));
        float zz = (float) (rZ * Math.sin(RotationAngle / 2));
        float ww = (float) Math.cos(RotationAngle / 2);
        CrystalRenderUtils.INSTANCE.getCubeletStatus()[cubletId] = Quaternion.mul(new Quaternion(xx,yy,zz,ww), CrystalRenderUtils.INSTANCE.getCubeletStatus()[cubletId], null);
    }

    private void drawCubelet(float scale, int x, int y, int z, int cubletId){
        GlStateManager.pushMatrix();
        GlStateManager.translate(x*CUBELET_SCALE, y*CUBELET_SCALE, z*CUBELET_SCALE);
        GlStateManager.pushMatrix();
        GlStateManager.rotate(CrystalRenderUtils.INSTANCE.getCubeletStatus()[cubletId]);
        this.cube.render(scale);
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
    }
 */

}