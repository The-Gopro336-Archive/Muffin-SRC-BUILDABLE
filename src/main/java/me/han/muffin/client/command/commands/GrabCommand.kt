package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.StringUtils
import me.han.muffin.client.utils.extensions.mc.utils.toStringFormat

object GrabCommand: Command(arrayOf("grab"), Argument("coords|ip")) {

    override fun dispatch(): String {
        val actionArgument = getArgument("coords|ip")?.value ?: return "Invalid values."

        if (actionArgument.equals("coords", ignoreCase = true)) {
            StringUtils.copyToClipboard(Globals.mc.player.positionVector.toStringFormat())
        } else if (actionArgument.equals("ip", ignoreCase = true)) {
            if (Globals.mc.currentServerData != null) StringUtils.copyToClipboard(Globals.mc.currentServerData!!.serverIP)
        }
        return "Copied the selected type."
    }

}