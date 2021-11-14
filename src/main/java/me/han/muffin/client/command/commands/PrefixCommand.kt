package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.CommandManager

object PrefixCommand: Command(arrayOf("prefix"), Argument("character")) {

    override fun dispatch(): String {
        val prefix = getArgument("character")?.value ?: return "Invalid prefix."
        CommandManager.prefix = prefix
        return ChatManager.textColour + prefix + "is now your prefix."
    }

}