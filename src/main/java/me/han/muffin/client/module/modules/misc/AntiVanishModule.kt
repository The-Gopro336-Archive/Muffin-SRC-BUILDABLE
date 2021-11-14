package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.timer.Timer
import net.minecraft.network.play.server.SPacketPlayerListItem
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import kotlin.concurrent.thread

internal object AntiVanishModule: Module("AntiVanish", Category.MISC, "Shows in chat when a person is in /vanish") {
    private val vanishedList = ArrayList<UUID>()
    private val vanishedTimer = Timer()

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || event.packet !is SPacketPlayerListItem) return

        if (event.packet.action == SPacketPlayerListItem.Action.UPDATE_LATENCY) {
            for (data in event.packet.entries) {
                if (Globals.mc.connection?.getPlayerInfo(data.profile.id) == null && !checkList(data.profile.id) && vanishedTimer.passedSeconds(1)) {
                    thread {
                        val name = EntityUtil.getNameFromUUID(data.profile.id.toString()) ?: return@thread
                        ChatManager.sendDeleteMessage("$name has gone into vanish.", name, ChatIDs.ANTI_VANISH)
                    }
                    vanishedTimer.reset()
                }
            }
        }

    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        vanishedList.removeIf { uuid ->
            if (Globals.mc.connection?.getPlayerInfo(uuid) != null) {
                if (vanishedTimer.passedSeconds(1)) {
                    thread {
                        val name = EntityUtil.getNameFromUUID(uuid.toString()) ?: return@thread
                        ChatManager.sendDeleteMessage("$name has no longer vanish.", name, ChatIDs.ANTI_VANISH)
                    }
                    vanishedTimer.reset()
                }
                true
            } else {
                false
            }
        }

    }

    @Listener
    private fun onLogout(event: ServerEvent.Disconnect) {
        if (event.state != EventStageable.EventStage.PRE) return
        vanishedList.clear()
    }

    private fun checkList(uuid: UUID) =
        if (vanishedList.contains(uuid)) {
            vanishedList.remove(uuid)
            true
        } else {
            vanishedList.add(uuid)
            false
        }

}