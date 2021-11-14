package me.han.muffin.client.event.events.client

import me.han.muffin.client.event.EventCancellable
import net.minecraft.entity.Entity

data class AttackEvent(val entity: Entity): EventCancellable()