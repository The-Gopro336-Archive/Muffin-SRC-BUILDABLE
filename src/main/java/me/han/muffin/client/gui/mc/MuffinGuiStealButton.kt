package me.han.muffin.client.gui.mc

import me.han.muffin.client.core.Globals
import me.han.muffin.client.module.modules.player.ChestStealerModule
import net.minecraft.client.gui.GuiButton

class MuffinGuiStealButton(x: Int, y: Int) :

    GuiButton(696969, x, y, 50, 20, "Steal") {
    override fun mouseReleased(mouseX: Int, mouseY: Int) {
        if (ChestStealerModule.mode.value == ChestStealerModule.Mode.MANUAL) {
            ChestStealerModule.stealing = false
            playPressSound(Globals.mc.soundHandler)
        }
        super.mouseReleased(mouseX, mouseY)
    }

}