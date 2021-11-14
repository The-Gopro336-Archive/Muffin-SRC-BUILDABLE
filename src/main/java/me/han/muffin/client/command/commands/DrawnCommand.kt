package me.han.muffin.client.command.commands

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ModuleManager

object DrawnCommand: Command(arrayOf("draw", "drawn"), Argument("module")) {

    override fun dispatch(): String {
        val moduleArgument = getArgument("module")?.value ?: return "Invalid values."

        val m = ModuleManager.getModule(moduleArgument) ?: return "Unknown module '$moduleArgument'."

        if (m.isDrawn) m.isDrawn = false else if (!m.isDrawn) m.isDrawn = true
        return m.name + if (m.isDrawn) ChatFormatting.GREEN.toString() + " has drawn!" else ChatFormatting.RED.toString() + " had not drawn anymore!"
    }

}