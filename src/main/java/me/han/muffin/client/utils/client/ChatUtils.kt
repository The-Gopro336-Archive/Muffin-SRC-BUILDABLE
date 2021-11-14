package me.han.muffin.client.utils.client

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.utils.math.RandomUtils
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentBase
import java.util.regex.Pattern

object ChatUtils {
    private val plainColour get() = ChatManager.textColour
    private val darkColour get() = ChatManager.darkTextColour
    val PREFIX get() = darkColour + "[" + plainColour + Muffin.MODNAME + darkColour + "]" + plainColour + " "

    const val CHAT_SUFFIX_OLD = " \u2665 \u1D0D\u1D1C\uA730\uA730\u026A\u0274"
    const val CHAT_SUFFIX_NEW = " \u30C4 \u2C98\u03C5\u2A0D\u2A0D\u2AEF\uFB21"
    const val SECTIONSIGN = "\u00a7"

    fun sendMessage(message: String) {
        Globals.mc.ingameGUI?.chatGUI?.printChatMessage(ChatMessage(PREFIX + message))
    }

    fun sendMessage(message: String, id: Int) {
        Globals.mc.ingameGUI?.chatGUI?.printChatMessageWithOptionalDeletion(ChatMessage(PREFIX + message), id)
    }

    fun deleteMessage(id: Int) {
        Globals.mc.ingameGUI?.chatGUI?.deleteChatLine(id)
    }

    fun sendComponent(component: ITextComponent, id: Int) {
        Globals.mc.ingameGUI?.chatGUI?.printChatMessageWithOptionalDeletion(component, id)
    }

    fun appendChatSuffix(message: String, suffix: String): String {
        return cropMaxLengthMessage(cropMaxLengthMessage(message, suffix.length) + suffix)
    }

    fun generateRandomHexSuffix(n: Int): String {
        return "[" + Integer.toHexString((RandomUtils.random.nextInt() + 11) * RandomUtils.random.nextInt()).substring(0, n) + ']'
    }

    fun cropMaxLengthMessage(messageParam: String, length: Int): String {
        return if (messageParam.length > 255 - length) messageParam.substring(0, 255 - length) else messageParam
    }

    fun cropMaxLengthMessage(s: String): String {
        return cropMaxLengthMessage(s, 0)
    }

    class ChatMessage(var text: String): TextComponentBase() {
        init {
            val p = Pattern.compile("&[0123456789abcdefrlonmk]")
            val m = p.matcher(text)
            val sb = StringBuffer()
            while (m.find()) {
                val replacement = "\u00A7" + m.group().substring(1)
                m.appendReplacement(sb, replacement)
            }
            m.appendTail(sb)
            this.text = sb.toString()
        }
        override fun getUnformattedComponentText(): String {
            return text
        }
        override fun createCopy(): ITextComponent {
            return ChatMessage(text)
        }
    }

}