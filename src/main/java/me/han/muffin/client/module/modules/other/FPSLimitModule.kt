package me.han.muffin.client.module.modules.other

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.SettingEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.NumberValue
import org.lwjgl.opengl.Display
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object FPSLimitModule: Module("FPSLimit", Category.OTHERS, true, "Limit your fps.") {

    private val defaultFps = NumberValue(120, 1, 1500, 5, "DefaultFps")
    private val worldFps = NumberValue(0, 0, 1500, 5, "WorldFps")
    private val guiFps = NumberValue(120, 1, 1500, 5, "GuiFps")
    private val noFocusFps = NumberValue(20, 1, 1500, 5, "UnFocusFps")

    init {
        addSettings(defaultFps, worldFps, guiFps, noFocusFps)
    }

    override fun onDisable() {
        Globals.mc.gameSettings.limitFramerate = defaultFps.value
    }

    @Listener
    private fun onSetting(event: SettingEvent) {
        if (event.module != this) return
        Globals.mc.gameSettings.limitFramerate = getFps()
    }

    fun getFps(): Int {
        return if (noFocusFps.value > 0 && !Display.isActive()) {
            noFocusFps.value
        } else if (Globals.mc.currentScreen != null) {
            if (guiFps.value > 0)
                guiFps.value else defaultFps.value
        } else {
            if (worldFps.value > 0)
                worldFps.value else defaultFps.value
        }
    }

}