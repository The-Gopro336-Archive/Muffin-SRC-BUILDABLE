package me.han.muffin.client.config.utils

import java.io.File
import java.io.FileWriter
import java.io.IOException

object ConfigUtils {

    fun fixEmptyJson(file: File) {
        if (!file.exists()) file.createNewFile()
        var notEmpty = false
        file.forEachLine { notEmpty = notEmpty || it.trim().isNotBlank() }

        if (!notEmpty) {
            try {
                val fileWriter = FileWriter(file)
                fileWriter.write("{}")
                fileWriter.close()
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        }
    }

}