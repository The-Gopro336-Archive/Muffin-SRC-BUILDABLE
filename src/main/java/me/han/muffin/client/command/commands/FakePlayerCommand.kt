package me.han.muffin.client.command.commands

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.command.Command
import me.han.muffin.client.module.modules.hidden.FakePlayerModule

object FakePlayerCommand: Command(arrayOf("fakeplayer", "fp", "fplayer")) {

    override fun dispatch(): String {
        FakePlayerModule.toggle()

        return when {
            FakePlayerModule.isEnabled -> ChatFormatting.GREEN.toString() + "FakePlayer has turned ON"
            FakePlayerModule.isDisabled -> ChatFormatting.RED.toString() + "FakePlayer has turned OFF"
            else -> "Should not be here."
        }
    }

}