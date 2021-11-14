package me.han.muffin.client.command.commands

import me.han.muffin.client.Muffin
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.CommandManager

object CommandsCommand: Command(arrayOf("commands", "cmds")) {

    override fun dispatch(): String {
        Muffin.getInstance().getCommandManager().commands.forEach {
            ChatManager.sendMessage(ChatManager.darkTextColour + CommandManager.prefix + ChatManager.textColour + it.aliases[0] + "&r")
        }
        return "Total commands are ${Muffin.getInstance().getCommandManager().commands.size}."
    }

}