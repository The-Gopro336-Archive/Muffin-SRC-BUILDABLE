package me.han.muffin.client.event.events.render

data class RenderLivingBaseEvent(
    val prevRenderYawOffset: Float,
    val renderYawOffset: Float,
    val prevRotationYawHead: Float,
    val rotationYawHead: Float,
    val prevRotationPitch: Float,
    val rotationPitch: Float
)