package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.altmanager.YggdrasilPayload
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.utils.extensions.mixin.misc.mcSession
import net.minecraft.util.Session
import kotlin.concurrent.thread

object NonPremiumLoginCommand: Command(arrayOf("lc", "loginc", "logincrack", "cracklogin"), Argument("name")) {
    override fun dispatch(): String {
        val username = getArgument("name")?.value ?: return "Invalid name."
        YggdrasilPayload.loginOffline(username)
        return "Login as ${Globals.mc.session.username} as crack account."
    }
}

object PremiumLoginCommand: Command(arrayOf("lp", "loginp", "loginpremium", "premiumlogin"), Argument("name"), Argument("password")) {

    override fun dispatch(): String {
        val username = getArgument("name")?.value ?: return "Invalid username."
        val password = getArgument("password")?.value ?: return "Invalid password."

        var messages = "Failed to login with name $username as premium account."

        thread {
            val result = YggdrasilPayload.login(username, password)
            if (result == 0) messages = "Login as ${Globals.mc.session.username} as premium account."
            ChatManager.sendMessage(messages)
        }

        return "Logging in..."
    }
}

object FirstAccountCommand: Command(arrayOf("lf", "loginf", "loginfirst", "firstlogin")) {
    var firstSession: Session? = null
    override fun dispatch(): String {
        if (firstSession == null) return "Invalid."
        Globals.mc.mcSession = firstSession!!
        return "Login back as ${Globals.mc.session.username}."
    }
}