package me.han.muffin.client.utils.extensions.mc.item

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow

/**
 * Try to swap selected hotbar slot to [block] that matches with [predicateItem]
 *
 * Or move an item from storage slot to an empty slot or slot that matches [predicateSlot]
 * or slot 0 if none
 */
fun swapToBlockOrMove(
    block: Block,
    predicateItem: (ItemStack) -> Boolean = { true },
    predicateSlot: (ItemStack) -> Boolean = { true }
): Boolean {
    return if (swapToBlock(block, predicateItem)) {
        true
    } else {
        Minecraft.getMinecraft().player.storageSlots.firstBlock(block, predicateItem)?.let {
            moveToHotbar(it, predicateSlot)
            true
        } ?: false
    }
}

/**
 * Try to swap selected hotbar slot to [item] that matches with [predicateItem]
 *
 * Or move an item from storage slot to an empty slot or slot that matches [predicateSlot]
 * or slot 0 if none
 */
fun swapToItemOrMove(
    item: Item,
    predicateItem: (ItemStack) -> Boolean = { true },
    predicateSlot: (ItemStack) -> Boolean = { true }
): Boolean {
    return if (swapToItem(item, predicateItem)) {
        true
    } else {
        Minecraft.getMinecraft().player.storageSlots.firstItem(item, predicateItem)?.let {
            moveToHotbar(it, predicateSlot)
            true
        } ?: false
    }
}

/**
 * Try to swap selected hotbar slot to item with [itemID] that matches with [predicateItem]
 *
 * Or move an item from storage slot to an empty slot or slot that matches [predicateSlot]
 * or slot 0 if none
 */
fun swapToItemOrMove(
    itemID: Int,
    predicateItem: (ItemStack) -> Boolean = { true },
    predicateSlot: (ItemStack) -> Boolean = { true }
): Boolean {
    return if (swapToID(itemID, predicateItem)) {
        true
    } else {
        Minecraft.getMinecraft().player.storageSlots.firstID(itemID, predicateItem)?.let {
            moveToHotbar(it, predicateSlot)
            true
        } ?: false
    }
}


/**
 * Try to swap selected hotbar slot to [block] that matches with [predicate]
 */
fun swapToBlock(block: Block, predicate: (ItemStack) -> Boolean = { true }): Boolean {
    return Minecraft.getMinecraft().player.hotbarSlots.firstBlock(block, predicate)?.let {
        swapToSlot(it.hotbarSlot)
        true
    } ?: false
}


/**
 * Try to swap selected hotbar slot to [item] that matches with [predicate]
 */
fun swapToItem(item: Item, predicate: (ItemStack) -> Boolean = { true }): Boolean {
    return Minecraft.getMinecraft().player.hotbarSlots.firstItem(item, predicate)?.let {
        swapToSlot(it.hotbarSlot)
        true
    } ?: false
}

/**
 * Try to swap selected hotbar slot to item with [itemID] that matches with [predicate]
 */
fun swapToID(itemID: Int, predicate: (ItemStack) -> Boolean = { true }): Boolean {
    return Minecraft.getMinecraft().player.hotbarSlots.firstID(itemID, predicate)?.let {
        swapToSlot(it.hotbarSlot)
        true
    } ?: false
}

/**
 * Swap the selected hotbar slot to [hotbarSlot]
 */
fun swapToSlot(hotbarSlot: HotbarSlot) {
    swapToSlot(hotbarSlot.hotbarSlot)
}

/**
 * Swap the selected hotbar slot to [slot]
 */
fun swapToSlot(slot: Int) {
    if (slot !in 0..8) return
    Minecraft.getMinecraft().player.inventory.currentItem = slot
    Minecraft.getMinecraft().playerController.updateController()
}

/**
 * Swaps the item in [slotFrom] with the first empty hotbar slot
 * or matches with [predicate] or slot 0 if none of those found
 */
inline fun moveToHotbar(slotFrom: Slot, predicate: (ItemStack) -> Boolean): Short {
    return moveToHotbar(slotFrom.slotNumber, predicate)
}

/**
 * Swaps the item in [slotFrom] with the first empty hotbar slot
 * or matches with [predicate] or slot 0 if none of those found
 */
inline fun moveToHotbar(slotFrom: Int, predicate: (ItemStack) -> Boolean): Short {
    val hotbarSlots = Minecraft.getMinecraft().player.hotbarSlots
    val slotTo = hotbarSlots.firstItem(Items.AIR)?.hotbarSlot
        ?: hotbarSlots.firstByStack(predicate)?.hotbarSlot ?: 0

    return moveToHotbar(slotFrom, slotTo)
}

/**
 * Swaps the item in [slotFrom] with the hotbar slot [slotTo].
 */
fun moveToHotbar(slotFrom: Slot, slotTo: HotbarSlot): Short {
    return moveToHotbar(0, slotFrom, slotTo)
}

/**
 * Swaps the item in [slotFrom] with the hotbar slot [hotbarSlotTo].
 */
fun moveToHotbar(windowId: Int, slotFrom: Slot, hotbarSlotTo: HotbarSlot): Short {
    return moveToHotbar(windowId, slotFrom.slotNumber, hotbarSlotTo.hotbarSlot)
}

/**
 * Swaps the item in [slotFrom] with the hotbar slot [hotbarSlotTo].
 */
fun moveToHotbar(slotFrom: Int, hotbarSlotTo: Int): Short {
    return moveToHotbar(0, slotFrom, hotbarSlotTo)
}

/**
 * Swaps the item in [slotFrom] with the hotbar slot [hotbarSlotTo].
 */
fun moveToHotbar(windowId: Int, slotFrom: Int, hotbarSlotTo: Int): Short {
    // mouseButton is actually the hotbar
    swapToSlot(hotbarSlotTo)
    return clickSlot(windowId, slotFrom, hotbarSlotTo, type = ClickType.SWAP)
}

/**
 * Move the item in [slotFrom]  to [slotTo] in player inventory,
 * if [slotTo] contains an item, then move it to [slotFrom]
 */
fun moveToSlot(slotFrom: Slot, slotTo: Slot): ShortArray {
    return moveToSlot(0, slotFrom.slotNumber, slotTo.slotNumber)
}

/**
 * Move the item in [slotFrom]  to [slotTo] in player inventory,
 * if [slotTo] contains an item, then move it to [slotFrom]
 */
fun moveToSlot(slotFrom: Int, slotTo: Int): ShortArray {
    return moveToSlot(0, slotFrom, slotTo)
}


/**
 * Move the item in [slotFrom] to [slotTo] in [windowId],
 * if [slotTo] contains an item, then move it to [slotFrom]
 */
fun moveToSlot(windowId: Int, slotFrom: Int, slotTo: Int): ShortArray {
    return shortArrayOf(
        clickSlot(windowId, slotFrom, type = ClickType.PICKUP),
        clickSlot(windowId, slotTo, type = ClickType.PICKUP),
        clickSlot(windowId, slotFrom, type = ClickType.PICKUP)
    )
}

/**
 * Move all the item that equals to the item in [slotTo] to [slotTo] in player inventory
 * Note: Not working
 */
fun moveAllToSlot(slotTo: Int): ShortArray {
    return shortArrayOf(
        clickSlot(slot = slotTo, type = ClickType.PICKUP_ALL),
        clickSlot(slot = slotTo, type = ClickType.PICKUP)
    )
}

/**
 * Quick move (Shift + Click) the item in [slot] in player inventory
 */
fun quickMoveSlot(slot: Int): Short {
    return quickMoveSlot(0, slot)
}

/**
 * Quick move (Shift + Click) the item in [slot] in specified [windowId]
 */
fun quickMoveSlot(windowId: Int, slot: Int): Short {
    return clickSlot(windowId, slot, type = ClickType.QUICK_MOVE)
}

/**
 * Quick move (Shift + Click) the item in [slot] in player inventory
 */
fun quickMoveSlot(slot: Slot): Short {
    return quickMoveSlot(0, slot)
}

/**
 * Quick move (Shift + Click) the item in [slot] in specified [windowId]
 */
fun quickMoveSlot(windowId: Int, slot: Slot): Short {
    return clickSlot(windowId, slot, type = ClickType.QUICK_MOVE)
}


/**
 * Throw all the item in [slot] in player inventory
 */
fun throwAllInSlot(slot: Int): Short {
    return throwAllInSlot(0, slot)
}

/**
 * Throw all the item in [slot] in specified [windowId]
 */
fun throwAllInSlot(windowId: Int, slot: Int): Short {
    return clickSlot(windowId, slot, 1, ClickType.THROW)
}

/**
 * Throw all the item in [slot] in player inventory
 */
fun throwAllInSlot(slot: Slot): Short {
    return throwAllInSlot(0, slot)
}

/**
 * Throw all the item in [slot] in specified [windowId]
 */
fun throwAllInSlot(windowId: Int, slot: Slot): Short {
    return clickSlot(windowId, slot, 1, ClickType.THROW)
}

/**
 * Put the item currently holding by mouse to somewhere or throw it
 */
fun removeHoldingItem() {
    if (Minecraft.getMinecraft().player.inventory.itemStack.isEmpty) return

    val slot = Minecraft.getMinecraft().player.inventoryContainer.getSlots(9..45)
        .firstItem(Items.AIR)?.slotNumber // Get empty slots in inventory and offhand
        ?: Minecraft.getMinecraft().player.craftingSlots.firstItem(Items.AIR)?.slotNumber // Get empty slots in crafting slot
        ?: -999 // Throw on the ground

    clickSlot(slot = slot, type = ClickType.PICKUP)
}


/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id
 */
fun clickSlot(windowId: Int = 0, slot: Slot, mouseButton: Int = 0, type: ClickType): Short {
    return clickSlot(windowId, slot.slotNumber, mouseButton, type)
}

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id
 */
fun clickSlot(windowId: Int = 0, slot: Int, mouseButton: Int = 0, type: ClickType): Short {
    val container =
        if (windowId == 0) Minecraft.getMinecraft().player.inventoryContainer else Minecraft.getMinecraft().player.openContainer
    container ?: return -32768

    val playerInventory = Minecraft.getMinecraft().player.inventory ?: return -32768
    val transactionID = container.getNextTransactionID(playerInventory)
    val itemStack = container.slotClick(slot, mouseButton, type, Minecraft.getMinecraft().player)

    Minecraft.getMinecraft().connection?.sendPacket(
        CPacketClickWindow(
            windowId,
            slot,
            mouseButton,
            type,
            itemStack,
            transactionID
        )
    )
    Minecraft.getMinecraft().playerController.updateController()


    return transactionID
}