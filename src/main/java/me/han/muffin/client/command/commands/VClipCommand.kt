package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals

object VClipCommand: Command(arrayOf("vclip", "vc", "v"), Argument("blocks")) {

    override fun dispatch(): String {
        val blocks = getArgument("blocks")?.value?.toDouble() ?: return "Invalid blocks argument"

        Globals.mc.player.entityBoundingBox = Globals.mc.player.entityBoundingBox.offset(0.0, blocks, 0.0)
        return String.format("Teleported %s &e%s&7 block(s).", if (blocks < 0) "down" else "up", blocks)
    }

}