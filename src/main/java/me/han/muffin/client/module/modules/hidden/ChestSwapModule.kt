package me.han.muffin.client.module.modules.hidden

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.KeyPressedEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.AutoArmourModule
import me.han.muffin.client.module.modules.exploits.XCarryModule
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.client.BindUtils
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.moveToSlot
import net.minecraft.block.Block
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemElytra
import net.minecraft.item.ItemStack
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.function.Predicate

internal object ChestSwapModule: Module("ChestSwapper", Category.HIDDEN, true, true) {
    var shouldSwap = false
    var lastSwap = false

    @Listener
    private fun onKeyClicked(event: KeyPressedEvent) {
        if (fullNullCheck()) return
        if (BindUtils.checkIsClickedToggle(AutoArmourModule.chestSwap.value)) shouldSwap = !shouldSwap
    }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (shouldSwap && !lastSwap) {
            val slot = InventoryUtils.findArmourSlot(EntityEquipmentSlot.CHEST, bestArmour = true, curse = false, preferElytra = true, xCarry = XCarryModule.isEnabled, minDurability = 5)
            val chestSlot = Globals.mc.player.inventoryContainer.getSlot(6).stack
            if (chestSlot.isEmpty) {
                if (slot != -1) {
                    moveToSlot(slot, 6)
                }
                return
            }
            if (slot != -1) {
                moveToSlot(6, slot)
            }

            lastSwap = true
        }

        if (lastSwap && !shouldSwap) {
            val chestSlot = Globals.mc.player.inventoryContainer.getSlot(6).stack
            if (chestSlot.isEmpty) {
                val slot = InventoryUtils.findArmourSlot(EntityEquipmentSlot.CHEST, bestArmour = true, curse = false, preferElytra = false, xCarry = XCarryModule.isEnabled, minDurability = 5)
                if (slot != -1) {
                    moveToSlot(slot, 6)
                }
                return
            }
            val slot = InventoryUtils.findArmourSlot(EntityEquipmentSlot.CHEST, bestArmour = true, curse = false, preferElytra = chestSlot.item is ItemElytra, xCarry = XCarryModule.isEnabled, minDurability = 5)
            if (slot != -1) {
                moveToSlot(6, slot)
            }
            lastSwap = false
        }
    }

}