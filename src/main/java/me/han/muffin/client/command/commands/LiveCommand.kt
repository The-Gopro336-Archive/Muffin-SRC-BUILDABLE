package me.han.muffin.client.command.commands

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.command.Command

object LiveCommand: Command(arrayOf("live")) {
    var isLive = false

    override fun dispatch(): String {
        isLive = !isLive

        return if (isLive) ChatFormatting.RED.toString() + "You're now streaming. Don't fucking click the gui button."
        else ChatFormatting.GREEN.toString() + "ok you can now use gui with no fucking problem."
    }

}