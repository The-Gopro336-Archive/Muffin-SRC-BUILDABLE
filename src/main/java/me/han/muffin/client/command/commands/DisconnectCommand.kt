package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import net.minecraft.util.text.TextComponentString

object DisconnectCommand: Command(arrayOf("disconnect", "disc", "dc")) {

    override fun dispatch(): String {
        if (Globals.mc.isSingleplayer) return "You are in single player."

        if (Globals.mc.connection == null) {
            Globals.mc.world.sendQuittingDisconnectingPacket()
        } else {
            Globals.mc.connection?.networkManager?.closeChannel(TextComponentString("Disconnected by using Disconnect command."))
        }

        return "Disconnecting..."
    }

}