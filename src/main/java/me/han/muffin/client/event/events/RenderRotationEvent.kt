package me.han.muffin.client.event.events

import me.han.muffin.client.event.EventStageable
import net.minecraft.client.entity.AbstractClientPlayer

/*
data class RenderRotationEvent(
    var prevRenderYawOffset: Float,
    var renderYawOffset: Float,
    var prevRotationYawHead: Float,
    var rotationYawHead: Float,
    var prevRotationPitch: Float,
    var rotationPitch: Float)
 */
data class RenderRotationEvent(
    val stage: EventStageable.EventStage,
    val entity: AbstractClientPlayer,
    val x: Double,
    val y: Double,
    val z: Double,
    val entityYaw: Float,
    val partialTicks: Float
)