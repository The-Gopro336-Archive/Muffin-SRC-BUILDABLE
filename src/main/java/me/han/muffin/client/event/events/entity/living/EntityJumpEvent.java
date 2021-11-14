package me.han.muffin.client.event.events.entity.living;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.entity.EntityLivingBase;

public class EntityJumpEvent extends EventCancellable {

    /**
     * The state of the event
     */
    private final EventStage state;

    /**
     * The {@link EntityLivingBase} that is jumping
     */
    private final EntityLivingBase entity;

    public EntityJumpEvent(EventStage state, EntityLivingBase entity) {
        this.state = state;
        this.entity = entity;
    }

    /**
     * @return The state of the event
     */
    public EventStage getState() {
        return this.state;
    }

    /**
     * @return The {@link EntityLivingBase} that is jumping
     */
    public EntityLivingBase getEntity() {
        return this.entity;
    }

}