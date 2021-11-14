package me.han.muffin.client.module.modules.render

import me.han.muffin.client.event.events.render.Render2DEvent
import me.han.muffin.client.gui.font.util.Opacity
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color

internal object CustomEnchantModule: Module("CustomEnchant", Category.RENDER, "Custom enchantment colours.") {
    private val rainbow = Value(true, "Rainbow")
    private val rainbowSpeed = NumberValue({ rainbow.value },3f, 1f, 15f, 0.05f, "RainbowSpeed")
    private val rainbowBrightness = NumberValue({ rainbow.value }, 0.6f, 0.25f, 1.0f, 0.05f, "RainbowBrightness")
    private val rainbowWidth = NumberValue({ rainbow.value }, 10f, 1f, 20f, 0.05f, "RainbowWidth")

    private val red = NumberValue({ !rainbow.value },255, 0, 255, 2, "Red")
    private val green = NumberValue({ !rainbow.value },255, 0, 255, 2, "Green")
    private val blue = NumberValue({ !rainbow.value },0, 0, 255, 2, "Blue")
    private val alpha = NumberValue({ !rainbow.value },120, 0, 255, 2, "Alpha")

    private var h = 0F
    private val hue = Opacity(0)
    private var width = 0f
    var colour = Color(red.value, green.value, blue.value, alpha.value)

    init {
        addSettings(rainbow, rainbowSpeed, rainbowBrightness, rainbowWidth, red, green, blue, alpha)
    }

    @Listener
    private fun onRender2D(event: Render2DEvent) {
        if (fullNullCheck()) return

        if (rainbow.value) {
            h = hue.opacity
            hue.interp(256f, rainbowSpeed.value - 1.0)
            if (hue.opacity > 255) hue.opacity = 0.0F
            width = rainbowWidth.value
            if (h > 255) h = 0F
            val preRainbow = Color.getHSBColor(h / 255.0f, rainbowBrightness.value, 1.0f)
            colour = Color(preRainbow.red, preRainbow.green, preRainbow.blue, alpha.value)
        } else {
            colour = Color(red.value, green.value, blue.value, alpha.value)
        }
    }

}