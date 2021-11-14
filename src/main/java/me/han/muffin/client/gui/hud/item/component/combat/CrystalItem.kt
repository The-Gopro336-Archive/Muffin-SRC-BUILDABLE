package me.han.muffin.client.gui.hud.item.component.combat

import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.manager.managers.ItemManager
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.Value
import net.minecraft.item.ItemStack

object CrystalItem: HudItem("CrystalRender", HudCategory.Combat, 200, 215) {
    private val sideText = Value(true, "SideText")

    init {
        addSettings(sideText)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (ItemManager.crystalStack != ItemStack.EMPTY) {
            RenderUtils.renderItemOnScreen(x + 1, y, ItemManager.crystalStack, sideText.value, ItemManager.crystalStack.count)
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