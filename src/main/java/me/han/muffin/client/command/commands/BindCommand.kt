package me.han.muffin.client.command.commands

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.ModuleManager
import org.lwjgl.input.Keyboard

object BindCommand: Command(arrayOf("bind"), Argument("module"), Argument("key")) {

    override fun dispatch(): String {
        val moduleArgument = getArgument("module")?.value ?: return "Invalid values."
        val keyArgument = getArgument("key")?.value?.toUpperCase() ?: return "Invalid values."

        val module = ModuleManager.getModule(moduleArgument) ?: return "No such module exists."

        val key = Keyboard.getKeyIndex(keyArgument)
        module.bind = key
        return module.name + ChatFormatting.GRAY + " has been bound to " + ChatManager.textColour + Keyboard.getKeyName(key).toUpperCase()
    }

}