package me.han.muffin.client.command.commands

import me.han.muffin.client.Muffin
import me.han.muffin.client.command.Command
import java.awt.Desktop
import java.io.File
import java.io.IOException

object OpenFolderCommand: Command(arrayOf("openfolder", "openthefolder", "folder", "folderopen", "of", "folderopener")) {

    override fun dispatch(): String {
        try {
            Desktop.getDesktop().open(File(Muffin.getInstance().directory.toURI()))
        } catch (e: IOException) {
            return "Failed to open folder."
        }
        return "Opened the ${Muffin.MODNAME} folder."
    }

}