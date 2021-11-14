package me.han.muffin.client.module.modules.other

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.command.commands.LiveCommand
import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.hud.GuiHud
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import org.lwjgl.input.Keyboard

object HudEditorModule : Module("HudEditor", Category.OTHERS, true, false, Keyboard.KEY_J, "Toggle HudEditor.") {
    var startCounting = false
    var count = 0
    var ticksRun = 0

    @Override
    override fun onEnable() {
        if (fullNullCheck()) return

        if (LiveCommand.isLive) {
            startCounting = true
        }

        if (startCounting) {
            startCounting = false
            count++

            if (ticksRun < 20 && ClickGUI.count > 2) {
                Globals.mc.displayGuiScreen(GuiHud.getGuiHud())
                disable()
                ticksRun = 0
                count = 0
                return
            }

            ChatManager.sendDeleteMessage("${ChatFormatting.RED} You had clicked ${ClickGUI.count} time", "clicked", ChatIDs.HUD_EDITOR_LIVE_COUNT)
            return
        }

        Globals.mc.displayGuiScreen(GuiHud.getGuiHud())
        disable()
    }

}