package me.han.muffin.client.utils.network

import me.han.muffin.client.core.Globals
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.ServerListEntryLanDetected
import net.minecraft.client.gui.ServerListEntryNormal
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData

object ServerUtils {

    var serverData: ServerData? = null
    @JvmField
    var seconds = 0

    fun connectToLastServer() {
        if (serverData == null) return
        Globals.mc.displayGuiScreen(GuiConnecting(GuiMultiplayer(GuiMainMenu()), Globals.mc, serverData!!))
    }

    fun setServerType(iGuiListEntry: IGuiListEntry?) {
        if (iGuiListEntry is ServerListEntryNormal) {
            serverData = iGuiListEntry.serverData
            return
        }
        if (iGuiListEntry is ServerListEntryLanDetected) {
            val lanServerInfo = iGuiListEntry.serverData
            serverData = ServerData("Singleplayer", lanServerInfo.serverIpPort, true)
        }
    }

    fun getRemoteIp(): String {
        var serverIp = "Singleplayer"
        if (Globals.mc.world.isRemote) {
            val serverData = Globals.mc.currentServerData
            if (serverData != null) serverIp = serverData.serverIP
        }
        return serverIp
    }

}