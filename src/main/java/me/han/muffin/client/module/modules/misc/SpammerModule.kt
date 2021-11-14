package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.world.WorldClientInitEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.FileUtils
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.StringValue
import me.han.muffin.client.value.Value
import net.minecraft.network.play.client.CPacketChatMessage
import net.minecraft.util.StringUtils
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object SpammerModule: Module("Spammer", Category.MISC, "Spam da chat.") {
    private val mode = EnumValue(Mode.File, "Mode")
    private val type = EnumValue(Type.Private, "Type")
    private val spamTarget = StringValue("Meow_Nightnight", "SpamTarget")

    private val greenText = Value({ mode.value == Mode.File },false, "GreenText")
    private val random = Value({ mode.value == Mode.File },false, "Random")
    private val loop = Value(true, "Loop")

    // private val delayType = EnumValue(DelayType.Millisecond, "DelayType")
    private val delay = NumberValue(1.5F, 0.0F, 10.0F, 0.1F, "Delay")


    private val loadFile = Value(false, "LoadFile")

    private val spamMessages = ArrayList<String>()
    private const val defaultMessage = "i like big cock"

    private const val Muffin_On_Top = "muffin on top"

    private val fileName = "${Muffin.getInstance().directory.absolutePath}/spammer.txt"

    private val timer = Timer()
    private var sentPlayers = ArrayList<String>()

    private enum class Mode {
        File, Muffin
    }

    private enum class Type {
        All, Chat, Private
    }

    private enum class DelayType {
        Millisecond, Tick, Second
    }

    init {
        addSettings(mode, type, spamTarget, greenText, random, loop, delay, loadFile)
    }

    override fun onEnable() {
        if (fullNullCheck()) return
        readSpamFile()
    }

    override fun onDisable() {
        spamMessages.clear()
        timer.reset()
    }

    @Listener
    private fun onWorldInit(event: WorldClientInitEvent) {
        disable()
    }

    @Listener
    private fun onConnect(event: ServerEvent.Connect) {
        disable()
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        disable()
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (loadFile.value) {
            readSpamFile()
            loadFile.value = false
        }

        if (!timer.passed(delay.value * 1000.0)) return

        if (mode.value == Mode.Muffin) {
            var msg = Muffin_On_Top

            when (type.value) {
                Type.Private -> {
                    msg = "/msg " + spamTarget.value + msg
                }
                Type.Chat -> {
                    var spamTarget = "BigCock"
                    val connection = Globals.mc.connection ?: return
                    val playerInfoMap = connection.playerInfoMap ?: return

                    for (info in playerInfoMap) {
                        if (info == null || info.displayName == null) continue
                        val name = StringUtils.stripControlCodes(info.displayName!!.formattedText)
                        if (name == Globals.mc.player.name || FriendManager.isFriend(name) || sentPlayers.contains(name)) continue
                        spamTarget = name
                        sentPlayers.add(name)
                        break
                    }

                    if (spamTarget == "BigCock") {
                        sentPlayers.clear()
                        return
                    }

                    msg = "/msg $spamTarget $msg"
                }
                Type.All -> {
                }
            }

            Globals.mc.player.sendChatMessage(msg)
        } else if (spamMessages.isNotEmpty()) {

            var messageOut: String

            if (random.value) {
                val index = RandomUtils.random.nextInt(spamMessages.size)
                messageOut = spamMessages[index]
                spamMessages.removeAt(index)
            } else {
                messageOut = spamMessages[0]
                spamMessages.removeAt(0)
            }

            spamMessages.add(messageOut)
            if (greenText.value) messageOut = "> $messageOut"

            Globals.mc.player.connection.sendPacket(CPacketChatMessage(messageOut.replace("\u00a7".toRegex(), "")))
        }

        timer.reset()
    }

    private fun readSpamFile() {
        val fileInput = FileUtils.readTextFileAllLines(fileName)
        if (fileInput.isEmpty()) return

        spamMessages.clear()
        for (message in fileInput) {
            if (message.replace("\\s".toRegex(), "").isEmpty()) continue
            spamMessages.add(message)
        }

        if (spamMessages.isEmpty()) spamMessages.add(defaultMessage)
    }

}