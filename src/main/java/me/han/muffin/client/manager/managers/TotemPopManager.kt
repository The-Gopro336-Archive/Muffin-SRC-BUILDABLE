package me.han.muffin.client.manager.managers

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.entity.TotemPopEvent
import me.han.muffin.client.event.events.entity.player.PlayerDeathEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.world.WorldEntityEvent
import me.han.muffin.client.event.events.world.WorldEvent
import me.han.muffin.client.module.modules.misc.NotificationsModule
import me.han.muffin.client.utils.ChatIDs
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

object TotemPopManager {
    private val playerPopMap = WeakHashMap<EntityPlayer, Int>()

    fun initListener() {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onDeath(event: PlayerDeathEvent) {
        if (!NotificationsModule.isEnabled || !NotificationsModule.totem.value) return

        if (event.player == Globals.mc.player) {
            playerPopMap.clear()
            return
        }

        val player = event.player
        if (selfCheck(player.name) && playerPopMap.containsKey(player)) {
            val formattedName = formatName(player.name)
            ChatManager.sendDeleteMessage(formattedName + " died after popping " + ChatFormatting.RED + getTotemPops(player) + ChatManager.darkTextColour + " totem" + (if (getTotemPops(player) == 1) "" else "s") + ".", formattedName, ChatIDs.TOTEM_POPS)
            playerPopMap.remove(player)
        }
    }

    @Listener
    private fun onTotemPop(event: TotemPopEvent) {
        if (event.entity !is EntityPlayer) return
        val entity = event.entity

        if (!NotificationsModule.isEnabled || !NotificationsModule.totem.value) return

        val name = entity.name
        val formattedName = formatName(name)

        if (playerPopMap[entity] == null) {
            playerPopMap[entity] = 1
            val msg = formattedName + " had popped " + ChatFormatting.GREEN + 1 + ChatManager.darkTextColour + " totem" + ChatManager.textColour + " currently."
            ChatManager.sendDeleteMessage(msg, formattedName, ChatIDs.TOTEM_POPS)
        } else if (playerPopMap[entity] != null) {
            var popCounter = getTotemPops(entity)
            popCounter++
            playerPopMap[entity] = popCounter
            val msg = formattedName + " had popped " + ChatFormatting.GREEN + popCounter + ChatManager.darkTextColour + " totem" + (if (popCounter == 1) "" else "s") + ChatManager.textColour + " currently."
            ChatManager.sendDeleteMessage(msg, formattedName, ChatIDs.TOTEM_POPS)
        }

    }

    @Listener
    private fun onWorldEntityRemove(event: WorldEntityEvent.Remove) {
        if (event.entity == null) return
        if (!NotificationsModule.isEnabled || !NotificationsModule.resetLeavingVisual.value) return
        val entity = event.entity ?: return
        val entityName = entity.name
        if (playerPopMap.containsKey(entity)) playerPopMap.clear()
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        playerPopMap.clear()
    }

    @Listener
    private fun onWorldUnload(event: WorldEvent.Unload) {
        playerPopMap.clear()
    }

    private fun friendCheck(name: String): Boolean {
        if (FriendManager.isFriend(name)) return true
        return true
    }

    private fun selfCheck(name: String): Boolean {
        return !name.equals(Globals.mc.player.name, ignoreCase = true)
    }

    private fun isSelf(name: String): Boolean {
        return name.equals(Globals.mc.player.name, ignoreCase = true)
    }

    private fun formatName(paramName: String): String {
        val isSelf = isSelf(paramName)
        val isFriend = FriendManager.isFriend(paramName)

        var extraText = if (isFriend) "Your friend " else ""
        val placeholderName = if (isSelf) "You".also { extraText = "" } else paramName

        return ChatManager.textColour + extraText + ChatManager.darkTextColour + placeholderName + ChatManager.textColour
    }

    private fun grammar(name: String): String {
        return if (isSelf(name)) "my" else "their"
    }

    fun getTotemPops(player: Entity): Int {
        return if (!NotificationsModule.isEnabled || !NotificationsModule.totem.value) 0 else playerPopMap[player] ?: return 0
    }

    fun getTotemPopString(player: Entity): String {
        return ChatFormatting.RED.toString() + if (getTotemPops(player) <= 0) "" else "-" + getTotemPops(player) + " "
    }

}