package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.Muffin
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.FileUtils
import net.minecraft.util.text.TextFormatting
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels

internal object NoteBotModule: Module("NoteBot", Category.MISC) {
    private val songsFile = File(Muffin.getInstance().getDirectory(), "songs")

    private var areaBlocksCounter = 0
    //private var discoveredBlocks = HashMap<>

    override fun onEnable() {
        if (fullNullCheck()) return

        if (songsFile.listFiles() == null) {
            downloadSongs()
            return
        }

        areaBlocksCounter = 0

    }

    override fun onDisable() {
    }

    private fun downloadSongs() {
        Thread {
            try {
                val file = File(songsFile, "songs.zip")
                FileOutputStream(file).channel.transferFrom(Channels.newChannel(URL("https://www.futureclient.net/future/songs.zip").openStream()), 0L, 4294967295L)
                FileUtils.unZip(file, songsFile)
                file.deleteOnExit()
                ChatManager.sendMessage(String.format("Successfully downloaded songs! You now have %d songs.", songsFile.listFiles().size))
                ChatManager.sendMessage(String.format("You can always type %s.downloadsongs%s to get all of the songs and %s.openfolder%s to see the songs in the Songs folder.", TextFormatting.GREEN.toString(), TextFormatting.GRAY.toString(), TextFormatting.GREEN.toString(), TextFormatting.GRAY.toString()))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    private class Note {

    }

}