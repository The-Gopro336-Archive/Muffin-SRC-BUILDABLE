package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.item.*
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiEnchantment
import net.minecraft.client.gui.GuiMerchant
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.inventory.*
import net.minecraft.init.Items
import net.minecraft.item.ItemShulkerBox
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object ChestStealerModule : Module("ChestStealer", Category.PLAYER, "Automatically steal or store items from containers") {
    val mode = EnumValue(Mode.TOGGLE,"Mode")
    private val movingMode = EnumValue(MovingMode.QUICK_MOVE,"MovingMode")
    private val delay = NumberValue(250, 0, 1000, 25, "Delay")
    private val onlyShulkers = Value(false,"OnlyShulkers",)

    enum class Mode {
        ALWAYS, TOGGLE, MANUAL
    }

    private enum class MovingMode {
        QUICK_MOVE, PICKUP, THROW
    }

    private enum class ContainerMode(val offset: Int) {
        STEAL(36), STORE(0)
    }

    init {
        addSettings(mode, movingMode, delay, onlyShulkers)
    }

    var stealing = false
    var storing = false
    val timer = TickTimer()

    @Listener
    private fun onTicking(event: TickEvent) {
        stealing = if (isContainerOpen() && (stealing || mode.value == Mode.ALWAYS)) {
            stealOrStore(getStealingSlot(), ContainerMode.STEAL)
        } else {
            false
        }

        storing = if (isContainerOpen() && (storing || mode.value == Mode.ALWAYS)) {
            stealOrStore(getStoringSlot(), ContainerMode.STORE)
        } else {
            false
        }
    }

    private fun canSteal(): Boolean {
        return getStealingSlot() != null
    }

    private fun canStore(): Boolean {
        return getStoringSlot() != null
    }

    private fun isContainerOpen(): Boolean {
        return Globals.mc.player.openContainer != null && isValidGui()
    }

    fun isValidGui(): Boolean {
        return Globals.mc.currentScreen !is GuiEnchantment
            && Globals.mc.currentScreen !is GuiMerchant
            && Globals.mc.currentScreen !is GuiRepair
            && Globals.mc.currentScreen !is GuiBeacon
            && Globals.mc.currentScreen !is GuiCrafting
            && Globals.mc.currentScreen !is GuiContainerCreative
            && Globals.mc.currentScreen !is GuiInventory
    }

    @JvmStatic
    fun updateButton(button: GuiButton, left: Int, size: Int, top: Int) {
        if (fullNullCheck()) return
        if (isEnabled && isContainerOpen()) {
            if (button.id == 696969) {
                val str = if (stealing) {
                    "Stop"
                } else {
                    "Steal"
                }

                button.x = left + size + 2
                button.y = top + 2
                button.enabled = canSteal() and !storing
                button.visible = true
                button.displayString = str
            } else if (button.id == 420420) {
                val str = if (storing) {
                    "Stop"
                } else {
                    "Store"
                }

                button.x = left + size + 2
                button.y = top + 24
                button.enabled = canStore() and !stealing
                button.visible = true
                button.displayString = str
            }
        } else {
            button.visible = false
        }
    }

    private fun stealOrStore(slot: Int?, containerMode: ContainerMode): Boolean {
        if (slot == null) return false

        val size = getContainerSlotSize()
        val rangeStart = if (containerMode == ContainerMode.STEAL) size else 0
        val slotTo = Globals.mc.player.openContainer.getSlots(rangeStart until size + containerMode.offset).firstEmpty()
            ?: return false
        val windowID = Globals.mc.player.openContainer.windowId

        if (timer.tick(delay.value.toLong())) {
            when (movingMode.value) {
                MovingMode.QUICK_MOVE -> quickMoveSlot(windowID, slot)
                MovingMode.PICKUP -> moveToSlot(windowID, slot, slotTo.slotNumber)
                MovingMode.THROW -> throwAllInSlot(windowID, slot)
            }
        }

        return true
    }

    private fun getStealingSlot(): Int? {
        val container = Globals.mc.player.openContainer.inventory

        for (slot in 0 until getContainerSlotSize()) {
            val item = container[slot].item
            if (item == Items.AIR) continue
            if (!onlyShulkers.value || item is ItemShulkerBox) return slot
        }

        return null
    }

    private fun getStoringSlot(): Int? {
        val container = Globals.mc.player.openContainer.inventory
        val size = getContainerSlotSize()

        for (slot in size until size + 36) {
            val item = container[slot].item
            if (item == Items.AIR) continue
            if (!onlyShulkers.value || item is ItemShulkerBox) {
                return slot
            }
        }

        return null
    }

    private fun getContainerSlotSize(): Int {
        if (Globals.mc.currentScreen !is GuiContainer) return 0
        return Globals.mc.player.openContainer.inventorySlots.size - 36
    }

}