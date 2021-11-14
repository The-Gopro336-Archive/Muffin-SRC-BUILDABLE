package me.han.muffin.client.module.modules.hidden

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalHotbarManager.serverSideItem
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.entity.WeaponUtils
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.ContainerHopper
import net.minecraft.item.ItemAir
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object Pull32k: Module("Pull32k", Category.HIDDEN, true) {

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        var foundAir = false
        var enchantedSwordIndex = -1

        for (i in 0 until 9) {
            val hotbarStack = Globals.mc.player.inventory.mainInventory[i]
            if (WeaponUtils.isSuperWeapon(hotbarStack)) {
                enchantedSwordIndex = i
            }
        }

        if (enchantedSwordIndex != -1 && LocalHotbarManager.serverSideHotbar != enchantedSwordIndex) {
            InventoryUtils.swapSlot(enchantedSwordIndex)
            return
        }

        val openContainer = Globals.mc.player.openContainer ?: return
        if (openContainer !is ContainerHopper || openContainer.inventorySlots.isEmpty()) return

        for (i in 0 until 5) {
            val hopperStack = openContainer.inventorySlots[0].inventory.getStackInSlot(i)
            if (hopperStack.isEmpty || !WeaponUtils.isSuperWeapon(hopperStack)) continue
            enchantedSwordIndex = i
            break
        }

        if (enchantedSwordIndex == -1) {
            return
        }

        for (i in 0 until 9) {
            val itemStack = Globals.mc.player.inventory.mainInventory[i]
            if (itemStack.item !is ItemAir) continue

            if (LocalHotbarManager.serverSideHotbar != i) InventoryUtils.swapSlot(i)

            foundAir = true
            break
        }

        if (foundAir || !WeaponUtils.isSuperWeapon(Globals.mc.player.serverSideItem)) {
            Globals.mc.playerController.windowClick(openContainer.windowId, enchantedSwordIndex, LocalHotbarManager.serverSideHotbar, ClickType.SWAP, Globals.mc.player)
        }

    }


}