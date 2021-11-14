package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ModuleManager

object LegitCommand: Command(arrayOf("legit")) {

    override fun dispatch(): String {
        ModuleManager.modules.forEach { if (it.isEnabled) it.toggle() }
        return "All modules has turned off."
    }

}