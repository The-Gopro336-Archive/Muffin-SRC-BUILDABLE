package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData

object ConnectCommand: Command(arrayOf("connect", "c"), Argument("ip")) {

    override fun dispatch(): String {
        val serverIPArgument = getArgument("ip")?.value ?: return "Invalid values."

        val serverData = ServerData("", serverIPArgument, false)
        Globals.mc.world.sendQuittingDisconnectingPacket()
        Globals.mc.loadWorld(null)
        Globals.mc.displayGuiScreen(GuiConnecting(GuiMultiplayer(GuiMainMenu()), Globals.mc, serverData))
        return "Connecting to ${serverData.serverIP}..."
    }

}