package me.han.muffin.client.gui.hud.item.component.combat

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.module.modules.render.FillEspModule
import me.han.muffin.client.utils.entity.EntityUtil

object FillEspItem: HudItem("FillDetector", HudCategory.Combat, 5 ,30) {

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (EntityUtil.fullNullCheck() || FillEspModule.isDisabled) return

        val filledPlayers = FillEspModule.filledPlayer
        if (filledPlayers.isNullOrEmpty()) return

        var posY = 0
        for (player in filledPlayers) {

            val placeholder = player.name + " has filled himself."
            Muffin.getInstance().fontManager.drawStringWithShadow(ChatFormatting.RED.toString() + placeholder, x, y + posY)
            width = Muffin.getInstance().fontManager.getStringWidth(placeholder).toFloat()
            height = Muffin.getInstance().fontManager.stringHeight.toFloat()
            posY += Muffin.getInstance().fontManager.stringHeight
        }
    }


}