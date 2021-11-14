package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.entity.TotemPopEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.world.WorldClientInitEvent
import me.han.muffin.client.manager.managers.ItemManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.WeaponUtils
import me.han.muffin.client.utils.extensions.mc.entity.realHealth
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.SPacketDisconnect
import net.minecraft.util.text.TextComponentString
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

/**
 * @author han
 * Created by han on 29/6/2020
 */
internal object AutoLogModule : Module("AutoLog", Category.PLAYER, "Automatically log when in danger or on low health.") {

    private val logDisable = Value(true, "LogDisable")
    private val superWeapon = Value(true, "32k")
    private val superRange = NumberValue({ superWeapon.value },6F, 1F, 15F, 1F, "32kRange")

    private val totemCount = NumberValue(0, -1, 12, 1, "TotemCount")
    private val health = NumberValue(3F, 0F, 30F, 1F, "HealthValue")
    private val popCount = NumberValue(2, 0, 15, 1, "PopCount")

    private var popCounter = 0

    init {
        addSettings(logDisable, superWeapon, superRange, totemCount, health, popCount)
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck() || !canLog()) return

        if (superWeapon.value) {
            for (entity in Globals.mc.world.playerEntities) {
                if (entity !is EntityPlayer || EntityUtil.isntValid(entity, superRange.value.toDouble()) || !WeaponUtils.isSuperWeapon(entity.heldItemMainhand)) continue
                disconnectPlayer("ez logged due to 32k")
                break
            }
        }

        if (health.value > 0 && Globals.mc.player.realHealth <= health.value) {
            disconnectPlayer("ez logged due to low health")
        }

    }

    @Listener
    private fun onTotemPop(event: TotemPopEvent) {
        if (popCount.value == 0 || event.entity != Globals.mc.player || canLog()) return

        if (++popCounter > popCount.value) {
            disconnectPlayer("ez logged due to popping totem")
            popCounter = 0
        }
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        if (logDisable.value) disable()
    }

    @Listener
    private fun onWorldClientInit(event: WorldClientInitEvent) {
        popCounter = 0
    }

    private fun canLog(): Boolean {
        if (totemCount.value == -1) return true
        return ItemManager.totemStack.count < totemCount.value
    }

    private fun disconnectPlayer(reason: String) {
        Globals.mc.player.connection.handleDisconnect(SPacketDisconnect(TextComponentString(reason)))
    }

}