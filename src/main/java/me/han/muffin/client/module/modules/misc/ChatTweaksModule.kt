package me.han.muffin.client.module.modules.misc

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.ModuleEvent
import me.han.muffin.client.event.events.gui.RemoveChatBoxEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.NotificationManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.client.ChatUtils
import me.han.muffin.client.utils.color.BetterChatFormatting
import me.han.muffin.client.utils.extensions.mixin.netty.packetMessage
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.ChatLine
import net.minecraft.network.play.client.CPacketChatMessage
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.ChatType
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.text.SimpleDateFormat
import java.util.*

internal object ChatTweaksModule : Module("ChatTweaks", Category.MISC, true, "Improve your chat.") {
    private val chatTimeStamps = Value(true, "TimeStamps")
    private val colorMode = EnumValue({ chatTimeStamps.value }, SpecialOrNormal.Special, "ColourMode")
    private val timeFormat = EnumValue({ chatTimeStamps.value },TimeFormat.TwelveWithAMPM, "TimeFormat")
    private val timestampSecs = Value({ chatTimeStamps.value },false, "WithSeconds")

    private val cleanChat = Value(true, "CleanChat")
    private val announceUsage = EnumValue(MessageMode.Chat, "AnnounceUsage")

    val suffixMode = EnumValue(SuffixMode.Off, "SuffixMode")
    private val infiniteLength = Value(true, "InfiniteLength")

    private val antiEz = Value(false, "AntiEz")
    private val antiDiscord = Value(false, "AntiDiscord")
    private val antiBlyat = Value(false, "AntiBlyat")

    val selfChatHighlight = Value(true, "SelfChatHighlight")
    private val selfChatColour = EnumValue({ selfChatHighlight.value }, BetterChatFormatting.DARK_PURPLE, "SelfChatColour")

    init {
        addSettings(
            chatTimeStamps, colorMode, timeFormat, timestampSecs,
            cleanChat,
            announceUsage,
            suffixMode,
            infiniteLength,
            antiEz, antiDiscord, antiBlyat,
            selfChatHighlight, selfChatColour
        )
    }

    private enum class MessageMode {
        Off, Chat, Notification
    }

    enum class SuffixMode {
        Off, Oldie, New
    }

    enum class TimeFormat {
        Twelve, TwelveWithAMPM, TwentyFour
    }

    enum class SpecialOrNormal {
        Normal, Special
    }

    @JvmStatic val isTimeStampEnabled: Boolean get() = isEnabled && chatTimeStamps.value

    fun getFormattedChatName(name: String): String {
        val prefixColour = selfChatColour.value.mcFormat.toString()
        return prefixColour + name + ChatFormatting.RESET.toString()
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (suffixMode.value != SuffixMode.Off) {
            if (event.packet !is CPacketChatMessage) return

            var message = event.packet.message
            if (message.startsWith("/")) return

            if (!Character.isLetter(message[0]) && !Character.isDigit(message[0])) {
                if (!(message[0] == '>' || message[0] == '^')) {
                    return
                }
            }

            message = when (suffixMode.value) {
                SuffixMode.Off -> ""
                SuffixMode.Oldie -> ChatUtils.appendChatSuffix(message, ChatUtils.CHAT_SUFFIX_OLD)
                SuffixMode.New -> ChatUtils.appendChatSuffix(message, ChatUtils.CHAT_SUFFIX_NEW)
            }

            event.packet.packetMessage = message.replace(ChatUtils.SECTIONSIGN.toRegex(), "")
        }

    }


    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is SPacketChat && event.packet.chatComponent is TextComponentString && event.packet.type != ChatType.GAME_INFO) {
            val formattedText = event.packet.chatComponent.formattedText

            if (formattedText.contains("> ")) {
                val text = formattedText.substring(formattedText.indexOf("> ")).toLowerCase()
                if (text.contains("ez") && antiEz.value) event.cancel()
                if (antiDiscord.value && text.contains("discord")) event.cancel()
                if (antiBlyat.value) for (element in text) if (Character.UnicodeBlock.of(element) == Character.UnicodeBlock.CYRILLIC) event.cancel()
            }

        }

    }

    @Listener
    private fun onModule(event: ModuleEvent) {
        if (fullNullCheck() || announceUsage.value == MessageMode.Off) return

        val module = event.module
        if (module.isDrawn || module.category == Category.HIDDEN || module.category == Category.OTHERS) return

        val normalColor = ChatManager.textColour
        val darkColor = ChatManager.darkTextColour

        if (event.type == ModuleEvent.Type.Enable) {
            if (announceUsage.value == MessageMode.Notification)
                NotificationManager.addNotification(NotificationManager.NotificationType.Success, "", darkColor + module.name + normalColor + " was" + ChatFormatting.GREEN + " enabled.")
            else
                ChatManager.sendDeleteMessage(darkColor + module.name + normalColor + " was" + ChatFormatting.GREEN + " enabled.", module.name, ChatIDs.MODULE)
        }

        if (event.type == ModuleEvent.Type.Disable) {
            if (announceUsage.value == MessageMode.Notification)
                NotificationManager.addNotification(NotificationManager.NotificationType.Error, "", darkColor + module.name + normalColor + " was" + ChatFormatting.RED + " disabled.")
            else
                ChatManager.sendDeleteMessage(darkColor + module.name + normalColor + " was" + ChatFormatting.RED + " disabled.", module.name, ChatIDs.MODULE)
        }

    }

    @Listener
    private fun onRemoveChatBox(event: RemoveChatBoxEvent) {
        if (cleanChat.value) event.cancel()
    }

    @JvmStatic
    fun handleSetChatLine(drawnChatLines: MutableList<ChatLine>, chatLines: MutableList<ChatLine>, chatComponent: ITextComponent, chatLineId: Int, updateCounter: Int, displayOnly: Boolean, ci: CallbackInfo) {
        if (isDisabled || !infiniteLength.value) return

        while (drawnChatLines.isNotEmpty() && drawnChatLines.size > 5000) {
            drawnChatLines.removeLast()
        }

        if (!displayOnly) {
            chatLines.add(0, ChatLine(updateCounter, chatComponent, chatLineId))
            while (chatLines.isNotEmpty() && chatLines.size > 5000) {
                chatLines.removeLast()
            }
        }

        ci.cancel()
    }

    @JvmStatic
    fun getChatTimeStampsFormat(): String {
        val dateFormat = when (timeFormat.value) {
            TimeFormat.Twelve -> if (timestampSecs.value) "hh:mm:ss" else "hh:mm"
            TimeFormat.TwelveWithAMPM -> if (timestampSecs.value) "hh:mm:ss a" else "hh:mm a"
            TimeFormat.TwentyFour -> if (timestampSecs.value) "HH:mm:ss" else "HH:mm"
        }

        val time = SimpleDateFormat(dateFormat).format(Date())
        return when (colorMode.value) {
            SpecialOrNormal.Normal -> ChatManager.textColour + "<" + time + "> " + ChatFormatting.RESET
            SpecialOrNormal.Special -> ChatManager.darkTextColour + "<" + ChatManager.textColour + time + ChatManager.darkTextColour + ">" + ChatFormatting.RESET + " "
        }
    }

}