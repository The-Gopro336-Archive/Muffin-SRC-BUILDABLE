package me.han.muffin.client.gui.hud.item.component.world;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.manager.managers.CameraManager;
import me.han.muffin.client.utils.camera.Camera;
import me.han.muffin.client.utils.entity.EntityUtil;
import me.han.muffin.client.utils.render.RenderUtils;
import me.han.muffin.client.value.NumberValue;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class RearViewItem extends HudItem {

    private final Camera overviewCamera = new Camera();
    private final NumberValue<Float> zoom = new NumberValue<>(50.0F, 0.0F, 100.0F, 1.0F, "Zoom");

    public RearViewItem() {
        super("RearView", HudCategory.World,100, 50);
        addSettings(zoom);
        CameraManager.INSTANCE.addCamera(overviewCamera);
        setWidth(120);
        setHeight(120);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderUtils.drawRect(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0x99101010);
        RenderUtils.drawRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF202020);
        Muffin.getInstance().getFontManager().drawStringWithShadow(getDisplayName(), getX() + 2, getY() + 2, 0xFFFFFFFF);

        if (!EntityUtil.fullNullCheck()) {
            this.overviewCamera.setRendering(true);
            if (this.overviewCamera.isValid()) {
                final Vec3d ground = this.getGround(partialTicks);

                if (ground != null) {
                    //final Vec3d forward = MathUtil.direction(mc.player.rotationYaw);
                    //final float factor = 30.0f;
                    //this.overviewCamera.setPos(ground.add(0, this.getDist(partialTicks), 0).subtract(forward.x * factor, forward.y * factor, forward.z * factor));
                    this.overviewCamera.setPos(ground.add(0, this.getDist(partialTicks), 0));
                    this.overviewCamera.setYaw(Globals.mc.player.rotationYaw);
                    this.overviewCamera.setPitch(90.0f);
                    this.overviewCamera.render(this.getX() + 2, this.getY() + 12, this.getX() + this.getWidth() - 2, this.getY() + this.getHeight() - 2);
                }
            }
        }

        RenderUtils.drawTriangle(this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 + 5, 4, 0, 0x70101010);
        RenderUtils.drawTriangle(this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 + 5 + 0.5f, 2.5f, 0, 0xAAFFFFFF);
    }

    private Vec3d getGround(float partialTicks) {
        final Vec3d eyes = Globals.mc.player.getPositionEyes(partialTicks);
        final RayTraceResult ray = Globals.mc.world.rayTraceBlocks(eyes, eyes.subtract(0, 3, 0), false);
        if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
            return ray.hitVec;
        }
        return eyes;
    }

    private double getDist(float partialTicks) {
        final Vec3d eyes = Globals.mc.player.getPositionEyes(partialTicks);
        final RayTraceResult ray = Globals.mc.world.rayTraceBlocks(eyes, eyes.add(0, this.zoom.getValue(), 0), false);
        if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
            return Globals.mc.player.getDistance(ray.hitVec.x, ray.hitVec.y, ray.hitVec.z) - 4;
        }
        return this.zoom.getValue();
    }


}