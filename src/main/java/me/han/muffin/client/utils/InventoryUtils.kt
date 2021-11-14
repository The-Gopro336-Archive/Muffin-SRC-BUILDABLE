package me.han.muffin.client.utils

import me.han.muffin.client.core.Globals
import me.han.muffin.client.module.modules.combat.AutoArmourModule
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.mixin.entity.syncCurrentPlayItem
import net.minecraft.block.Block
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Items
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.*

object InventoryUtils {

    enum class InventoryHotbar {
        Hotbar, Inventory, Crafting, All
    }

    private fun getSlotById(id: Int): Int {
        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack == ItemStack.EMPTY) continue
            if (Item.getIdFromItem(stack.item) == id) return i
        }
        return -1
    }

    @JvmStatic
    fun findBlock(block: Block): Int {
        return findItem(Item.getItemFromBlock(block)) //return findItem(new ItemStack(block).getItem());
    }

    fun findItem(item: Item): Int {
        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty || item != stack.item) continue
            return i
        }
        return -1
    }

    fun findItemWithStack(item: Item): Pair<ItemStack, Int> {
        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty || stack.item != item) continue
            return Pair(stack, i)
        }
        return Pair(ItemStack.EMPTY, -1)
    }

    fun findGenericBlock(): Int {
        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty || stack.item !is ItemBlock) continue
            return i
        }
        return -1
    }

    fun findGenericBlockStack(): Pair<ItemStack, Int> {
        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty || stack.item !is ItemBlock) continue
            return stack to i
        }
        return ItemStack.EMPTY to -1
    }

    fun findItemFullInventory(item: Item): Int {
        for (i in Globals.mc.player.inventoryContainer.inventory.indices) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty || item != stack.item) continue
            return i
        }
        return -1
    }

    fun getFreeHotbarSlot(): Int {
        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack == ItemStack.EMPTY || stack.item == Items.AIR) return i
        }
        return -1
    }

    @JvmStatic
    fun getShulkerSlotInHotbar(): Pair<Int, ItemStack> {
        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.getStackInSlot(i)
            if (stack == ItemStack.EMPTY || stack.item !is ItemShulkerBox) continue
            return Pair(i, stack)
        }
        return Pair(-1, ItemStack.EMPTY)
    }

    @JvmStatic
    fun getItemsCount(item: Item): Int {
        var items = 0
        for (i in Globals.mc.player.inventoryContainer.inventory.indices) {
            val stack = Globals.mc.player.inventoryContainer.inventory[i]
            if (stack.isEmpty || stack.item != item) continue
            items += stack.count
        }
        return items
    }

    fun getItemsStackCount(item: Item): Int {
        var items = 0
        for (i in Globals.mc.player.inventoryContainer.inventory.indices) {
            val stack = Globals.mc.player.inventoryContainer.inventory[i]
            if (stack.isEmpty || stack.item != item) continue
            ++items
        }
        return items
    }

    fun getBlocksCount(block: Block): Int {
        var blocks = 0
        for (i in Globals.mc.player.inventoryContainer.inventory.indices) {
            val stack = Globals.mc.player.inventoryContainer.inventory[i]
            if (stack.isEmpty || stack.item != Item.getItemFromBlock(block)) continue
            blocks += stack.count
        }
        return blocks
    }

    fun getBlocksStackCount(block: Block): Int {
        var blocks = 0
        for (i in Globals.mc.player.inventoryContainer.inventory.indices) {
            val stack = Globals.mc.player.inventoryContainer.inventory[i]
            if (stack.isEmpty || stack.item != Item.getItemFromBlock(block)) continue
            ++blocks
        }
        return blocks
    }

    fun getEmptyXCarry(): Int {
        for (i in 1 until 5) {
            val stack = Globals.mc.player.inventoryContainer.inventorySlots[i].stack
            if (!stack.isEmpty || stack.item != Items.AIR) continue
            return i
        }
        return -1
    }

    fun isSlotEmpty(i: Int): Boolean {
        return Globals.mc.player.inventoryContainer.inventorySlots[i].stack.isEmpty
    }

    fun findEmptySlots(inventoryHotbar: InventoryHotbar, withXCarry: Boolean): List<Int> {
        val outPut = ArrayList<Int>()
        for ((key, value) in getSlots(inventoryHotbar)) {
            if (!value.isEmpty && value.item != Items.AIR) continue
            outPut.add(key)
        }
        if (withXCarry) {
            for (i in 1..4) {
                val craftingSlot = Globals.mc.player.inventoryContainer.inventorySlots[i]
                val craftingStack = craftingSlot.stack
                if (!craftingStack.isEmpty && craftingStack.item != Items.AIR) continue
                outPut.add(i)
            }
        }
        if (outPut.isEmpty()) {
            outPut.add(-999)
        }
        return outPut
    }

    /**
     * Returns slots contains item with given item id in player inventory
     *
     * @return Array contains slot index, null if no item found
     */
    fun getSlots(min: Int, max: Int, itemID: Int): ArrayList<Int>? {
        val slots = ArrayList<Int>()
        for (i in min until max) {
            if (Item.getIdFromItem(Globals.mc.player.inventory.getStackInSlot(i).item) == itemID) {
                slots.add(i)
            }
        }
        return if (slots.isNotEmpty()) slots else null
    }

    /**
     * Returns slots in full inventory contains item with given [itemId] in player inventory
     * This is same as [getSlots] but it returns full inventory slot index
     *
     * @return Array contains full inventory slot index, null if no item found
     */
    fun getSlotsFullInv(min: Int, max: Int, itemId: Int): ArrayList<Int>? {
        val slots = ArrayList<Int>()
        for (i in min until max) {
            if (Item.getIdFromItem(Globals.mc.player.inventoryContainer.inventory[i].item) == itemId) {
                slots.add(i)
            }
        }
        return if (slots.isNotEmpty()) slots else null
    }

    /**
     * Returns slots contains with given item id in player inventory (without hotbar)
     *
     * @return Array contains slot index, null if no item found
     */
    fun getSlotsNoHotbar(itemId: Int): ArrayList<Int>? {
        return getSlots(9, 35, itemId)
    }

    /**
     * Returns slots contains item with given item id in player hotbar
     *
     * @return Array contains slot index, null if no item found
     */
    fun getSlotsHotbar(itemId: Int): ArrayList<Int>? {
        return getSlots(0, 8, itemId)
    }

    /**
     * Returns slots contains with given [itemId] in player inventory (without hotbar)
     * This is same as [getSlots] but it returns full inventory slot index
     *
     * @return Array contains slot index, null if no item found
     */
    fun getSlotsFullInvNoHotbar(itemId: Int): ArrayList<Int>? {
        return getSlotsFullInv(9, 35, itemId)
    }

    /**
     * Try to swap current held item to item with given [itemID]
     */
    fun swapSlotToItem(itemID: Int) {
        if (getSlotsHotbar(itemID) != null) {
            swapSlot(getSlotsHotbar(itemID)!![0])
        }
        Globals.mc.playerController.syncCurrentPlayItem()
    }

    /**
     * Swap current held item to given [slot]
     */
    @JvmStatic
    fun swapSlot(slot: Int) {
        Globals.mc.player.inventory.currentItem = slot
        Globals.mc.playerController.syncCurrentPlayItem()
    }

    @JvmStatic
    fun findSwordAndAxe(): Int {
        var damage = -1.0f
        var slot = -1
        var item: Item? = null

        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.mainInventory[i]
            if (stack.item is ItemAir) continue
            val itemMaxDamage = stack.maxDamage.toFloat()
            if (stack.item is ItemSword && itemMaxDamage > damage) {
                slot = i
                damage = itemMaxDamage
                item = stack.item
            }
        }

        if (item != null) return slot

        for (i in 0 until 9) {
            val stack = Globals.mc.player.inventory.mainInventory[i]
            if (stack.item is ItemAir) continue
            val itemMaxDamage = stack.maxDamage.toFloat()
            if (stack.item is ItemAxe && itemMaxDamage > damage) {
                slot = i
                damage = itemMaxDamage
            }
        }
        return slot
    }

    fun findArmourSlot(type: EntityEquipmentSlot, bestArmour: Boolean, curse: Boolean, preferElytra: Boolean, xCarry: Boolean, minDurability: Int): Int {
        var slot = -1
        var damage = if (bestArmour) 0F else 10000F

        for (i in Globals.mc.player.inventoryContainer.inventory.indices) {
            // @see: https://wiki.vg/Inventory, 0 is crafting slot, and 5,6,7,8 are Armor slots

            if (i == 0 || i == 5 || i == 6 || i == 7 || i == 8) continue
            if (!xCarry) if (i == 1 || i == 2 || i == 3 || i == 4) continue

            val stack = Globals.mc.player.inventoryContainer.getSlot(i).stack
            if (stack.isEmpty || stack.item == Items.AIR) continue

            if (stack.item is ItemArmor) {
                val armor = stack.item as ItemArmor
                if (armor.armorType != type) continue

                val currentDamage = AutoArmourModule.getArmorValue(stack)
                val cursed = curse && EnchantmentHelper.hasBindingCurse(stack)

                if (EntityUtil.getArmorPct(stack) < minDurability) continue

                if (bestArmour) {
                    if (currentDamage > damage && !cursed) {
                        damage = currentDamage
                        slot = i
                    }
                } else {
                    if (currentDamage < damage && !cursed) {
                        damage = currentDamage
                        slot = i
                    }
                }

            } else if (type == EntityEquipmentSlot.CHEST && preferElytra && stack.item is ItemElytra && EntityUtil.getArmorPct(stack) > 3) {
                return i
            }

        }
        return slot
    }

    fun getSlots(inv: InventoryHotbar): Map<Int, ItemStack> {
        return when (inv) {
            InventoryHotbar.Hotbar -> findItemInInventory(36, 44)
            InventoryHotbar.Inventory -> findItemInInventory(9, 35)
            InventoryHotbar.Crafting -> findItemInInventory(1, 4)
            InventoryHotbar.All -> findItemInInventory(9, 44)
        }
    }

    private fun findItemInInventory(current: Int, last: Int): Map<Int, ItemStack> {
        val fullInventorySlots = HashMap<Int, ItemStack>()
        for (i in current..last) fullInventorySlots[i] = Globals.mc.player.inventoryContainer.inventory[i]
        return fullInventorySlots
    }

}