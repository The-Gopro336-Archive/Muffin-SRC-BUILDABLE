package me.han.muffin.client.event.events.entity

import me.han.muffin.client.event.EventCancellable
import net.minecraft.entity.Entity

object EntityTurnEvent {
    data class Pre(val entity: Entity, var yaw: Float, var pitch: Float): EventCancellable()
    data class Post(val entity: Entity, val yaw: Float, val pitch: Float)
}