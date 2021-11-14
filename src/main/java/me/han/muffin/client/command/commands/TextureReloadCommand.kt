package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.TextureManager

internal object TextureReloadCommand: Command(arrayOf("texreload")) {

    override fun dispatch(): String {
        TextureManager.loadTextureThread.start()
        return "Attempting to reload client textures."
    }

}