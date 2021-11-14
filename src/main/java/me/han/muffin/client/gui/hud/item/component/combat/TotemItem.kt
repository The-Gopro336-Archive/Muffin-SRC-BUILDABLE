package me.han.muffin.client.gui.hud.item.component.combat

import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.manager.managers.ItemManager
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.Value
import net.minecraft.item.ItemStack

object TotemItem: HudItem("TotemRender", HudCategory.Combat, 200, 200) {
    private val sideText = Value(true, "SideText")

    init {
        addSettings(sideText)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (ItemManager.totemStack != ItemStack.EMPTY) {
            RenderUtils.renderItemOnScreen(x, y, ItemManager.totemStack, sideText.value, ItemManager.totemStack.count)
        }

        if (sideText.value) {
            width = 35F
            height = 17F
        } else {
            width = 19F
            height = 19F
        }
    }


}