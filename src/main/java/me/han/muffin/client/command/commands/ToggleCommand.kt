package me.han.muffin.client.command.commands

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ModuleManager

object ToggleCommand: Command(arrayOf("toggle", "t"), Argument("module")) {

    override fun dispatch(): String {
        val moduleArgument = getArgument("module")?.value ?: return "Invalid module argument"
        val module = ModuleManager.getModule(moduleArgument) ?: return "No such module exists."

        module.toggle()
        return module.name + ChatFormatting.GRAY + " has been " + if (module.isEnabled) ChatFormatting.GREEN.toString() + "enabled" else ChatFormatting.RED.toString() + "disabled"
    }

}