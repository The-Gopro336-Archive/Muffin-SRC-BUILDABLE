package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.gui.font.util.Opacity
import me.han.muffin.client.module.modules.other.ColorControl
import me.han.muffin.client.utils.color.Colour

object ColourManager {
    var red = 255
    var green = 255
    var blue = 255
    var alpha = 255

    val glRed: Float get() = red / 255F
    val glGreen: Float get() = green / 255F
    val glBlue: Float get() = blue / 255F
    val glAlpha: Float get() = alpha / 255F

    val colour: Colour get() = Colour(red, green, blue, alpha)
    val hexColour: Int get() = colour.toHex()

    private val rainbowHue = Opacity(0)
    private var width = 0F

    fun initListener() {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    // @Listener
    private fun onTicking(event: TickEvent) {
        val rainbowSpeed = ColorControl.INSTANCE.rainbowSpeed.value
        val rainbowWidth = ColorControl.INSTANCE.rainbowWidth.value

        rainbowHue.interp(256F, rainbowSpeed - 1.0)
        if (rainbowHue.opacity > 255F) rainbowHue.opacity = 0.0F

    }

}