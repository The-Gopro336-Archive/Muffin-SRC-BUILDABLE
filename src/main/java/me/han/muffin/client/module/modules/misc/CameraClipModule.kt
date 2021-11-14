package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.event.events.render.OrientCameraEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.NumberValue
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object CameraClipModule : Module("CameraClip", Category.MISC, true, "Makes F5 mode more usable.") {
    private val cameraDistance = NumberValue(6.0, 0.1, 50.0, 0.1, "Distance")

    init {
        addSettings(cameraDistance)
    }

    @Listener
    private fun onOrientCamera(event: OrientCameraEvent) {
        event.distance = cameraDistance.value
    }

}