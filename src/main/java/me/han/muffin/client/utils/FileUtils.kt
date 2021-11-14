package me.han.muffin.client.utils

import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FileUtils {

    fun appendTextFile(data: String, file: String): Boolean {
        try {
            val path = Paths.get(file, *arrayOfNulls(0))
            Files.write(path, Collections.singletonList(data), StandardCharsets.UTF_8, if (Files.exists(path, *arrayOfNulls<LinkOption>(0))) StandardOpenOption.APPEND else StandardOpenOption.CREATE)
        } catch (e: IOException) {
            println("WARNING: Unable to write file: $file")
            return false
        }
        return true
    }

    fun readTextFileAllLines(file: String): List<String> {
        return try {
            val path = Paths.get(file, *arrayOfNulls(0))
            Files.readAllLines(path, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            println("WARNING: Unable to read file, creating new file: $file")
            appendTextFile("", file)
            Collections.emptyList()
        }
    }

    fun unZip(targetZip: File, targetFolder: File) {
        val buffer = ByteArray(1024)
        val zipInputStream = ZipInputStream(FileInputStream(targetZip))

        try {
            if (!targetFolder.exists()) targetFolder.mkdir()
            var zipEntry: ZipEntry?
            while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                val name = zipEntry!!.name
                val file = File(targetFolder, name)
                File(file.parent).mkdirs()

                val fileOutput = FileOutputStream(file)
                var readed: Int
                while (zipInputStream.read(buffer).also { readed = it } != -1) fileOutput.write(buffer, 0, readed)

                fileOutput.close()
                zipInputStream.closeEntry()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        zipInputStream.close()
    }

    private fun extractFile(zipIn: ZipInputStream, filePath: String) {
        try {
            val bos = BufferedOutputStream(FileOutputStream(filePath))
            val bytesIn = ByteArray(1024)//ByteArray(4096)
            var read: Int
            while (zipIn.read(bytesIn).also { read = it } != -1) {
                bos.write(bytesIn, 0, read)
            }
            bos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
