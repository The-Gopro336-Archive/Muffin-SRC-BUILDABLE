package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.events.UpdateFramebufferSizeEvent
import me.han.muffin.client.event.events.render.FovModifierEvent
import me.han.muffin.client.event.events.render.RenderSkyEvent
import me.han.muffin.client.event.events.render.entity.HurtCamEvent
import me.han.muffin.client.event.events.render.overlay.RenderOverlayEvent
import me.han.muffin.client.utils.camera.Camera
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object CameraManager {
    private val cameraList = ArrayList<Camera>()

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    fun update() {
        for (cam in cameraList) {
            if (!cam.isRecording && cam.isRendering) {
                cam.updateFbo()
            }
        }
    }

    @Listener
    private fun onRenderOverlay(event: RenderOverlayEvent) {
        if (isCameraRecording()) {
            event.cancel()
        }
    }

    @Listener
    private fun onFboResize(event: UpdateFramebufferSizeEvent) {
        for (cam in cameraList) {
            cam.resize()
        }
    }

    @Listener
    private fun onFovModifier(event: FovModifierEvent) {
        if (isCameraRecording()) {
            event.fov = 90.0f
            event.cancel()
        }
    }

    @Listener
    private fun onHurtCamEffect(event: HurtCamEvent) {
        if (isCameraRecording()) {
            event.cancel()
        }
    }

    @Listener
    private fun onRenderSky(event: RenderSkyEvent) {
        if (isCameraRecording()) {
            event.cancel()
        }
    }

    fun addCamera(cam: Camera) {
        cameraList.add(cam)
    }

    fun unload() {
        cameraList.clear()
        Muffin.getInstance().eventManager.removeEventListener(this)
    }

    fun isCameraRecording(): Boolean {
        for (cam in cameraList) {
            if (cam.isRecording) {
                return true
            }
        }
        return false
    }

}
