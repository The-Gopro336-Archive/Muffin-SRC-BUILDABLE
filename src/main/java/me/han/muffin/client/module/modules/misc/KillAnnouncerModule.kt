package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.ClientChatReceiveEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.entity.player.PlayerDeathEvent
import me.han.muffin.client.event.events.network.PlayerConnectEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.client.ChatUtils
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.StringValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketChatMessage
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object KillAnnouncerModule: Module("KillAnnouncer", Category.MISC, "Automatically announce after you kill an enemy.") {
    private val detectMethod = EnumValue(DetectMethod.Death, "DetectMethod")

    private val detectLog = Value(true, "DetectLog")
    private val onLog = Value({ detectLog.value },false, "OnLog")
    private val onReJoin = Value({ detectLog.value },false, "OnRejoin")
    private val removeTime = NumberValue({ detectLog.value && onReJoin.value}, 3, 1, 10, 1, "RemoveTime")

    private val tabCheck = Value(false, "TabCheck")
    private val toxicMode = Value(false, "ToxicMode")
    private val peaceMode = Value(true, "PeaceMode")
    private val clientName = Value(false, "ClientName")
    private val customize = Value(false, "Custom")
    private val customString = StringValue("GG.", "CustomValue")
    private val customLogString = StringValue("why you log", "CustomLog")
    private val timeoutTicks = NumberValue(20, 1, 50, 1, "TimeoutTicks")

    private val loggedPlayer = LinkedHashMap<String, Timer>()
    private val attackedPlayers = LinkedHashMap<EntityPlayer, Int>() // <Player, Last Attack Time>

    private val lockObject = Any()

    init {
        addSettings(detectMethod, detectLog, onLog, onReJoin, removeTime, tabCheck, toxicMode, peaceMode, clientName, customize, customString, customLogString, timeoutTicks)
    }

    override fun onDisable() {
        loggedPlayer.clear()
    }

    private enum class DetectMethod {
        Broadcast, Death
    }

    @Listener
    private fun onClientChatReceive(event: ClientChatReceiveEvent) {
        if (detectMethod.value != DetectMethod.Broadcast || Globals.mc.player == null || Globals.mc.player.isDead || Globals.mc.player.health <= 0.0f) return

        val message = event.packet.chatComponent.unformattedText

        if (!message.contains(Globals.mc.player.name, true)) return

        for (player in attackedPlayers.keys) {
            if (!message.contains(player.name, true)) continue
            sendEzMessage(player)
            break // Break right after removing so we don't get exception
        }
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (fullNullCheck()) return

        if (!Globals.mc.player.isAlive) {
            attackedPlayers.clear()
            loggedPlayer.clear()
            return
        }

        /*
        if (detectLog.value) {
            for (player in attackedPlayers.keys) {
                if (Globals.mc.connection?.getPlayerInfo(player.name) != null) continue
                sendLogMessage(player)
                break
            }
        }
         */

        if (detectLog.value && onReJoin.value) {
            loggedPlayer.values.removeIf { it.passedSeconds(removeTime.value.times(60)) }
        }

        // Update attacked Entity
        val attacked = Globals.mc.player.lastAttackedEntity
        if (attacked is EntityPlayer && attacked != Globals.mc.player && attacked.isAlive) {
            attackedPlayers[attacked] = Globals.mc.player.lastAttackedEntityTime
        }


        // Remove players if they are out of world or we haven't attack them again in 300 ticks (5 seconds)
        attackedPlayers.entries.removeIf { !it.key.isAddedToWorld || Globals.mc.player.ticksExisted - it.value > 100 }
    }

    @Listener
    private fun onEntityDeath(event: PlayerDeathEvent) {
        // Check death
        if (detectMethod.value == DetectMethod.Death) {
            for (player in attackedPlayers.keys) {
                if (!player.isDead && player.health > 0.0f) continue
                if (tabCheck.value && Globals.mc.connection?.getPlayerInfo(player.name) == null) continue
                if (player != event.player) continue
                sendEzMessage(player)
                break // Break right after removing so we don't get exception
            }
        }
    }

    @Listener
    private fun onPlayerJoin(event: PlayerConnectEvent.Join) {
        if (detectLog.value && onReJoin.value) {
            if (tabCheck.value && Globals.mc.connection?.getPlayerInfo(event.username) == null) return
            val removePlayer = loggedPlayer.remove(event.username)
            if (removePlayer != null) {
                sendLogMessage(event.username)
            }
        }
    }

    @Listener
    private fun onPlayerLeave(event: PlayerConnectEvent.Leave) {
        if (detectLog.value && onLog.value) {
            for (player in attackedPlayers.keys) {
                if (player.name != event.username || player.uniqueID != event.uuid) continue
                val playerName = player.name

                if (onReJoin.value) loggedPlayer[playerName] = Timer()
                sendLogMessage(playerName)
                break
            }
        }
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        synchronized(lockObject) {
            attackedPlayers.clear()
            loggedPlayer.clear()
        }
    }

    private fun sendEzMessage(player: EntityPlayer) {
        val message = StringBuilder()

        if (customize.value) {
            message.append(" ").append(customString.value).append(" ")
        }

        if (peaceMode.value) {
            message.append(" gg ")
        }

        if (toxicMode.value) {
            message.append(" You just got ez'ed by Muffin ")
        }

        if (clientName.value) {
            message.append(" Muffin is really good! ")
        }

        message.append(player.name)

        var messageSanitized = message.toString().replace(ChatUtils.SECTIONSIGN.toRegex(), "")
        if (messageSanitized.length > 255) {
            messageSanitized = messageSanitized.substring(0, 255)
        }

        if (ChatTweaksModule.isEnabled && ChatTweaksModule.suffixMode.value != ChatTweaksModule.SuffixMode.Off) {
            messageSanitized += when (ChatTweaksModule.suffixMode.value) {
                ChatTweaksModule.SuffixMode.Off -> ""
                ChatTweaksModule.SuffixMode.Oldie -> ChatUtils.CHAT_SUFFIX_OLD
                ChatTweaksModule.SuffixMode.New -> ChatUtils.CHAT_SUFFIX_NEW
                else -> ""
            }
        }

        Globals.mc.player.connection?.sendPacket(CPacketChatMessage(messageSanitized))
        attackedPlayers.remove(player)
    }


    private fun sendLogMessage(name: String) {
        val message = StringBuilder()

        message.append(customLogString.value).append(" ").append(name)

        var messageSanitized = message.toString().replace(ChatUtils.SECTIONSIGN.toRegex(), "")
        if (messageSanitized.length > 255) {
            messageSanitized = messageSanitized.substring(0, 255)
        }

        if (ChatTweaksModule.isEnabled && ChatTweaksModule.suffixMode.value != ChatTweaksModule.SuffixMode.Off)
            messageSanitized += when (ChatTweaksModule.suffixMode.value) {
                ChatTweaksModule.SuffixMode.Off -> ""
                ChatTweaksModule.SuffixMode.Oldie -> ChatUtils.CHAT_SUFFIX_OLD
                ChatTweaksModule.SuffixMode.New -> ChatUtils.CHAT_SUFFIX_NEW
                else -> ""
            }

        Globals.mc.player.connection?.sendPacket(CPacketChatMessage(messageSanitized))
        loggedPlayer.remove(name)
    }

}