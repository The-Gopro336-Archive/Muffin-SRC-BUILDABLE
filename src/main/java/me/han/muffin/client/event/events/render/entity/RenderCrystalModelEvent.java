package me.han.muffin.client.event.events.render.entity;

import me.han.muffin.client.event.EventCancellable;
import me.han.muffin.client.event.EventStageable;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;

public class RenderCrystalModelEvent extends EventCancellable {
    private final EventStageable.EventStage stage;
    private final ModelBase modelBase;
    private Entity entity;
    private final float limbSwing;
    private final float limbSwingAmount;
    private final float age;
    private final float headYaw;
    private final float headPitch;
    private final float scale;

    public RenderCrystalModelEvent(EventStage stage, ModelBase modelBase, Entity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, float scale) {
        this.stage = stage;
        this.modelBase = modelBase;
        this.entity = entity;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.age = age;
        this.headYaw = headYaw;
        this.headPitch = headPitch;
        this.scale = scale;
    }

    @Override
    public EventStage getStage() {
        return stage;
    }

    public ModelBase getModelBase() {
        return modelBase;
    }

    public Entity getEntity() {
        return entity;
    }

    public float getLimbSwing() {
        return limbSwing;
    }

    public float getLimbSwingAmount() {
        return limbSwingAmount;
    }

    public float getAge() {
        return age;
    }

    public float getHeadYaw() {
        return headYaw;
    }

    public float getHeadPitch() {
        return headPitch;
    }

    public float getScale() {
        return scale;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

}