package me.han.muffin.client.utils.client

import me.han.muffin.client.Muffin
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.mixin.ClientLoader
import me.han.muffin.client.module.modules.misc.NotificationsModule
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionEvent
import java.io.IOException
import javax.imageio.ImageIO

class TrayUtils {
    var trayIcon: TrayIcon? = null
    var systemTray: SystemTray? = null

    private fun onDisable(event: ActionEvent) {
        NotificationsModule.desktopMsg.value = false
        systemTray?.remove(trayIcon)
    }

    init {
        if (!SystemTray.isSupported()) {
            NotificationsModule.disable()
            ChatManager.sendMessage("Your computer does not support system tray icons.")
            ClientLoader.LOGGER.error("Your computer does not support system tray icons.")
        } else {
            val inputStream = javaClass.getResourceAsStream("/assets/minecraft/textures/muffin/icon.png")

            val icon = try {
                ImageIO.read(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }

            if (icon == null) {
                NotificationsModule.desktopMsg.value = false
            } else {
                trayIcon = TrayIcon(icon, "Muffin")
                val menu = PopupMenu()
                systemTray = SystemTray.getSystemTray()
                trayIcon!!.toolTip = StringBuilder().insert(0, Muffin.MODNAME).append(" ").append(Muffin.MODVER).append(" notifications").toString()

                val disableNotifications = MenuItem("Disable Notifications")
                menu.add(disableNotifications)
                disableNotifications.addActionListener { onDisable(it) }

                trayIcon!!.popupMenu = menu
                systemTray!!.add(trayIcon)
            }
        }
    }

}