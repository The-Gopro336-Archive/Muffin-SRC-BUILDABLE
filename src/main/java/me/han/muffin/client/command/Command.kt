package me.han.muffin.client.command

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.utils.client.ChatUtils
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentBase
import net.minecraft.util.text.TextComponentString
import java.util.*
import java.util.regex.Pattern

abstract class Command(val aliases: Array<String>, vararg argument: Argument) {
    private val arguments = argument

    fun dispatch(input: Array<String>): String {
        var valid = false

        if (input.size < arguments.size) {
            return input[0] + " " + syntax
        } else if (input.size - 1 > arguments.size) {
            return ChatFormatting.GRAY.toString() + "Maximum number of arguments is " + ChatManager.textColour + arguments.size + ChatFormatting.GRAY
        }

        if (arguments.isNotEmpty()) {
            for (index in arguments.indices) {
                val argument = arguments[index].apply {
                    present = index < input.size
                    value = input[index + 1]
                }
                valid = argument.present
            }
        } else {
            valid = true
        }

        return if (valid) dispatch() ?: "Invalid argument(s)." else "Invalid argument(s)."
    }

    fun getArgument(label: String): Argument? {
        return arguments.firstOrNull { label.equals(it.name, ignoreCase = true) }
    }

    val syntax: String get() {
        return StringJoiner(" ").run {
            for (argument in arguments) add(ChatManager.darkTextColour + "[" + ChatManager.textColour + argument.name + ChatManager.darkTextColour + "]")
            toString()
        }
    }

    class ChatMessage constructor(var text: String): TextComponentBase() {
        override fun getUnformattedComponentText(): String {
            return text
        }

        override fun createCopy(): ITextComponent {
            return ChatMessage(text)
        }

        init {
            val pattern = Pattern.compile("&[0123456789abcdefrlonmk]")
            val m = pattern.matcher(text)

            val sb = StringBuffer()
            while (m.find()) {
                val replacement = "\u00A7" + m.group().substring(1)
                m.appendReplacement(sb, replacement)
            }
            m.appendTail(sb)
            this.text = sb.toString()
        }
    }

    abstract fun dispatch(): String?

    companion object {
        private var lastChatLine = 420

        @JvmStatic
        fun sendChatMessageWithDeletion(s: String) {
            if (Globals.mc.ingameGUI != null) {
                Globals.mc.ingameGUI.chatGUI.deleteChatLine(lastChatLine)
                Globals.mc.ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(TextComponentString("").appendText(ChatUtils.PREFIX.trim()).appendSibling(TextComponentString(ChatManager.textColour + " " + s)), lastChatLine++)
                if (lastChatLine > 420) lastChatLine = 420
            }
        }
    }


}