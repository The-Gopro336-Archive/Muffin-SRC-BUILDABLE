package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.math.MathUtils

object HClipCommand: Command(arrayOf("hclip", "hc", "h"), Argument("blocks")) {

    override fun dispatch(): String {
        val blocks = getArgument("blocks")?.value?.toDouble() ?: return "Invalid values."

        val direction = MathUtils.direction(Globals.mc.player.rotationYaw)
        val entity = (if (Globals.mc.player.ridingEntity != null) Globals.mc.player.ridingEntity else Globals.mc.player) ?: return "This should not happen."

        entity.setPosition(entity.posX + (1.0F * blocks * direction.x + 0.0F * blocks * direction.y), entity.posY, entity.posZ + (1.0F * blocks * direction.y - 0.0F * blocks * direction.x))
        return "Teleported %s &e%s&7 block(s).".format(if (blocks < 0) "back" else "forward", blocks)
    }

}