package me.han.muffin.client.event.events.entity.player

import me.han.muffin.client.event.EventCancellable
import net.minecraft.entity.Entity

data class PlayerApplyCollisionEvent(val entity: Entity): EventCancellable()