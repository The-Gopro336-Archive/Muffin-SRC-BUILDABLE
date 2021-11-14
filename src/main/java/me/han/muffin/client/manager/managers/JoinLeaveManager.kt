package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.PlayerConnectEvent
import me.han.muffin.client.utils.entity.EntityUtil
import net.minecraft.network.play.server.SPacketPlayerListItem
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object JoinLeaveManager {

    fun addListener() {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || EntityUtil.fullNullCheck()) return

        if (event.packet !is SPacketPlayerListItem) return

        if (SPacketPlayerListItem.Action.ADD_PLAYER != event.packet.action && SPacketPlayerListItem.Action.REMOVE_PLAYER != event.packet.action) {
            return
        }

        event.packet.entries
            .filter { !it.profile.name.isNullOrEmpty() || it.profile.id != null }
            .forEach { data ->
                val uuid = data.profile.id ?: return
                when (event.packet.action) {
                    SPacketPlayerListItem.Action.ADD_PLAYER -> {
                        Muffin.getInstance().eventManager.dispatchEvent(PlayerConnectEvent.Join(data.profile.name, uuid))
                    }
                    SPacketPlayerListItem.Action.REMOVE_PLAYER -> {
                        val player = Globals.mc.world.getPlayerEntityByUUID(uuid) ?: return@forEach
                        Muffin.getInstance().eventManager.dispatchEvent(PlayerConnectEvent.Leave(player.name, uuid))
                    }
                }
            }
    }

}