package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import net.minecraft.network.play.server.SPacketTeams
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.concurrent.thread

/**
 * @author han
 * Allow you to see who joining or leaving the 2b2t queue.
 */
object BBTTQueueManager {
    private val playerMap = HashMap<String, Long>().synchronized()
    private var newThread: Thread? = null

    fun init() {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    fun close() {
        Muffin.getInstance().eventManager.removeEventListener(this)
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || !isOnRightServer() || event.packet !is SPacketTeams) return

        if (newThread == null || !newThread!!.isAlive || newThread!!.isInterrupted) {
            newThread = thread {
                doLogging(event.packet)
            }
        }

    }

    private fun doLogging(packet: SPacketTeams) {
        val joinedPlayers = packet.players
        val name = joinedPlayers.firstOrNull() ?: return

        when (packet.action) {
            3 -> {
                playerMap[name] = System.currentTimeMillis()
                ChatManager.sendDeleteMessage("Player $name has joined the queue.", name, ChatIDs.QUEUEPEEK)
            }
            4 -> {
                val startTime = playerMap.remove(name) ?: return

                val timeWaited = System.currentTimeMillis() - startTime

                val seconds = timeWaited / 1000 % 60
                val minutes = timeWaited / 1000 / 60 % 60
                val hours = timeWaited / 1000 / 3600

                val timePlaceholder = "%dh %dm %ds".format(hours, minutes, seconds)

                ChatManager.sendDeleteMessage("Player $name left the queue. They waited for $timePlaceholder.", name, ChatIDs.QUEUEPEEK)
            }
        }
    }

    private fun isOnRightServer(): Boolean = InfoUtils.getServerIP()?.toLowerCase().equals("2b2t.org") && Globals.mc.player != null && Globals.mc.player.isSpectator

}