package me.han.muffin.client.event.events.entity.living

import net.minecraft.entity.Entity

data class EntityItemPickupEvent(var entity: Entity, var quality: Int)