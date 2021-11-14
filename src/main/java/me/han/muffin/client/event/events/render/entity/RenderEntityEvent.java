package me.han.muffin.client.event.events.render.entity;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.entity.Entity;

public class RenderEntityEvent extends EventCancellable {

    private final EventStage stage;
    private final Entity entity;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float partialTicks;

    public RenderEntityEvent(EventStage stage, Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        this.stage = stage;
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.partialTicks = partialTicks;
    }

    @Override
    public EventStage getStage() {
        return stage;
    }

    public Entity getEntity() {
        return entity;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPartialTicks(float partialTicks) {
        this.partialTicks = partialTicks;
    }
}
