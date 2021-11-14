package me.han.muffin.client.config

import me.han.muffin.client.Muffin
import me.han.muffin.client.manager.managers.ProfileManager
import me.han.muffin.client.module.modules.other.DiscordPresence

object ConfigSaver {

    class RuntimeSaver: Thread() {
        override fun run() {
            super.run()
            save()
        }

        fun save() {
            DiscordPresence.rpc.Discord_Shutdown()
            CommandPrefixConfig.saveCommandPrefix()
            FriendsConfig.saveFriend()
            ProfileManager.saveAll()
            CustomFontConfig.saveCustomFont()
            Muffin.getInstance().altManagerConfig.saveAccounts()
            Muffin.getInstance().hudManager.saveConfigShutDown()
        }
    }

    fun saveConfig() {
        DiscordPresence.rpc.Discord_Shutdown()

        if (Muffin.getInstance().trayUtils != null) {
            if (Muffin.getInstance().trayUtils.systemTray != null) Muffin.getInstance().trayUtils.systemTray!!.remove(Muffin.getInstance().trayUtils.trayIcon)
            Muffin.getInstance().trayUtils = null
        }

        CommandPrefixConfig.saveCommandPrefix()
        FriendsConfig.saveFriend()
        ProfileManager.saveAll()
        CustomFontConfig.saveCustomFont()
        Muffin.getInstance().altManagerConfig.saveAccounts()
        Muffin.getInstance().hudManager.saveConfigShutDown()
    }

}