package me.han.muffin.client.command.commands

import me.han.muffin.client.Muffin
import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.config.CommandPrefixConfig
import me.han.muffin.client.config.ConfigSaver
import me.han.muffin.client.config.CustomFontConfig
import me.han.muffin.client.config.FriendsConfig
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.ProfileManager
import me.han.muffin.client.utils.ChatIDs
import kotlin.concurrent.thread

object ConfigCommand: Command(arrayOf("config"), Argument("save/load")) {

    override fun dispatch(): String {
        val actionArgument = getArgument("save/load")?.value?.toLowerCase() ?: return "Invalid values."

        if (actionArgument.equals("save", ignoreCase = true)) {
            thread {
                ConfigSaver.saveConfig()
                ChatManager.sendDeleteMessage("Config has saved to file.", "saved", ChatIDs.CONFIG_COMMAND)
            }
            return "Saving file..."
        } else if (actionArgument.equals("load", ignoreCase = true)) {
            thread {
                load()
                ChatManager.sendDeleteMessage("Config has loaded.", "loaded", ChatIDs.CONFIG_COMMAND)
            }
            return "Loading config..."
        }

        return ""
    }

    private fun load() {
        ProfileManager.saveAll()
        CommandPrefixConfig.loadCommandPrefix()
        CustomFontConfig.loadCustomFont()
        FriendsConfig.loadFriend()
        Muffin.getInstance().altManagerConfig.loadAccounts()
        Muffin.getInstance().hudManager.items.forEach { it.loadSettings() }
    }

}