package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command

object VanishCommand: Command(arrayOf("vanish"), Argument("dismount/remount")) {

    override fun dispatch(): String {
        TODO("Not yet implemented")
    }

}