package me.han.muffin.client.event.events.entity.living;

import me.han.muffin.client.event.EventCancellable;
import me.han.muffin.client.event.EventStageable;
import net.minecraft.entity.EntityLivingBase;

/**
 * Called before and after {@link EntityLivingBase#travel(float, float, float)}
 * with respective states. The main purpose of the event is to allow the developer
 * to control the movement of any entities that are instances of {@link net.minecraft.client.entity.EntityPlayerSP}.
 * This would include the local player and bots. Cancelling is only effective in the
 * {@link me.han.muffin.client.event.EventStageable.EventStage #PRE} state, and prevents the entity from "travelling".
 *
 * @author Brady
 * @since 8/16/2017
 */
public class EntityTravelEvent extends EventCancellable {

    /**
     * State of when the event was called relative to the invokation
     * of {@link EntityLivingBase#travel(float, float, float)}
     */
    private final EventStageable.EventStage state;

    /**
     * The {@link EntityLivingBase} that is "travelling"
     */
    private final EntityLivingBase entity;

    /**
     * Strafe travel amount
     */
    private float strafe;

    /**
     * Vertical travel amount
     */
    private float vertical;

    /**
     * Forward travel amount
     */
    private float forward;

    public EntityTravelEvent(EventStageable.EventStage state, EntityLivingBase entity, float strafe, float vertical, float forward) {
        this.entity = entity;
        this.state = state;
        this.strafe = strafe;
        this.vertical = vertical;
        this.forward = forward;
    }

    /**
     * Sets the strafe travel amount to be passed down to {@link EntityLivingBase#travel(float, float, float)}
     * @param strafe New strafe travel amount
     * @return This event
     */
    public EntityTravelEvent setStrafe(float strafe) {
        this.strafe = strafe;
        return this;
    }

    /**
     * Sets the vertical travel amount to be passed down to {@link EntityLivingBase#travel(float, float, float)}
     * @param vertical New strafe travel amount
     * @return This event
     */
    public EntityTravelEvent setVertical(float vertical) {
        this.vertical = vertical;
        return this;
    }

    /**
     * Sets the forward travel amount to be passed down to {@link EntityLivingBase#travel(float, float, float)}
     * @param forward New forward travel amount
     * @return This event
     */
    public EntityTravelEvent setForward(float forward) {
        this.forward = forward;
        return this;
    }

    /**
     * @return The strafe travel amount
     */
    public float getStrafe() {
        return this.strafe;
    }

    /**
     * @return The vertical travel amount
     */
    public float getVertical() {
        return this.vertical;
    }

    /**
     * @return The forward travel amount
     */
    public float getForward() {
        return this.forward;
    }

    /**
     * @return The state of this event
     */
    public EventStageable.EventStage getState() {
        return this.state;
    }

    /**
     * @return The entity "travelling"
     */
    public EntityLivingBase getEntity() {
        return this.entity;
    }

}