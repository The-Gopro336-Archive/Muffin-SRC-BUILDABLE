package me.han.muffin.client.event.events.render;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;

public class OutlineEvent extends EventCancellable {
    private final Entity entity;
    private final ICamera iCamera;
    private final float partialTicks;

    public OutlineEvent(Entity entity, ICamera iCamera, float partialTicks) {
        this.entity = entity;
        this.iCamera = iCamera;
        this.partialTicks = partialTicks;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public ICamera getiCamera() {
        return iCamera;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}

