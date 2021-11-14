package me.han.muffin.client.config

import me.han.muffin.client.Muffin
import me.han.muffin.client.manager.managers.CommandManager
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object CommandPrefixConfig {
    private val file = File(Muffin.getInstance().getDirectory(), "command_prefix.txt")

    fun saveCommandPrefix() {
        try {
            if (!file.exists()) file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            FileWriter(file).buffered().use {
                it.write(CommandManager.prefix)
                it.newLine()

                it.flush()
                it.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCommandPrefix() {
        try {
            if (!file.exists()) file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (!file.exists()) return

        try {
            FileReader(file).buffered().use { reader ->
                var readLine: String? = null
                while (reader.readLine()?.let { readLine = it } != null) {
                    try {
                        val split = readLine!!.split(":".toRegex()).toTypedArray()
                        CommandManager.prefix = split[0]
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                reader.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}