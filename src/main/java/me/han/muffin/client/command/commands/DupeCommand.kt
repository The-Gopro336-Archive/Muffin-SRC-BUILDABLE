package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import net.minecraft.inventory.ClickType
import net.minecraft.item.ItemAir
import net.minecraft.util.EnumHand

object DupeCommand: Command(arrayOf("dupe", "dupeexploit", "dupekick", "dropkick", "dupeall", "inventorydupe"), Argument("hand/all")) {

    override fun dispatch(): String {
        if (Globals.mc.isSingleplayer) return "You are in singleplayer."
        val dupeModeArgument = getArgument("hand/all")?.value?.toLowerCase() ?: return "Invalid values."

        return when (dupeModeArgument) {
            "hand" -> {
                return if (doHand()) "Duping..."
                else "Put an item in your hand."
            }
            "hend" -> {
                return if (doHand()) "Duping..."
                else "Put an item in your hand."
            }
            "all" -> {
                doInventory()
                return "Dropping and kicking."
            }
            "inventory" -> {
                doInventory()
                return "Dropping and kicking."
            }
            else -> "Dupes (2): hand, all"
        }
    }

    private fun doHand(): Boolean {
        if (Globals.mc.player.getHeldItem(EnumHand.MAIN_HAND).item !is ItemAir) {
            Globals.mc.player.dropItem(true)
            Globals.mc.world.sendQuittingDisconnectingPacket()
            return true
        }
        return false
    }

    private fun doInventory(): Boolean {
        for (i in 9 until 45) {
            Globals.mc.playerController.windowClick(0, i, 1, ClickType.THROW, Globals.mc.player)
        }
        Globals.mc.world.sendQuittingDisconnectingPacket()
        return true
    }

}