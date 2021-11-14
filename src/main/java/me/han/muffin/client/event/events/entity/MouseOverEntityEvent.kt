package me.han.muffin.client.event.events.entity

import net.minecraft.entity.Entity

data class MouseOverEntityEvent(
    var entity: Entity
)