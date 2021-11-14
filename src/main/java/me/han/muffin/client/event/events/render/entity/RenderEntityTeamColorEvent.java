package me.han.muffin.client.event.events.render.entity;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.entity.Entity;

public class RenderEntityTeamColorEvent extends EventCancellable {
    private final Entity entity;
    private int color;

    public RenderEntityTeamColorEvent(Entity entity, int color) {
        this.entity = entity;
        this.color = color;
    }

    public Entity getEntity() {
        return entity;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

}