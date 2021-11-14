package me.han.muffin.client.event.events.render;

import me.han.muffin.client.event.EventCancellable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;

public class RenderArmorLayerEvent extends EventCancellable {
    public EntityLivingBase Entity;

    public RenderArmorLayerEvent(EntityLivingBase p_Entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, EntityEquipmentSlot slotIn) {
        Entity = p_Entity;
    }
}