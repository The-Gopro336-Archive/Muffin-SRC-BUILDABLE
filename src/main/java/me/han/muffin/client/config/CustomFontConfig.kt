package me.han.muffin.client.config

import me.han.muffin.client.Muffin
import me.han.muffin.client.module.modules.other.FontsModule
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object CustomFontConfig {
    private val file = File(Muffin.getInstance().getDirectory(), "custom_font.txt")

    fun saveCustomFont() {
        try {
            if (!file.exists()) file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val currentSystemFont = Muffin.getInstance().fontManager.systemFont
        val saveFontName = currentSystemFont ?: "client"

        try {
            FileWriter(file).buffered().use {
                it.write(saveFontName)
                it.flush()
                it.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCustomFont() {
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
                        val firstText = split[0]
                        if (firstText == "client") {
                            Muffin.getInstance().fontManager.systemFont = null
                        } else {
                            Muffin.getInstance().fontManager.systemFont = firstText
                        }

                        Muffin.getInstance().fontManager.setSystemFont(FontsModule.sizeFont)
                        Muffin.getInstance().fontManager.setFontStyle(FontsModule.styleFont)
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