package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.TimeoutFlag
import me.han.muffin.client.utils.extensions.kotlin.firstEntryOrNull
import me.han.muffin.client.utils.extensions.kotlin.firstKeyOrNull
import me.han.muffin.client.utils.extensions.kotlin.firstValue
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.item.HotbarSlot
import me.han.muffin.client.utils.extensions.mixin.entity.currentPlayerItem
import me.han.muffin.client.utils.timer.TickTimer
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketHeldItemChange
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*

object LocalHotbarManager {
    // <Module, <Slot, <Reset Time>
    private val spoofingModule = TreeMap<Module, TimeoutFlag<Int>>(compareByDescending { it.modulePriority }).synchronized()
    private val hotbarTimer = TickTimer()

    var serverSideHotbar = 0; private set
    var swapTime = 0L; private set

    val EntityPlayerSP.serverSideItem: ItemStack
        get() = inventory.mainInventory[serverSideHotbar]

    init {
        Muffin.getInstance().eventManager.addEventListener(this)
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE || !hotbarTimer.tick(250)) return

        val prevFirstKey = spoofingModule.firstKeyOrNull()
        trimMap()
        val newEntry = spoofingModule.firstEntryOrNull()

        if (spoofingModule.isEmpty()) {
            resetHotbarPacket()
        } else if (prevFirstKey != null && newEntry != null && prevFirstKey != newEntry.key) {
            sendHotbarPacket(newEntry.value.value)
        }
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                if (event.packet is CPacketHeldItemChange && spoofingModule.isNotEmpty() && event.packet.slotId != serverSideHotbar) {
                    event.cancel()
                }
            }
            EventStageable.EventStage.POST -> {
                if (event.isCanceled || event.packet !is CPacketHeldItemChange) return

                if (event.packet.slotId != serverSideHotbar) {
                    serverSideHotbar = event.packet.slotId
                    swapTime = System.currentTimeMillis()
                }
            }
        }
    }

    fun Module.spoofHotbar(slot: HotbarSlot, timeout: Long = 250L) {
        spoofHotbar(slot.hotbarSlot, timeout)
    }

    fun Module.spoofHotbar(slot: Int, timeout: Long = 250L) {
        if (slot in 0..8) {
            spoofingModule[this] = TimeoutFlag.relative(slot, timeout)
            if (spoofingModule.firstKeyOrNull() == this) {
                sendHotbarPacket(slot)
            }
        }
    }

    fun Module.resetHotbar() {
        val prevFirstKey = spoofingModule.firstKeyOrNull()

        spoofingModule.remove(this)

        if (spoofingModule.isEmpty()) {
            resetHotbarPacket()
        } else if (prevFirstKey != null && prevFirstKey == this) {
            spoofingModule.firstEntryOrNull()?.let { (_, flag) ->
                sendHotbarPacket(flag.value)
            }
        }
    }

    private fun trimMap() {
        while (spoofingModule.isNotEmpty() && spoofingModule.firstValue().timeout()) {
            spoofingModule.pollFirstEntry()
        }
    }

    private fun sendHotbarPacket(slot: Int) {
        if (serverSideHotbar != slot) {
            serverSideHotbar = slot
            swapTime = System.currentTimeMillis()
            Globals.mc.connection?.sendPacket(CPacketHeldItemChange(slot))
        }
    }

    private fun resetHotbarPacket() {
        sendHotbarPacket(Globals.mc.playerController?.currentPlayerItem ?: 0)
    }

}