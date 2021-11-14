package me.han.muffin.client.gui.hud.item.component.client

import me.han.muffin.client.Muffin
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.value.StringValue
import me.han.muffin.client.value.Value

object WatermarkItem: HudItem("Watermark", HudCategory.Client, 4, 2) {
    private val custom = Value(false, "CustomWatermark")
    private val customValue = StringValue("Muffin", "CWatermark")
    private val showVersion = Value(true, "ShowVersion")

    init {
        addSettings(custom, customValue, showVersion)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        var logo = Muffin.MODNAME

        if (custom.value) logo = customValue.value
        if (showVersion.value) logo = logo + " " + Muffin.MODVER

        Muffin.getInstance().fontManager.drawStringWithShadow(logo, x, y)
        width = Muffin.getInstance().fontManager.getStringWidth(logo).toFloat()
        height = Muffin.getInstance().fontManager.stringHeight.toFloat()
    }

}