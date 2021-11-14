package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.module.modules.other.StreamerModeModule

object FakeNameCommand: Command(arrayOf("np", "nameprotect"), Argument("name")) {

    override fun dispatch(): String {
        val nameArgument = getArgument("name")?.value ?: return "Please type a name."
        StreamerModeModule.fakeName = nameArgument
        return "Successfully changed fake name"
    }


}