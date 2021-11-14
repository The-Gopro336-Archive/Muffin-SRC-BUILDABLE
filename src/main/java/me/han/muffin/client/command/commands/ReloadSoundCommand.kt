package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import me.han.muffin.client.imixin.misc.ISoundHandler

object ReloadSoundCommand: Command(arrayOf("reloadsound", "rs", "rsound")) {

    override fun dispatch(): String {
        return try {
            (Globals.mc.soundHandler as ISoundHandler).soundManager.reloadSoundSystem()
            "Successfully reload sound system!"
        } catch (e: Exception) {
            "Could not restart sound manager: $e"
        }
    }

}