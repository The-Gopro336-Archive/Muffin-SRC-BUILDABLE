package me.han.muffin.client.imixin.render.entity

interface IEntityRenderer {

    fun orientCameraVoid(partialTicks: Float)
    fun setupCameraTransformVoid(partialTicks: Float, pass: Int)
    fun renderWorldPassVoid(pass: Int, partialTicks: Float, finishTimeNano: Long)

}