package me.han.muffin.client.gui.hud.item.component.client

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.module.modules.other.StreamerModeModule
import me.han.muffin.client.value.StringValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.ScaledResolution
import java.util.*

object WelcomerItem: HudItem("Welcomer", HudCategory.Client, 300, 2) {

    private val custom = Value(false, "Custom")
    private val header = StringValue("Hello", "Header")
    private val footer = StringValue(".", "Footer")
    private val center = Value(false, "Center")

    init {
        addSettings(custom, header, footer, center)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        val fakeName = if (StreamerModeModule.isEnabled) StreamerModeModule.fakeName else Globals.mc.player.name

        val welcome =
            if (custom.value) header.value + " " + fakeName + " " + footer.value
            else getHeaders() + fakeName + " :)"

        if (center.value) {
            val scaledResolution = ScaledResolution(Globals.mc)
            x = scaledResolution.scaledWidth / 2 - Muffin.getInstance().fontManager.getStringWidth(welcome) / 2
            center.value = false
        }

        Muffin.getInstance().fontManager.drawStringWithShadow(welcome, x, y)
        width = Muffin.getInstance().fontManager.getStringWidth(welcome).toFloat()
        height = Muffin.getInstance().fontManager.stringHeight.toFloat()
    }

    private fun getHeaders(): String {
        val timeOfDay = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        return when {
            timeOfDay < 5 -> {
                "It's midnight "
            }
            timeOfDay < 12 -> {
                "Good morning "
            }
            timeOfDay < 16 -> {
                "Good afternoon "
            }
            timeOfDay < 21 -> {
                "Good evening "
            }
            else -> "Good night "
        }
    }

    private fun getFooter(): String {
        val timeOfDay = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        return when {
            timeOfDay < 5 -> {
                " :"
            }
            timeOfDay < 12 -> {
                "Good morning "
            }
            timeOfDay < 16 -> {
                "Good afternoon "
            }
            timeOfDay < 21 -> {
                "Good evening "
            }
            else -> "Good night "
        }
    }

}