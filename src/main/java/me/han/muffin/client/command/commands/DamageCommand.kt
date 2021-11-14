package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Command
import me.han.muffin.client.utils.entity.PlayerUtil

object DamageCommand: Command(arrayOf("damage", "dmg", "td")) {

    override fun dispatch(): String {
        PlayerUtil.damageHypixel()
        return "Damaged."
    }

}